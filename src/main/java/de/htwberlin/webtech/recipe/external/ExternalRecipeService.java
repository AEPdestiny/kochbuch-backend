package de.htwberlin.webtech.recipe.external;

import de.htwberlin.webtech.recipe.entity.Recipe;
import de.htwberlin.webtech.recipe.external.dto.TheMealDbMeal;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ExternalRecipeService {

    private static final Logger LOG = Logger.getLogger(ExternalRecipeService.class);
    private static final String DEFAULT_SEARCH = "pasta";

    private final ExternalRecipeClient client;

    public ExternalRecipeService(ExternalRecipeClient client) {
        this.client = client;
    }

    public List<Recipe> fetchExternalRecipes() {
        return fetchExternalRecipes(null);
    }

    public List<Recipe> fetchExternalRecipes(String search) {
        String query = normalizeSearch(search);
        try {
            return client.searchMeals(query).stream()
                    .map(this::mapToRecipe)
                    .toList();
        } catch (RuntimeException e) {
            LOG.warnf("Could not fetch external recipes from TheMealDB for search '%s': %s. Returning empty list.", query, e.getMessage());
            return List.of();
        }
    }

    Recipe mapToRecipe(TheMealDbMeal meal) {
        return new Recipe(
                valueOrDefault(meal.getStrMeal(), "External recipe"),
                valueOrDefault(meal.getStrMealThumb(), ""),
                0,
                0,
                0,
                "",
                firstNonBlank(meal.getStrCategory(), meal.getStrArea()),
                0.0,
                mapIngredients(meal),
                valueOrDefault(meal.getStrInstructions(), "No instructions available."),
                false,
                true
        );
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return DEFAULT_SEARCH;
        }
        return search.trim();
    }

    private String mapIngredients(TheMealDbMeal meal) {
        List<String> ingredients = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            String ingredient = meal.getIngredient(i);
            if (ingredient == null || ingredient.isBlank()) {
                continue;
            }

            String measure = meal.getMeasure(i);
            if (measure == null || measure.isBlank()) {
                ingredients.add(ingredient.trim());
            } else {
                ingredients.add(measure.trim() + " " + ingredient.trim());
            }
        }
        return String.join(", ", ingredients);
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return "";
    }
}
