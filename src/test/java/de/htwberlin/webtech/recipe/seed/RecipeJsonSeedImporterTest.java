package de.htwberlin.webtech.recipe.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.htwberlin.webtech.recipe.entity.Recipe;
import de.htwberlin.webtech.recipe.repository.RecipeRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecipeJsonSeedImporterTest {

    @Test
    void seedFiles_should_support_language_folder_structure_and_legacy_english_files() {
        RecipeJsonSeedImporter importer = new RecipeJsonSeedImporter(mock(RecipeRepository.class), new ObjectMapper());

        List<RecipeJsonSeedImporter.SeedFile> files = importer.seedFiles();

        assertTrue(files.stream().anyMatch(file -> file.language().equals("en")
                && file.category().equals("breakfast")
                && file.fileNames().contains("recipes/en/breakfast.json")));
        assertTrue(files.stream().anyMatch(file -> file.language().equals("de")
                && file.category().equals("lunch")
                && file.fileNames().contains("recipes/de/lunch.json")));
        assertTrue(files.stream().anyMatch(file -> file.language().equals("ru")
                && file.category().equals("snack")
                && file.fileNames().contains("recipes/ru/snacks.json")));
        assertTrue(files.stream().anyMatch(file -> file.language().equals("en")
                && file.category().equals("breakfast")
                && file.fileNames().contains("recipesbreakfast.json")));
        assertTrue(files.stream().anyMatch(file -> file.language().equals("en")
                && file.category().equals("dinner")
                && file.fileNames().contains("recipesDinner.json")));
    }

    @Test
    void importFile_should_import_language_category_and_calories_from_language_folder() {
        RecipeRepository repository = mock(RecipeRepository.class);
        when(repository.findByTitleAndCategoryAndLanguage(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        RecipeJsonSeedImporter importer = new RecipeJsonSeedImporter(repository, new ObjectMapper());

        int imported = importer.importFile(new RecipeJsonSeedImporter.SeedFile(
                List.of("recipes/en/breakfast.json"),
                "breakfast",
                "en"
        ));

        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(repository).persist(captor.capture());
        Recipe recipe = captor.getValue();
        assertEquals(1, imported);
        assertEquals("Seed Breakfast", recipe.getTitle());
        assertEquals("breakfast", recipe.getCategory());
        assertEquals("en", recipe.getLanguage());
        assertEquals(420, recipe.getCalories());
        assertEquals(18.5, recipe.getProtein());
    }

    @Test
    void importFile_should_import_non_english_language_and_category_from_folder() {
        RecipeRepository repository = mock(RecipeRepository.class);
        when(repository.findByTitleAndCategoryAndLanguage(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        RecipeJsonSeedImporter importer = new RecipeJsonSeedImporter(repository, new ObjectMapper());

        int imported = importer.importFile(new RecipeJsonSeedImporter.SeedFile(
                List.of("recipes/de/lunch.json"),
                "lunch",
                "de"
        ));

        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(repository).persist(captor.capture());
        Recipe recipe = captor.getValue();
        assertEquals(1, imported);
        assertEquals("Seed Mittagessen", recipe.getTitle());
        assertEquals("lunch", recipe.getCategory());
        assertEquals("de", recipe.getLanguage());
        assertEquals(510, recipe.getCalories());
    }

    @Test
    void importFile_should_avoid_duplicates_per_title_category_and_language() {
        RecipeRepository repository = mock(RecipeRepository.class);
        Recipe existingRecipe = new Recipe();
        when(repository.findByTitleAndCategoryAndLanguage("Seed Breakfast", "breakfast", "en"))
                .thenReturn(Optional.empty(), Optional.of(existingRecipe));
        RecipeJsonSeedImporter importer = new RecipeJsonSeedImporter(repository, new ObjectMapper());
        RecipeJsonSeedImporter.SeedFile seedFile = new RecipeJsonSeedImporter.SeedFile(
                List.of("recipes/en/breakfast.json"),
                "breakfast",
                "en"
        );

        assertEquals(1, importer.importFile(seedFile));
        assertEquals(0, importer.importFile(seedFile));

        verify(repository, times(1)).persist(org.mockito.ArgumentMatchers.any(Recipe.class));
    }

    @Test
    void importFile_should_skip_missing_files_without_persisting_test_data() {
        RecipeRepository repository = mock(RecipeRepository.class);
        RecipeJsonSeedImporter importer = new RecipeJsonSeedImporter(repository, new ObjectMapper());

        int imported = importer.importFile(new RecipeJsonSeedImporter.SeedFile(
                List.of("recipes/de/missing.json"),
                "lunch",
                "de"
        ));

        assertEquals(0, imported);
        verify(repository, never()).persist(org.mockito.ArgumentMatchers.any(Recipe.class));
    }
}
