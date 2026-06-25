package de.htwberlin.webtech.recipe.image;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

@ApplicationScoped
public class SupabaseStorageService {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;
    private final Optional<String> supabaseUrl;
    private final Optional<String> serviceRoleKey;
    private final Optional<String> bucket;

    public SupabaseStorageService(
            @ConfigProperty(name = "SUPABASE_URL") Optional<String> supabaseUrl,
            @ConfigProperty(name = "SUPABASE_SERVICE_ROLE_KEY") Optional<String> serviceRoleKey,
            @ConfigProperty(name = "SUPABASE_BUCKET") Optional<String> bucket
    ) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        this.supabaseUrl = supabaseUrl.filter(value -> !value.isBlank());
        this.serviceRoleKey = serviceRoleKey.filter(value -> !value.isBlank());
        this.bucket = bucket.filter(value -> !value.isBlank());
    }

    public String upload(Path file, String objectPath, String contentType) {
        String baseUrl = configured(supabaseUrl, "Supabase URL is not configured.");
        String key = configured(serviceRoleKey, "Supabase service role key is not configured.");
        String configuredBucket = configured(bucket, "Supabase bucket is not configured.");

        URI uploadUri = URI.create(normalizeBaseUrl(baseUrl)
                + "/storage/v1/object/"
                + encodeSegment(configuredBucket)
                + "/"
                + encodeObjectPath(objectPath));

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uploadUri)
                    .timeout(TIMEOUT)
                    .header("Authorization", "Bearer " + key)
                    .header("apikey", key)
                    .header("Content-Type", contentType)
                    .header("x-upsert", "false")
                    .POST(HttpRequest.BodyPublishers.ofFile(file))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new SupabaseStorageException("Image upload failed.");
            }
            return publicUrl(baseUrl, configuredBucket, objectPath);
        } catch (IOException e) {
            throw new SupabaseStorageException("Image upload could not be completed.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SupabaseStorageException("Image upload was interrupted.", e);
        }
    }

    private String configured(Optional<String> value, String message) {
        return value.orElseThrow(() -> new SupabaseStorageException(message));
    }

    private String publicUrl(String baseUrl, String configuredBucket, String objectPath) {
        return normalizeBaseUrl(baseUrl)
                + "/storage/v1/object/public/"
                + encodeSegment(configuredBucket)
                + "/"
                + encodeObjectPath(objectPath);
    }

    private String normalizeBaseUrl(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String encodeObjectPath(String objectPath) {
        String[] segments = objectPath.split("/");
        StringBuilder encoded = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                encoded.append('/');
            }
            encoded.append(encodeSegment(segments[i]));
        }
        return encoded.toString();
    }

    private String encodeSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
