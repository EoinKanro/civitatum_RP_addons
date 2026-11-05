package team.civitatum.starclock.discord;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class WebhookClient {
    private static final int TIMEOUT_MILLIS = 10_000;
    /** Discord отклоняет запросы со стандартным User-Agent Java; формат взят из его документации. */
    private static final String USER_AGENT = "DiscordBot (civitatum_starclock, 0.1.0)";

    /** Блокирует поток до ответа, поэтому вызывается только из рабочего потока DiscordNotifier. */
    public static int post(String url, String json, byte[] image, String imageName) throws IOException {
        String boundary = UUID.randomUUID().toString();
        byte[] body = multipart(boundary, json, image, imageName);
        var connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        try {
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            connection.setFixedLengthStreamingMode(body.length);
            try (var output = connection.getOutputStream()) {
                output.write(body);
            }
            return connection.getResponseCode();
        } finally {
            connection.disconnect();
        }
    }

    static byte[] multipart(String boundary, String json, byte[] image, String imageName) {
        var body = new ByteArrayOutputStream();
        body.writeBytes(bytes("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"payload_json\"\r\n"
                + "Content-Type: application/json\r\n\r\n" + json + "\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"files[0]\"; filename=\"" + imageName + "\"\r\n"
                + "Content-Type: image/png\r\n\r\n"));
        body.writeBytes(image);
        body.writeBytes(bytes("\r\n--" + boundary + "--\r\n"));
        return body.toByteArray();
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private WebhookClient() {}
}
