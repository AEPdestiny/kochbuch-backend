package de.htwberlin.webtech.recipe.external;

import de.htwberlin.webtech.recipe.external.dto.TheMealDbMeal;

import java.util.List;

public interface ExternalRecipeClient {

    List<TheMealDbMeal> searchMeals(String search);
}
