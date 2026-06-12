package de.htwberlin.webtech.mealplan.dto;

public class MealPlanEntryRequest {

    private Long recipeId;
    private String customTitle;

    public Long getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Long recipeId) {
        this.recipeId = recipeId;
    }

    public String getCustomTitle() {
        return customTitle;
    }

    public void setCustomTitle(String customTitle) {
        this.customTitle = customTitle;
    }
}
