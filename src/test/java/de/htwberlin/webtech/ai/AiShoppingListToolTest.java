package de.htwberlin.webtech.ai;

import de.htwberlin.webtech.ai.tools.AiShoppingListTool;
import de.htwberlin.webtech.ai.tools.AiShoppingListToolResult;
import de.htwberlin.webtech.pantry.entity.PantryItem;
import de.htwberlin.webtech.pantry.repository.PantryItemRepository;
import de.htwberlin.webtech.shopping.dto.ShoppingListItemRequest;
import de.htwberlin.webtech.shopping.entity.ShoppingListItem;
import de.htwberlin.webtech.shopping.service.ShoppingListService;
import de.htwberlin.webtech.user.entity.AppUser;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AiShoppingListToolTest {

    private final ShoppingListService shoppingListService = mock(ShoppingListService.class);
    private final PantryItemRepository pantryItemRepository = mock(PantryItemRepository.class);
    private final AiShoppingListTool underTest = new AiShoppingListTool(shoppingListService, pantryItemRepository);

    @Test
    void addMissingIngredients_should_add_only_missing_items() {
        AppUser user = user();
        doReturn(List.of()).when(shoppingListService).listMine(user);
        doReturn(List.of()).when(pantryItemRepository).findByOwner(user);

        AiShoppingListToolResult result = underTest.addMissingIngredients(user, List.of("Limette", "Reis"));

        assertEquals(List.of("Limette", "Reis"), result.addedItems());
        ArgumentCaptor<ShoppingListItemRequest> requestCaptor = ArgumentCaptor.forClass(ShoppingListItemRequest.class);
        verify(shoppingListService, times(2)).create(requestCaptor.capture(), eq(user));
        assertEquals(List.of("Limette", "Reis"), requestCaptor.getAllValues().stream().map(ShoppingListItemRequest::getName).toList());
    }

    @Test
    void addMissingIngredients_should_skip_items_already_in_pantry() {
        AppUser user = user();
        doReturn(List.of()).when(shoppingListService).listMine(user);
        doReturn(List.of(pantryItem("Avocado"))).when(pantryItemRepository).findByOwner(user);

        AiShoppingListToolResult result = underTest.addMissingIngredients(user, List.of("Avocado", "Limette"));

        assertEquals(List.of("Limette"), result.addedItems());
        assertEquals(List.of("Avocado"), result.skippedPantryItems());
        verify(shoppingListService).create(any(ShoppingListItemRequest.class), eq(user));
    }

    @Test
    void addMissingIngredients_should_skip_items_already_on_shopping_list() {
        AppUser user = user();
        doReturn(List.of(shoppingItem("Limette"))).when(shoppingListService).listMine(user);
        doReturn(List.of()).when(pantryItemRepository).findByOwner(user);

        AiShoppingListToolResult result = underTest.addMissingIngredients(user, List.of("Limette", "Reis"));

        assertEquals(List.of("Reis"), result.addedItems());
        assertEquals(List.of("Limette"), result.skippedShoppingListItems());
        verify(shoppingListService).create(any(ShoppingListItemRequest.class), eq(user));
    }

    @Test
    void addMissingIngredients_should_not_add_duplicate_ingredient_names_from_same_request() {
        AppUser user = user();
        doReturn(List.of()).when(shoppingListService).listMine(user);
        doReturn(List.of()).when(pantryItemRepository).findByOwner(user);

        AiShoppingListToolResult result = underTest.addMissingIngredients(user, List.of("Limette", "Limette"));

        assertEquals(List.of("Limette"), result.addedItems());
        verify(shoppingListService, times(1)).create(any(ShoppingListItemRequest.class), eq(user));
    }

    @Test
    void addMissingIngredients_should_report_no_change_when_everything_is_skipped() {
        AppUser user = user();
        doReturn(List.of(shoppingItem("Limette"))).when(shoppingListService).listMine(user);
        doReturn(List.of(pantryItem("Reis"))).when(pantryItemRepository).findByOwner(user);

        AiShoppingListToolResult result = underTest.addMissingIngredients(user, List.of("Limette", "Reis"));

        assertTrue(result.addedItems().isEmpty());
        assertEquals(List.of("Reis"), result.skippedPantryItems());
        assertEquals(List.of("Limette"), result.skippedShoppingListItems());
    }

    @Test
    void addMissingIngredients_should_normalize_quantities_preparation_notes_and_plural_for_pantry_skip() {
        AppUser user = user();
        doReturn(List.of()).when(shoppingListService).listMine(user);
        doReturn(List.of(pantryItem("Ei"), pantryItem("Rapsöl"))).when(pantryItemRepository).findByOwner(user);

        AiShoppingListToolResult result = underTest.addMissingIngredients(user, List.of(
                "2 Eier",
                "1 EL Rapsöl zum Braten",
                "Salz",
                "Pfeffer nach Geschmack"
        ));

        assertEquals(List.of("Salz", "Pfeffer"), result.addedItems());
        assertEquals(List.of("Eier", "Rapsöl"), result.skippedPantryItems());
        ArgumentCaptor<ShoppingListItemRequest> requestCaptor = ArgumentCaptor.forClass(ShoppingListItemRequest.class);
        verify(shoppingListService, times(2)).create(requestCaptor.capture(), eq(user));
        assertEquals(List.of("Salz", "Pfeffer"), requestCaptor.getAllValues().stream().map(ShoppingListItemRequest::getName).toList());
    }

    @Test
    void addMissingIngredients_should_reject_instruction_fragments() {
        AppUser user = user();
        doReturn(List.of()).when(shoppingListService).listMine(user);
        doReturn(List.of()).when(pantryItemRepository).findByOwner(user);

        AiShoppingListToolResult result = underTest.addMissingIngredients(user, List.of(
                "Zutaten: Milchreis",
                "koennen Sie den Milchreis mit Milch",
                "einem wenig Zucker oder Suessstoff kochen",
                "Scheiben Brot. Du hast bereits Eier",
                "Kartoffeln. Du hast bereits Eier",
                "Rapsoel im Vorrat",
                "aber noch keine Kartoffeln",
                "bitte in die Einkaufsliste",
                "Salz"
        ));

        assertEquals(List.of("Milchreis", "Salz"), result.addedItems());
        ArgumentCaptor<ShoppingListItemRequest> requestCaptor = ArgumentCaptor.forClass(ShoppingListItemRequest.class);
        verify(shoppingListService, times(2)).create(requestCaptor.capture(), eq(user));
        assertEquals(List.of("Milchreis", "Salz"), requestCaptor.getAllValues().stream().map(ShoppingListItemRequest::getName).toList());
    }

    @Test
    void addMissingIngredients_should_reject_meal_plan_fragments() {
        AppUser user = user();
        doReturn(List.of()).when(shoppingListService).listMine(user);
        doReturn(List.of()).when(pantryItemRepository).findByOwner(user);

        AiShoppingListToolResult result = underTest.addMissingIngredients(user, List.of(
                "es fuer sonntag abend",
                "das morgen abend",
                "fuer sonntag abend",
                "zum Wochenplan",
                "fuege es hinzu",
                "Salz"
        ));

        assertEquals(List.of("Salz"), result.addedItems());
        ArgumentCaptor<ShoppingListItemRequest> requestCaptor = ArgumentCaptor.forClass(ShoppingListItemRequest.class);
        verify(shoppingListService, times(1)).create(requestCaptor.capture(), eq(user));
        assertEquals("Salz", requestCaptor.getValue().getName());
    }

    @Test
    void addMissingIngredients_should_store_fraction_quantities_units_and_clean_names() {
        AppUser user = user();
        doReturn(List.of()).when(shoppingListService).listMine(user);
        doReturn(List.of()).when(pantryItemRepository).findByOwner(user);

        AiShoppingListToolResult result = underTest.addMissingIngredients(user, List.of(
                "1/2 Schalotte",
                "1/2 TL Knoblauch",
                "1/2 EL Basilikum",
                "8 g Spinat",
                "2 Pilze",
                "etwas Olivenöl nach Bedarf"
        ));

        assertEquals(List.of("Schalotte", "Knoblauch", "Basilikum", "Spinat", "Pilze", "Olivenöl"), result.addedItems());
        ArgumentCaptor<ShoppingListItemRequest> requestCaptor = ArgumentCaptor.forClass(ShoppingListItemRequest.class);
        verify(shoppingListService, times(6)).create(requestCaptor.capture(), eq(user));
        List<ShoppingListItemRequest> requests = requestCaptor.getAllValues();

        assertRequest(requests.get(0), "Schalotte", new BigDecimal("0.5"), null);
        assertRequest(requests.get(1), "Knoblauch", new BigDecimal("0.5"), "TL");
        assertRequest(requests.get(2), "Basilikum", new BigDecimal("0.5"), "EL");
        assertRequest(requests.get(3), "Spinat", new BigDecimal("8"), "g");
        assertRequest(requests.get(4), "Pilze", new BigDecimal("2"), null);
        assertRequest(requests.get(5), "Olivenöl", null, null);
    }

    @Test
    void addMissingIngredients_should_parse_full_real_world_quantity_case_without_broken_names() {
        AppUser user = user();
        doReturn(List.of()).when(shoppingListService).listMine(user);
        doReturn(List.of(pantryItem("Ei"))).when(pantryItemRepository).findByOwner(user);

        AiShoppingListToolResult result = underTest.addMissingIngredients(user, List.of(
                "1/2 Schalotte",
                "1/2 TL Knoblauch",
                "2 Pilze",
                "4 Cherrytomaten",
                "1/2 EL Basilikum",
                "8 g Spinat",
                "etwas Olivenöl nach Bedarf"
        ));

        assertEquals(List.of("Schalotte", "Knoblauch", "Pilze", "Cherrytomaten", "Basilikum", "Spinat", "Olivenöl"), result.addedItems());
        ArgumentCaptor<ShoppingListItemRequest> requestCaptor = ArgumentCaptor.forClass(ShoppingListItemRequest.class);
        verify(shoppingListService, times(7)).create(requestCaptor.capture(), eq(user));
        List<ShoppingListItemRequest> requests = requestCaptor.getAllValues();

        assertEquals(List.of("Schalotte", "Knoblauch", "Pilze", "Cherrytomaten", "Basilikum", "Spinat", "Olivenöl"),
                requests.stream().map(ShoppingListItemRequest::getName).toList());
        assertTrue(requests.stream().map(ShoppingListItemRequest::getName).noneMatch(name ->
                name.contains("/2") || name.contains("TL") || name.contains("EL") || name.contains("nach Bedarf") || name.contains("etwas")));
        assertRequest(requests.get(0), "Schalotte", new BigDecimal("0.5"), null);
        assertRequest(requests.get(1), "Knoblauch", new BigDecimal("0.5"), "TL");
        assertRequest(requests.get(2), "Pilze", new BigDecimal("2"), null);
        assertRequest(requests.get(3), "Cherrytomaten", new BigDecimal("4"), null);
        assertRequest(requests.get(4), "Basilikum", new BigDecimal("0.5"), "EL");
        assertRequest(requests.get(5), "Spinat", new BigDecimal("8"), "g");
        assertRequest(requests.get(6), "Olivenöl", null, null);
    }

    private ShoppingListItem shoppingItem(String name) {
        ShoppingListItem item = new ShoppingListItem();
        item.setName(name);
        return item;
    }

    private PantryItem pantryItem(String name) {
        PantryItem item = new PantryItem();
        item.setName(name);
        return item;
    }

    private AppUser user() {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setUsername("produuser");
        user.setEmail("user@example.com");
        user.setPasswordHash("hash");
        return user;
    }

    private void assertRequest(ShoppingListItemRequest request, String name, BigDecimal quantity, String unit) {
        assertEquals(name, request.getName());
        if (quantity == null) {
            assertNull(request.getQuantity());
        } else {
            assertEquals(0, quantity.compareTo(request.getQuantity()));
        }
        assertEquals(unit, request.getUnit());
    }
}
