package de.htwberlin.webtech.mealplan;

import de.htwberlin.webtech.mealplan.dto.MealPlanShoppingListResponse;
import de.htwberlin.webtech.mealplan.entity.MealPlan;
import de.htwberlin.webtech.mealplan.entity.MealSlot;
import de.htwberlin.webtech.mealplan.repository.MealPlanRepository;
import de.htwberlin.webtech.mealplan.service.MealPlanShoppingListService;
import de.htwberlin.webtech.pantry.entity.PantryItem;
import de.htwberlin.webtech.pantry.repository.PantryItemRepository;
import de.htwberlin.webtech.recipe.entity.Recipe;
import de.htwberlin.webtech.shopping.entity.ShoppingListItem;
import de.htwberlin.webtech.shopping.repository.ShoppingListItemRepository;
import de.htwberlin.webtech.user.entity.AppUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

class MealPlanShoppingListServiceTest {

    private final MealPlanRepository mealPlanRepository = mock(MealPlanRepository.class);
    private final PantryItemRepository pantryItemRepository = mock(PantryItemRepository.class);
    private final ShoppingListItemRepository shoppingListItemRepository = mock(ShoppingListItemRepository.class);
    private final MealPlanShoppingListService underTest = new MealPlanShoppingListService(
            mealPlanRepository,
            pantryItemRepository,
            shoppingListItemRepository
    );

    @Test
    @DisplayName("createShoppingList should add missing remainder after pantry quantity")
    void createShoppingList_should_add_missing_remainder_after_pantry_quantity() {
        AppUser owner = user(1L);
        LocalDate weekStart = LocalDate.of(2026, 6, 1);
        doReturn(List.of(mealPlan(owner, recipe("Tomato Pasta", "500 g Tomaten"))))
                .when(mealPlanRepository).findByOwnerAndPlannedDateBetween(owner, weekStart, weekStart.plusDays(6));
        doReturn(List.of(pantry("Tomaten", BigDecimal.valueOf(300), "g", owner))).when(pantryItemRepository).findByOwner(owner);
        doReturn(List.of()).when(shoppingListItemRepository).findByOwner(owner);

        MealPlanShoppingListResponse result = underTest.createShoppingList(owner, weekStart);

        ArgumentCaptor<ShoppingListItem> captor = ArgumentCaptor.forClass(ShoppingListItem.class);
        verify(shoppingListItemRepository).persist(captor.capture());
        assertEquals(1, result.getAdded().size());
        assertEquals("Tomaten", captor.getValue().getName());
        assertEquals(0, BigDecimal.valueOf(200).compareTo(captor.getValue().getQuantity()));
        assertEquals("g", captor.getValue().getUnit());
    }

    @Test
    @DisplayName("createShoppingList should skip ingredient fully covered by pantry")
    void createShoppingList_should_skip_covered_ingredient() {
        AppUser owner = user(1L);
        LocalDate weekStart = LocalDate.of(2026, 6, 1);
        doReturn(List.of(mealPlan(owner, recipe("Omelette", "2 Eier"))))
                .when(mealPlanRepository).findByOwnerAndPlannedDateBetween(owner, weekStart, weekStart.plusDays(6));
        doReturn(List.of(pantry("Eier", BigDecimal.valueOf(6), "Stück", owner))).when(pantryItemRepository).findByOwner(owner);
        doReturn(List.of()).when(shoppingListItemRepository).findByOwner(owner);

        MealPlanShoppingListResponse result = underTest.createShoppingList(owner, weekStart);

        assertEquals(1, result.getSkippedBecauseInPantry().size());
        verify(shoppingListItemRepository, never()).persist(org.mockito.ArgumentMatchers.any(ShoppingListItem.class));
    }

