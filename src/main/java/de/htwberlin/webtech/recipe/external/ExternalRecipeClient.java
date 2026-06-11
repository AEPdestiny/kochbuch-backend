package de.htwberlin.webtech.recipe.external;

import de.htwberlin.webtech.recipe.external.dto.SpoonacularRecipe;
import de.htwberlin.webtech.recipe.external.dto.SpoonacularIngredientMatch;

import java.util.List;
import java.util.Optional;

public interface ExternalRecipeClient {

    List<SpoonacularRecipe> searchRecipes(String search);

    Optional<SpoonacularRecipe> getRecipeInformation(Long id);

    List<SpoonacularIngredientMatch> findByIngredients(List<String> ingredients);
}
