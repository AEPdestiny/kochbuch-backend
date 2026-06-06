package de.htwberlin.webtech.pantry;

import de.htwberlin.webtech.pantry.entity.PantryItem;
import de.htwberlin.webtech.pantry.repository.PantryItemRepository;
import de.htwberlin.webtech.recipe.PostgresDevServicesTestProfile;
import de.htwberlin.webtech.user.entity.AppUser;
import de.htwberlin.webtech.user.entity.Role;
import de.htwberlin.webtech.user.repository.AppUserRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestProfile(PostgresDevServicesTestProfile.class)
class PantryItemRepositoryIntegrationTest {

    @Inject
    PantryItemRepository repository;

    @Inject
    AppUserRepository userRepository;

    @Test
    @TestTransaction
    void should_save_pantry_item() {
        AppUser owner = user("pantry-save-owner", "pantry-save-owner@example.com");
        userRepository.persistAndFlush(owner);
        PantryItem item = item("Rice", owner);

        repository.persistAndFlush(item);

        assertNotNull(item.getId());
        assertNotNull(item.getCreatedAt());
        assertNotNull(item.getUpdatedAt());
    }

    @Test
    @TestTransaction
    void should_find_pantry_items_by_owner() {
        AppUser owner = user("pantry-owner", "pantry-owner@example.com");
        AppUser other = user("pantry-other", "pantry-other@example.com");
        userRepository.persist(owner);
        userRepository.persist(other);

        repository.persist(item("Rice", owner));
        repository.persist(item("Milk", other));
        repository.flush();

        var result = repository.findByOwner(owner);

        assertEquals(1, result.size());
        assertEquals("Rice", result.get(0).getName());
        assertEquals(owner.getEmail(), result.get(0).getOwner().getEmail());
    }

    @Test
    @TestTransaction
    void should_delete_pantry_item() {
        AppUser owner = user("pantry-delete-owner", "pantry-delete-owner@example.com");
        userRepository.persistAndFlush(owner);
        PantryItem item = item("Delete Me", owner);
        repository.persistAndFlush(item);
        Long id = item.getId();

        boolean deleted = repository.deleteById(id);
        repository.flush();

        assertTrue(deleted);
        assertNull(repository.findById(id));
    }

    private PantryItem item(String name, AppUser owner) {
        PantryItem item = new PantryItem();
        item.setName(name);
        item.setQuantity(BigDecimal.valueOf(2));
        item.setUnit("kg");
        item.setCategory("Grains");
        item.setOwner(owner);
        return item;
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
