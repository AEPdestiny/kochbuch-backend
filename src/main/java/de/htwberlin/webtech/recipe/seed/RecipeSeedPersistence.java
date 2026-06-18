package de.htwberlin.webtech.recipe.seed;

import de.htwberlin.webtech.recipe.entity.Recipe;

interface RecipeSeedPersistence {

    boolean upsertSeedRecipe(Recipe recipe);
}
