package de.htwberlin.webtech.recipe.external.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SpoonacularRecipe {

    private Long id;
    private String title;
    private String image;
    private Integer readyInMinutes;
    private Integer servings;
    private String sourceUrl;
    private String summary;
    private String instructions;
    private List<String> dishTypes;
    private List<String> diets;
    private List<SpoonacularIngredient> extendedIngredients;
    private List<SpoonacularAnalyzedInstruction> analyzedInstructions;
    private SpoonacularNutrition nutrition;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Integer getReadyInMinutes() {
        return readyInMinutes;
    }

    public void setReadyInMinutes(Integer readyInMinutes) {
        this.readyInMinutes = readyInMinutes;
    }

    public Integer getServings() {
        return servings;
    }

    public void setServings(Integer servings) {
        this.servings = servings;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public List<String> getDishTypes() {
        return dishTypes;
    }

    public void setDishTypes(List<String> dishTypes) {
        this.dishTypes = dishTypes;
    }

    public List<String> getDiets() {
        return diets;
    }

    public void setDiets(List<String> diets) {
        this.diets = diets;
    }

    public List<SpoonacularIngredient> getExtendedIngredients() {
        return extendedIngredients;
    }

    public void setExtendedIngredients(List<SpoonacularIngredient> extendedIngredients) {
        this.extendedIngredients = extendedIngredients;
    }

    public List<SpoonacularAnalyzedInstruction> getAnalyzedInstructions() {
        return analyzedInstructions;
    }

    public void setAnalyzedInstructions(List<SpoonacularAnalyzedInstruction> analyzedInstructions) {
        this.analyzedInstructions = analyzedInstructions;
    }

    public SpoonacularNutrition getNutrition() {
        return nutrition;
    }

    public void setNutrition(SpoonacularNutrition nutrition) {
        this.nutrition = nutrition;
    }
}
