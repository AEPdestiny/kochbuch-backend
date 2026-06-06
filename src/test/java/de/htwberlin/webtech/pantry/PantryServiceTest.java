package de.htwberlin.webtech.pantry;

import de.htwberlin.webtech.pantry.dto.PantryItemRequest;
import de.htwberlin.webtech.pantry.entity.PantryItem;
import de.htwberlin.webtech.pantry.exception.PantryItemNotFoundException;
import de.htwberlin.webtech.pantry.mapper.PantryItemMapper;
import de.htwberlin.webtech.pantry.repository.PantryItemRepository;
import de.htwberlin.webtech.pantry.service.PantryService;
import de.htwberlin.webtech.shared.exception.ForbiddenException;
import de.htwberlin.webtech.user.entity.AppUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class PantryServiceTest {

    private final PantryItemRepository repo = mock(PantryItemRepository.class);
    private final PantryService underTest = new PantryService(repo, new PantryItemMapper());

    @Test
    @DisplayName("listMine should delegate to repository")
    void listMine_should_delegate_to_repository() {
        AppUser owner = user(1L);
        doReturn(List.of(item("Rice", owner))).when(repo).findByOwner(owner);

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
        PantryItemRequest request = request("Rice");

        PantryItem result = underTest.create(request, owner);

        verify(repo).persist(result);
        verifyNoMoreInteractions(repo);
        assertSame(owner, result.getOwner());
        assertEquals("Rice", result.getName());
    }

    @Test
    @DisplayName("update should allow owner")
    void update_should_allow_owner() {
        AppUser owner = user(1L);
        PantryItem existing = item("Rice", owner);
        doReturn(existing).when(repo).findById(1L);

        PantryItem result = underTest.update(1L, request("Beans"), owner);

        verify(repo).findById(1L);
        verifyNoMoreInteractions(repo);
        assertSame(existing, result);
        assertEquals("Beans", result.getName());
    }

    @Test
    @DisplayName("update should reject different owner")
    void update_should_reject_different_owner() {
        PantryItem existing = item("Rice", user(1L));
        doReturn(existing).when(repo).findById(1L);

        assertThrows(ForbiddenException.class, () -> underTest.update(1L, request("Beans"), user(2L)));

        verify(repo).findById(1L);
        verifyNoMoreInteractions(repo);
    }

    @Test
    @DisplayName("update should throw not found for missing item")
    void update_should_throw_not_found_for_missing_item() {
        doReturn(null).when(repo).findById(99L);

        assertThrows(PantryItemNotFoundException.class, () -> underTest.update(99L, request("Beans"), user(1L)));

        verify(repo).findById(99L);
        verifyNoMoreInteractions(repo);
    }

    @Test
    @DisplayName("delete should allow owner")
    void delete_should_allow_owner() {
        AppUser owner = user(1L);
        PantryItem existing = item("Rice", owner);
        doReturn(existing).when(repo).findById(1L);

        underTest.delete(1L, owner);

        verify(repo).findById(1L);
        verify(repo).delete(existing);
        verifyNoMoreInteractions(repo);
    }

    @Test
    @DisplayName("delete should reject different owner")
    void delete_should_reject_different_owner() {
        PantryItem existing = item("Rice", user(1L));
        doReturn(existing).when(repo).findById(1L);

        assertThrows(ForbiddenException.class, () -> underTest.delete(1L, user(2L)));

        verify(repo).findById(1L);
        verifyNoMoreInteractions(repo);
    }

    private PantryItemRequest request(String name) {
        PantryItemRequest request = new PantryItemRequest();
        request.setName(name);
        request.setQuantity(BigDecimal.valueOf(2));
        request.setUnit("kg");
        request.setCategory("Grains");
        return request;
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

    private AppUser user(Long id) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUsername("user-" + id);
        user.setEmail("user-" + id + "@example.com");
        user.setPasswordHash("hash");
        return user;
    }
}
