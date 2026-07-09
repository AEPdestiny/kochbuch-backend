package de.htwberlin.webtech.ai.tools;

import de.htwberlin.webtech.mealplan.dto.MealPlanEntryRequest;
import de.htwberlin.webtech.mealplan.entity.MealPlan;
import de.htwberlin.webtech.mealplan.entity.MealSlot;
import de.htwberlin.webtech.mealplan.repository.MealPlanRepository;
import de.htwberlin.webtech.mealplan.service.MealPlanService;
import de.htwberlin.webtech.recipe.entity.Recipe;
import de.htwberlin.webtech.user.entity.AppUser;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;

@ApplicationScoped
public class AiMealPlanTool {

    private final MealPlanService mealPlanService;
    private final MealPlanRepository mealPlanRepository;

    public AiMealPlanTool(MealPlanService mealPlanService, MealPlanRepository mealPlanRepository) {
        this.mealPlanService = mealPlanService;
        this.mealPlanRepository = mealPlanRepository;
    }

    public AiMealPlanToolResult addToMealPlan(AppUser currentUser,
                                              LocalDate targetDate,
                                              MealSlot mealSlot,
                                              Long recipeId,
                                              String customTitle) {
        if (currentUser == null || targetDate == null || mealSlot == null) {
            throw new IllegalArgumentException("targetDate and mealSlot must be provided.");
        }
        String cleanTitle = cleanTitle(customTitle);
        if (recipeId == null && cleanTitle == null) {
            throw new IllegalArgumentException("recipeId or customTitle must be provided.");
        }

        MealPlan existing = mealPlanRepository.findByOwnerAndPlannedDateAndMealSlot(currentUser, targetDate, mealSlot)
                .orElse(null);
        if (existing != null) {
            return new AiMealPlanToolResult(false, true, cleanTitle, mealPlanTitle(existing), targetDate, mealSlot);
        }

        MealPlanEntryRequest request = new MealPlanEntryRequest();
        request.setRecipeId(recipeId);
        request.setCustomTitle(cleanTitle);
        MealPlan created = mealPlanService.setRecipeForSlot(currentUser, targetDate, mealSlot, request);
        return new AiMealPlanToolResult(true, false, mealPlanTitle(created), null, targetDate, mealSlot);
    }

    private String cleanTitle(String customTitle) {
        if (customTitle == null || customTitle.isBlank()) {
            return null;
        }
        return customTitle.trim().replaceAll("\\s+", " ");
    }

    private String mealPlanTitle(MealPlan mealPlan) {
        if (mealPlan == null) {
            return null;
        }
        Recipe recipe = mealPlan.getRecipe();
        if (recipe != null && recipe.getTitle() != null && !recipe.getTitle().isBlank()) {
            return recipe.getTitle().trim();
        }
        return cleanTitle(mealPlan.getCustomTitle());
    }
}
