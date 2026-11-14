package kr.co.iefriends.pcsx2;

/**
 * Minimal fail-closed Android JNI response contract for pinned ARMSX2.
 *
 * Pinned common/HTTPDownloaderAndroid.cpp binds this class during
 * NativeApp.initialize(), even when the private CARTDECK probe has no
 * achievement or cover-art networking. Omitting the class makes JNI binding
 * incomplete. Deliberately do not enable network in the debug probe.
 *
 * The class is debug-only and MUST NOT be treated as a complete production
 * HTTP implementation or a game-playability pass.
 */
public final class HttpClient {
    private HttpClient() {}

    /** Exact field names/types expected by HTTPDownloaderAndroid::BindFromJNI. */
    public static final class Response {
        public int statusCode = -1;
        public String contentType = "";
        public byte[] data = new byte[0];
    }

    /**
     * Exact JNI method:
     * (Ljava/lang/String;Ljava/lang/String;[BLjava/lang/String;I)
     * Lkr/co/iefriends/pcsx2/HttpClient$Response;
     *
     * No network requests in the isolated PS2 test process. Unsupported
     * services return a normal transport failure rather than a missing class
     * or uncaught JNI exception.
     */
    public static Response doRequest(
            String url, String method, byte[] postData, String userAgent, int timeoutMs) {
        return new Response();
    }
}
