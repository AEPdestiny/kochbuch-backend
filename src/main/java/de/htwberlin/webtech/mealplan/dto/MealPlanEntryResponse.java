package de.htwberlin.webtech.mealplan.dto;

import de.htwberlin.webtech.recipe.dto.RecipeResponse;

import java.time.LocalDate;

public class MealPlanEntryResponse {

    private Long id;
    private LocalDate plannedDate;
    private String mealSlot;
    private RecipeResponse recipe;
    private String customTitle;
    private Integer calories;
    private Integer caloriesSnapshot;
    private Double proteinSnapshot;
    private String imageUrlSnapshot;
    private String externalRecipeId;
    private String externalSource;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getPlannedDate() {
        return plannedDate;
    }

    public void setPlannedDate(LocalDate plannedDate) {
        this.plannedDate = plannedDate;
    }

    public String getMealSlot() {
        return mealSlot;
    }

    public void setMealSlot(String mealSlot) {
        this.mealSlot = mealSlot;
    }

    public RecipeResponse getRecipe() {
        return recipe;
    }

    public void setRecipe(RecipeResponse recipe) {
        this.recipe = recipe;
    }

    public String getCustomTitle() {
        return customTitle;
    }

    public void setCustomTitle(String customTitle) {
        this.customTitle = customTitle;
    }

    public Integer getCalories() {
        return calories;
    }

    public void setCalories(Integer calories) {
        this.calories = calories;
    }

    public Integer getCaloriesSnapshot() {
        return caloriesSnapshot;
    }

    public void setCaloriesSnapshot(Integer caloriesSnapshot) {
        this.caloriesSnapshot = caloriesSnapshot;
    }

    public Double getProteinSnapshot() {
        return proteinSnapshot;
    }

    public void setProteinSnapshot(Double proteinSnapshot) {
        this.proteinSnapshot = proteinSnapshot;
    }

    public String getImageUrlSnapshot() {
        return imageUrlSnapshot;
    }

    public void setImageUrlSnapshot(String imageUrlSnapshot) {
        this.imageUrlSnapshot = imageUrlSnapshot;
    }

    public String getExternalRecipeId() {
        return externalRecipeId;
    }

    public void setExternalRecipeId(String externalRecipeId) {
        this.externalRecipeId = externalRecipeId;
    }

    public String getExternalSource() {
        return externalSource;
    }

    public void setExternalSource(String externalSource) {
        this.externalSource = externalSource;
    }
}
