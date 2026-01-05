package de.htwberlin.webtech.service;

import de.htwberlin.webtech.model.Recipe;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ExternalRecipeService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String BASE_URL = "https://dummyjson.com/recipes?limit=20";

    public List<Recipe> fetchExternalRecipes() {
        Map response = restTemplate.getForObject(BASE_URL, Map.class);
        if (response == null || !response.containsKey("recipes")) {
            return List.of();
        }

        List<Map<String, Object>> recipesJson = (List<Map<String, Object>>) response.get("recipes");
        List<Recipe> result = new ArrayList<>();

        for (Map<String, Object> r : recipesJson) {
            Recipe recipe = mapToRecipe(r);
            result.add(recipe);
        }

        return result;
    }

    private Recipe mapToRecipe(Map<String, Object> r) {
        String title = (String) r.getOrDefault("name", "Unbekanntes Rezept");
        String imageUrl = (String) r.getOrDefault("image", "");
        int prepTime = ((Number) r.getOrDefault("prepTimeMinutes", 0)).intValue();
        int cookTime = ((Number) r.getOrDefault("cookTimeMinutes", 0)).intValue();
        int servings = ((Number) r.getOrDefault("servings", 0)).intValue();
        String difficulty = (String) r.getOrDefault("difficulty", "");
        String category = (String) r.getOrDefault("cuisine", "");
        double rating = ((Number) r.getOrDefault("rating", 0.0)).doubleValue();

        List<String> ingredientsList = (List<String>) r.getOrDefault("ingredients", List.of());
        String ingredients = String.join(", ", ingredientsList);

        List<String> instructionsList = (List<String>) r.getOrDefault("instructions", List.of());
        String instructions = String.join("\n", instructionsList);

        boolean favorite = false;

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
                favorite,
                true
        );
    }
}
