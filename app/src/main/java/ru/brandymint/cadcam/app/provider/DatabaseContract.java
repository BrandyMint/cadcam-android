package ru.brandymint.cadcam.app.provider;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by alexey on 28.04.14.
 */
public class DatabaseContract {

    public static final int SYNC_STATUS_NEW = 0;

    public static final int SYNC_STATUS_UPLOADING = 1;

    public static final int SYNC_STATUS_UPLOADED = 2;

    public interface PhotoColumns {
        String URL = "url";

        String FILE_NAME = "file_name";

        String SYNC_STATUS = "sync_status";

        String CREATED_AT = "created_at";

        String LATITIUDE = "latitude";

        String LONGITUDE = "longitude";

        String COMMENT = "comment";
    }

    public static final String CONTENT_AUTHORITY = "ru.brandymint.cadcam.app.provider";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_PHOTO = "photo";

    public static class Photos implements PhotoColumns, BaseColumns {

        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_PHOTO);

        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
                + "/vnd.rubrandymintcadcamappprovider.photos";

        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
                + "/vnd.rubrandymintcadcamappprovider.photos";

        public static final String DEFAULT_SORT = PhotoColumns.CREATED_AT + " DESC";
    }

}
