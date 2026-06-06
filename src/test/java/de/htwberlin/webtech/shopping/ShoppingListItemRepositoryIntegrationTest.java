package de.htwberlin.webtech.shopping;

import de.htwberlin.webtech.recipe.PostgresDevServicesTestProfile;
import de.htwberlin.webtech.shopping.entity.ShoppingListItem;
import de.htwberlin.webtech.shopping.repository.ShoppingListItemRepository;
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
class ShoppingListItemRepositoryIntegrationTest {

    @Inject
    ShoppingListItemRepository repository;

    @Inject
    AppUserRepository userRepository;

    @Test
    @TestTransaction
    void should_save_shopping_list_item() {
        AppUser owner = user("shopping-save-owner", "shopping-save-owner@example.com");
        userRepository.persistAndFlush(owner);
        ShoppingListItem item = item("Tomatoes", owner);

        repository.persistAndFlush(item);

        assertNotNull(item.getId());
        assertNotNull(item.getCreatedAt());
        assertNotNull(item.getUpdatedAt());
    }

    @Test
    @TestTransaction
    void should_find_shopping_list_items_by_owner() {
        AppUser owner = user("shopping-owner", "shopping-owner@example.com");
        AppUser other = user("shopping-other", "shopping-other@example.com");
        userRepository.persist(owner);
        userRepository.persist(other);

        repository.persist(item("Tomatoes", owner));
        repository.persist(item("Milk", other));
        repository.flush();

        var result = repository.findByOwner(owner);

        assertEquals(1, result.size());
        assertEquals("Tomatoes", result.get(0).getName());
        assertEquals(owner.getEmail(), result.get(0).getOwner().getEmail());
    }

    @Test
    @TestTransaction
    void should_update_checked_state() {
        AppUser owner = user("shopping-update-owner", "shopping-update-owner@example.com");
        userRepository.persistAndFlush(owner);
        ShoppingListItem item = item("Tomatoes", owner);
        repository.persistAndFlush(item);

        item.setChecked(true);
        repository.flush();

        ShoppingListItem found = repository.findById(item.getId());
        assertTrue(found.isChecked());
    }

    @Test
    @TestTransaction
    void should_delete_shopping_list_item() {
        AppUser owner = user("shopping-delete-owner", "shopping-delete-owner@example.com");
        userRepository.persistAndFlush(owner);
        ShoppingListItem item = item("Delete Me", owner);
        repository.persistAndFlush(item);
        Long id = item.getId();

        boolean deleted = repository.deleteById(id);
        repository.flush();

        assertTrue(deleted);
        assertNull(repository.findById(id));
    }

    private ShoppingListItem item(String name, AppUser owner) {
        ShoppingListItem item = new ShoppingListItem();
        item.setName(name);
        item.setQuantity(BigDecimal.valueOf(3));
        item.setUnit("piece");
        item.setCategory("Vegetables");
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
