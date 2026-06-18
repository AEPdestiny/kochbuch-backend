package de.htwberlin.webtech.recipe.mapper;

import de.htwberlin.webtech.recipe.dto.RecipeRequest;
import de.htwberlin.webtech.recipe.dto.RecipeResponse;
import de.htwberlin.webtech.recipe.entity.Recipe;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class RecipeMapper {

    public Recipe toEntity(RecipeRequest request) {
        Recipe recipe = new Recipe();
        updateEntity(recipe, request);
        return recipe;
    }

    public RecipeResponse toResponse(Recipe recipe) {
        RecipeResponse response = new RecipeResponse();
        response.setId(recipe.getId());
        response.setTitle(recipe.getTitle());
        response.setImageUrl(recipe.getImageUrl());
        response.setPrepTimeMinutes(recipe.getPrepTimeMinutes());
        response.setCookTimeMinutes(recipe.getCookTimeMinutes());
        response.setServings(recipe.getServings());
        response.setDifficulty(recipe.getDifficulty());
        response.setCategory(recipe.getCategory());
        response.setRating(recipe.getRating());
        response.setIngredients(recipe.getIngredients());
        response.setInstructions(recipe.getInstructions());
        response.setFavorite(recipe.isFavorite());
        response.setPublished(recipe.isPublished());
        response.setCalories(recipe.getCalories());
        response.setProtein(recipe.getProtein());
        response.setAlcohol(recipe.getAlcohol());
        response.setAlcoholPercent(recipe.getAlcoholPercent());
        response.setUserCreated(recipe.getOwner() != null);
        response.setLanguage(recipe.getLanguage());
        response.setExternalId(recipe.getExternalId());
        response.setSource(recipe.getExternalId() == null ? "dishly" : "spoonacular");
        response.setSourceUrl(recipe.getSourceUrl());
        response.setSourceName(recipe.getSourceName());
        response.setDishTypes(recipe.getDishTypes());
        response.setDiets(recipe.getDiets());
        response.setVegetarian(recipe.isVegetarian());
        response.setVegan(recipe.isVegan());
        response.setGlutenFree(recipe.isGlutenFree());
        response.setDairyFree(recipe.isDairyFree());
        return response;
    }

    public List<RecipeResponse> toResponseList(List<Recipe> recipes) {
        return recipes.stream()
                .map(this::toResponse)
                .toList();
    }

    public void updateEntity(Recipe recipe, RecipeRequest request) {
        recipe.setTitle(request.getTitle());
        recipe.setImageUrl(request.getImageUrl());
        recipe.setPrepTimeMinutes(request.getPrepTimeMinutes());
        recipe.setCookTimeMinutes(request.getCookTimeMinutes());
        recipe.setServings(request.getServings());
        recipe.setDifficulty(request.getDifficulty());
        recipe.setCategory(request.getCategory());
        recipe.setRating(request.getRating());
        recipe.setIngredients(request.getIngredients());
        recipe.setInstructions(request.getInstructions());
        recipe.setFavorite(request.isFavorite());
        recipe.setPublished(request.isPublished());
        recipe.setCalories(request.getCalories());
        recipe.setProtein(request.getProtein());
        recipe.setAlcohol(request.getAlcohol());
        recipe.setAlcoholPercent(request.getAlcoholPercent());
        recipe.setLanguage(request.getLanguage());
    }

    public void updateEntity(Recipe recipe, Recipe source) {
        recipe.setTitle(source.getTitle());
        recipe.setImageUrl(source.getImageUrl());
        recipe.setPrepTimeMinutes(source.getPrepTimeMinutes());
        recipe.setCookTimeMinutes(source.getCookTimeMinutes());
        recipe.setServings(source.getServings());
        recipe.setDifficulty(source.getDifficulty());
        recipe.setCategory(source.getCategory());
        recipe.setRating(source.getRating());
        recipe.setIngredients(source.getIngredients());
        recipe.setInstructions(source.getInstructions());
        recipe.setFavorite(source.isFavorite());
        recipe.setPublished(source.isPublished());
        recipe.setCalories(source.getCalories());
        recipe.setProtein(source.getProtein());
        recipe.setAlcohol(source.getAlcohol());
        recipe.setAlcoholPercent(source.getAlcoholPercent());
        recipe.setLanguage(source.getLanguage());
    }
}
