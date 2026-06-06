package de.htwberlin.webtech.mealplan.dto;

import jakarta.validation.constraints.NotNull;

public class MealPlanEntryRequest {

    @NotNull(message = "must not be null")
    private Long recipeId;

    public Long getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Long recipeId) {
        this.recipeId = recipeId;
    }
}
