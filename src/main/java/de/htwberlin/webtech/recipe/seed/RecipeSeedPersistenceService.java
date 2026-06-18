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
    public long deleteInvalidSeedRecipesWithoutIngredients() {
        return repository.deleteInvalidSeedRecipesWithoutIngredients();
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public boolean persistIfMissing(Recipe recipe) {
        if (repository.findByTitleAndCategoryAndLanguage(
                recipe.getTitle(),
                recipe.getCategory(),
                recipe.getLanguage()
        ).isPresent()) {
            return false;
        }

        repository.persist(recipe);
        repository.flush();
        return true;
    }
}
