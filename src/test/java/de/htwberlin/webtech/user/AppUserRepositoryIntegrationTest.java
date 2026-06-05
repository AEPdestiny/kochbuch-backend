package de.htwberlin.webtech.user;

import de.htwberlin.webtech.recipe.PostgresDevServicesTestProfile;
import de.htwberlin.webtech.user.entity.AppUser;
import de.htwberlin.webtech.user.entity.Role;
import de.htwberlin.webtech.user.repository.AppUserRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestProfile(PostgresDevServicesTestProfile.class)
class AppUserRepositoryIntegrationTest {

    @Inject
    AppUserRepository repository;

    @Test
    @TestTransaction
    void should_save_app_user() {
        AppUser user = user("save-user", "save-user@example.com");

        repository.persistAndFlush(user);

        assertNotNull(user.getId());
        assertNotNull(user.getCreatedAt());
        assertEquals(Role.USER, user.getRole());
    }

    @Test
    @TestTransaction
    void should_find_app_user_by_email() {
        AppUser user = user("email-user", "email-user@example.com");
        repository.persistAndFlush(user);

        var found = repository.findByEmail("email-user@example.com");

        assertTrue(found.isPresent());
        assertEquals("email-user", found.get().getUsername());
    }

    @Test
    @TestTransaction
    void should_find_app_user_by_username() {
        AppUser user = user("username-user", "username-user@example.com");
        repository.persistAndFlush(user);

        var found = repository.findByUsername("username-user");

        assertTrue(found.isPresent());
        assertEquals("username-user@example.com", found.get().getEmail());
    }

    @Test
    @TestTransaction
    void should_reject_duplicate_email() {
        repository.persistAndFlush(user("first-email-user", "duplicate-email@example.com"));

        AppUser duplicate = user("second-email-user", "duplicate-email@example.com");

        assertThrows(PersistenceException.class, () -> repository.persistAndFlush(duplicate));
    }

    @Test
    @TestTransaction
    void should_reject_duplicate_username() {
        repository.persistAndFlush(user("duplicate-username", "first-username@example.com"));

        AppUser duplicate = user("duplicate-username", "second-username@example.com");

        assertThrows(PersistenceException.class, () -> repository.persistAndFlush(duplicate));
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
