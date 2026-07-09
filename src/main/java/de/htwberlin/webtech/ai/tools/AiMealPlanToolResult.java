package de.htwberlin.webtech.ai.tools;

import de.htwberlin.webtech.mealplan.entity.MealSlot;

import java.time.LocalDate;

public record AiMealPlanToolResult(
        boolean success,
        boolean conflict,
        String plannedTitle,
        String existingTitle,
        LocalDate targetDate,
        MealSlot mealSlot
) {
}
