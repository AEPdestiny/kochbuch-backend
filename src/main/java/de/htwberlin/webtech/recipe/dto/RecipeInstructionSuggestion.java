package de.htwberlin.webtech.recipe.dto;

import java.util.List;

public class RecipeInstructionSuggestion {

    private String sourceTitle;
    private String sourceUrl;
    private List<String> steps = List.of();
    private double confidence;
    private String reason;

    public RecipeInstructionSuggestion() {
    }

    public RecipeInstructionSuggestion(String sourceTitle, String sourceUrl, List<String> steps, double confidence, String reason) {
        this.sourceTitle = sourceTitle;
        this.sourceUrl = sourceUrl;
        this.steps = steps;
        this.confidence = confidence;
        this.reason = reason;
    }

    public String getSourceTitle() {
        return sourceTitle;
    }

    public void setSourceTitle(String sourceTitle) {
        this.sourceTitle = sourceTitle;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public List<String> getSteps() {
        return steps;
    }

    public void setSteps(List<String> steps) {
        this.steps = steps;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
