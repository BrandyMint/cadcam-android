package ru.brandymint.cadcam.app.provider;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.v4.database.DatabaseUtilsCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import ru.brandymint.cadcam.app.BuildConfig;

public class Provider extends ContentProvider {

    private static final boolean DBG = BuildConfig.DEBUG && true;
    private static final String TAG = Provider.class.getSimpleName();

    private static final int PHOTOS = 0;

    private static final int PHOTO_ID = 1;

    private Database mDatabaseHelper;

    private static final UriMatcher sURIMatcher = buildUriMatcher();

    public Provider() {
    }

    private static UriMatcher buildUriMatcher() {
        UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

        matcher.addURI(DatabaseContract.CONTENT_AUTHORITY, "photo", PHOTOS);
        matcher.addURI(DatabaseContract.CONTENT_AUTHORITY, "photo/*", PHOTO_ID);

        return matcher;
    }


    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        Cursor cursor;
        String limit;
        final List<String> limits;
        String id;

        if (DBG) Log.v(TAG, "uri: " + uri);
        cursor = null;
        limits = uri.getQueryParameters("limit");
        limit = limits.isEmpty() ? null : limits.get(limits.size() - 1);

        switch (sURIMatcher.match(uri)) {
            case PHOTOS:
                cursor = getPhotos(uri, projection, selection, selectionArgs, sortOrder, limit);
                break;
            case PHOTO_ID:
                id = uri.getLastPathSegment();
                if (id == null) {
                    throw new IllegalArgumentException("Startup ID not defined");
                }
                selection = DatabaseUtilsCompat.concatenateWhere(selection,
                        DatabaseContract.Photos._ID + "=?");
                selectionArgs = DatabaseUtilsCompat.appendSelectionArgs(selectionArgs,
                        new String[]{id});
                cursor = getPhotos(uri, projection, selection, selectionArgs, sortOrder, "1");
                break;
            default:
                throw new IllegalArgumentException("Unknown uri" + uri);
        }

        if (cursor != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }

        return cursor;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db;
        final int rowsAffected;
        String id;

        switch (sURIMatcher.match(uri)) {
            case PHOTOS:
                db = mDatabaseHelper.getWritableDatabase();
                rowsAffected = db.delete(Database.Tables.PHOTOS, selection, selectionArgs);
                if (rowsAffected > 0 || (selection == null)) {
                    getContext().getContentResolver().notifyChange(uri, null, false);
                }
                break;
            case PHOTO_ID:
                id = uri.getLastPathSegment();
                db = mDatabaseHelper.getWritableDatabase();
                if (id == null) {
                    throw new IllegalArgumentException("Startup ID not defined");
                }
                selection = DatabaseUtilsCompat.concatenateWhere(selection,
                        DatabaseContract.Photos._ID + "=?");
                selectionArgs = DatabaseUtilsCompat.appendSelectionArgs(selectionArgs,
                        new String[]{id});
                rowsAffected = db.delete(Database.Tables.PHOTOS, selection, selectionArgs);
                if (rowsAffected > 0 || (selection == null)) {
                    getContext().getContentResolver().notifyChange(DatabaseContract.Photos.CONTENT_URI, null, false);
                }
                break;
            default:
                throw new IllegalArgumentException("unknown uri " + uri);
        }
        return rowsAffected;
    }

    @Override
    public String getType(Uri uri) {
        switch (sURIMatcher.match(uri)) {
            case PHOTOS:
                return DatabaseContract.Photos.CONTENT_TYPE;
            case PHOTO_ID:
                return DatabaseContract.Photos.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown uri" + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db;
        final long row_id;
        final String uuid;

        db = mDatabaseHelper.getWritableDatabase();
        switch (sURIMatcher.match(uri)) {
            case PHOTOS:
                row_id = db.insertOrThrow(Database.Tables.PHOTOS, null, values);
                if (row_id >= 0) {
                    final Uri newUri = Uri.withAppendedPath(DatabaseContract.Photos.CONTENT_URI, String.valueOf(row_id));
                    getContext().getContentResolver().notifyChange(newUri, null, false);
                    return newUri;
                }
            default:
                throw new IllegalArgumentException("Unknown uri" + uri);
        }
    }

    @Override
    public boolean onCreate() {
        mDatabaseHelper = new Database(getContext());
        return true;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        final SQLiteDatabase db;
        String where;
        int rowsAffected = 0;
        String whereArgs[];

        switch (sURIMatcher.match(uri)) {
            case PHOTO_ID:
                db = mDatabaseHelper.getWritableDatabase();
                String id = uri.getLastPathSegment();
                where = DatabaseUtilsCompat.concatenateWhere(selection, DatabaseContract.Photos._ID + "=?");
                whereArgs = DatabaseUtilsCompat.appendSelectionArgs(selectionArgs, new String[]{id});

                rowsAffected = db.update(Database.Tables.PHOTOS, values, where, whereArgs);
                if (rowsAffected > 0) {
                    getContext().getContentResolver().notifyChange(uri, null, false);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown uri" + uri);
        }
        return rowsAffected;
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            final int numOperations = operations.size();
            final ContentProviderResult[] results = new ContentProviderResult[numOperations];
            for (int i = 0; i < numOperations; i++) {
                results[i] = operations.get(i).apply(this, results, i);
            }
            db.setTransactionSuccessful();
            return results;
        } finally {
            db.endTransaction();
        }
    }


    private Cursor getPhotos(Uri uri, String[] projection, String selection, String[] selectionArgs,
                             String sortOrder, String limit) {
        final SQLiteQueryBuilder builder;

        builder = new SQLiteQueryBuilder();
        builder.setTables(Database.Tables.PHOTOS);
        final SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        return builder.query(db, projection, selection, selectionArgs, null, null,
                sortOrder == null ? DatabaseContract.Photos.DEFAULT_SORT : sortOrder,
                limit);
    }

}
