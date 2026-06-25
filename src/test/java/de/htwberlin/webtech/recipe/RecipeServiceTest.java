package de.htwberlin.webtech.recipe;

import de.htwberlin.webtech.recipe.entity.Recipe;
import de.htwberlin.webtech.recipe.exception.RecipeNotFoundException;
import de.htwberlin.webtech.recipe.mapper.RecipeMapper;
import de.htwberlin.webtech.recipe.repository.RecipeRepository;
import de.htwberlin.webtech.recipe.service.RecipeService;
import de.htwberlin.webtech.shared.exception.ForbiddenException;
import de.htwberlin.webtech.user.entity.AppUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class RecipeServiceTest {

    private final RecipeRepository repo = mock(RecipeRepository.class);
    private final RecipeService underTest = new RecipeService(repo, new RecipeMapper());

    @Test
    @DisplayName("findAll should return only published recipes")
    void findAll_should_return_only_published_recipes() {
        var r1 = recipe("Pasta");
        var r2 = recipe("Soup");
        doReturn(List.of()).when(repo).findRandomPublishedByLanguageAndCategory("en", "breakfast", 75);
        doReturn(List.of()).when(repo).findRandomPublishedByLanguageAndCategory("en", "lunch", 75);
        doReturn(List.of()).when(repo).findRandomPublishedByLanguageAndCategory("en", "dinner", 75);
        doReturn(List.of()).when(repo).findRandomPublishedByLanguageAndCategory("en", "snack", 75);
        doReturn(List.of(r1, r2)).when(repo).findRandomPublishedByLanguage("en", 200);

        var result = underTest.findAll();

        verify(repo).findRandomPublishedByLanguageAndCategory("en", "breakfast", 75);
        verify(repo).findRandomPublishedByLanguageAndCategory("en", "lunch", 75);
        verify(repo).findRandomPublishedByLanguageAndCategory("en", "dinner", 75);
        verify(repo).findRandomPublishedByLanguageAndCategory("en", "snack", 75);
        verify(repo).findRandomPublishedByLanguage("en", 200);
        verifyNoMoreInteractions(repo);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("findAllPublished should call repo.findRandomPublished")
    void findAllPublished_should_call_findRandomPublished() {
        var published = recipe("Cake");
        published.setPublished(true);
        doReturn(List.of(published)).when(repo).findRandomPublishedByLanguageAndCategory("en", "breakfast", 75);

        var result = underTest.findAllPublished();

        verify(repo).findRandomPublishedByLanguageAndCategory("en", "breakfast", 75);
        verify(repo).findRandomPublishedByLanguageAndCategory("en", "lunch", 75);
        verify(repo).findRandomPublishedByLanguageAndCategory("en", "dinner", 75);
        verify(repo).findRandomPublishedByLanguageAndCategory("en", "snack", 75);
        verify(repo).findRandomPublishedByLanguage("en", 200);
        verifyNoMoreInteractions(repo);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isPublished());
    }

    @Test
    @DisplayName("findAllPublished should filter by language")
    void findAllPublished_should_filter_by_language() {
        var published = recipe("Deutsches Rezept");
        published.setLanguage("de");
        doReturn(List.of(published)).when(repo).findRandomPublishedByLanguageAndCategory("de", "lunch", 75);

        var result = underTest.findAllPublished("de");

        verify(repo).findRandomPublishedByLanguageAndCategory("de", "breakfast", 75);
        verify(repo).findRandomPublishedByLanguageAndCategory("de", "lunch", 75);
        verify(repo).findRandomPublishedByLanguageAndCategory("de", "dinner", 75);
        verify(repo).findRandomPublishedByLanguageAndCategory("de", "snack", 75);
        verify(repo).findRandomPublishedByLanguage("de", 200);
        verifyNoMoreInteractions(repo);
        assertEquals(1, result.size());
        assertEquals("de", result.get(0).getLanguage());
    }

    @Test
    @DisplayName("findAllPublished should use local language search when search is present")
    void findAllPublished_should_use_search_query() {
        var published = recipe("Sushi Reis");
        published.setLanguage("de");
        doReturn(List.of(published)).when(repo).searchRandomPublishedByLanguage("de", "sushi", 200);

        var result = underTest.findAllPublished("de", "sushi");

        verify(repo).searchRandomPublishedByLanguage("de", "sushi", 200);
        verifyNoMoreInteractions(repo);
        assertEquals(1, result.size());
        assertEquals("Sushi Reis", result.get(0).getTitle());
    }

    @Test
    @DisplayName("findAllPublished should remove visible system duplicates from search results")
    void findAllPublished_should_remove_system_duplicates_from_search_results() {
        var incomplete = recipe("Rindereintopf aus dem Slow Cooker");
        incomplete.setId(10L);
        incomplete.setLanguage("de");
        incomplete.setExternalId("beef-stew-1");
        incomplete.setImageUrl("");
        incomplete.setIngredients("0");
        incomplete.setInstructions("");

        var complete = recipe("Rindereintopf aus dem Schongarer");
        complete.setId(11L);
        complete.setLanguage("de");
        complete.setExternalId("beef-stew-1");
        complete.setImageUrl("https://example.com/beef.jpg");
        complete.setIngredients("500 g Rindfleisch\n300 g Kartoffeln");
        complete.setInstructions("Alles langsam schmoren.");

        doReturn(List.of(incomplete, complete)).when(repo).searchRandomPublishedByLanguage("de", "rind", 200);

        var result = underTest.findAllPublished("de", "rind");

        assertEquals(1, result.size());
        assertEquals("Rindereintopf aus dem Schongarer", result.get(0).getTitle());
    }

    @Test
    @DisplayName("findAllPublished should keep user-created recipes even when similar to seed recipes")
    void findAllPublished_should_keep_user_created_recipes_when_similar_to_seed_recipes() {
        var seed = recipe("Sushi Bowl");
        seed.setId(20L);
        seed.setLanguage("de");
        seed.setExternalId("sushi-1");

        var userRecipe = recipe("Sushi Bowl");
        userRecipe.setId(21L);
        userRecipe.setLanguage("de");
        userRecipe.setOwner(user(1L, "owner@example.com"));

        doReturn(List.of(seed, userRecipe)).when(repo).searchRandomPublishedByLanguage("de", "sushi", 200);

        var result = underTest.findAllPublished("de", "sushi");

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(recipe -> recipe.getOwner() != null));
        assertTrue(result.stream().anyMatch(recipe -> recipe.getOwner() == null));
    }

    @Test
    @DisplayName("findAllPublished should return at most 100 recipes with 25 per category target")
    void findAllPublished_should_return_at_most_100_recipes() {
        List<Recipe> breakfast = numberedRecipes("Breakfast", 1, 25);
        List<Recipe> lunch = numberedRecipes("Lunch", 26, 25);
        List<Recipe> dinner = numberedRecipes("Dinner", 51, 25);
        List<Recipe> snack = numberedRecipes("Snack", 76, 25);
        doReturn(breakfast).when(repo).findRandomPublishedByLanguageAndCategory("en", "breakfast", 75);
        doReturn(lunch).when(repo).findRandomPublishedByLanguageAndCategory("en", "lunch", 75);
        doReturn(dinner).when(repo).findRandomPublishedByLanguageAndCategory("en", "dinner", 75);
        doReturn(snack).when(repo).findRandomPublishedByLanguageAndCategory("en", "snack", 75);

        var result = underTest.findAllPublished("en");

        verify(repo).findRandomPublishedByLanguageAndCategory("en", "breakfast", 75);
        verify(repo).findRandomPublishedByLanguageAndCategory("en", "lunch", 75);
        verify(repo).findRandomPublishedByLanguageAndCategory("en", "dinner", 75);
        verify(repo).findRandomPublishedByLanguageAndCategory("en", "snack", 75);
        verifyNoMoreInteractions(repo);
        assertEquals(100, result.size());
    }

    @Test
    @DisplayName("findMine should call repo.findByOwner")
    void findMine_should_call_findByOwner() {
        var owner = user(1L, "owner@example.com");
        var mine = recipe("Mine");
        mine.setOwner(owner);
        doReturn(List.of(mine)).when(repo).findByOwner(owner);

        var result = underTest.findMine(owner);

        verify(repo).findByOwner(owner);
        verifyNoMoreInteractions(repo);
        assertEquals(1, result.size());
        assertSame(owner, result.get(0).getOwner());
    }

    @Test
    @DisplayName("create should throw IllegalArgumentException when title is empty")
    void create_should_throw_when_title_empty() {
        var recipe = new Recipe("", "", 0, 0, 0,
                "", "", 0.0, "ing", "instr", false, true);

        var thrown = assertThrows(IllegalArgumentException.class, () -> underTest.create(recipe));

        assertTrue(thrown.getMessage().contains("Title"));
        verifyNoMoreInteractions(repo);
    }

    @Test
    @DisplayName("create with owner should set owner and persist")
    void create_with_owner_should_set_owner_and_persist() {
        var recipe = recipe("Owned Pasta");
        var owner = user(1L, "salma@example.com");

        var result = underTest.create(recipe, owner);

        verify(repo).persist(recipe);
        verifyNoMoreInteractions(repo);
        assertSame(owner, result.getOwner());
    }

    @Test
    @DisplayName("create with owner should preserve private or published state")
    void create_with_owner_should_preserve_publication_state() {
        var owner = user(1L, "owner@example.com");
        var privateRecipe = recipe("Private");
        privateRecipe.setPublished(false);
        privateRecipe.setLanguage("de");
        var publishedRecipe = recipe("Published");
        publishedRecipe.setPublished(true);
        publishedRecipe.setLanguage("en");

        var privateResult = underTest.create(privateRecipe, owner);
        var publishedResult = underTest.create(publishedRecipe, owner);

        assertTrue(!privateResult.isPublished());
        assertTrue(publishedResult.isPublished());
        assertEquals("de", privateResult.getLanguage());
        assertEquals("en", publishedResult.getLanguage());
        verify(repo).persist(privateRecipe);
        verify(repo).persist(publishedRecipe);
        verifyNoMoreInteractions(repo);
    }

    @Test
    @DisplayName("findById should throw when recipe not found")
    void findById_should_throw_when_not_found() {
        Long id = 42L;
        doReturn(null).when(repo).findById(id);

        var thrown = assertThrows(RecipeNotFoundException.class, () -> underTest.findById(id));

        assertTrue(thrown.getMessage().contains("Recipe with ID " + id));
        verify(repo).findById(id);
        verifyNoMoreInteractions(repo);
    }

    @Test
    @DisplayName("findVisibleById should allow published recipe without user")
    void findVisibleById_should_allow_published_recipe_without_user() {
        var published = recipe("Published");
        doReturn(published).when(repo).findById(1L);

        var result = underTest.findVisibleById(1L, null);

        verify(repo).findById(1L);
        verifyNoMoreInteractions(repo);
        assertSame(published, result);
    }

    @Test
    @DisplayName("findVisibleById should hide private recipe without user")
    void findVisibleById_should_hide_private_recipe_without_user() {
        var privateRecipe = recipe("Private");
        privateRecipe.setPublished(false);
        doReturn(privateRecipe).when(repo).findById(1L);

        assertThrows(RecipeNotFoundException.class, () -> underTest.findVisibleById(1L, null));

        verify(repo).findById(1L);
        verifyNoMoreInteractions(repo);
    }

    @Test
    @DisplayName("findVisibleById should allow private recipe for owner")
    void findVisibleById_should_allow_private_recipe_for_owner() {
        var owner = user(1L, "owner@example.com");
        var privateRecipe = recipe("Private");
        privateRecipe.setPublished(false);
        privateRecipe.setOwner(owner);
        doReturn(privateRecipe).when(repo).findById(1L);

        var result = underTest.findVisibleById(1L, owner);

        verify(repo).findById(1L);
        verifyNoMoreInteractions(repo);
        assertSame(privateRecipe, result);
    }

    @Test
    @DisplayName("findVisibleById should hide private recipe from other user")
    void findVisibleById_should_hide_private_recipe_from_other_user() {
        var privateRecipe = recipe("Private");
        privateRecipe.setPublished(false);
        privateRecipe.setOwner(user(1L, "owner@example.com"));
        doReturn(privateRecipe).when(repo).findById(1L);

        assertThrows(RecipeNotFoundException.class, () -> underTest.findVisibleById(1L, user(2L, "other@example.com")));

        verify(repo).findById(1L);
        verifyNoMoreInteractions(repo);
    }

    @Test
    @DisplayName("delete should delegate to repo.deleteById")
    void delete_should_delegate_to_repository() {
        doReturn(true).when(repo).deleteById(5L);

        underTest.delete(5L);

        verify(repo).deleteById(5L);
        verifyNoMoreInteractions(repo);
    }

    @Test
    @DisplayName("delete with owner should allow owner")
    void delete_with_owner_should_allow_owner() {
        var owner = user(1L, "owner@example.com");
        var existing = recipe("Owned");
        existing.setOwner(owner);
        doReturn(existing).when(repo).findById(1L);

        underTest.delete(1L, owner);

        verify(repo).findById(1L);
        verify(repo).delete(existing);
        verifyNoMoreInteractions(repo);
    }

    @Test
    @DisplayName("delete with owner should reject different user")
    void delete_with_owner_should_reject_different_user() {
        var existing = recipe("Owned");
        existing.setOwner(user(1L, "owner@example.com"));
        doReturn(existing).when(repo).findById(1L);

        assertThrows(ForbiddenException.class, () -> underTest.delete(1L, user(2L, "other@example.com")));

        verify(repo).findById(1L);
        verifyNoMoreInteractions(repo);
    }

    @Test
    @DisplayName("delete with owner should reject legacy recipe without owner")
    void delete_with_owner_should_reject_legacy_recipe_without_owner() {
        doReturn(recipe("Legacy")).when(repo).findById(1L);

        assertThrows(ForbiddenException.class, () -> underTest.delete(1L, user(1L, "owner@example.com")));

        verify(repo).findById(1L);
        verifyNoMoreInteractions(repo);
    }

    @Test
    @DisplayName("update should copy fields and return managed entity")
    void update_should_copy_fields_and_return_managed_entity() {
        var existing = recipe("Old");
        doReturn(existing).when(repo).findById(1L);
        var updated = new Recipe("New", "", 10, 20, 2,
                "medium", "NewCat", 4.0, "new", "new", true, true);

        var result = underTest.update(1L, updated);

        verify(repo).findById(1L);
        verifyNoMoreInteractions(repo);
        assertSame(existing, result);
        assertEquals("New", result.getTitle());
        assertTrue(result.isFavorite());
        assertTrue(result.isPublished());
    }

    @Test
    @DisplayName("update with owner should allow owner")
    void update_with_owner_should_allow_owner() {
        var owner = user(1L, "owner@example.com");
        var existing = recipe("Old");
        existing.setOwner(owner);
        doReturn(existing).when(repo).findById(1L);

        var result = underTest.update(1L, recipe("New"), owner);

        verify(repo).findById(1L);
        verifyNoMoreInteractions(repo);
        assertSame(existing, result);
        assertEquals("New", result.getTitle());
    }

    @Test
    @DisplayName("update with owner should reject different user")
    void update_with_owner_should_reject_different_user() {
        var existing = recipe("Old");
        existing.setOwner(user(1L, "owner@example.com"));
        doReturn(existing).when(repo).findById(1L);

        assertThrows(ForbiddenException.class, () -> underTest.update(1L, recipe("New"), user(2L, "other@example.com")));

        verify(repo).findById(1L);
        verifyNoMoreInteractions(repo);
    }

    @Test
    @DisplayName("update with owner should reject legacy recipe without owner")
    void update_with_owner_should_reject_legacy_recipe_without_owner() {
        doReturn(recipe("Old")).when(repo).findById(1L);

        assertThrows(ForbiddenException.class, () -> underTest.update(1L, recipe("New"), user(1L, "owner@example.com")));

        verify(repo).findById(1L);
        verifyNoMoreInteractions(repo);
    }

    private Recipe recipe(String title) {
        return new Recipe(title, "", 10, 20, 2,
                "easy", "Italian", 4.5,
                "noodles", "cook", false, true);
    }

    private List<Recipe> numberedRecipes(String prefix, int firstId, int count) {
        List<Recipe> recipes = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            Recipe recipe = recipe(prefix + " " + i);
            recipe.setId((long) firstId + i);
            recipes.add(recipe);
        }
        return recipes;
    }

    private AppUser user(Long id, String email) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUsername("salma");
        user.setEmail(email);
        user.setPasswordHash("hash");
        return user;
    }
}
