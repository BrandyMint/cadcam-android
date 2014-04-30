package ru.brandymint.cadcam.app.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import ru.brandymint.cadcam.app.BuildConfig;

/**
 * Created by alexey on 29.04.14.
 */
public class Database extends SQLiteOpenHelper {

    private static final boolean DBG = BuildConfig.DEBUG && true;
    private static final String TAG = Database.class.getSimpleName();

    private static final String DATABASE_NAME = "cadcam.db";
    private static final int DATABASE_VERSION = 2;

    interface Tables {
        String PHOTOS = "photos";
    }

    public Database(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        if (DBG) Log.d(TAG, "onCreate()");
        db.execSQL("CREATE TABLE " + Tables.PHOTOS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + DatabaseContract.Photos.SYNC_STATUS + " INTEGER NOT NULL DEFAULT 0,"
                + DatabaseContract.Photos.CREATED_AT + " INTEGER NOT NULL DEFAULT 0,"
                + DatabaseContract.Photos.FILE_NAME + " VARCHAR(300) UNIQUE NOT NULL,"
                + DatabaseContract.Photos.LATITIUDE + " REAL NOT NULL,"
                + DatabaseContract.Photos.LONGITUDE + " REAL NOT NULL,"
                + DatabaseContract.Photos.COMMENT + " TEXT NOT NULL DEFAULT '',"
                + DatabaseContract.Photos.URL + " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (DBG) Log.d(TAG, "onUpgrade from " + oldVersion + " to " + newVersion);
        db.execSQL("DROP TABLE IF EXISTS " + Tables.PHOTOS);
        onCreate(db);
    }
}
