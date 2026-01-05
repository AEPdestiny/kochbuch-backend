package de.htwberlin.webtech.service;

import de.htwberlin.webtech.model.Recipe;
import de.htwberlin.webtech.repo.RecipeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecipeService {

    @Autowired
    RecipeRepository repo;

    public RecipeService(RecipeRepository repo) {
        this.repo = repo;
    }

    public Recipe create(Recipe recipe) {
        if (recipe.getTitle() == null || recipe.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title cannot be empty.");
        }
        if (recipe.getIngredients() == null || recipe.getIngredients().isBlank()) {
            throw new IllegalArgumentException("Ingredients cannot be empty.");
        }
        if (recipe.getInstructions() == null || recipe.getInstructions().isBlank()) {
            throw new IllegalArgumentException("Instructions cannot be empty.");
        }
        if (!recipe.isPublished()) {
            recipe.setPublished(false);
        }
        return repo.save(recipe);
    }

    public List<Recipe> findAll() {
        return repo.findAll();
    }

    public List<Recipe> findAllPublished() {
        return repo.findByPublishedTrue();
    }

    public Recipe findById(Long id) {
        return repo.findById(id).orElseThrow(() ->
                new IllegalArgumentException("Recipe with ID " + id + " not found."));
    }

    public Recipe update(Long id, Recipe updated) {
        Recipe existing = findById(id);

        existing.setTitle(updated.getTitle());
        existing.setImageUrl(updated.getImageUrl());
        existing.setPrepTimeMinutes(updated.getPrepTimeMinutes());
        existing.setCookTimeMinutes(updated.getCookTimeMinutes());
        existing.setServings(updated.getServings());
        existing.setDifficulty(updated.getDifficulty());
        existing.setCategory(updated.getCategory());
        existing.setRating(updated.getRating());
        existing.setIngredients(updated.getIngredients());
        existing.setInstructions(updated.getInstructions());
        existing.setFavorite(updated.isFavorite());
        existing.setPublished(updated.isPublished());

        return repo.save(existing);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }
}
