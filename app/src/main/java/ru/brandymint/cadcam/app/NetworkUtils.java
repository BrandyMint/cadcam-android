package ru.brandymint.cadcam.app;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;

import java.io.IOException;
import java.util.UUID;

public class NetworkUtils {
    public static final boolean DBG = BuildConfig.DEBUG && true;
    public static final String TAG = NetworkUtils.class.getSimpleName();

    private static final String USER_ID_SHARED_PREFS_NAME="user_id";
    private static final String PREFS_KEY_USER_ID = "user_id";

    private static volatile NetworkUtils mUtils;

    private final HttpTransport mTransport;

    private final JsonFactory mJsonFactory;

    private String mDeviceId;

    private NetworkUtils() {
        mTransport = new NetHttpTransport();
        mJsonFactory = new AndroidJsonFactory();
    }

    public static synchronized NetworkUtils getInstance() {
        if (mUtils == null) {
            mUtils = new NetworkUtils();
        }
        return mUtils;
    }

    public HttpTransport getTransport() {
        return mTransport;
    }

    public JsonFactory getJsonFactory() {
        return mJsonFactory;
    }

    public HttpRequestFactory createRequestFactory(final Context context) {
        return mTransport.createRequestFactory(new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest request) throws IOException {
                request.setConnectTimeout(Constants.HTTP_REQUEST_TIMEOUT_MS);
                request.setReadTimeout(Constants.HTTP_READ_TIMEOUT_MS);
                request.setNumberOfRetries(2);
                request.setParser(mJsonFactory.createJsonObjectParser());
                HttpHeaders hs = request.getHeaders();
                hs.set(Constants.HEADER_X_DEVICE_ID, getUserId(context));
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    hs.set("Connection", "close");
                }
            }
        });
    }

    public String getUserId(Context context) {
        if (mDeviceId != null) return mDeviceId;

        String deviceId = context.getSharedPreferences(USER_ID_SHARED_PREFS_NAME, 0).getString(PREFS_KEY_USER_ID, null);
        if (deviceId == null) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
                deviceId = Settings.Secure.getString(context.getContentResolver(),
                        Settings.Secure.ANDROID_ID);
            }
            if (TextUtils.isEmpty(deviceId)
                    || "9774d56d682e549c".equalsIgnoreCase(deviceId)
                    || "0123456789ABCDEF".equalsIgnoreCase(deviceId)
                    ) {
                deviceId = UUID.randomUUID().toString();
            }
            context.getSharedPreferences(USER_ID_SHARED_PREFS_NAME, 0).edit().putString(PREFS_KEY_USER_ID, deviceId).commit();
        }
        if (DBG) Log.v(TAG, "userId: " + deviceId);
        mDeviceId = deviceId;
        return deviceId;
    }


}
