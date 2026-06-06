package de.htwberlin.webtech.recipe.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.htwberlin.webtech.recipe.external.dto.TheMealDbMeal;
import de.htwberlin.webtech.recipe.external.dto.TheMealDbSearchResponse;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@ApplicationScoped
public class TheMealDbExternalRecipeClient implements ExternalRecipeClient {

    private static final String SEARCH_URL = "https://www.themealdb.com/api/json/v1/1/search.php?s=";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public TheMealDbExternalRecipeClient(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public List<TheMealDbMeal> searchMeals(String search) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SEARCH_URL + encode(search)))
                .timeout(TIMEOUT)
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ExternalRecipeClientException("TheMealDB returned HTTP " + response.statusCode());
            }

            TheMealDbSearchResponse searchResponse = objectMapper.readValue(response.body(), TheMealDbSearchResponse.class);
            if (searchResponse.getMeals() == null) {
                return List.of();
            }
            return searchResponse.getMeals();
        } catch (IOException e) {
            throw new ExternalRecipeClientException("Could not read TheMealDB response.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalRecipeClientException("TheMealDB request was interrupted.", e);
        }
    }

    private String encode(String search) {
        return URLEncoder.encode(search == null ? "" : search, StandardCharsets.UTF_8);
    }
}
