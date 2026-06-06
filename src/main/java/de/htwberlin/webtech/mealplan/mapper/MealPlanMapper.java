package de.htwberlin.webtech.mealplan.mapper;

import de.htwberlin.webtech.mealplan.dto.MealPlanEntryResponse;
import de.htwberlin.webtech.mealplan.dto.MealPlanWeekResponse;
import de.htwberlin.webtech.mealplan.entity.MealPlan;
import de.htwberlin.webtech.recipe.mapper.RecipeMapper;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.List;

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
        response.setRecipe(recipeMapper.toResponse(mealPlan.getRecipe()));
        return response;
    }

    public MealPlanWeekResponse toWeekResponse(LocalDate weekStart, LocalDate weekEnd, List<MealPlan> entries) {
        MealPlanWeekResponse response = new MealPlanWeekResponse();
        response.setWeekStart(weekStart);
        response.setWeekEnd(weekEnd);
        response.setEntries(entries.stream()
                .map(this::toResponse)
                .toList());
        return response;
    }
}
