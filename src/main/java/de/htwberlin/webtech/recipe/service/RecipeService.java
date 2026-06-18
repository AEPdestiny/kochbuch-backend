package de.htwberlin.webtech.recipe.service;

import de.htwberlin.webtech.recipe.entity.Recipe;
import de.htwberlin.webtech.recipe.exception.RecipeNotFoundException;
import de.htwberlin.webtech.recipe.mapper.RecipeMapper;
import de.htwberlin.webtech.recipe.repository.RecipeRepository;
import de.htwberlin.webtech.shared.exception.ForbiddenException;
import de.htwberlin.webtech.user.entity.AppUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class RecipeService {

    private static final int PUBLIC_RECIPE_LIMIT = 100;
    private static final int CATEGORY_TARGET = 25;
    private static final List<String> PUBLIC_CATEGORIES = List.of("breakfast", "lunch", "dinner", "snack");

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

    public List<Recipe> findAll(String language) {
        return findAllPublished(language);
    }

    public List<Recipe> findAllPublished() {
        return findAllPublished("en");
    }

    public List<Recipe> findAllPublished(String language) {
        String normalizedLanguage = normalizeLanguage(language);
        List<Recipe> balanced = new ArrayList<>();
        Set<Long> seenIds = new LinkedHashSet<>();

        for (String category : PUBLIC_CATEGORIES) {
            addUnique(balanced, seenIds, repo.findRandomPublishedByLanguageAndCategory(normalizedLanguage, category, CATEGORY_TARGET));
        }

        if (balanced.size() < PUBLIC_RECIPE_LIMIT) {
            addUnique(balanced, seenIds, repo.findRandomPublishedByLanguage(normalizedLanguage, PUBLIC_RECIPE_LIMIT));
        }

        return balanced.stream()
                .limit(PUBLIC_RECIPE_LIMIT)
                .toList();
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

    private String normalizeLanguage(String language) {
        return language == null || language.isBlank() ? "en" : language.trim().toLowerCase();
    }

    private void addUnique(List<Recipe> target, Set<Long> seenIds, List<Recipe> candidates) {
        for (Recipe candidate : candidates) {
            Long id = candidate.getId();
            if (id != null && !seenIds.add(id)) {
                continue;
            }
            target.add(candidate);
        }
    }
}
