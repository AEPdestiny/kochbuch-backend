package de.htwberlin.webtech.ai.model;

import de.htwberlin.webtech.mealplan.entity.MealSlot;

import java.time.LocalDate;
import java.util.List;

public record AiActionPlan(
        AiActionType type,
        double confidence,
        Long recipeId,
        String externalRecipeId,
        String recipeTitle,
        LocalDate targetDate,
        MealSlot mealSlot,
        List<String> ingredients,
        String location,
        boolean requiresConfirmation
) {
}
