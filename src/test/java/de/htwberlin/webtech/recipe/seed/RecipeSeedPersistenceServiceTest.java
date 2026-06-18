package de.htwberlin.webtech.recipe.seed;

import de.htwberlin.webtech.recipe.entity.Recipe;
import de.htwberlin.webtech.recipe.repository.RecipeRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecipeSeedPersistenceServiceTest {

    @Test
    void upsertSeedRecipe_should_update_existing_seed_recipe_by_external_id() {
        RecipeRepository repository = mock(RecipeRepository.class);
        Recipe existing = recipe("Old Pancakes", "breakfast", "en");
        existing.setExternalId("12345");
        existing.setCalories(100);
        existing.setProtein(4.0);

        Recipe incoming = recipe("New Pancakes", "breakfast", "en");
        incoming.setExternalId("12345");
        incoming.setImageUrl("https://example.com/new.jpg");
        incoming.setIngredients("flour, milk");
        incoming.setInstructions("1. Mix\n2. Bake");
        incoming.setCalories(420);
        incoming.setProtein(18.5);
        incoming.setAlcohol(1.2);
        incoming.setAlcoholPercent(0.4);
        incoming.setSourceUrl("https://example.com/source");
        incoming.setSourceName("Example Kitchen");
        incoming.setDishTypes("breakfast");
        incoming.setDiets("vegetarian");
        incoming.setVegetarian(true);
        incoming.setGlutenFree(true);

        when(repository.findSeedByExternalIdCategoryAndLanguage("12345", "breakfast", "en"))
                .thenReturn(Optional.of(existing));

        RecipeSeedPersistenceService service = new RecipeSeedPersistenceService(repository);

        boolean inserted = service.upsertSeedRecipe(incoming);

        assertFalse(inserted);
        assertEquals("New Pancakes", existing.getTitle());
        assertEquals("https://example.com/new.jpg", existing.getImageUrl());
        assertEquals("flour, milk", existing.getIngredients());
        assertEquals("1. Mix\n2. Bake", existing.getInstructions());
        assertEquals(420, existing.getCalories());
        assertEquals(18.5, existing.getProtein());
        assertEquals(1.2, existing.getAlcohol());
        assertEquals(0.4, existing.getAlcoholPercent());
        assertEquals("https://example.com/source", existing.getSourceUrl());
        assertEquals("Example Kitchen", existing.getSourceName());
        assertEquals("breakfast", existing.getDishTypes());
        assertEquals("vegetarian", existing.getDiets());
        assertTrue(existing.isVegetarian());
        assertTrue(existing.isGlutenFree());
        verify(repository, never()).persist(any(Recipe.class));
        verify(repository).flush();
    }

    @Test
    void upsertSeedRecipe_should_insert_when_no_ownerless_seed_exists() {
        RecipeRepository repository = mock(RecipeRepository.class);
        Recipe incoming = recipe("User Recipe Title", "dinner", "en");
        incoming.setExternalId("999");

        when(repository.findSeedByExternalIdCategoryAndLanguage("999", "dinner", "en"))
                .thenReturn(Optional.empty());
        when(repository.findSeedByTitleCategoryAndLanguage("User Recipe Title", "dinner", "en"))
                .thenReturn(Optional.empty());

        RecipeSeedPersistenceService service = new RecipeSeedPersistenceService(repository);

        boolean inserted = service.upsertSeedRecipe(incoming);

        assertTrue(inserted);
        verify(repository).persist(incoming);
        verify(repository).flush();
    }

    private Recipe recipe(String title, String category, String language) {
        Recipe recipe = new Recipe();
        recipe.setTitle(title);
        recipe.setCategory(category);
        recipe.setLanguage(language);
        recipe.setPublished(true);
        recipe.setFavorite(false);
        return recipe;
    }
}