    @Test
    @DisplayName("createShoppingList should put unsafe unit comparison into review")
    void createShoppingList_should_review_unsafe_unit_comparison() {
        AppUser owner = user(1L);
        LocalDate weekStart = LocalDate.of(2026, 6, 1);
        doReturn(List.of(mealPlan(owner, recipe("Butter Sauce", "1 EL Butter"))))
                .when(mealPlanRepository).findByOwnerAndPlannedDateBetween(owner, weekStart, weekStart.plusDays(6));
        doReturn(List.of(pantry("Butter", BigDecimal.valueOf(250), "g", owner))).when(pantryItemRepository).findByOwner(owner);
        doReturn(List.of()).when(shoppingListItemRepository).findByOwner(owner);

        MealPlanShoppingListResponse result = underTest.createShoppingList(owner, weekStart);

        assertEquals(1, result.getNeedsReview().size());
        verify(shoppingListItemRepository, never()).persist(org.mockito.ArgumentMatchers.any(ShoppingListItem.class));
    }

    @Test
    @DisplayName("createShoppingList should avoid duplicate existing shopping list entries")
    void createShoppingList_should_avoid_existing_shopping_list_entry() {
        AppUser owner = user(1L);
        LocalDate weekStart = LocalDate.of(2026, 6, 1);
        doReturn(List.of(mealPlan(owner, recipe("Rice Bowl", "200 g Reis"))))
                .when(mealPlanRepository).findByOwnerAndPlannedDateBetween(owner, weekStart, weekStart.plusDays(6));
        doReturn(List.of()).when(pantryItemRepository).findByOwner(owner);
        doReturn(List.of(shopping("Reis", BigDecimal.valueOf(200), "g", owner))).when(shoppingListItemRepository).findByOwner(owner);

        MealPlanShoppingListResponse result = underTest.createShoppingList(owner, weekStart);

        assertEquals(1, result.getAlreadyOnShoppingList().size());
        verify(shoppingListItemRepository, never()).persist(org.mockito.ArgumentMatchers.any(ShoppingListItem.class));
    }

    @Test
    @DisplayName("createShoppingList should report custom title entries for review")
    void createShoppingList_should_report_custom_title_entries_for_review() {
        AppUser owner = user(1L);
        LocalDate weekStart = LocalDate.of(2026, 6, 1);
        MealPlan custom = new MealPlan();
        custom.setOwner(owner);
        custom.setPlannedDate(weekStart);
        custom.setMealSlot(MealSlot.DINNER);
        custom.setCustomTitle("Sushi Abend");
        doReturn(List.of(custom)).when(mealPlanRepository).findByOwnerAndPlannedDateBetween(owner, weekStart, weekStart.plusDays(6));
        doReturn(List.of()).when(pantryItemRepository).findByOwner(owner);
        doReturn(List.of()).when(shoppingListItemRepository).findByOwner(owner);

        MealPlanShoppingListResponse result = underTest.createShoppingList(owner, weekStart);

        assertEquals(1, result.getNeedsReview().size());
        assertEquals("Sushi Abend", result.getNeedsReview().get(0).getName());
        verify(shoppingListItemRepository, never()).persist(org.mockito.ArgumentMatchers.any(ShoppingListItem.class));
    }

    private MealPlan mealPlan(AppUser owner, Recipe recipe) {
        MealPlan entry = new MealPlan();
        entry.setOwner(owner);
        entry.setPlannedDate(LocalDate.of(2026, 6, 1));
        entry.setMealSlot(MealSlot.DINNER);
        entry.setRecipe(recipe);
        return entry;
    }

    private Recipe recipe(String title, String ingredients) {
        Recipe recipe = new Recipe();
        recipe.setTitle(title);
        recipe.setIngredients(ingredients);
        return recipe;
    }

    private PantryItem pantry(String name, BigDecimal quantity, String unit, AppUser owner) {
        PantryItem item = new PantryItem();
        item.setName(name);
        item.setQuantity(quantity);
        item.setUnit(unit);
        item.setOwner(owner);
        return item;
    }

    private ShoppingListItem shopping(String name, BigDecimal quantity, String unit, AppUser owner) {
        ShoppingListItem item = new ShoppingListItem();
        item.setName(name);
        item.setQuantity(quantity);
        item.setUnit(unit);
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
