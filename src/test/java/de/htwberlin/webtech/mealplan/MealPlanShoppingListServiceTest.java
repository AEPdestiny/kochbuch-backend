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
    @DisplayName("createShoppingList should use real planned recipe ingredients instead of needs review")
    void createShoppingList_should_use_real_planned_recipe_ingredients_instead_of_needs_review() {
        AppUser owner = user(1L);
        LocalDate weekStart = LocalDate.of(2026, 6, 1);
        doReturn(List.of(
                mealPlan(owner, recipe("Tomato Pasta", "200 g Tomaten\n1 Zehe Knoblauch")),
                mealPlan(owner, recipe("Rice Bowl", "150 g Reis"))
        )).when(mealPlanRepository).findByOwnerAndPlannedDateBetween(owner, weekStart, weekStart.plusDays(6));
        doReturn(List.of()).when(pantryItemRepository).findByOwner(owner);
        doReturn(List.of()).when(shoppingListItemRepository).findByOwner(owner);

        MealPlanShoppingListResponse result = underTest.createShoppingList(owner, weekStart);

        ArgumentCaptor<ShoppingListItem> captor = ArgumentCaptor.forClass(ShoppingListItem.class);
        verify(shoppingListItemRepository, org.mockito.Mockito.times(3)).persist(captor.capture());
        assertEquals(3, result.getAdded().size());
        assertEquals(0, result.getNeedsReview().size());
        assertEquals(List.of("Tomaten", "Knoblauch", "Reis"), captor.getAllValues().stream()
                .map(ShoppingListItem::getName)
                .toList());
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
    @DisplayName("createShoppingList should avoid duplicate when existing item has no unit but need has a unit")
    void createShoppingList_should_avoid_duplicate_when_units_differ() {
        AppUser owner = user(1L);
        LocalDate weekStart = LocalDate.of(2026, 6, 1);
        doReturn(List.of(mealPlan(owner, recipe("Rice Bowl", "200 g Reis"))))
                .when(mealPlanRepository).findByOwnerAndPlannedDateBetween(owner, weekStart, weekStart.plusDays(6));
        doReturn(List.of()).when(pantryItemRepository).findByOwner(owner);
        // Existing item has no unit (manually added), need has "g" — must not create duplicate
        doReturn(List.of(shopping("Reis", null, null, owner))).when(shoppingListItemRepository).findByOwner(owner);

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

    @Test
    @DisplayName("createShoppingList should parse 'Tasse' as a recognized unit")
    void createShoppingList_should_parse_tasse_unit() {
        AppUser owner = user(1L);
        LocalDate weekStart = LocalDate.of(2026, 6, 1);
        doReturn(List.of(mealPlan(owner, recipe("Pancakes", "0.75 Tasse Mehl"))))
                .when(mealPlanRepository).findByOwnerAndPlannedDateBetween(owner, weekStart, weekStart.plusDays(6));
        doReturn(List.of()).when(pantryItemRepository).findByOwner(owner);
        doReturn(List.of()).when(shoppingListItemRepository).findByOwner(owner);

        MealPlanShoppingListResponse result = underTest.createShoppingList(owner, weekStart);

        ArgumentCaptor<ShoppingListItem> captor = ArgumentCaptor.forClass(ShoppingListItem.class);
        verify(shoppingListItemRepository).persist(captor.capture());
        assertEquals("Mehl", captor.getValue().getName());
        assertEquals("cup", captor.getValue().getUnit());
    }

    @Test
    @DisplayName("createShoppingList should parse 'cup' as a recognized unit")
    void createShoppingList_should_parse_cup_unit() {
        AppUser owner = user(1L);
        LocalDate weekStart = LocalDate.of(2026, 6, 1);
        doReturn(List.of(mealPlan(owner, recipe("Pancakes", "1 cup Milch"))))
                .when(mealPlanRepository).findByOwnerAndPlannedDateBetween(owner, weekStart, weekStart.plusDays(6));
        doReturn(List.of()).when(pantryItemRepository).findByOwner(owner);
        doReturn(List.of()).when(shoppingListItemRepository).findByOwner(owner);

        MealPlanShoppingListResponse result = underTest.createShoppingList(owner, weekStart);

        ArgumentCaptor<ShoppingListItem> captor = ArgumentCaptor.forClass(ShoppingListItem.class);
        verify(shoppingListItemRepository).persist(captor.capture());
        assertEquals("Milch", captor.getValue().getName());
        assertEquals("cup", captor.getValue().getUnit());
    }

    @Test
    @DisplayName("createShoppingList should parse 'piece' as a recognized unit")
    void createShoppingList_should_parse_piece_unit() {
        AppUser owner = user(1L);
        LocalDate weekStart = LocalDate.of(2026, 6, 1);
        doReturn(List.of(mealPlan(owner, recipe("Salad", "2 piece Tomate"))))
                .when(mealPlanRepository).findByOwnerAndPlannedDateBetween(owner, weekStart, weekStart.plusDays(6));
        doReturn(List.of()).when(pantryItemRepository).findByOwner(owner);
        doReturn(List.of()).when(shoppingListItemRepository).findByOwner(owner);

        MealPlanShoppingListResponse result = underTest.createShoppingList(owner, weekStart);

        ArgumentCaptor<ShoppingListItem> captor = ArgumentCaptor.forClass(ShoppingListItem.class);
        verify(shoppingListItemRepository).persist(captor.capture());
        assertEquals("Tomate", captor.getValue().getName());
        assertEquals("piece", captor.getValue().getUnit());
    }

    @Test
    @DisplayName("createShoppingList should parse unicode fractions and German cup unit")
    void createShoppingList_should_parse_unicode_fraction_and_tasse_unit() {
        AppUser owner = user(1L);
        LocalDate weekStart = LocalDate.of(2026, 6, 1);
        doReturn(List.of(mealPlan(owner, recipe("Herb Bowl", "\u00bc Tasse Petersilie, gehackt"))))
                .when(mealPlanRepository).findByOwnerAndPlannedDateBetween(owner, weekStart, weekStart.plusDays(6));
        doReturn(List.of()).when(pantryItemRepository).findByOwner(owner);
        doReturn(List.of()).when(shoppingListItemRepository).findByOwner(owner);

        underTest.createShoppingList(owner, weekStart);

        ArgumentCaptor<ShoppingListItem> captor = ArgumentCaptor.forClass(ShoppingListItem.class);
        verify(shoppingListItemRepository).persist(captor.capture());
        assertEquals("Petersilie, gehackt", captor.getValue().getName());
        assertEquals(0, new BigDecimal("0.25").compareTo(captor.getValue().getQuantity()));
        assertEquals("cup", captor.getValue().getUnit());
    }

    @Test
    @DisplayName("createShoppingList should parse new English and German units without putting them in the name")
    void createShoppingList_should_parse_new_units_without_unit_fragments_in_name() {
        AppUser owner = user(1L);
        LocalDate weekStart = LocalDate.of(2026, 6, 1);
        doReturn(List.of(mealPlan(owner, recipe("New Units",
                "6 ounces ham\n2 Stiele Petersilie\n1,25 t Öl\nlarge cloves Knoblauch"))))
                .when(mealPlanRepository).findByOwnerAndPlannedDateBetween(owner, weekStart, weekStart.plusDays(6));
        doReturn(List.of()).when(pantryItemRepository).findByOwner(owner);
        doReturn(List.of()).when(shoppingListItemRepository).findByOwner(owner);

        underTest.createShoppingList(owner, weekStart);

        ArgumentCaptor<ShoppingListItem> captor = ArgumentCaptor.forClass(ShoppingListItem.class);
        verify(shoppingListItemRepository, org.mockito.Mockito.times(4)).persist(captor.capture());
        List<ShoppingListItem> items = captor.getAllValues();
        assertEquals(List.of("ham", "Petersilie", "Öl", "Knoblauch"), items.stream().map(ShoppingListItem::getName).toList());
        assertEquals("oz", items.get(0).getUnit());
        assertEquals("stalk", items.get(1).getUnit());
        assertEquals("tsp", items.get(2).getUnit());
        assertEquals("clove", items.get(3).getUnit());
    }

    @Test
    @DisplayName("createShoppingList should parse package, spoon, weight and slice units")
    void createShoppingList_should_parse_package_spoon_weight_and_slice_units() {
        AppUser owner = user(1L);
        LocalDate weekStart = LocalDate.of(2026, 6, 1);
        doReturn(List.of(mealPlan(owner, recipe("Mixed Units",
                "1 Packung Knorr-Sauce Hollandaise\n1 Teelöffel weißer Essig\n0.5 oz butter\n1 lb potatoes\n2 Scheiben Schinken\n3 slices ham"))))
                .when(mealPlanRepository).findByOwnerAndPlannedDateBetween(owner, weekStart, weekStart.plusDays(6));
        doReturn(List.of()).when(pantryItemRepository).findByOwner(owner);
        doReturn(List.of()).when(shoppingListItemRepository).findByOwner(owner);

        underTest.createShoppingList(owner, weekStart);

        ArgumentCaptor<ShoppingListItem> captor = ArgumentCaptor.forClass(ShoppingListItem.class);
        verify(shoppingListItemRepository, org.mockito.Mockito.times(6)).persist(captor.capture());
        List<ShoppingListItem> items = captor.getAllValues();
        assertEquals(List.of(
                "Knorr-Sauce Hollandaise",
                "weißer Essig",
                "butter",
                "potatoes",
                "Schinken",
                "ham"
        ), items.stream().map(ShoppingListItem::getName).toList());
        assertEquals(List.of("package", "tsp", "oz", "lb", "slice", "slice"),
                items.stream().map(ShoppingListItem::getUnit).toList());
        assertEquals(0, new BigDecimal("0.5").compareTo(items.get(2).getQuantity()));
        assertEquals(0, BigDecimal.ONE.compareTo(items.get(3).getQuantity()));
    }

    @Test
    @DisplayName("createShoppingList should strip bullet markers before parsing ingredients")
    void createShoppingList_should_strip_bullet_markers_before_parsing_ingredients() {
        AppUser owner = user(1L);
        LocalDate weekStart = LocalDate.of(2026, 6, 1);
        doReturn(List.of(mealPlan(owner, recipe("Bullet Ingredients",
                "\u20221/2 tbsp soy sauce\n\u20222 package shrimp flavored udon\n\u2022scallion, finely cut\n.1/2 cup fish cake, thinly sliced flat\n.1 tsp minced garlic\n\u20222 eggs, poached"))))
                .when(mealPlanRepository).findByOwnerAndPlannedDateBetween(owner, weekStart, weekStart.plusDays(6));
        doReturn(List.of()).when(pantryItemRepository).findByOwner(owner);
        doReturn(List.of()).when(shoppingListItemRepository).findByOwner(owner);

        underTest.createShoppingList(owner, weekStart);

        ArgumentCaptor<ShoppingListItem> captor = ArgumentCaptor.forClass(ShoppingListItem.class);
        verify(shoppingListItemRepository, org.mockito.Mockito.times(6)).persist(captor.capture());
        List<ShoppingListItem> items = captor.getAllValues();
        assertEquals(List.of(
                "soy sauce",
                "shrimp flavored udon",
                "scallion, finely cut",
                "fish cake, thinly sliced flat",
                "minced garlic",
                "eggs, poached"
        ), items.stream().map(ShoppingListItem::getName).toList());
        assertEquals(java.util.Arrays.asList("tbsp", "package", null, "cup", "tsp", null),
                items.stream().map(ShoppingListItem::getUnit).toList());
        assertEquals(0, new BigDecimal("0.5").compareTo(items.get(0).getQuantity()));
        assertEquals(0, BigDecimal.valueOf(2).compareTo(items.get(1).getQuantity()));
        assertEquals(0, BigDecimal.ONE.compareTo(items.get(4).getQuantity()));
        assertEquals(0, BigDecimal.valueOf(2).compareTo(items.get(5).getQuantity()));
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
