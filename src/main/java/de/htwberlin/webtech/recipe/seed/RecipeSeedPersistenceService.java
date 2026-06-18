package de.htwberlin.webtech.recipe.seed;

import de.htwberlin.webtech.recipe.entity.Recipe;
import de.htwberlin.webtech.recipe.repository.RecipeRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class RecipeSeedPersistenceService implements RecipeSeedPersistence {

    private final RecipeRepository repository;

    public RecipeSeedPersistenceService(RecipeRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public boolean upsertSeedRecipe(Recipe recipe) {
        Recipe existing = repository.findSeedByExternalIdCategoryAndLanguage(
                        recipe.getExternalId(),
                        recipe.getCategory(),
                        recipe.getLanguage()
                )
                .or(() -> repository.findSeedByTitleCategoryAndLanguage(
                        recipe.getTitle(),
                        recipe.getCategory(),
                        recipe.getLanguage()
                ))
                .orElse(null);

        if (existing != null) {
            updateSeedRecipe(existing, recipe);
            repository.flush();
            return false;
        }

        repository.persist(recipe);
        repository.flush();
        return true;
    }

    private void updateSeedRecipe(Recipe existing, Recipe source) {
        existing.setExternalId(source.getExternalId());
        existing.setTitle(source.getTitle());
        existing.setImageUrl(source.getImageUrl());
        existing.setPrepTimeMinutes(source.getPrepTimeMinutes());
        existing.setCookTimeMinutes(source.getCookTimeMinutes());
        existing.setServings(source.getServings());
        existing.setDifficulty(source.getDifficulty());
        existing.setCategory(source.getCategory());
        existing.setLanguage(source.getLanguage());
        existing.setRating(source.getRating());
        existing.setIngredients(source.getIngredients());
        existing.setInstructions(source.getInstructions());
        existing.setCalories(source.getCalories());
        existing.setProtein(source.getProtein());
        existing.setAlcohol(source.getAlcohol());
        existing.setAlcoholPercent(source.getAlcoholPercent());
        existing.setSourceUrl(source.getSourceUrl());
        existing.setSourceName(source.getSourceName());
        existing.setDishTypes(source.getDishTypes());
        existing.setDiets(source.getDiets());
        existing.setVegetarian(source.isVegetarian());
        existing.setVegan(source.isVegan());
        existing.setGlutenFree(source.isGlutenFree());
        existing.setDairyFree(source.isDairyFree());
        existing.setFavorite(false);
        existing.setPublished(true);
    }
}
