package common;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

public final class ApiClient {

    public static final String BASE_URL = "http://localhost:4567";
    public static final String JSON = "application/json";
    public static final String XML = "application/xml";

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(TIMEOUT)
            .build();

    private ApiClient() {
    }

    public static ApiResponse get(String path) {
        return send("GET", path, null, JSON, JSON);
    }

    public static ApiResponse head(String path) {
        return send("HEAD", path, null, JSON, JSON);
    }

    public static ApiResponse delete(String path) {
        return send("DELETE", path, null, JSON, JSON);
    }

    public static ApiResponse post(String path, String body) {
        return send("POST", path, body, JSON, JSON);
    }

    public static ApiResponse put(String path, String body) {
        return send("PUT", path, body, JSON, JSON);
    }

    public static ApiResponse send(String method, String path, String body, String contentType, String accept) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                .timeout(TIMEOUT)
                .method(method, bodyPublisherFor(body));
        addHeaderIfPresent(builder, "Content-Type", contentType);
        addHeaderIfPresent(builder, "Accept", accept);
        HttpRequest request = builder.build();
        try {
            HttpResponse<String> response = HTTP.send(request, BodyHandlers.ofString());
            return new ApiResponse(response.statusCode(), response.headers(), response.body());
        } catch (IOException e) {
            throw new UncheckedIOException(method + " " + path + " failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(method + " " + path + " was interrupted", e);
        }
    }

    private static BodyPublisher bodyPublisherFor(String body) {
        return body == null ? BodyPublishers.noBody() : BodyPublishers.ofString(body);
    }

    private static void addHeaderIfPresent(HttpRequest.Builder builder, String name, String value) {
        if (value != null) {
            builder.header(name, value);
        }
    }
}
