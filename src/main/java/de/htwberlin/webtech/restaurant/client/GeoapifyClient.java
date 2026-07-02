package de.htwberlin.webtech.restaurant.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.htwberlin.webtech.restaurant.client.dto.GeoapifyFeature;
import de.htwberlin.webtech.restaurant.client.dto.GeoapifyProperties;
import de.htwberlin.webtech.restaurant.client.dto.GeoapifyResponse;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

@ApplicationScoped
public class GeoapifyClient {

    private static final String PLACES_URL = "https://api.geoapify.com/v2/places";
    private static final String REVERSE_URL = "https://api.geoapify.com/v1/geocode/reverse";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Optional<String> apiKey;

    public GeoapifyClient(ObjectMapper objectMapper,
                          @ConfigProperty(name = "GEOAPIFY_API_KEY") Optional<String> apiKey) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
    }

    public GeoapifyResponse searchRestaurants(String query,
                                              double latitude,
                                              double longitude,
                                              int radiusMeters,
                                              int limit) {
        String configuredApiKey = apiKey
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new GeoapifyClientException("Geoapify API key is not configured."));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri(query, latitude, longitude, radiusMeters, limit, configuredApiKey))
                .timeout(TIMEOUT)
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new GeoapifyClientException("Geoapify returned HTTP " + response.statusCode());
            }
            return objectMapper.readValue(response.body(), GeoapifyResponse.class);
        } catch (IOException e) {
            throw new GeoapifyClientException("Could not read Geoapify response.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GeoapifyClientException("Geoapify request was interrupted.", e);
        }
    }

    /** Result of reverse geocoding: city name plus country context to disambiguate ambiguous city names. */
    public record ReverseGeocodeResult(String city, String country, String countryCode) {
    }

    public ReverseGeocodeResult reverseGeocode(double lat, double lon) {
        String configuredApiKey = apiKey.filter(v -> !v.isBlank()).orElse(null);
        if (configuredApiKey == null) return null;
        String url = REVERSE_URL + "?lat=" + lat + "&lon=" + lon + "&apiKey=" + encode(configuredApiKey);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(TIMEOUT).GET().build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) return null;
            GeoapifyResponse geo = objectMapper.readValue(response.body(), GeoapifyResponse.class);
            if (geo.getFeatures() == null || geo.getFeatures().isEmpty()) return null;
            GeoapifyFeature feature = geo.getFeatures().get(0);
            if (feature.getProperties() == null) return null;
            GeoapifyProperties props = feature.getProperties();
            String city = firstNonBlank(props.getCity(), props.getTown(), props.getVillage(), props.getCounty());
            if (city == null) return null;
            return new ReverseGeocodeResult(city, blankToNull(props.getCountry()), blankToNull(props.getCountryCode()));
        } catch (Exception e) {
            return null;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private URI uri(String query,
                    double latitude,
                    double longitude,
                    int radiusMeters,
                    int limit,
                    String configuredApiKey) {
        String url = PLACES_URL
                + "?categories=catering.restaurant"
                + "&filter=circle:" + longitude + "," + latitude + "," + radiusMeters
                + "&bias=proximity:" + longitude + "," + latitude
                + "&limit=" + limit
                + "&apiKey=" + encode(configuredApiKey);
        if (query != null && !query.isBlank()) {
            url += "&name=" + encode(query);
        }
        return URI.create(url);
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
