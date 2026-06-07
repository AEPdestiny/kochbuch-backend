package de.htwberlin.webtech.recipe.external.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SpoonacularSearchResponse {

    private List<SpoonacularRecipe> results;

    public List<SpoonacularRecipe> getResults() {
        return results;
    }

    public void setResults(List<SpoonacularRecipe> results) {
        this.results = results;
    }
}
