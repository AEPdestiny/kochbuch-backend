package de.htwberlin.webtech.recipe.dto;

import java.util.List;

public class RecipeInstructionSuggestionResponse {

    private Long recipeId;
    private boolean hasRealInstructions;
    private boolean configured;
    private String message;
    private List<RecipeInstructionSuggestion> suggestions = List.of();

    public Long getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Long recipeId) {
        this.recipeId = recipeId;
    }

    public boolean isHasRealInstructions() {
        return hasRealInstructions;
    }

    public void setHasRealInstructions(boolean hasRealInstructions) {
        this.hasRealInstructions = hasRealInstructions;
    }

    public boolean isConfigured() {
        return configured;
    }

    public void setConfigured(boolean configured) {
        this.configured = configured;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<RecipeInstructionSuggestion> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<RecipeInstructionSuggestion> suggestions) {
        this.suggestions = suggestions;
    }
}
