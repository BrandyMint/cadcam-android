package ru.brandymint.cadcam.app;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.MultipartContent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;

import ru.brandymint.cadcam.app.provider.DatabaseContract;

public class SyncService extends Service {
    private static final String TAG = "SyncService";
    private static final boolean DBG = BuildConfig.DEBUG & true;

    public static final String ACTION_ALL = "ru.brandymint.cadcam.app.ACTION_ALL";
    public static final String ACTION_UPLOAD = "ru.brandymint.cadcam.app.ACTION_UPLOAD";
    public static final String ACTION_CLEANUP = "ru.brandymint.cadcam.app.ACTION_CLEANUP";

    public static final String ARG_UPLOAD_IMAGE_URI = "ru.brandymint.cadcam.app.ARG_UPLOAD_IMAGE_URI";

    private static final String POST_KEY_LATITUDE = "latitude";
    private static final String POST_KEY_LONGITUDE = "longitude";
    private static final String POST_KEY_DEVICE_ID = "device_id";
    private static final String POST_KEY_COMMENT = "comment";
    private static final String POST_KEY_IMAGE = "image";

    private static final int ACTION_DO_ALL = 0;
    private static final int ACTION_DO_UPLOAD = 1;
    private static final int ACTION_DO_CLEANUP = 2;

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    public SyncService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        switch (intent.getAction()) {
            case ACTION_UPLOAD:
                msg.what = ACTION_DO_UPLOAD;
                msg.obj = intent.getParcelableExtra(ARG_UPLOAD_IMAGE_URI);
                mServiceHandler.removeMessages(ACTION_DO_UPLOAD);
                break;
            case ACTION_CLEANUP:
                msg.what = ACTION_DO_CLEANUP;
                break;
            case ACTION_ALL:
                msg.what = ACTION_DO_ALL;
                break;
            default:
                throw new IllegalArgumentException();
        }
        mServiceHandler.sendMessage(msg);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mServiceLooper.quit();
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            int startId = msg.arg1;
            switch (msg.what) {
                case ACTION_DO_ALL:
                    uploadPictures(null);
                    cleanupPictures();
                    break;
                case ACTION_DO_UPLOAD:
                    uploadPictures((Uri)msg.obj);
                    break;
                case ACTION_DO_CLEANUP:
                    break;
            }
            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1);
        }
    }

    private void uploadPictures(Uri uri) {
        ContentResolver cr = getContentResolver();
        if (uri == null) uri = DatabaseContract.Photos.CONTENT_URI;

        Cursor c = cr.query(uri, null,
                DatabaseContract.Photos.SYNC_STATUS + " != ?",
                new String[]{String.valueOf(DatabaseContract.SYNC_STATUS_UPLOADED)},
                null);

        if (c == null) return;

        PhotoColumns idxes = new PhotoColumns(c);

        try {
            while (c.moveToNext()) {
                uploadPicture(c, idxes);
            }
        } finally {
            c.close();
        }
    }

    private void uploadPicture(Cursor cursor, @Nullable PhotoColumns indexes) {
        long id;
        int status;
        boolean deleteRecord = false;
        boolean success = false;

        if (indexes == null) indexes = new PhotoColumns(cursor);
        ContentResolver cr = getContentResolver();

        id = cursor.getLong(indexes.colIdxId);
        status = DatabaseContract.SYNC_STATUS_UPLOADING;

        try {
            updateStatus(id, DatabaseContract.SYNC_STATUS_UPLOADING);
            String filename = cursor.getString(indexes.colIdxFilename);
            double lon = cursor.getDouble(indexes.colIdxLongitude);
            double lat = cursor.getDouble(indexes.colIdxLatitude);
            String comment = cursor.getString(indexes.colComment);
            PostImageResponse response = uploadPicture(filename, lat, lon, comment);
            status = DatabaseContract.SYNC_STATUS_UPLOADED;

            ContentValues cv = new ContentValues(2);
            cv.put(DatabaseContract.Photos.SYNC_STATUS, status);
            cv.put(DatabaseContract.Photos.URL, response.link);
            cr.update(ContentUris.withAppendedId(DatabaseContract.Photos.CONTENT_URI, id), cv, null, null);
            success = true;
        } catch (FileNotFoundException nfe) {
            deleteRecord = true;
        } catch (Throwable e) {
            Log.e(TAG, "upload error", e);
            status = DatabaseContract.SYNC_STATUS_NEW;
        } finally {
            if (!success) {
                if (deleteRecord) {
                    cr.delete(ContentUris.withAppendedId(DatabaseContract.Photos.CONTENT_URI, id), null, null);
                } else {
                    updateStatus(id, status);
                }
            }
        }
    }

    private PostImageResponse uploadPicture(String fileName, double lat, double lon, String comment) throws IOException {
        HttpRequest request;
        MultipartFormDataContent post;

        File file = new File(Utils.getPicturesDirectory(), fileName);
        if (!file.exists()) throw new FileNotFoundException("File " + file + " not found");

        post = new MultipartFormDataContent();
        post.addPart(new MultipartContent.Part(new ByteArrayContent(null, String.valueOf(lat).getBytes())), POST_KEY_LATITUDE, null);
        post.addPart(new MultipartContent.Part(new ByteArrayContent(null, String.valueOf(lon).getBytes())), POST_KEY_LONGITUDE, null);
        post.addPart(new MultipartContent.Part(new ByteArrayContent(null, NetworkUtils.getInstance().getUserId(this).getBytes())), POST_KEY_DEVICE_ID, null);
        post.addPart(new MultipartContent.Part(new ByteArrayContent(null, comment.getBytes())), POST_KEY_COMMENT, null);

        FileContent fileContent = new FileContent("application/octet-stream", file);
        post.addPart(new MultipartContent.Part(fileContent), POST_KEY_IMAGE, file.getName());
        request = NetworkUtils.getInstance().createRequestFactory(this).buildPostRequest(new GenericUrl(Constants.ENDPOINT_CADCAM), post);

        request.getHeaders().set(Constants.HEADER_GEO_POSITION, formatGeoPositionHeader(lat, lon));
        return request.execute().parseAs(PostImageResponse.class);
    }

    private void updateStatus(long id, int newStatus) {
        ContentResolver cr = getContentResolver();
        ContentValues cv = new ContentValues(1);
        cv.put(DatabaseContract.Photos.SYNC_STATUS, newStatus);
        cr.update(ContentUris.withAppendedId(DatabaseContract.Photos.CONTENT_URI, id), cv, null, null);
    }

    private String formatGeoPositionHeader(double lat, double lon) {
        return String.format(Locale.US, "%.6f;%.6f", lat, lon);
    }

    private void cleanupPictures() {

    }

    public static class PhotoColumns {
        final int colIdxId, colIdxFilename, colIdxSyncStatus, colIdxLatitude, colIdxLongitude, colComment;

        public PhotoColumns(Cursor c) {
            colIdxId = c.getColumnIndex(DatabaseContract.Photos._ID);
            colIdxFilename = c.getColumnIndex(DatabaseContract.Photos.FILE_NAME);
            colIdxSyncStatus = c.getColumnIndex(DatabaseContract.Photos.SYNC_STATUS);
            colIdxLatitude = c.getColumnIndex(DatabaseContract.Photos.LATITIUDE);
            colIdxLongitude = c.getColumnIndex(DatabaseContract.Photos.LONGITUDE);
            colComment = c.getColumnIndex(DatabaseContract.Photos.COMMENT);
        }

    }

}
