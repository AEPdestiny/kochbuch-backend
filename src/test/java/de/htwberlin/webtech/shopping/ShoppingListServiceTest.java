package de.htwberlin.webtech.shopping;

import de.htwberlin.webtech.shared.exception.ForbiddenException;
import de.htwberlin.webtech.shopping.dto.ShoppingListItemRequest;
import de.htwberlin.webtech.shopping.entity.ShoppingListItem;
import de.htwberlin.webtech.shopping.exception.ShoppingListItemNotFoundException;
import de.htwberlin.webtech.shopping.mapper.ShoppingListItemMapper;
import de.htwberlin.webtech.shopping.repository.ShoppingListItemRepository;
import de.htwberlin.webtech.shopping.service.ShoppingListService;
import de.htwberlin.webtech.user.entity.AppUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class ShoppingListServiceTest {

    private final ShoppingListItemRepository repo = mock(ShoppingListItemRepository.class);
    private final ShoppingListService underTest = new ShoppingListService(repo, new ShoppingListItemMapper());

    @Test
    @DisplayName("listMine should delegate to repository")
    void listMine_should_delegate_to_repository() {
        AppUser owner = user(1L);
        doReturn(List.of(item("Tomatoes", owner))).when(repo).findByOwner(owner);

        var result = underTest.listMine(owner);

        verify(repo).findByOwner(owner);
        verifyNoMoreInteractions(repo);
        assertEquals(1, result.size());
        assertSame(owner, result.get(0).getOwner());
    }

    @Test
    @DisplayName("create should set owner and persist")
    void create_should_set_owner_and_persist() {
        AppUser owner = user(1L);
        ShoppingListItemRequest request = request("Tomatoes", false);

        ShoppingListItem result = underTest.create(request, owner);

        verify(repo).persist(result);
        verifyNoMoreInteractions(repo);
        assertSame(owner, result.getOwner());
        assertEquals("Tomatoes", result.getName());
    }

    @Test
    @DisplayName("update should allow owner and update checked")
    void update_should_allow_owner_and_update_checked() {
        AppUser owner = user(1L);
        ShoppingListItem existing = item("Tomatoes", owner);
        doReturn(existing).when(repo).findById(1L);

        ShoppingListItem result = underTest.update(1L, request("Cherry Tomatoes", true), owner);

        verify(repo).findById(1L);
        verifyNoMoreInteractions(repo);
        assertSame(existing, result);
        assertEquals("Cherry Tomatoes", result.getName());
        assertTrue(result.isChecked());
    }

    @Test
    @DisplayName("update should reject different owner")
    void update_should_reject_different_owner() {
        ShoppingListItem existing = item("Tomatoes", user(1L));
        doReturn(existing).when(repo).findById(1L);

        assertThrows(ForbiddenException.class, () -> underTest.update(1L, request("Cherry Tomatoes", false), user(2L)));

        verify(repo).findById(1L);
        verifyNoMoreInteractions(repo);
    }

    @Test
    @DisplayName("update should throw not found for missing item")
    void update_should_throw_not_found_for_missing_item() {
        doReturn(null).when(repo).findById(99L);

        assertThrows(ShoppingListItemNotFoundException.class, () -> underTest.update(99L, request("Tomatoes", false), user(1L)));

        verify(repo).findById(99L);
        verifyNoMoreInteractions(repo);
    }

    @Test
    @DisplayName("delete should allow owner")
    void delete_should_allow_owner() {
        AppUser owner = user(1L);
        ShoppingListItem existing = item("Tomatoes", owner);
        doReturn(existing).when(repo).findById(1L);

        underTest.delete(1L, owner);

        verify(repo).findById(1L);
        verify(repo).delete(existing);
        verifyNoMoreInteractions(repo);
    }

    @Test
    @DisplayName("delete should reject different owner")
    void delete_should_reject_different_owner() {
        ShoppingListItem existing = item("Tomatoes", user(1L));
        doReturn(existing).when(repo).findById(1L);

        assertThrows(ForbiddenException.class, () -> underTest.delete(1L, user(2L)));

        verify(repo).findById(1L);
        verifyNoMoreInteractions(repo);
    }

    private ShoppingListItemRequest request(String name, boolean checked) {
        ShoppingListItemRequest request = new ShoppingListItemRequest();
        request.setName(name);
        request.setQuantity(BigDecimal.valueOf(3));
        request.setUnit("piece");
        request.setCategory("Vegetables");
        request.setChecked(checked);
        return request;
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

    private AppUser user(Long id) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUsername("user-" + id);
        user.setEmail("user-" + id + "@example.com");
        user.setPasswordHash("hash");
        return user;
    }
}
