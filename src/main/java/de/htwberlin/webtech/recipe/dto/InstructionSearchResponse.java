package de.htwberlin.webtech.recipe.dto;

import java.util.List;

public class InstructionSearchResponse {

    private boolean configured;
    private String message;
    private String googleSearchUrl;
    private List<InstructionSearchResult> results = List.of();

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

    public String getGoogleSearchUrl() {
        return googleSearchUrl;
    }

    public void setGoogleSearchUrl(String googleSearchUrl) {
        this.googleSearchUrl = googleSearchUrl;
    }

    public List<InstructionSearchResult> getResults() {
        return results;
    }

    public void setResults(List<InstructionSearchResult> results) {
        this.results = results == null ? List.of() : results;
    }
}
