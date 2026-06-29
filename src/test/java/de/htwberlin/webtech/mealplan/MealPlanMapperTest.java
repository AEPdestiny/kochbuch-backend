package de.htwberlin.webtech.mealplan;

import de.htwberlin.webtech.mealplan.dto.MealPlanWeekResponse;
import de.htwberlin.webtech.mealplan.entity.MealPlan;
import de.htwberlin.webtech.mealplan.entity.MealSlot;
import de.htwberlin.webtech.mealplan.mapper.MealPlanMapper;
import de.htwberlin.webtech.recipe.entity.Recipe;
import de.htwberlin.webtech.recipe.mapper.RecipeMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MealPlanMapperTest {

    private final MealPlanMapper underTest = new MealPlanMapper(new RecipeMapper());

    private static final LocalDate MONDAY = LocalDate.of(2026, 6, 1);
    private static final LocalDate TUESDAY = LocalDate.of(2026, 6, 2);
    private static final LocalDate SUNDAY = LocalDate.of(2026, 6, 7);

    @Test
    void toWeekResponse_includes_free_text_calories_in_daily_total() {
        MealPlan entry = freeTextEntry(MONDAY, "Sushi", 480);

        MealPlanWeekResponse response = underTest.toWeekResponse(MONDAY, SUNDAY, List.of(entry));

        assertEquals(1, response.getCaloriesByDate().size());
        assertEquals(480, response.getCaloriesByDate().get(MONDAY.toString()));
        assertEquals(480, response.getTotalCalories());
    }

    @Test
    void toWeekResponse_includes_recipe_calories_in_daily_total() {
        MealPlan entry = recipeEntry(MONDAY, 650);

        MealPlanWeekResponse response = underTest.toWeekResponse(MONDAY, SUNDAY, List.of(entry));

        assertEquals(1, response.getCaloriesByDate().size());
        assertEquals(650, response.getCaloriesByDate().get(MONDAY.toString()));
        assertEquals(650, response.getTotalCalories());
    }

    @Test
    void toWeekResponse_sums_multiple_entries_on_same_day() {
        MealPlan breakfast = freeTextEntry(MONDAY, "Müsli", 300);
        breakfast.setMealSlot(MealSlot.BREAKFAST);
        MealPlan dinner = recipeEntry(MONDAY, 700);
        dinner.setMealSlot(MealSlot.DINNER);

        MealPlanWeekResponse response = underTest.toWeekResponse(MONDAY, SUNDAY, List.of(breakfast, dinner));

        assertEquals(1, response.getCaloriesByDate().size());
        assertEquals(1000, response.getCaloriesByDate().get(MONDAY.toString()));
        assertEquals(1000, response.getTotalCalories());
    }

    @Test
    void toWeekResponse_splits_totals_by_day_for_entries_on_different_days() {
        MealPlan monday = freeTextEntry(MONDAY, "Lasagne", 550);
        MealPlan tuesday = freeTextEntry(TUESDAY, "Sushi", 480);

        MealPlanWeekResponse response = underTest.toWeekResponse(MONDAY, SUNDAY, List.of(monday, tuesday));

        assertEquals(2, response.getCaloriesByDate().size());
        assertEquals(550, response.getCaloriesByDate().get(MONDAY.toString()));
        assertEquals(480, response.getCaloriesByDate().get(TUESDAY.toString()));
        assertEquals(1030, response.getTotalCalories());
    }

    @Test
    void toWeekResponse_ignores_entries_without_calories() {
        MealPlan noCalories = freeTextEntry(MONDAY, "Unbekannt", null);
        MealPlan withCalories = freeTextEntry(TUESDAY, "Pasta", 400);

        MealPlanWeekResponse response = underTest.toWeekResponse(MONDAY, SUNDAY, List.of(noCalories, withCalories));

        assertFalse(response.getCaloriesByDate().containsKey(MONDAY.toString()));
        assertTrue(response.getCaloriesByDate().containsKey(TUESDAY.toString()));
        assertEquals(400, response.getCaloriesByDate().get(TUESDAY.toString()));
        assertEquals(400, response.getTotalCalories());
    }

    @Test
    void toWeekResponse_returns_empty_map_and_zero_total_when_no_calories_known() {
        MealPlan entry = freeTextEntry(MONDAY, "Geheimessen", null);

        MealPlanWeekResponse response = underTest.toWeekResponse(MONDAY, SUNDAY, List.of(entry));

        assertTrue(response.getCaloriesByDate().isEmpty());
        assertEquals(0, response.getTotalCalories());
    }

    @Test
    void toWeekResponse_returns_empty_map_and_zero_total_for_empty_week() {
        MealPlanWeekResponse response = underTest.toWeekResponse(MONDAY, SUNDAY, List.of());

        assertTrue(response.getCaloriesByDate().isEmpty());
        assertEquals(0, response.getTotalCalories());
    }

    @Test
    void toWeekResponse_recipe_entry_with_null_calories_does_not_contribute() {
        MealPlan recipeNoCalories = recipeEntry(MONDAY, null);
        MealPlan freewithCalories = freeTextEntry(MONDAY, "Snack", 200);
        freewithCalories.setMealSlot(MealSlot.SNACK);

        MealPlanWeekResponse response = underTest.toWeekResponse(MONDAY, SUNDAY, List.of(recipeNoCalories, freewithCalories));

        assertEquals(1, response.getCaloriesByDate().size());
        assertEquals(200, response.getCaloriesByDate().get(MONDAY.toString()));
        assertEquals(200, response.getTotalCalories());
    }

    private MealPlan freeTextEntry(LocalDate date, String title, Integer caloriesSnapshot) {
        MealPlan entry = new MealPlan();
        entry.setCustomTitle(title);
        entry.setCaloriesSnapshot(caloriesSnapshot);
        entry.setPlannedDate(date);
        entry.setMealSlot(MealSlot.DINNER);
        return entry;
    }

    private MealPlan recipeEntry(LocalDate date, Integer recipeCalories) {
        Recipe recipe = new Recipe("Pasta", "", 10, 20, 2,
                "easy", "lunch", 4.0, "noodles", "cook", false, true);
        recipe.setCalories(recipeCalories);
        MealPlan entry = new MealPlan();
        entry.setRecipe(recipe);
        entry.setPlannedDate(date);
        entry.setMealSlot(MealSlot.DINNER);
        return entry;
    }
}
