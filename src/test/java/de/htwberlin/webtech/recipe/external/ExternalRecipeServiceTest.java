package de.htwberlin.webtech.recipe.external;

import de.htwberlin.webtech.recipe.entity.Recipe;
import de.htwberlin.webtech.recipe.external.dto.TheMealDbMeal;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
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
    void fetchExternalRecipes_should_cache_same_search_term() {
        CountingClient client = new CountingClient(List.of(meal("Arrabiata")));
        ExternalRecipeService service = new ExternalRecipeService(client);

        List<Recipe> first = service.fetchExternalRecipes("chicken");
        List<Recipe> second = service.fetchExternalRecipes("chicken");

        assertEquals(1, client.callCount);
        assertEquals("Arrabiata", first.getFirst().getTitle());
        assertEquals("Arrabiata", second.getFirst().getTitle());
    }

    @Test
    void fetchExternalRecipes_should_normalize_search_terms_for_cache_key() {
        CountingClient client = new CountingClient(List.of(meal("Chicken Curry")));
        ExternalRecipeService service = new ExternalRecipeService(client);

        service.fetchExternalRecipes("Chicken");
        service.fetchExternalRecipes(" chicken ");
        service.fetchExternalRecipes("chicken");

        assertEquals(1, client.callCount);
        assertEquals("chicken", client.lastSearch);
    }

    @Test
    void fetchExternalRecipes_should_reload_after_ttl_expires() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-06T10:00:00Z"));
        CountingClient client = new CountingClient(
                List.of(meal("First Chicken")),
                List.of(meal("Fresh Chicken"))
        );
        ExternalRecipeService service = new ExternalRecipeService(client, clock);

        List<Recipe> first = service.fetchExternalRecipes("chicken");
        clock.advanceMinutes(16);
        List<Recipe> second = service.fetchExternalRecipes("chicken");

        assertEquals(2, client.callCount);
        assertEquals("First Chicken", first.getFirst().getTitle());
        assertEquals("Fresh Chicken", second.getFirst().getTitle());
    }

    @Test
    void fetchExternalRecipes_should_return_stale_cache_when_refresh_fails() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-06T10:00:00Z"));
        CountingClient client = new CountingClient(List.of(meal("Cached Chicken")));
        ExternalRecipeService service = new ExternalRecipeService(client, clock);

        service.fetchExternalRecipes("chicken");
        clock.advanceMinutes(16);
        client.fail = true;
        List<Recipe> stale = service.fetchExternalRecipes("chicken");

        assertEquals(2, client.callCount);
        assertEquals("Cached Chicken", stale.getFirst().getTitle());
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
        return meal("Arrabiata");
    }

    private TheMealDbMeal meal(String title) {
        TheMealDbMeal meal = new TheMealDbMeal();
        meal.setIdMeal("52771");
        meal.setStrMeal(title);
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

    private static class CountingClient implements ExternalRecipeClient {

        private final List<List<TheMealDbMeal>> responses;
        private int callCount;
        private boolean fail;
        private String lastSearch;

        @SafeVarargs
        private CountingClient(List<TheMealDbMeal>... responses) {
            this.responses = List.of(responses);
        }

        @Override
        public List<TheMealDbMeal> searchMeals(String search) {
            callCount++;
            lastSearch = search;
            if (fail) {
                throw new ExternalRecipeClientException("TheMealDB unavailable");
            }
            int index = Math.min(callCount - 1, responses.size() - 1);
            return responses.get(index);
        }
    }

    private static class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advanceMinutes(long minutes) {
            instant = instant.plusSeconds(minutes * 60);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
