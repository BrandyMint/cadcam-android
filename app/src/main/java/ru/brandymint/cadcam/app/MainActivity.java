package ru.brandymint.cadcam.app;

import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.api.client.http.HttpTransport;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.brandymint.cadcam.app.provider.DatabaseContract;

import static ru.brandymint.cadcam.app.EnterCommentDialogFragment.EnterCommentDialogListener;

public class MainActivity extends ActionBarActivity implements PhotoFragment.OnFragmentInteractionListener, EnterCommentDialogListener {
    private static final boolean DBG = BuildConfig.DEBUG && true;
    private static final String TAG = "MainActivity";

    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;

    private static final String KEY_CURRENT_IMAGE_TIMESTAMP = "KEY_CURRENT_IMAGE_TIMESTAMP";
    private static final String KEY_CURRENT_IMAGE_LOCATION = "KEY_CURRENT_IMAGE_LOCATION";
    private static final String KEY_LAST_LOCATION = "KEY_LAST_LOCATION";
    private static final int SELECT_IMAGE = 1;
    private Date mCurrentImageTimestamp;
    private Location mCurrentImageLocation;

    private Location mLastLocation = null;
    private Uri selectedImageUri;


    void setSelectedImageUri( Uri mSelectedImageUri) {
        selectedImageUri = mSelectedImageUri;
    }

    Uri getSelectedImageUri() {
        return selectedImageUri ;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {

            if (DBG) Logger.getLogger(HttpTransport.class.getName()).setLevel(Level.CONFIG);

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, PhotoFragment.newInstance())
                    .commit();
            startSyncService();
            Intent i = getIntent();
            if (Intent.ACTION_MAIN.equals(i == null ? null : i.getAction())) {
                takePhoto();
            }
        } else {
            if (savedInstanceState.containsKey(KEY_CURRENT_IMAGE_TIMESTAMP)) {
                mCurrentImageTimestamp = new Date(savedInstanceState.getLong(KEY_CURRENT_IMAGE_TIMESTAMP));
            } else {
                mCurrentImageTimestamp = null;
            }

            mCurrentImageLocation = savedInstanceState.getParcelable(KEY_CURRENT_IMAGE_LOCATION);
            mLastLocation = savedInstanceState.getParcelable(KEY_LAST_LOCATION);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_take_photo:
                takePhoto();
                return true;
            case R.id.action_select_photo:
                takeImage();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                File fileUri = Utils.getOutputMediaFile(mCurrentImageTimestamp);
                setSelectedImageUri(null);
                /*
                try {
                    // XXX: http://stackoverflow.com/a/17930305/2971719
                    sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                            Uri.fromFile(fileUri)));
                } catch (Throwable ignore) {
                    Log.i(TAG, "", ignore);
                }
                */

                mCurrentImageLocation = getLocation();
                EnterCommentDialogFragment fragment = new EnterCommentDialogFragment();
                fragment.show(getSupportFragmentManager(), "enterComment");
            } else if (resultCode == RESULT_CANCELED) {
                // User cancelled the image capture
                mCurrentImageTimestamp = null;
            } else {
                // Image capture failed, advise user
                mCurrentImageTimestamp = null;
            }

        }

        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_IMAGE) {
                setSelectedImageUri(data.getData());

                EnterCommentDialogFragment fragment = new EnterCommentDialogFragment();
                fragment.show(getSupportFragmentManager(), "enterComment");
            }
        }
    }
    public String getPath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        if(cursor!=null)
        {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        else return null;
    }


    @Override
    protected void onStart() {
        super.onStart();
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (lm != null) {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10 * 1000, 10, mLocationListener);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (lm != null) {
            lm.removeUpdates(mLocationListener);
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mCurrentImageTimestamp != null)
            outState.putLong(KEY_CURRENT_IMAGE_TIMESTAMP, mCurrentImageTimestamp.getTime());
        if (mLastLocation != null) outState.putParcelable(KEY_LAST_LOCATION, mLastLocation);
        if (mCurrentImageLocation != null) outState.putParcelable(KEY_CURRENT_IMAGE_LOCATION, mCurrentImageLocation);
    }

    private void takePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        mCurrentImageTimestamp = new Date();
        mCurrentImageLocation = null;
        Uri fileUri = Utils.getOutputMediaFileNameUri(mCurrentImageTimestamp);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
    }

    private void takeImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        mCurrentImageTimestamp = new Date();
        selectedImageUri = null;
        Uri fileUri = Utils.getOutputMediaFileNameUri(mCurrentImageTimestamp);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"),SELECT_IMAGE);
    }

    private Uri savePhotoMetadata(File filename, String comment, Location location) {
        ContentValues cv = new ContentValues();
        cv.put(DatabaseContract.Photos.CREATED_AT, mCurrentImageTimestamp.getTime());
        cv.put(DatabaseContract.Photos.FILE_NAME, filename.getName());
        cv.put(DatabaseContract.Photos.COMMENT, comment);

        cv.put(DatabaseContract.Photos.LATITIUDE, location.getLatitude());
        cv.put(DatabaseContract.Photos.LONGITUDE, location.getLongitude());

        return getContentResolver().insert(DatabaseContract.Photos.CONTENT_URI, cv);
    }

    @Override
    public void onPhotoSelected(long id, String url) {
        if (TextUtils.isEmpty(url)) {
            Intent i = new Intent(this, SyncService.class);
            i.setAction(SyncService.ACTION_UPLOAD);
            i.putExtra(SyncService.ARG_UPLOAD_IMAGE_URI, Uri.withAppendedPath(DatabaseContract.Photos.CONTENT_URI, String.valueOf(id)));
            startService(i);
        } else {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            try {
                startActivity(i);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "No application can handle this request,"
                        + " Please install a webbrowser", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }
    }

    private void startSyncService() {
        Intent i = new Intent(this, SyncService.class);
        i.setAction(SyncService.ACTION_ALL);
        startService(i);
    }

    private void startUploadImage(Uri uri) {
        Intent i = new Intent(this, SyncService.class);
        i.setAction(SyncService.ACTION_UPLOAD);
        i.putExtra(SyncService.ARG_UPLOAD_IMAGE_URI, uri);
        startService(i);
    }

    @Override
    public void onDialogClosed(String comment) {
        Location l = mCurrentImageLocation;
        Uri imageUri;
        if (l == null) l = getLocation();

        if (l == null) {
            Toast.makeText(this, R.string.error_location_not_found, Toast.LENGTH_LONG).show();
            Utils.getOutputMediaFile(mCurrentImageTimestamp).delete();
        } else {

            Uri selectImageUri = getSelectedImageUri();
            if (selectImageUri == null) {
                imageUri = savePhotoMetadata(Utils.getOutputMediaFile(mCurrentImageTimestamp), comment, l);
            } else{
                String ImageUriString =  getPath(selectImageUri);
                File ImageUriFileBefore =  new File(ImageUriString);
                String destinationPath = Utils.getOutputMediaFile(mCurrentImageTimestamp).getPath();
                File destination = new File(destinationPath);
                try
                {
                    FileUtils.copyFile(ImageUriFileBefore, destination);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
                imageUri = savePhotoMetadata(destination, comment, l);
            }
            startUploadImage(imageUri);
        }
    }

    @Nullable
    private Location getLocation() {
        Location l = mLastLocation;

        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) return l;

        for (String provider : lm.getAllProviders()) {
            Location newL = lm.getLastKnownLocation(provider);
            l = Utils.isBetterLocation(newL, l) ? newL : l;
        }

        return l;
    }

    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (location != null) mLastLocation = location;
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

}
