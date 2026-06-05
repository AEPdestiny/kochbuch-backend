package de.htwberlin.webtech.recipe;

import de.htwberlin.webtech.recipe.entity.Recipe;
import de.htwberlin.webtech.recipe.repository.RecipeRepository;
import de.htwberlin.webtech.user.entity.AppUser;
import de.htwberlin.webtech.user.entity.Role;
import de.htwberlin.webtech.user.repository.AppUserRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestProfile(PostgresDevServicesTestProfile.class)
class RecipeRepositoryIntegrationTest {

    @Inject
    RecipeRepository repository;

    @Inject
    AppUserRepository userRepository;

    @Test
    @TestTransaction
    void should_save_recipe() {
        Recipe recipe = recipe("Saved Pasta", true);

        repository.persistAndFlush(recipe);

        assertNotNull(recipe.getId());
    }

    @Test
    @TestTransaction
    void should_find_recipe_by_id() {
        Recipe recipe = recipe("Readable Pasta", true);
        repository.persistAndFlush(recipe);

        Recipe found = repository.findById(recipe.getId());

        assertNotNull(found);
        assertEquals("Readable Pasta", found.getTitle());
    }

    @Test
    @TestTransaction
    void should_filter_published_recipes() {
        repository.persist(recipe("Published Pasta", true));
        repository.persist(recipe("Private Soup", false));
        repository.flush();

        var published = repository.findPublished();

        assertEquals(1, published.size());
        assertEquals("Published Pasta", published.get(0).getTitle());
        assertTrue(published.get(0).isPublished());
    }

    @Test
    @TestTransaction
    void should_delete_recipe() {
        Recipe recipe = recipe("Delete Me", true);
        repository.persistAndFlush(recipe);
        Long id = recipe.getId();

        boolean deleted = repository.deleteById(id);
        repository.flush();

        assertTrue(deleted);
        assertNull(repository.findById(id));
    }

    @Test
    @TestTransaction
    void should_save_and_read_recipe_with_owner() {
        AppUser owner = user("recipe-owner", "recipe-owner@example.com");
        userRepository.persistAndFlush(owner);
        Recipe recipe = recipe("Owned Recipe", true);
        recipe.setOwner(owner);

        repository.persistAndFlush(recipe);
        Recipe found = repository.findById(recipe.getId());

        assertNotNull(found);
        assertNotNull(found.getOwner());
        assertEquals("recipe-owner@example.com", found.getOwner().getEmail());
    }

    private Recipe recipe(String title, boolean published) {
        return new Recipe(
                title,
                "",
                10,
                20,
                2,
                "easy",
                "Italian",
                4.5,
                "noodles",
                "cook",
                false,
                published
        );
    }

    private AppUser user(String username, String email) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash("$2a$10$123456789012345678901uPj6z5oE0l3m6QmS1uJ9p4K7yJrB9xjG");
        user.setRole(Role.USER);
        return user;
    }
}
