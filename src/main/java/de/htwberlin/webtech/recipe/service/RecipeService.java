package de.htwberlin.webtech.recipe.service;

import de.htwberlin.webtech.recipe.entity.Recipe;
import de.htwberlin.webtech.recipe.exception.RecipeNotFoundException;
import de.htwberlin.webtech.recipe.mapper.RecipeMapper;
import de.htwberlin.webtech.recipe.repository.RecipeRepository;
import de.htwberlin.webtech.shared.exception.ForbiddenException;
import de.htwberlin.webtech.user.entity.AppUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class RecipeService {

    private final RecipeRepository repo;
    private final RecipeMapper mapper;

    public RecipeService(RecipeRepository repo, RecipeMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    @Transactional
    public Recipe create(Recipe recipe) {
        validate(recipe);
        if (!recipe.isPublished()) {
            recipe.setPublished(false);
        }
        repo.persist(recipe);
        return recipe;
    }

    @Transactional
    public Recipe create(Recipe recipe, AppUser owner) {
        recipe.setOwner(owner);
        return create(recipe);
    }

    public List<Recipe> findAll() {
        return findAllPublished();
    }

    public List<Recipe> findAllPublished() {
        return repo.findPublished();
    }

    public List<Recipe> findMine(AppUser currentUser) {
        return repo.findByOwner(currentUser);
    }

    public Recipe findById(Long id) {
        Recipe recipe = repo.findById(id);
        if (recipe == null) {
            throw new RecipeNotFoundException(id);
        }
        return recipe;
    }

    public Recipe findVisibleById(Long id, AppUser currentUser) {
        Recipe recipe = findById(id);
        if (recipe.isPublished() || isOwner(recipe, currentUser)) {
            return recipe;
        }
        throw new RecipeNotFoundException(id);
    }

    @Transactional
    public Recipe update(Long id, Recipe updated) {
        validate(updated);
        Recipe existing = findById(id);
        mapper.updateEntity(existing, updated);

        return existing;
    }

    @Transactional
    public Recipe update(Long id, Recipe updated, AppUser currentUser) {
        validate(updated);
        Recipe existing = findById(id);
        ensureOwner(existing, currentUser);
        mapper.updateEntity(existing, updated);

        return existing;
    }

    @Transactional
    public void delete(Long id) {
        boolean deleted = repo.deleteById(id);
        if (!deleted) {
            throw new RecipeNotFoundException(id);
        }
    }

    @Transactional
    public void delete(Long id, AppUser currentUser) {
        Recipe existing = findById(id);
        ensureOwner(existing, currentUser);
        repo.delete(existing);
    }

    private void validate(Recipe recipe) {
        if (recipe.getTitle() == null || recipe.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title cannot be empty.");
        }
        if (recipe.getIngredients() == null || recipe.getIngredients().isBlank()) {
            throw new IllegalArgumentException("Ingredients cannot be empty.");
        }
        if (recipe.getInstructions() == null || recipe.getInstructions().isBlank()) {
            throw new IllegalArgumentException("Instructions cannot be empty.");
        }
    }

    private void ensureOwner(Recipe recipe, AppUser currentUser) {
        if (recipe.getOwner() == null) {
            throw new ForbiddenException("Only the recipe owner may access this recipe.");
        }
        if (!isOwner(recipe, currentUser)) {
            throw new ForbiddenException("Only the recipe owner may access this recipe.");
        }
    }

    private boolean isOwner(Recipe recipe, AppUser currentUser) {
        return recipe.getOwner() != null
                && currentUser != null
                && currentUser.getId() != null
                && currentUser.getId().equals(recipe.getOwner().getId());
    }
}
