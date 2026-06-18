package de.htwberlin.webtech.recipe.dto;

import jakarta.validation.constraints.NotBlank;

public class InstructionSearchRequest {

    @NotBlank(message = "must not be blank")
    private String recipeTitle;
    private String sourceUrl;
    private String sourceName;

    public String getRecipeTitle() {
        return recipeTitle;
    }

    public void setRecipeTitle(String recipeTitle) {
        this.recipeTitle = recipeTitle;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }
}
