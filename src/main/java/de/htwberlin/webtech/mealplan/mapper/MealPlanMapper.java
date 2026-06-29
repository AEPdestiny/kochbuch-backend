package de.htwberlin.webtech.mealplan.mapper;

import de.htwberlin.webtech.mealplan.dto.MealPlanEntryResponse;
import de.htwberlin.webtech.mealplan.dto.MealPlanWeekResponse;
import de.htwberlin.webtech.mealplan.entity.MealPlan;
import de.htwberlin.webtech.recipe.mapper.RecipeMapper;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class MealPlanMapper {

    private final RecipeMapper recipeMapper;

    public MealPlanMapper(RecipeMapper recipeMapper) {
        this.recipeMapper = recipeMapper;
    }

    public MealPlanEntryResponse toResponse(MealPlan mealPlan) {
        MealPlanEntryResponse response = new MealPlanEntryResponse();
        response.setId(mealPlan.getId());
        response.setPlannedDate(mealPlan.getPlannedDate());
        response.setMealSlot(mealPlan.getMealSlot().name().toLowerCase());
        if (mealPlan.getRecipe() != null) {
            response.setRecipe(recipeMapper.toResponse(mealPlan.getRecipe()));
        }
        response.setCustomTitle(mealPlan.getCustomTitle());
        response.setCalories(resolveCalories(mealPlan));
        response.setCaloriesSnapshot(mealPlan.getCaloriesSnapshot());
        response.setProteinSnapshot(mealPlan.getProteinSnapshot());
        response.setImageUrlSnapshot(mealPlan.getImageUrlSnapshot());
        response.setExternalRecipeId(mealPlan.getExternalRecipeId());
        response.setExternalSource(mealPlan.getExternalSource());
        return response;
    }

    public MealPlanWeekResponse toWeekResponse(LocalDate weekStart, LocalDate weekEnd, List<MealPlan> entries) {
        List<MealPlanEntryResponse> entryResponses = entries.stream()
                .map(this::toResponse)
                .toList();

        Map<String, Integer> caloriesByDate = entryResponses.stream()
                .filter(e -> e.getCalories() != null)
                .collect(Collectors.groupingBy(
                        e -> e.getPlannedDate().toString(),
                        Collectors.summingInt(MealPlanEntryResponse::getCalories)
                ));
        int totalCalories = caloriesByDate.values().stream().mapToInt(Integer::intValue).sum();

        MealPlanWeekResponse response = new MealPlanWeekResponse();
        response.setWeekStart(weekStart);
        response.setWeekEnd(weekEnd);
        response.setEntries(entryResponses);
        response.setCaloriesByDate(caloriesByDate);
        response.setTotalCalories(totalCalories);
        return response;
    }

    private Integer resolveCalories(MealPlan mealPlan) {
        if (mealPlan.getRecipe() != null && mealPlan.getRecipe().getCalories() != null) {
            return mealPlan.getRecipe().getCalories();
        }
        return mealPlan.getCaloriesSnapshot();
    }
}
