package de.htwberlin.webtech.recipe.external.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TheMealDbSearchResponse {

    private List<TheMealDbMeal> meals;

    public List<TheMealDbMeal> getMeals() {
        return meals;
    }

    public void setMeals(List<TheMealDbMeal> meals) {
        this.meals = meals;
    }
}
