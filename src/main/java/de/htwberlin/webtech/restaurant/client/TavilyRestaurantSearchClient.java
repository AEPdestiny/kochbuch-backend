package de.htwberlin.webtech.restaurant.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class TavilyRestaurantSearchClient {

    private static final URI SEARCH_URI = URI.create("https://api.tavily.com/search");
    private static final Duration TIMEOUT = Duration.ofSeconds(8);
    private static final int MAX_RESULTS = 5;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Optional<String> apiKey;

    public TavilyRestaurantSearchClient(
            ObjectMapper objectMapper,
            @ConfigProperty(name = "tavily.api.key") Optional<String> apiKey
    ) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey.filter(key -> !key.isBlank());
    }

    public boolean isConfigured() {
        return apiKey.isPresent();
    }

    public List<TavilyRestaurantResult> search(String recipeTitle, String location) {
        if (apiKey.isEmpty()) {
            return List.of();
        }
        String query = "\"" + recipeTitle + "\" Restaurant " + location;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(SEARCH_URI)
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody(apiKey.get(), query), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return List.of();
            }
            return mapResults(response.body());
        } catch (IOException e) {
            return List.of();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of();
        }
    }

    private String requestBody(String key, String query) throws IOException {
        return objectMapper.writeValueAsString(new TavilySearchRequest(key, query, MAX_RESULTS));
    }

    private List<TavilyRestaurantResult> mapResults(String body) throws IOException {
        JsonNode results = objectMapper.readTree(body).path("results");
        if (!results.isArray()) {
            return List.of();
        }
        List<TavilyRestaurantResult> mapped = new ArrayList<>();
        for (JsonNode result : results) {
            String title = result.path("title").asText("").trim();
            String url = result.path("url").asText("").trim();
            String content = result.path("content").asText("").trim();
            if (!title.isBlank() && !url.isBlank()) {
                mapped.add(new TavilyRestaurantResult(title, url, content));
            }
            if (mapped.size() == MAX_RESULTS) {
                break;
            }
        }
        return mapped;
    }

    record TavilySearchRequest(String api_key, String query, int max_results) {
    }

    public record TavilyRestaurantResult(String title, String url, String content) {
    }
}
