package de.htwberlin.webtech.recipe.external;

import de.htwberlin.webtech.recipe.dto.ExternalRecipeDetailResponse;
import de.htwberlin.webtech.recipe.dto.ExternalRecipeMatchResponse;
import de.htwberlin.webtech.recipe.dto.RecipeResponse;
import de.htwberlin.webtech.recipe.external.dto.SpoonacularAnalyzedInstruction;
import de.htwberlin.webtech.recipe.external.dto.SpoonacularIngredient;
import de.htwberlin.webtech.recipe.external.dto.SpoonacularIngredientMatch;
import de.htwberlin.webtech.recipe.external.dto.SpoonacularInstructionStep;
import de.htwberlin.webtech.recipe.external.dto.SpoonacularNutrient;
import de.htwberlin.webtech.recipe.external.dto.SpoonacularNutrition;
import de.htwberlin.webtech.recipe.external.dto.SpoonacularRecipe;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalRecipeServiceTest {

    @Test
    void fetchExternalRecipes_should_map_spoonacular_recipes_to_recipe_responses() {
        ExternalRecipeService service = new ExternalRecipeService(new StaticClient(List.of(recipe())));

        List<RecipeResponse> recipes = service.fetchExternalRecipes("pasta");

        assertEquals(1, recipes.size());
        RecipeResponse recipe = recipes.getFirst();
        assertEquals(716429L, recipe.getId());
        assertEquals("716429", recipe.getExternalId());
        assertEquals("spoonacular", recipe.getSource());
        assertEquals("Pasta with Garlic", recipe.getTitle());
        assertEquals("https://example.com/pasta.jpg", recipe.getImageUrl());
        assertEquals("main course", recipe.getCategory());
        assertEquals("2 cups pasta, 1 tbsp olive oil", recipe.getIngredients());
        assertEquals("Cook pasta. Serve warm.", recipe.getInstructions());
        assertEquals(20, recipe.getCookTimeMinutes());
        assertEquals(2, recipe.getServings());
        assertEquals(510, recipe.getCalories());
    }

    @Test
    void fetchExternalRecipes_should_use_default_search_when_search_is_blank() {
        CapturingClient client = new CapturingClient(List.of(recipe()));
        ExternalRecipeService service = new ExternalRecipeService(client);

        service.fetchExternalRecipes(" ");

        assertEquals("pasta", client.search);
    }

    @Test
    void fetchExternalRecipes_should_cache_same_search_term() {
        CountingClient client = new CountingClient(List.of(recipe("Pasta One")));
        ExternalRecipeService service = new ExternalRecipeService(client);

        List<RecipeResponse> first = service.fetchExternalRecipes("chicken");
        List<RecipeResponse> second = service.fetchExternalRecipes("chicken");

        assertEquals(1, client.callCount);
        assertEquals("Pasta One", first.getFirst().getTitle());
        assertEquals("Pasta One", second.getFirst().getTitle());
    }

    @Test
    void fetchExternalRecipes_should_normalize_search_terms_for_cache_key() {
        CountingClient client = new CountingClient(List.of(recipe("Chicken Curry")));
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
                List.of(recipe("First Chicken")),
                List.of(recipe("Fresh Chicken"))
        );
        ExternalRecipeService service = new ExternalRecipeService(client, clock);

        List<RecipeResponse> first = service.fetchExternalRecipes("chicken");
        clock.advanceMinutes(16);
        List<RecipeResponse> second = service.fetchExternalRecipes("chicken");

        assertEquals(2, client.callCount);
        assertEquals("First Chicken", first.getFirst().getTitle());
        assertEquals("Fresh Chicken", second.getFirst().getTitle());
    }

    @Test
    void fetchExternalRecipes_should_return_stale_cache_when_refresh_fails() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-06T10:00:00Z"));
        CountingClient client = new CountingClient(List.of(recipe("Cached Chicken")));
        ExternalRecipeService service = new ExternalRecipeService(client, clock);

        service.fetchExternalRecipes("chicken");
        clock.advanceMinutes(16);
        client.fail = true;
        List<RecipeResponse> stale = service.fetchExternalRecipes("chicken");

        assertEquals(2, client.callCount);
        assertEquals("Cached Chicken", stale.getFirst().getTitle());
    }

    @Test
    void fetchExternalRecipes_should_return_empty_list_when_client_fails() {
        ExternalRecipeService service = new ExternalRecipeService(new FailingClient());

        List<RecipeResponse> recipes = service.fetchExternalRecipes("pasta");

        assertTrue(recipes.isEmpty());
    }

    @Test
    void fetchExternalRecipeDetail_should_return_ingredients_and_steps() {
        ExternalRecipeService service = new ExternalRecipeService(new StaticClient(List.of(recipe())));

        Optional<ExternalRecipeDetailResponse> detail = service.fetchExternalRecipeDetail(716429L);

        assertTrue(detail.isPresent());
        assertEquals("Pasta with Garlic", detail.get().getTitle());
        assertEquals(2, detail.get().getIngredients().size());
        assertEquals("pasta", detail.get().getIngredients().getFirst().getName());
        assertEquals("Cook pasta.", detail.get().getSteps().getFirst());
        assertEquals(510, detail.get().getCalories());
    }

    @Test
    void fetchExternalRecipeDetail_should_return_empty_when_client_fails() {
        ExternalRecipeService service = new ExternalRecipeService(new FailingClient());

        Optional<ExternalRecipeDetailResponse> detail = service.fetchExternalRecipeDetail(1L);

        assertTrue(detail.isEmpty());
    }

    @Test
    void findRecipesByIngredients_should_map_matches() {
        ExternalRecipeService service = new ExternalRecipeService(new StaticClient(List.of(recipe())));

        List<ExternalRecipeMatchResponse> matches = service.findRecipesByIngredients(List.of("pasta", "tomato"));

        assertEquals(1, matches.size());
        assertEquals("Pasta with Garlic", matches.getFirst().getTitle());
        assertEquals(1, matches.getFirst().getUsedIngredientCount());
        assertEquals(1, matches.getFirst().getMissedIngredientCount());
        assertEquals("pasta", matches.getFirst().getUsedIngredients().getFirst());
        assertEquals("garlic", matches.getFirst().getMissedIngredients().getFirst());
    }

    private SpoonacularRecipe recipe() {
        return recipe("Pasta with Garlic");
    }

    private SpoonacularRecipe recipe(String title) {
        SpoonacularRecipe recipe = new SpoonacularRecipe();
        recipe.setId(716429L);
        recipe.setTitle(title);
        recipe.setImage("https://example.com/pasta.jpg");
        recipe.setReadyInMinutes(20);
        recipe.setServings(2);
        recipe.setDishTypes(List.of("main course"));
        recipe.setDiets(List.of("vegetarian"));
        recipe.setInstructions("<p>Cook pasta. Serve warm.</p>");
        recipe.setSourceUrl("https://example.com/source");
        recipe.setExtendedIngredients(List.of(ingredient("pasta", "2 cups pasta", "2", "cups"), ingredient("olive oil", "1 tbsp olive oil", "1", "tbsp")));
        recipe.setAnalyzedInstructions(List.of(instruction("Cook pasta."), instruction("Serve warm.")));
        recipe.setNutrition(nutrition());
        return recipe;
    }

    private SpoonacularIngredient ingredient(String name, String original, String amount, String unit) {
        SpoonacularIngredient ingredient = new SpoonacularIngredient();
        ingredient.setName(name);
        ingredient.setOriginal(original);
        ingredient.setAmount(new BigDecimal(amount));
        ingredient.setUnit(unit);
        return ingredient;
    }

    private SpoonacularAnalyzedInstruction instruction(String text) {
        SpoonacularInstructionStep step = new SpoonacularInstructionStep();
        step.setStep(text);
        SpoonacularAnalyzedInstruction instruction = new SpoonacularAnalyzedInstruction();
        instruction.setSteps(List.of(step));
        return instruction;
    }

    private SpoonacularNutrition nutrition() {
        SpoonacularNutrient calories = new SpoonacularNutrient();
        calories.setName("Calories");
        calories.setAmount(new BigDecimal("510.4"));
        calories.setUnit("kcal");
        SpoonacularNutrition nutrition = new SpoonacularNutrition();
        nutrition.setNutrients(List.of(calories));
        return nutrition;
    }

    private static class StaticClient implements ExternalRecipeClient {

        private final List<SpoonacularRecipe> recipes;

        private StaticClient(List<SpoonacularRecipe> recipes) {
            this.recipes = recipes;
        }

        @Override
        public List<SpoonacularRecipe> searchRecipes(String search) {
            return recipes;
        }

        @Override
        public Optional<SpoonacularRecipe> getRecipeInformation(Long id) {
            return recipes.stream().filter(recipe -> id.equals(recipe.getId())).findFirst();
        }

        @Override
        public List<SpoonacularIngredientMatch> findByIngredients(List<String> ingredients) {
            SpoonacularIngredientMatch match = new SpoonacularIngredientMatch();
            match.setId(716429L);
            match.setTitle("Pasta with Garlic");
            match.setImage("https://example.com/pasta.jpg");
            match.setUsedIngredientCount(1);
            match.setMissedIngredientCount(1);
            match.setUsedIngredients(List.of(matchIngredient("pasta")));
            match.setMissedIngredients(List.of(matchIngredient("garlic")));
            return List.of(match);
        }

        private SpoonacularIngredient matchIngredient(String name) {
            SpoonacularIngredient ingredient = new SpoonacularIngredient();
            ingredient.setName(name);
            ingredient.setOriginal(name);
            return ingredient;
        }
    }

    private static class CapturingClient extends StaticClient {

        private String search;

        private CapturingClient(List<SpoonacularRecipe> recipes) {
            super(recipes);
        }

        @Override
        public List<SpoonacularRecipe> searchRecipes(String search) {
            this.search = search;
            return super.searchRecipes(search);
        }
    }

    private static class CountingClient implements ExternalRecipeClient {

        private final List<List<SpoonacularRecipe>> responses;
        private int callCount;
        private boolean fail;
        private String lastSearch;

        @SafeVarargs
        private CountingClient(List<SpoonacularRecipe>... responses) {
            this.responses = List.of(responses);
        }

        @Override
        public List<SpoonacularRecipe> searchRecipes(String search) {
            callCount++;
            lastSearch = search;
            if (fail) {
                throw new ExternalRecipeClientException("Spoonacular unavailable");
            }
            int index = Math.min(callCount - 1, responses.size() - 1);
            return responses.get(index);
        }

        @Override
        public Optional<SpoonacularRecipe> getRecipeInformation(Long id) {
            return Optional.empty();
        }

        @Override
        public List<SpoonacularIngredientMatch> findByIngredients(List<String> ingredients) {
            return List.of();
        }
    }

    private static class FailingClient implements ExternalRecipeClient {

        @Override
        public List<SpoonacularRecipe> searchRecipes(String search) {
            throw new ExternalRecipeClientException("Spoonacular unavailable");
        }

        @Override
        public Optional<SpoonacularRecipe> getRecipeInformation(Long id) {
            throw new ExternalRecipeClientException("Spoonacular unavailable");
        }

        @Override
        public List<SpoonacularIngredientMatch> findByIngredients(List<String> ingredients) {
            throw new ExternalRecipeClientException("Spoonacular unavailable");
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
