package de.htwberlin.webtech.recipe.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.htwberlin.webtech.recipe.entity.Recipe;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecipeJsonSeedImporterTest {

    private static final Path GERMAN_LUNCH_FILE = Path.of(
            "src", "main", "resources", "recipes", "de", "lunch.json"
    );
    private static final Path ENGLISH_BREAKFAST_FILE = Path.of(
            "src", "main", "resources", "recipes", "en", "breakfast.json"
    );

    @Test
    void seedFiles_should_support_german_english_folder_structure_and_legacy_english_files() {
        RecipeJsonSeedImporter importer = new RecipeJsonSeedImporter(mock(RecipeSeedPersistence.class), new ObjectMapper());

        List<RecipeJsonSeedImporter.SeedFile> files = importer.seedFiles();

        assertTrue(files.stream().anyMatch(file -> file.language().equals("en")
                && file.category().equals("breakfast")
                && file.fileNames().contains("recipes/en/breakfast.json")));
        assertTrue(files.stream().anyMatch(file -> file.language().equals("de")
                && file.category().equals("lunch")
                && file.fileNames().contains("recipes/de/lunch.json")));
        assertTrue(files.stream().allMatch(file -> file.language().equals("en") || file.language().equals("de")));
        assertFalse(files.stream().anyMatch(file -> file.fileNames().stream()
                .anyMatch(name -> name.matches(".*recipes/(ar|pl|ru|tr|zh)/.*"))));
        assertTrue(files.stream().anyMatch(file -> file.language().equals("en")
                && file.category().equals("breakfast")
                && file.fileNames().contains("recipesbreakfast.json")));
        assertTrue(files.stream().anyMatch(file -> file.language().equals("en")
                && file.category().equals("dinner")
                && file.fileNames().contains("recipesDinner.json")));
        assertTrue(files.stream().anyMatch(file -> file.language().equals("en")
                && file.category().equals("snack")
                && file.fileNames().contains("recipes/en/snack.json")));
    }

    @Test
    void importFile_should_import_language_category_and_calories_from_language_folder() {
        RecipeSeedPersistence persistence = mock(RecipeSeedPersistence.class);
        when(persistence.upsertSeedRecipe(any(Recipe.class))).thenReturn(true);
        RecipeJsonSeedImporter importer = new RecipeJsonSeedImporter(persistence, new ObjectMapper());

        int imported = importer.importFile(new RecipeJsonSeedImporter.SeedFile(
                List.of("recipes/en/breakfast.json"),
                "breakfast",
                "en"
        ));

        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(persistence).upsertSeedRecipe(captor.capture());
        Recipe recipe = captor.getValue();
        assertEquals(1, imported);
        assertEquals("Seed Breakfast", recipe.getTitle());
        assertEquals("breakfast", recipe.getCategory());
        assertEquals("en", recipe.getLanguage());
        assertEquals("12345", recipe.getExternalId());
        assertEquals(420, recipe.getCalories());
        assertEquals(18.5, recipe.getProtein());
        assertEquals(1.2, recipe.getAlcohol());
        assertEquals(0.4, recipe.getAlcoholPercent());
        assertEquals("https://example.com/source", recipe.getSourceUrl());
        assertEquals("Example Kitchen", recipe.getSourceName());
        assertEquals("breakfast, morning meal", recipe.getDishTypes());
        assertEquals("vegetarian", recipe.getDiets());
        assertTrue(recipe.isVegetarian());
        assertTrue(recipe.isGlutenFree());
        assertEquals("1. Mix ingredients.\n2. Cook gently.", recipe.getInstructions());
    }

    @Test
    void importFile_should_import_non_english_language_and_category_from_folder() {
        RecipeSeedPersistence persistence = mock(RecipeSeedPersistence.class);
        when(persistence.upsertSeedRecipe(any(Recipe.class))).thenReturn(true);
        RecipeJsonSeedImporter importer = new RecipeJsonSeedImporter(persistence, new ObjectMapper());

        int imported = importer.importFile(new RecipeJsonSeedImporter.SeedFile(
                List.of("recipes/de/lunch.json"),
                "lunch",
                "de"
        ));

        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(persistence).upsertSeedRecipe(captor.capture());
        Recipe recipe = captor.getValue();
        assertEquals(1, imported);
        assertEquals("Seed Mittagessen", recipe.getTitle());
        assertEquals("lunch", recipe.getCategory());
        assertEquals("de", recipe.getLanguage());
        assertEquals(510, recipe.getCalories());
    }

    @Test
    void productionGermanLunch_should_import_complete_searchable_recipes() throws Exception {
        RecipeSeedPersistence persistence = mock(RecipeSeedPersistence.class);
        when(persistence.upsertSeedRecipe(any(Recipe.class))).thenReturn(true);
        RecipeJsonSeedImporter importer = new RecipeJsonSeedImporter(persistence, new ObjectMapper());

        int imported;
        try (InputStream stream = Files.newInputStream(GERMAN_LUNCH_FILE)) {
            imported = importer.importStream(stream, "recipes/de/lunch.json", "lunch", "de");
        }

        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(persistence, times(100)).upsertSeedRecipe(captor.capture());
        List<Recipe> recipes = captor.getAllValues();

        assertEquals(100, imported);
        assertEquals(100, recipes.stream().map(Recipe::getExternalId).distinct().count());
        assertTrue(recipes.stream().allMatch(recipe -> "de".equals(recipe.getLanguage())));
        assertTrue(recipes.stream().allMatch(recipe -> "lunch".equals(recipe.getCategory())));
        assertTrue(recipes.stream().allMatch(recipe -> !recipe.getTitle().isBlank()));
        assertTrue(recipes.stream().allMatch(recipe -> !recipe.getImageUrl().isBlank()));
        assertTrue(recipes.stream().allMatch(recipe -> recipe.getCookTimeMinutes() >= 0));
        assertTrue(recipes.stream().allMatch(recipe -> recipe.getServings() > 0));
        assertTrue(recipes.stream().allMatch(recipe -> !recipe.getSourceUrl().isBlank()));
        assertTrue(recipes.stream().allMatch(recipe -> recipe.getCalories() != null));
        assertTrue(recipes.stream().allMatch(recipe -> recipe.getProtein() != null));
        assertTrue(recipes.stream().allMatch(recipe -> !recipe.getIngredients().isBlank()));
        assertTrue(recipes.stream().allMatch(recipe -> !recipe.getInstructions().isBlank()));

        Recipe representative = recipes.getFirst();
        assertNotNull(representative.getCalories());
        assertNotNull(representative.getProtein());
        assertFalse(representative.getIngredients().isBlank());
        assertFalse(representative.getInstructions().isBlank());
    }

    @Test
    void productionEnglishBreakfast_should_import_new_source_recipes_with_nutrition() throws Exception {
        RecipeSeedPersistence persistence = mock(RecipeSeedPersistence.class);
        when(persistence.upsertSeedRecipe(any(Recipe.class))).thenReturn(true);
        RecipeJsonSeedImporter importer = new RecipeJsonSeedImporter(persistence, new ObjectMapper());

        int imported;
        try (InputStream stream = Files.newInputStream(ENGLISH_BREAKFAST_FILE)) {
            imported = importer.importStream(stream, "recipes/en/breakfast.json", "breakfast", "en");
        }

        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(persistence, times(100)).upsertSeedRecipe(captor.capture());
        List<Recipe> recipes = captor.getAllValues();

        assertEquals(100, imported);
        assertEquals(100, recipes.stream().map(Recipe::getExternalId).distinct().count());
        assertTrue(recipes.stream().allMatch(recipe -> "en".equals(recipe.getLanguage())));
        assertTrue(recipes.stream().allMatch(recipe -> "breakfast".equals(recipe.getCategory())));
        assertTrue(recipes.stream().allMatch(recipe -> recipe.getCalories() != null));
        assertTrue(recipes.stream().allMatch(recipe -> recipe.getProtein() != null));
    }

    @Test
    void importFile_should_import_recipe_without_ingredients() {
        RecipeSeedPersistence persistence = mock(RecipeSeedPersistence.class);
        when(persistence.upsertSeedRecipe(any(Recipe.class))).thenReturn(true);
        RecipeJsonSeedImporter importer = new RecipeJsonSeedImporter(persistence, new ObjectMapper());

        int imported = importer.importFile(new RecipeJsonSeedImporter.SeedFile(
                List.of("recipes/en/missing-ingredients.json"),
                "breakfast",
                "en"
        ));

        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(persistence).upsertSeedRecipe(captor.capture());
        assertEquals(1, imported);
        assertEquals("No Ingredients Recipe", captor.getValue().getTitle());
        assertEquals("", captor.getValue().getIngredients());
    }

    @Test
    void importFile_should_import_recipe_with_empty_ingredients_list() {
        RecipeSeedPersistence persistence = mock(RecipeSeedPersistence.class);
        when(persistence.upsertSeedRecipe(any(Recipe.class))).thenReturn(true);
        RecipeJsonSeedImporter importer = new RecipeJsonSeedImporter(persistence, new ObjectMapper());

        int imported = importer.importFile(new RecipeJsonSeedImporter.SeedFile(
                List.of("recipes/en/empty-ingredients.json"),
                "breakfast",
                "en"
        ));

        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(persistence).upsertSeedRecipe(captor.capture());
        assertEquals(1, imported);
        assertEquals("Empty Ingredients Recipe", captor.getValue().getTitle());
        assertEquals("", captor.getValue().getIngredients());
    }

    @Test
    void importFile_should_import_ingredients_from_nutrition_ingredients() {
        RecipeSeedPersistence persistence = mock(RecipeSeedPersistence.class);
        when(persistence.upsertSeedRecipe(any(Recipe.class))).thenReturn(true);
        RecipeJsonSeedImporter importer = new RecipeJsonSeedImporter(persistence, new ObjectMapper());

        int imported = importer.importFile(new RecipeJsonSeedImporter.SeedFile(
                List.of("recipes/en/nutrition-ingredients.json"),
                "breakfast",
                "en"
        ));

        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(persistence).upsertSeedRecipe(captor.capture());
        Recipe recipe = captor.getValue();
        assertEquals(1, imported);
        assertEquals("100 g rice\n200 g beans", recipe.getIngredients());
        assertEquals(22.7, recipe.getProtein());
    }

    @Test
    void importFile_should_prefer_metric_measures_over_ugly_us_units() {
        RecipeSeedPersistence persistence = mock(RecipeSeedPersistence.class);
        when(persistence.upsertSeedRecipe(any(Recipe.class))).thenReturn(true);
        RecipeJsonSeedImporter importer = new RecipeJsonSeedImporter(persistence, new ObjectMapper());

        int imported = importer.importFile(new RecipeJsonSeedImporter.SeedFile(
                List.of("recipes/de/metric-ingredients.json"),
                "dinner",
                "de"
        ));

        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(persistence).upsertSeedRecipe(captor.capture());
        Recipe recipe = captor.getValue();
        assertEquals(1, imported);
        assertEquals("66 g Brokkoliröschen\n1 Prise Salz", recipe.getIngredients());
        assertEquals(230, recipe.getCalories());
        assertEquals(13.5, recipe.getProtein());
    }

    @Test
    void importStream_should_preserve_supported_units_when_structured_ingredient_has_no_original_text() {
        RecipeSeedPersistence persistence = mock(RecipeSeedPersistence.class);
        when(persistence.upsertSeedRecipe(any(Recipe.class))).thenReturn(true);
        RecipeJsonSeedImporter importer = new RecipeJsonSeedImporter(persistence, new ObjectMapper());
        String json = """
                [
                  {
                    "id": 92001,
                    "language": "en",
                    "title": "Unit Test Recipe",
                    "instructions": "Mix.",
                    "extendedIngredients": [
                      { "name": "ham", "amount": 6, "unit": "ounces" },
                      { "name": "oil", "amount": 1.25, "unit": "t" },
                      { "name": "parsley", "amount": 2, "unit": "stalks" },
                      { "name": "sauce", "amount": 1, "unit": "package" },
                      { "name": "potatoes", "amount": 1, "unit": "lb" }
                    ]
                  }
                ]
                """;

        int imported = importer.importStream(
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
                "inline.json",
                "lunch",
                "en"
        );

        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(persistence).upsertSeedRecipe(captor.capture());
        assertEquals(1, imported);
        assertEquals("6 oz ham\n1,25 tsp oil\n2 stalk parsley\n1 package sauce\n1 lb potatoes", captor.getValue().getIngredients());
    }

    @Test
    void importStream_should_normalize_safe_german_title_terms() {
        RecipeSeedPersistence persistence = mock(RecipeSeedPersistence.class);
        when(persistence.upsertSeedRecipe(any(Recipe.class))).thenReturn(true);
        RecipeJsonSeedImporter importer = new RecipeJsonSeedImporter(persistence, new ObjectMapper());
        String json = """
                [
                  {
                    "id": 9123,
                    "language": "de",
                    "title": "Instant Pot Slow Cooker Eintopf",
                    "ingredients": ["Kartoffeln", "Rindfleisch"],
                    "instructions": ["Kochen"],
                    "calories": 420,
                    "protein": 31.5
                  }
                ]
                """;

        int imported = importer.importStream(
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
                "inline.json",
                "dinner",
                "de"
        );

        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(persistence).upsertSeedRecipe(captor.capture());
        assertEquals(1, imported);
        assertEquals("Schnellkochtopf Schongarer Eintopf", captor.getValue().getTitle());
    }

    @Test
    void importFile_should_skip_recipe_when_language_does_not_match_seed_file() {
        RecipeSeedPersistence persistence = mock(RecipeSeedPersistence.class);
        RecipeJsonSeedImporter importer = new RecipeJsonSeedImporter(persistence, new ObjectMapper());

        int imported = importer.importFile(new RecipeJsonSeedImporter.SeedFile(
                List.of("recipes/de/lunch.json"),
                "lunch",
                "en"
        ));

        assertEquals(0, imported);
        verify(persistence, never()).upsertSeedRecipe(any(Recipe.class));
    }

    @Test
    void importFile_should_import_long_ingredients_instructions_and_image_url() {
        RecipeSeedPersistence persistence = mock(RecipeSeedPersistence.class);
        when(persistence.upsertSeedRecipe(any(Recipe.class))).thenReturn(true);
        RecipeJsonSeedImporter importer = new RecipeJsonSeedImporter(persistence, new ObjectMapper());

        int imported = importer.importFile(new RecipeJsonSeedImporter.SeedFile(
                List.of("recipes/en/long-fields.json"),
                "dinner",
                "en"
        ));

        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(persistence).upsertSeedRecipe(captor.capture());
        Recipe recipe = captor.getValue();
        assertEquals(1, imported);
        assertTrue(recipe.getIngredients().length() > 255);
        assertTrue(recipe.getInstructions().length() > 255);
        assertTrue(recipe.getImageUrl().length() > 150);
        assertEquals(34.5, recipe.getProtein());
    }

    @Test
    void importFile_should_continue_after_one_recipe_persist_fails() {
        RecipeSeedPersistence persistence = mock(RecipeSeedPersistence.class);
        when(persistence.upsertSeedRecipe(any(Recipe.class)))
                .thenThrow(new IllegalStateException("simulated persist error"))
                .thenReturn(true);
        RecipeJsonSeedImporter importer = new RecipeJsonSeedImporter(persistence, new ObjectMapper());

        int imported = importer.importFile(new RecipeJsonSeedImporter.SeedFile(
                List.of("recipes/en/one-bad-one-good.json"),
                "dinner",
                "en"
        ));

        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(persistence, times(2)).upsertSeedRecipe(captor.capture());
        assertEquals(1, imported);
        assertEquals("Broken Seed Recipe", captor.getAllValues().get(0).getTitle());
        assertEquals("Recovering Seed Recipe", captor.getAllValues().get(1).getTitle());
    }

    @Test
    void importFile_should_avoid_duplicates_per_title_category_and_language() {
        RecipeSeedPersistence persistence = mock(RecipeSeedPersistence.class);
        when(persistence.upsertSeedRecipe(any(Recipe.class))).thenReturn(true, false);
        RecipeJsonSeedImporter importer = new RecipeJsonSeedImporter(persistence, new ObjectMapper());
        RecipeJsonSeedImporter.SeedFile seedFile = new RecipeJsonSeedImporter.SeedFile(
                List.of("recipes/en/breakfast.json"),
                "breakfast",
                "en"
        );

        assertEquals(1, importer.importFile(seedFile));
        assertEquals(0, importer.importFile(seedFile));

        verify(persistence, times(2)).upsertSeedRecipe(any(Recipe.class));
    }

    @Test
    void importFile_should_skip_missing_files_without_persisting_test_data() {
        RecipeSeedPersistence persistence = mock(RecipeSeedPersistence.class);
        RecipeJsonSeedImporter importer = new RecipeJsonSeedImporter(persistence, new ObjectMapper());

        int imported = importer.importFile(new RecipeJsonSeedImporter.SeedFile(
                List.of("recipes/de/missing.json"),
                "lunch",
                "de"
        ));

        assertEquals(0, imported);
        verify(persistence, never()).upsertSeedRecipe(any(Recipe.class));
    }

    private String searchableText(Recipe recipe) {
        return String.join(" ",
                recipe.getTitle(),
                recipe.getIngredients(),
                recipe.getInstructions(),
                recipe.getDishTypes(),
                recipe.getDiets()
        ).toLowerCase();
    }
}
