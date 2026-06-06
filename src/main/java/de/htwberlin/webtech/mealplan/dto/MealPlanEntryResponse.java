package de.htwberlin.webtech.mealplan.dto;

import de.htwberlin.webtech.recipe.dto.RecipeResponse;

import java.time.LocalDate;

public class MealPlanEntryResponse {

    private Long id;
    private LocalDate plannedDate;
    private RecipeResponse recipe;

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

    public RecipeResponse getRecipe() {
        return recipe;
    }

    public void setRecipe(RecipeResponse recipe) {
        this.recipe = recipe;
    }
}
