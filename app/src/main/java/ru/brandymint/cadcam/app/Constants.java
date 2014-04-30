package ru.brandymint.cadcam.app;

/**
 * Created by alexey on 05.05.14.
 */
public interface Constants {

    public static final String API_URL = "http://cadcam.chebytoday.ru";

    public static final String ENDPOINT_CADCAM = API_URL + "/api/v1/cadcam.json";

    /**
     * Timeout (in ms) we specify for each http request
     */
    public static final int HTTP_REQUEST_TIMEOUT_MS = 30 * 1000;

    public static final int HTTP_READ_TIMEOUT_MS = 10 * 60 * 1000;

    public static final String HEADER_X_DEVICE_ID = "X-Device-Id";

    public static final String HEADER_GEO_POSITION = "Geo-Position";

}
