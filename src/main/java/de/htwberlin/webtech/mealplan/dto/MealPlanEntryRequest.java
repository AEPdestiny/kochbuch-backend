package de.htwberlin.webtech.mealplan.dto;

public class MealPlanEntryRequest {

    private Long recipeId;
    private String customTitle;
    private Integer caloriesSnapshot;
    private Double proteinSnapshot;
    private String imageUrlSnapshot;
    private String externalRecipeId;
    private String externalSource;

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
