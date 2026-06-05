package de.htwberlin.webtech.recipe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.htwberlin.webtech.recipe.entity.Recipe;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ExternalRecipeService {

    private static final String BASE_URL = "https://dummyjson.com/recipes?limit=0";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ExternalRecipeService(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
    }

    public List<Recipe> fetchExternalRecipes() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return List.of();
            }
            JsonNode recipes = objectMapper.readTree(response.body()).path("recipes");
            if (!recipes.isArray()) {
                return List.of();
            }

            List<Recipe> result = new ArrayList<>();
            for (JsonNode recipeJson : recipes) {
                result.add(mapToRecipe(recipeJson));
            }
            return result;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return List.of();
        }
    }

    private Recipe mapToRecipe(JsonNode r) {
        String title = text(r, "name", "Unbekanntes Rezept");
        String imageUrl = text(r, "image", "");
        int prepTime = r.path("prepTimeMinutes").asInt(0);
        int cookTime = r.path("cookTimeMinutes").asInt(0);
        int servings = r.path("servings").asInt(0);
        String difficulty = text(r, "difficulty", "");
        String category = text(r, "cuisine", "");
        double rating = r.path("rating").asDouble(0.0);
        String ingredients = joinArray(r.path("ingredients"), ", ");
        String instructions = joinArray(r.path("instructions"), "\n");

        return new Recipe(
                title,
                imageUrl,
                prepTime,
                cookTime,
                servings,
                difficulty,
                category,
                rating,
                ingredients,
                instructions,
                false,
                true
        );
    }

    private String text(JsonNode node, String field, String defaultValue) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? defaultValue : value.asText(defaultValue);
    }

    private String joinArray(JsonNode node, String delimiter) {
        if (!node.isArray()) {
            return "";
        }
        List<String> values = new ArrayList<>();
        for (JsonNode value : node) {
            values.add(value.asText(""));
        }
        return String.join(delimiter, values);
    }
}
