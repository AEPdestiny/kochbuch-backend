package de.htwberlin.webtech.mealplan;

import de.htwberlin.webtech.mealplan.dto.MealPlanEntryRequest;
import de.htwberlin.webtech.mealplan.entity.MealPlan;
import de.htwberlin.webtech.mealplan.entity.MealSlot;
import de.htwberlin.webtech.mealplan.exception.MealPlanEntryNotFoundException;
import de.htwberlin.webtech.mealplan.repository.MealPlanRepository;
import de.htwberlin.webtech.mealplan.service.MealPlanService;
import de.htwberlin.webtech.recipe.entity.Recipe;
import de.htwberlin.webtech.recipe.exception.RecipeNotFoundException;
import de.htwberlin.webtech.recipe.repository.RecipeRepository;
import de.htwberlin.webtech.shared.exception.ForbiddenException;
import de.htwberlin.webtech.user.entity.AppUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class MealPlanServiceTest {

    private final MealPlanRepository mealPlanRepository = mock(MealPlanRepository.class);
    private final RecipeRepository recipeRepository = mock(RecipeRepository.class);
    private final MealPlanService underTest = new MealPlanService(mealPlanRepository, recipeRepository);

    @Test
    @DisplayName("normalizeWeekStart should return Monday")
    void normalizeWeekStart_should_return_monday() {
        assertEquals(LocalDate.of(2026, 6, 1), underTest.normalizeWeekStart(LocalDate.of(2026, 6, 3)));
    }

    @Test
    @DisplayName("getWeek should normalize start date and delegate to repository")
    void getWeek_should_normalize_start_date_and_delegate() {
        AppUser owner = user(1L);
        LocalDate wednesday = LocalDate.of(2026, 6, 3);
        LocalDate monday = LocalDate.of(2026, 6, 1);
        LocalDate sunday = LocalDate.of(2026, 6, 7);
        doReturn(List.of(mealPlan(owner, recipe(1L, owner), monday))).when(mealPlanRepository)
                .findByOwnerAndPlannedDateBetween(owner, monday, sunday);

        List<MealPlan> result = underTest.getWeek(owner, wednesday);

        verify(mealPlanRepository).findByOwnerAndPlannedDateBetween(owner, monday, sunday);
        verifyNoMoreInteractions(mealPlanRepository, recipeRepository);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("setRecipeForDay should create entry for own recipe")
    void setRecipeForDay_should_create_entry_for_own_recipe() {
        AppUser owner = user(1L);
        Recipe recipe = recipe(10L, owner);
        LocalDate date = LocalDate.of(2026, 6, 1);
        doReturn(recipe).when(recipeRepository).findById(10L);
        doReturn(Optional.empty()).when(mealPlanRepository).findByOwnerAndPlannedDate(owner, date);
        doAnswer(invocation -> {
            MealPlan mealPlan = invocation.getArgument(0);
            assertNotNull(mealPlan.getRecipe());
            return null;
        }).when(mealPlanRepository).persist(any(MealPlan.class));

        MealPlan result = underTest.setRecipeForDay(owner, date, request(10L));

        ArgumentCaptor<MealPlan> captor = ArgumentCaptor.forClass(MealPlan.class);
        verify(recipeRepository).findById(10L);
        verify(mealPlanRepository).findByOwnerAndPlannedDate(owner, date);
        verify(mealPlanRepository).persist(captor.capture());
        verifyNoMoreInteractions(mealPlanRepository, recipeRepository);
        assertSame(owner, result.getOwner());
        assertSame(recipe, result.getRecipe());
        assertEquals(date, result.getPlannedDate());
        assertSame(result, captor.getValue());
        assertSame(recipe, captor.getValue().getRecipe());
    }

    @Test
    @DisplayName("setRecipeForDay should replace existing recipe")
    void setRecipeForDay_should_replace_existing_recipe() {
        AppUser owner = user(1L);
        Recipe oldRecipe = recipe(10L, owner);
        Recipe newRecipe = recipe(11L, owner);
        LocalDate date = LocalDate.of(2026, 6, 1);
        MealPlan existing = mealPlan(owner, oldRecipe, date);
        doReturn(newRecipe).when(recipeRepository).findById(11L);
        doReturn(Optional.of(existing)).when(mealPlanRepository).findByOwnerAndPlannedDate(owner, date);

        MealPlan result = underTest.setRecipeForDay(owner, date, request(11L));

        verify(recipeRepository).findById(11L);
        verify(mealPlanRepository).findByOwnerAndPlannedDate(owner, date);
        verifyNoMoreInteractions(mealPlanRepository, recipeRepository);
        assertSame(existing, result);
        assertSame(newRecipe, result.getRecipe());
    }

    @Test
    @DisplayName("setRecipeForSlot should create entry for selected slot")
    void setRecipeForSlot_should_create_entry_for_selected_slot() {
        AppUser owner = user(1L);
        Recipe recipe = recipe(10L, owner);
        LocalDate date = LocalDate.of(2026, 6, 1);
        doReturn(recipe).when(recipeRepository).findById(10L);
        doReturn(Optional.empty()).when(mealPlanRepository)
                .findByOwnerAndPlannedDateAndMealSlot(owner, date, MealSlot.BREAKFAST);

        MealPlan result = underTest.setRecipeForSlot(owner, date, MealSlot.BREAKFAST, request(10L));

        verify(recipeRepository).findById(10L);
        verify(mealPlanRepository).findByOwnerAndPlannedDateAndMealSlot(owner, date, MealSlot.BREAKFAST);
        verify(mealPlanRepository).persist(result);
        assertEquals(MealSlot.BREAKFAST, result.getMealSlot());
        assertSame(recipe, result.getRecipe());
    }

    @Test
    @DisplayName("setRecipeForSlot should create custom title entry")
    void setRecipeForSlot_should_create_custom_title_entry() {
        AppUser owner = user(1L);
        LocalDate date = LocalDate.of(2026, 6, 1);
        doReturn(Optional.empty()).when(mealPlanRepository)
                .findByOwnerAndPlannedDateAndMealSlot(owner, date, MealSlot.LUNCH);

        MealPlanEntryRequest request = customRequest("Sushi Abend");
        request.setCaloriesSnapshot(520);
        request.setImageUrlSnapshot("https://example.com/sushi.jpg");
        request.setExternalRecipeId("spoon-99");
        request.setExternalSource("spoonacular");

        MealPlan result = underTest.setRecipeForSlot(owner, date, MealSlot.LUNCH, request);

        verify(mealPlanRepository).findByOwnerAndPlannedDateAndMealSlot(owner, date, MealSlot.LUNCH);
        verify(mealPlanRepository).persist(result);
        verifyNoMoreInteractions(recipeRepository);
        assertEquals(MealSlot.LUNCH, result.getMealSlot());
        assertEquals("Sushi Abend", result.getCustomTitle());
        assertEquals(520, result.getCaloriesSnapshot());
        assertEquals("https://example.com/sushi.jpg", result.getImageUrlSnapshot());
        assertEquals("spoon-99", result.getExternalRecipeId());
        assertEquals("spoonacular", result.getExternalSource());
        assertEquals(null, result.getRecipe());
    }

    @Test
    @DisplayName("setRecipeForSlot should clear snapshots when replacing custom title with own recipe")
    void setRecipeForSlot_should_clear_snapshots_when_replacing_custom_title_with_own_recipe() {
        AppUser owner = user(1L);
        Recipe recipe = recipe(10L, owner);
        LocalDate date = LocalDate.of(2026, 6, 1);
        MealPlan existing = mealPlan(owner, null, date);
        existing.setMealSlot(MealSlot.SNACK);
        existing.setCustomTitle("Sushi frei");
        existing.setCaloriesSnapshot(500);
        existing.setExternalRecipeId("external-1");
        doReturn(recipe).when(recipeRepository).findById(10L);
        doReturn(Optional.of(existing)).when(mealPlanRepository)
                .findByOwnerAndPlannedDateAndMealSlot(owner, date, MealSlot.SNACK);

        MealPlan result = underTest.setRecipeForSlot(owner, date, MealSlot.SNACK, request(10L));

        assertSame(existing, result);
        assertSame(recipe, result.getRecipe());
        assertEquals(null, result.getCustomTitle());
        assertEquals(null, result.getCaloriesSnapshot());
        assertEquals(null, result.getExternalRecipeId());
    }

    @Test
    @DisplayName("setRecipeForSlot should create custom title entry for every slot")
    void setRecipeForSlot_should_create_custom_title_entry_for_every_slot() {
        AppUser owner = user(1L);
        LocalDate date = LocalDate.of(2026, 6, 1);
        for (MealSlot slot : MealSlot.values()) {
            if (slot == MealSlot.DINNER) {
                doReturn(Optional.empty()).when(mealPlanRepository).findByOwnerAndPlannedDate(owner, date);
            } else {
                doReturn(Optional.empty()).when(mealPlanRepository)
                        .findByOwnerAndPlannedDateAndMealSlot(owner, date, slot);
            }

            MealPlan result = underTest.setRecipeForSlot(owner, date, slot, customRequest("Freitext " + slot.name()));

            assertEquals(slot, result.getMealSlot());
            assertEquals("Freitext " + slot.name(), result.getCustomTitle());
            assertEquals(null, result.getRecipe());
        }
    }

    @Test
    @DisplayName("setRecipeForSlot should reject missing recipe and custom title")
    void setRecipeForSlot_should_reject_missing_recipe_and_custom_title() {
        assertThrows(IllegalArgumentException.class,
                () -> underTest.setRecipeForSlot(user(1L), LocalDate.of(2026, 6, 1), MealSlot.SNACK, customRequest(" ")));

        verifyNoMoreInteractions(mealPlanRepository, recipeRepository);
    }

    @Test
    @DisplayName("setRecipeForDay should reject foreign recipe")
    void setRecipeForDay_should_reject_foreign_recipe() {
        AppUser owner = user(1L);
        Recipe foreignRecipe = recipe(10L, user(2L));
        doReturn(foreignRecipe).when(recipeRepository).findById(10L);

        assertThrows(ForbiddenException.class, () -> underTest.setRecipeForDay(owner, LocalDate.of(2026, 6, 1), request(10L)));

        verify(recipeRepository).findById(10L);
        verifyNoMoreInteractions(mealPlanRepository, recipeRepository);
    }

    @Test
    @DisplayName("setRecipeForSlot should allow public seed recipe without owner")
    void setRecipeForSlot_should_allow_public_seed_recipe_without_owner() {
        AppUser owner = user(1L);
        Recipe publicRecipe = recipe(10L, null);
        LocalDate date = LocalDate.of(2026, 6, 1);
        doReturn(publicRecipe).when(recipeRepository).findById(10L);
        doReturn(Optional.empty()).when(mealPlanRepository)
                .findByOwnerAndPlannedDateAndMealSlot(owner, date, MealSlot.LUNCH);

        MealPlan result = underTest.setRecipeForSlot(owner, date, MealSlot.LUNCH, request(10L));

        verify(recipeRepository).findById(10L);
        verify(mealPlanRepository).findByOwnerAndPlannedDateAndMealSlot(owner, date, MealSlot.LUNCH);
        verify(mealPlanRepository).persist(result);
        assertSame(publicRecipe, result.getRecipe());
        assertEquals(null, result.getCustomTitle());
    }

    @Test
    @DisplayName("moveEntry should move entry into empty slot and preserve recipe")
    void moveEntry_should_move_entry_into_empty_slot_and_preserve_recipe() {
        AppUser owner = user(1L);
        LocalDate monday = LocalDate.of(2026, 6, 1);
        LocalDate tuesday = LocalDate.of(2026, 6, 2);
        Recipe recipe = recipe(10L, owner);
        MealPlan source = mealPlan(owner, recipe, monday);
        source.setMealSlot(MealSlot.DINNER);
        doReturn(Optional.of(source)).when(mealPlanRepository).findByIdOptional(1L);
        doReturn(Optional.empty()).when(mealPlanRepository)
                .findByOwnerAndPlannedDateAndMealSlot(owner, tuesday, MealSlot.BREAKFAST);
        doReturn(List.of(source)).when(mealPlanRepository)
                .findByOwnerAndPlannedDateBetween(owner, monday, monday.plusDays(6));

        List<MealPlan> result = underTest.moveEntry(owner, 1L, tuesday, MealSlot.BREAKFAST, true);

        assertEquals(tuesday, source.getPlannedDate());
        assertEquals(MealSlot.BREAKFAST, source.getMealSlot());
        assertSame(recipe, source.getRecipe());
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("moveEntry should swap occupied target slot and preserve both recipes")
    void moveEntry_should_swap_occupied_target_slot_and_preserve_both_recipes() {
        AppUser owner = user(1L);
        LocalDate monday = LocalDate.of(2026, 6, 1);
        LocalDate tuesday = LocalDate.of(2026, 6, 2);
        Recipe sourceRecipe = recipe(10L, owner);
        Recipe targetRecipe = recipe(11L, owner);
        MealPlan source = mealPlan(owner, sourceRecipe, monday);
        source.setMealSlot(MealSlot.DINNER);
        MealPlan target = mealPlan(owner, targetRecipe, tuesday);
        target.setId(2L);
        target.setMealSlot(MealSlot.BREAKFAST);
        target.setCaloriesSnapshot(480);
        doReturn(Optional.of(source)).when(mealPlanRepository).findByIdOptional(1L);
        doReturn(Optional.of(target)).when(mealPlanRepository)
                .findByOwnerAndPlannedDateAndMealSlot(owner, tuesday, MealSlot.BREAKFAST);
        doReturn(List.of(source, target)).when(mealPlanRepository)
                .findByOwnerAndPlannedDateBetween(owner, monday, monday.plusDays(6));

        List<MealPlan> result = underTest.moveEntry(owner, 1L, tuesday, MealSlot.BREAKFAST, true);

        assertEquals(monday, source.getPlannedDate());
        assertEquals(MealSlot.DINNER, source.getMealSlot());
        assertSame(targetRecipe, source.getRecipe());
        assertEquals(tuesday, target.getPlannedDate());
        assertEquals(MealSlot.BREAKFAST, target.getMealSlot());
        assertSame(sourceRecipe, target.getRecipe());
        assertEquals(null, target.getCaloriesSnapshot());
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("moveEntry should reject foreign entry")
    void moveEntry_should_reject_foreign_entry() {
        AppUser owner = user(1L);
        MealPlan source = mealPlan(user(2L), recipe(10L, user(2L)), LocalDate.of(2026, 6, 1));
        doReturn(Optional.of(source)).when(mealPlanRepository).findByIdOptional(1L);

        assertThrows(ForbiddenException.class,
                () -> underTest.moveEntry(owner, 1L, LocalDate.of(2026, 6, 2), MealSlot.LUNCH, true));
    }

    @Test
    @DisplayName("setRecipeForDay should reject unknown recipe")
    void setRecipeForDay_should_reject_unknown_recipe() {
        doReturn(null).when(recipeRepository).findById(99L);

        assertThrows(RecipeNotFoundException.class, () -> underTest.setRecipeForDay(user(1L), LocalDate.of(2026, 6, 1), request(99L)));

        verify(recipeRepository).findById(99L);
        verifyNoMoreInteractions(mealPlanRepository, recipeRepository);
    }

    @Test
    @DisplayName("setRecipeForDay should reject missing recipe id")
    void setRecipeForDay_should_reject_missing_recipe_id() {
        assertThrows(IllegalArgumentException.class,
                () -> underTest.setRecipeForDay(user(1L), LocalDate.of(2026, 6, 1), request(null)));

        verifyNoMoreInteractions(mealPlanRepository, recipeRepository);
    }

    @Test
    @DisplayName("deleteForDay should delete own entry")
    void deleteForDay_should_delete_own_entry() {
        AppUser owner = user(1L);
        LocalDate date = LocalDate.of(2026, 6, 1);
        MealPlan existing = mealPlan(owner, recipe(10L, owner), date);
        doReturn(Optional.of(existing)).when(mealPlanRepository).findByOwnerAndPlannedDate(owner, date);

        underTest.deleteForDay(owner, date);

        verify(mealPlanRepository).findByOwnerAndPlannedDate(owner, date);
        verify(mealPlanRepository).delete(existing);
        verifyNoMoreInteractions(mealPlanRepository, recipeRepository);
    }

    @Test
    @DisplayName("deleteForDay should throw not found for missing entry")
    void deleteForDay_should_throw_not_found_for_missing_entry() {
        AppUser owner = user(1L);
        LocalDate date = LocalDate.of(2026, 6, 1);
        doReturn(Optional.empty()).when(mealPlanRepository).findByOwnerAndPlannedDate(owner, date);

        assertThrows(MealPlanEntryNotFoundException.class, () -> underTest.deleteForDay(owner, date));

        verify(mealPlanRepository).findByOwnerAndPlannedDate(owner, date);
        verifyNoMoreInteractions(mealPlanRepository, recipeRepository);
    }

    @Test
    @DisplayName("deleteForSlot should delete selected slot")
    void deleteForSlot_should_delete_selected_slot() {
        AppUser owner = user(1L);
        LocalDate date = LocalDate.of(2026, 6, 1);
        MealPlan existing = mealPlan(owner, recipe(10L, owner), date);
        existing.setMealSlot(MealSlot.SNACK);
        doReturn(Optional.of(existing)).when(mealPlanRepository)
                .findByOwnerAndPlannedDateAndMealSlot(owner, date, MealSlot.SNACK);

        underTest.deleteForSlot(owner, date, MealSlot.SNACK);

        verify(mealPlanRepository).findByOwnerAndPlannedDateAndMealSlot(owner, date, MealSlot.SNACK);
        verify(mealPlanRepository).delete(existing);
    }

    private MealPlanEntryRequest request(Long recipeId) {
        MealPlanEntryRequest request = new MealPlanEntryRequest();
        request.setRecipeId(recipeId);
        return request;
    }

    private MealPlanEntryRequest customRequest(String customTitle) {
        MealPlanEntryRequest request = new MealPlanEntryRequest();
        request.setCustomTitle(customTitle);
        return request;
    }

    private MealPlan mealPlan(AppUser owner, Recipe recipe, LocalDate plannedDate) {
        MealPlan mealPlan = new MealPlan();
        mealPlan.setId(1L);
        mealPlan.setOwner(owner);
        mealPlan.setRecipe(recipe);
        mealPlan.setPlannedDate(plannedDate);
        return mealPlan;
    }

    private Recipe recipe(Long id, AppUser owner) {
        Recipe recipe = new Recipe("Pasta " + id, "", 10, 20, 2,
                "easy", "Italian", 4.5, "noodles", "cook", false, true);
        recipe.setId(id);
        recipe.setOwner(owner);
        return recipe;
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
