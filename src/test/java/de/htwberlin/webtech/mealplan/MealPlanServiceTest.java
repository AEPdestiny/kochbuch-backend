package de.htwberlin.webtech.mealplan;

import de.htwberlin.webtech.mealplan.dto.MealPlanEntryRequest;
import de.htwberlin.webtech.mealplan.entity.MealPlan;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    @DisplayName("setRecipeForDay should reject unknown recipe")
    void setRecipeForDay_should_reject_unknown_recipe() {
        doReturn(null).when(recipeRepository).findById(99L);

        assertThrows(RecipeNotFoundException.class, () -> underTest.setRecipeForDay(user(1L), LocalDate.of(2026, 6, 1), request(99L)));

        verify(recipeRepository).findById(99L);
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

    private MealPlanEntryRequest request(Long recipeId) {
        MealPlanEntryRequest request = new MealPlanEntryRequest();
        request.setRecipeId(recipeId);
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
