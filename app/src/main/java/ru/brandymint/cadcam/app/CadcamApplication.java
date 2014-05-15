package ru.brandymint.cadcam.app;

import android.app.Application;

import com.google.api.client.http.HttpTransport;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import java.util.logging.Logger;

@ReportsCrashes(
        formKey = "",
        formUri = BuildConfig.ACRA_URL,
        reportType = org.acra.sender.HttpSender.Type.JSON,
        httpMethod = org.acra.sender.HttpSender.Method.PUT,
        formUriBasicAuthLogin=BuildConfig.ACRA_LOGIN,
        formUriBasicAuthPassword=BuildConfig.ACRA_PASSWORD,
        mode = ReportingInteractionMode.TOAST,
        forceCloseDialogAfterToast = false,
        resToastText = R.string.crash_toast_text
        )
public class CadcamApplication extends Application {
    static final String TAG = "CadcamApplication";
    @SuppressWarnings("PointlessBooleanExpression")
    static final boolean DBG = BuildConfig.DEBUG & true;

    @Override
    public void onCreate() {
        super.onCreate();

        // ACRA мешает при отладке
        if (!BuildConfig.DEBUG) ACRA.init(this);

        if (DBG) {
            // Furthermore, you must enable logging on your device as follows:
            // adb shell setprop log.tag.HttpTransport DEBUG
            Logger.getLogger(HttpTransport.class.getName()).setLevel(java.util.logging.Level.CONFIG);
        }
    }
}
