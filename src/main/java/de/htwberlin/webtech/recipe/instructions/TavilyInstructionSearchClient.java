package de.htwberlin.webtech.recipe.instructions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.htwberlin.webtech.recipe.dto.InstructionSearchResult;
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
public class TavilyInstructionSearchClient implements InstructionSearchClient {

    private static final URI SEARCH_URI = URI.create("https://api.tavily.com/search");
    private static final Duration TIMEOUT = Duration.ofSeconds(8);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Optional<String> apiKey;

    public TavilyInstructionSearchClient(
            ObjectMapper objectMapper,
            @ConfigProperty(name = "tavily.api.key") Optional<String> apiKey
    ) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey.filter(key -> !key.isBlank());
    }

    @Override
    public List<InstructionSearchResult> search(String query) {
        String key = apiKey.orElseThrow(InstructionSearchNotConfiguredException::new);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(SEARCH_URI)
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody(key, query), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new InstructionSearchClientException("Tavily returned HTTP " + response.statusCode());
            }
            return mapResults(response.body());
        } catch (IOException e) {
            throw new InstructionSearchClientException("Could not read Tavily response.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InstructionSearchClientException("Tavily request was interrupted.", e);
        }
    }

    private String requestBody(String key, String query) throws IOException {
        return objectMapper.writeValueAsString(new TavilySearchRequest(key, query, 5));
    }

    private List<InstructionSearchResult> mapResults(String body) throws IOException {
        JsonNode results = objectMapper.readTree(body).path("results");
        if (!results.isArray()) {
            return List.of();
        }

        List<InstructionSearchResult> mapped = new ArrayList<>();
        for (JsonNode result : results) {
            String title = result.path("title").asText("").trim();
            String url = result.path("url").asText("").trim();
            String snippet = result.path("content").asText("").trim();
            if (!title.isBlank() && !url.isBlank()) {
                mapped.add(new InstructionSearchResult(title, url, snippet));
            }
            if (mapped.size() == 5) {
                break;
            }
        }
        return mapped;
    }

    record TavilySearchRequest(String api_key, String query, int max_results) {
    }
}
