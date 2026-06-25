package de.htwberlin.webtech.mealplan.dto;

import java.math.BigDecimal;

public class MealPlanShoppingListItemResponse {

    private String name;
    private BigDecimal quantity;
    private String unit;
    private String recipeTitle;
    private String ingredient;
    private String reason;

    public MealPlanShoppingListItemResponse() {
    }

    public MealPlanShoppingListItemResponse(String name, BigDecimal quantity, String unit, String recipeTitle, String ingredient, String reason) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.recipeTitle = recipeTitle;
        this.ingredient = ingredient;
        this.reason = reason;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getRecipeTitle() {
        return recipeTitle;
    }

    public void setRecipeTitle(String recipeTitle) {
        this.recipeTitle = recipeTitle;
    }

    public String getIngredient() {
        return ingredient;
    }

    public void setIngredient(String ingredient) {
        this.ingredient = ingredient;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
