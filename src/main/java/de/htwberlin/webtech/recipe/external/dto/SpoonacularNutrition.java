package de.htwberlin.webtech.recipe.external.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SpoonacularNutrition {

    private List<SpoonacularNutrient> nutrients;

    public List<SpoonacularNutrient> getNutrients() {
        return nutrients;
    }

    public void setNutrients(List<SpoonacularNutrient> nutrients) {
        this.nutrients = nutrients;
    }
}
