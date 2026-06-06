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
        doReturn(List.of(r1, r2)).when(repo).findPublished();

        var result = underTest.findAll();

        verify(repo).findPublished();
        verifyNoMoreInteractions(repo);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("findAllPublished should call repo.findPublished")
    void findAllPublished_should_call_findPublished() {
        var published = recipe("Cake");
        published.setPublished(true);
        doReturn(List.of(published)).when(repo).findPublished();

        var result = underTest.findAllPublished();

        verify(repo).findPublished();
        verifyNoMoreInteractions(repo);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isPublished());
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

    private AppUser user(Long id, String email) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUsername("salma");
        user.setEmail(email);
        user.setPasswordHash("hash");
        return user;
    }
}
