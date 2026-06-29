package de.htwberlin.webtech.recipe.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.htwberlin.webtech.recipe.external.dto.SpoonacularIngredientMatch;
import de.htwberlin.webtech.recipe.external.dto.SpoonacularRecipe;
import de.htwberlin.webtech.recipe.external.dto.SpoonacularSearchResponse;
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
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@ApplicationScoped
public class SpoonacularExternalRecipeClient implements ExternalRecipeClient {

    private static final String BASE_URL = "https://api.spoonacular.com/recipes";
    private static final Duration TIMEOUT = Duration.ofSeconds(6);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final SpoonacularKeyRotator keyRotator;

    public SpoonacularExternalRecipeClient(
            ObjectMapper objectMapper,
            @ConfigProperty(name = "spoonacular.api.keys") Optional<String> multiKeys,
            @ConfigProperty(name = "spoonacular.api.key") Optional<String> singleKey
    ) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        this.objectMapper = objectMapper;
        this.keyRotator = new SpoonacularKeyRotator(multiKeys, singleKey);
    }

    @Override
    public List<SpoonacularRecipe> searchRecipes(String search) {
        return searchRecipes(search, null, null, null, null);
    }

    @Override
    public List<SpoonacularRecipe> searchRecipes(String search, String diet, String intolerances, Integer maxReadyTime, String type) {
        HttpResponse<String> response = sendWithRotation(key -> {
            StringBuilder uriBuilder = new StringBuilder(BASE_URL + "/complexSearch"
                    + "?query=" + encode(search)
                    + "&number=100"
                    + "&addRecipeInformation=true"
                    + "&fillIngredients=true"
                    + "&instructionsRequired=false"
                    + "&apiKey=" + encode(key));
            appendIfPresent(uriBuilder, "diet", diet);
            appendIfPresent(uriBuilder, "intolerances", intolerances);
            appendIfPresent(uriBuilder, "type", type);
            if (maxReadyTime != null && maxReadyTime > 0) {
                uriBuilder.append("&maxReadyTime=").append(maxReadyTime);
            }
            return URI.create(uriBuilder.toString());
        });

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ExternalRecipeClientException("Spoonacular returned HTTP " + response.statusCode());
        }
        try {
            SpoonacularSearchResponse searchResponse = objectMapper.readValue(response.body(), SpoonacularSearchResponse.class);
            return searchResponse.getResults() == null ? List.of() : searchResponse.getResults();
        } catch (IOException e) {
            throw new ExternalRecipeClientException("Could not read Spoonacular response.", e);
        }
    }

    @Override
    public Optional<SpoonacularRecipe> getRecipeInformation(Long id) {
        HttpResponse<String> response = sendWithRotation(key ->
                URI.create(BASE_URL + "/" + id + "/information"
                        + "?includeNutrition=true"
                        + "&apiKey=" + encode(key)));

        if (response.statusCode() == 404) {
            return Optional.empty();
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ExternalRecipeClientException("Spoonacular returned HTTP " + response.statusCode());
        }
        try {
            return Optional.of(objectMapper.readValue(response.body(), SpoonacularRecipe.class));
        } catch (IOException e) {
            throw new ExternalRecipeClientException("Could not read Spoonacular response.", e);
        }
    }

    @Override
    public List<SpoonacularIngredientMatch> findByIngredients(List<String> ingredients) {
        HttpResponse<String> response = sendWithRotation(key ->
                URI.create(BASE_URL + "/findByIngredients"
                        + "?ingredients=" + encode(String.join(",", ingredients))
                        + "&number=12"
                        + "&ranking=1"
                        + "&ignorePantry=true"
                        + "&apiKey=" + encode(key)));

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ExternalRecipeClientException("Spoonacular returned HTTP " + response.statusCode());
        }
        try {
            return List.of(objectMapper.readValue(response.body(), SpoonacularIngredientMatch[].class));
        } catch (IOException e) {
            throw new ExternalRecipeClientException("Could not read Spoonacular response.", e);
        }
    }

    private HttpResponse<String> sendWithRotation(Function<String, URI> uriFactory) {
        if (keyRotator.isEmpty()) {
            throw new ExternalRecipeClientException("Spoonacular API key is not configured.");
        }
        int size = keyRotator.size();
        int base = keyRotator.currentIndex();
        for (int attempt = 0; attempt < size; attempt++) {
            int idx = (base + attempt) % size;
            String key = keyRotator.keyAt(idx);
            HttpResponse<String> response = send(uriFactory.apply(key));
            if (!SpoonacularKeyRotator.isKeyError(response.statusCode())) {
                return response;
            }
            keyRotator.advance();
        }
        throw new ExternalRecipeClientException(
                "All " + size + " Spoonacular API key(s) are exhausted or invalid. " +
                "Configure additional keys via SPOONACULAR_API_KEYS.");
    }

    private HttpResponse<String> send(URI uri) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(TIMEOUT)
                .GET()
                .build();
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new ExternalRecipeClientException("Could not reach Spoonacular.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalRecipeClientException("Spoonacular request was interrupted.", e);
        }
    }

    private void appendIfPresent(StringBuilder uriBuilder, String key, String value) {
        if (value != null && !value.isBlank()) {
            uriBuilder.append("&").append(key).append("=").append(encode(value.trim()));
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
