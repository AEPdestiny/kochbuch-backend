package de.htwberlin.webtech.recipe.external;

import de.htwberlin.webtech.recipe.entity.Recipe;
import de.htwberlin.webtech.recipe.external.dto.TheMealDbMeal;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalRecipeServiceTest {

    @Test
    void fetchExternalRecipes_should_map_the_meal_db_meals_to_recipes() {
        ExternalRecipeService service = new ExternalRecipeService(search -> List.of(meal()));

        List<Recipe> recipes = service.fetchExternalRecipes("pasta");

        assertEquals(1, recipes.size());
        Recipe recipe = recipes.getFirst();
        assertEquals("Arrabiata", recipe.getTitle());
        assertEquals("https://example.com/arrabiata.jpg", recipe.getImageUrl());
        assertEquals("Vegetarian", recipe.getCategory());
        assertEquals("Boil pasta and mix with sauce.", recipe.getInstructions());
        assertEquals("1 pound penne rigate, 1/4 cup olive oil, garlic", recipe.getIngredients());
        assertEquals(0, recipe.getPrepTimeMinutes());
        assertEquals(0, recipe.getCookTimeMinutes());
        assertEquals(0, recipe.getServings());
        assertEquals(0.0, recipe.getRating());
        assertFalse(recipe.isFavorite());
        assertTrue(recipe.isPublished());
    }

    @Test
    void fetchExternalRecipes_should_use_default_search_when_search_is_blank() {
        CapturingClient client = new CapturingClient(List.of(meal()));
        ExternalRecipeService service = new ExternalRecipeService(client);

        service.fetchExternalRecipes(" ");

        assertEquals("pasta", client.search);
    }

    @Test
    void fetchExternalRecipes_should_return_empty_list_when_client_fails() {
        ExternalRecipeService service = new ExternalRecipeService(search -> {
            throw new ExternalRecipeClientException("TheMealDB unavailable");
        });

        List<Recipe> recipes = service.fetchExternalRecipes("pasta");

        assertTrue(recipes.isEmpty());
    }

    private TheMealDbMeal meal() {
        TheMealDbMeal meal = new TheMealDbMeal();
        meal.setIdMeal("52771");
        meal.setStrMeal("Arrabiata");
        meal.setStrMealThumb("https://example.com/arrabiata.jpg");
        meal.setStrCategory("Vegetarian");
        meal.setStrArea("Italian");
        meal.setStrInstructions("Boil pasta and mix with sauce.");
        meal.putNumberedField("strIngredient1", "penne rigate");
        meal.putNumberedField("strMeasure1", "1 pound");
        meal.putNumberedField("strIngredient2", "olive oil");
        meal.putNumberedField("strMeasure2", "1/4 cup");
        meal.putNumberedField("strIngredient3", "garlic");
        meal.putNumberedField("strMeasure3", "");
        return meal;
    }

    private static class CapturingClient implements ExternalRecipeClient {

        private final List<TheMealDbMeal> meals;
        private String search;

        private CapturingClient(List<TheMealDbMeal> meals) {
            this.meals = meals;
        }

        @Override
        public List<TheMealDbMeal> searchMeals(String search) {
            this.search = search;
            return meals;
        }
    }
}
