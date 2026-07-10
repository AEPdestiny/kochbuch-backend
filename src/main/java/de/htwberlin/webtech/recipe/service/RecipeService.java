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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Core CRUD and query logic for recipes: creation/update/delete with ownership checks,
 * the caller's own recipes, and the public recipe feed. The public feed is not a plain
 * "latest N" query — {@link #findAllPublished(String, String)} balances results across
 * {@link #PUBLIC_CATEGORIES} and deduplicates near-identical entries (see
 * {@link #duplicateKey} / {@link #qualityScore}) so the feed doesn't collapse into one
 * category or show the same recipe twice under slightly different metadata.
 */
@ApplicationScoped
public class RecipeService {

    private static final int PUBLIC_RECIPE_LIMIT = 100;
    private static final int CATEGORY_TARGET = 25;
    private static final int CATEGORY_QUERY_LIMIT = 75;
    private static final int PUBLIC_QUERY_LIMIT = 200;
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
        return findAllPublished(language, null);
    }

    /**
     * Public recipe feed for the given language, optionally filtered by a search term.
     * With a search term: a single substring search, deduplicated and capped at
     * {@link #PUBLIC_RECIPE_LIMIT}. Without one: fills a balanced result set by querying
     * each of {@link #PUBLIC_CATEGORIES} up to {@link #CATEGORY_TARGET} entries each, then
     * tops up with a general random query if categories didn't fill the page — so the
     * default feed shows a mix of meal types instead of whichever category has the most rows.
     */
    public List<Recipe> findAllPublished(String language, String search) {
        String normalizedLanguage = normalizeLanguage(language);
        String normalizedSearch = search == null ? "" : search.trim();
        if (!normalizedSearch.isBlank()) {
            return deduplicatePublicRecipes(
                    repo.searchRandomPublishedByLanguage(normalizedLanguage, normalizedSearch, PUBLIC_QUERY_LIMIT),
                    null
            ).stream()
                    .limit(PUBLIC_RECIPE_LIMIT)
                    .toList();
        }

        List<Recipe> balanced = new ArrayList<>();
        Map<String, Integer> seenKeys = new HashMap<>();

        for (String category : PUBLIC_CATEGORIES) {
            addBestUnique(
                    balanced,
                    seenKeys,
                    repo.findRandomPublishedByLanguageAndCategory(normalizedLanguage, category, CATEGORY_QUERY_LIMIT),
                    category,
                    CATEGORY_TARGET
            );
        }

        if (balanced.size() < PUBLIC_RECIPE_LIMIT) {
            addBestUnique(
                    balanced,
                    seenKeys,
                    repo.findRandomPublishedByLanguage(normalizedLanguage, PUBLIC_QUERY_LIMIT),
                    null,
                    PUBLIC_RECIPE_LIMIT - balanced.size()
            );
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
        return findVisibleById(id, currentUser, null);
    }

    public Recipe findVisibleById(Long id, AppUser currentUser, String language) {
        Recipe recipe = findById(id);
        if (recipe.isPublished() || isOwner(recipe, currentUser)) {
            if (recipe.isPublished() && !isOwner(recipe, currentUser) && hasText(language)) {
                String requestedLanguage = normalizeLanguage(language);
                String recipeLanguage = normalizeLanguage(recipe.getLanguage());
                if (!requestedLanguage.equals(recipeLanguage)) {
                    return repo.findPublishedSeedSibling(recipe, requestedLanguage).orElse(recipe);
                }
            }
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

    private List<Recipe> deduplicatePublicRecipes(List<Recipe> candidates, String preferredCategory) {
        List<Recipe> result = new ArrayList<>();
        Map<String, Integer> seenKeys = new HashMap<>();
        addBestUnique(result, seenKeys, candidates, preferredCategory, PUBLIC_RECIPE_LIMIT);
        return result;
    }

    private void addBestUnique(
            List<Recipe> target,
            Map<String, Integer> seenKeys,
            List<Recipe> candidates,
            String preferredCategory,
            int maxNewEntries
    ) {
        int added = 0;
        for (Recipe candidate : candidates) {
            String key = duplicateKey(candidate);
            Integer existingIndex = seenKeys.get(key);
            if (existingIndex != null) {
                Recipe existing = target.get(existingIndex);
                if (isBetterPublicCandidate(candidate, existing, preferredCategory)) {
                    target.set(existingIndex, candidate);
                }
                continue;
            }
            if (added >= maxNewEntries) {
                continue;
            }
            target.add(candidate);
            seenKeys.put(key, target.size() - 1);
            added++;
        }
    }

    // Identity for deduplication: a user's own recipes are always distinct by id; external/
    // seeded recipes without an owner are matched by external id, then source URL, then a
    // normalized title+image pair — so the same recipe imported twice under different rows
    // still collapses to one feed entry.
    private String duplicateKey(Recipe recipe) {
        if (recipe.getOwner() != null) {
            return "user:" + (recipe.getId() == null ? System.identityHashCode(recipe) : recipe.getId());
        }
        String language = normalizeLanguage(recipe.getLanguage());
        if (hasText(recipe.getExternalId())) {
            return language + "|external:" + recipe.getExternalId().trim().toLowerCase(Locale.ROOT);
        }
        if (hasText(recipe.getSourceUrl())) {
            return language + "|url:" + recipe.getSourceUrl().trim().toLowerCase(Locale.ROOT);
        }
        return language
                + "|title-image:"
                + normalizeDuplicateText(recipe.getTitle())
                + "|"
                + normalizeDuplicateText(recipe.getImageUrl());
    }

    private boolean isBetterPublicCandidate(Recipe candidate, Recipe existing, String preferredCategory) {
        int candidateScore = qualityScore(candidate, preferredCategory);
        int existingScore = qualityScore(existing, preferredCategory);
        if (candidateScore != existingScore) {
            return candidateScore > existingScore;
        }
        if (candidate.getId() == null) {
            return false;
        }
        return existing.getId() == null || candidate.getId() < existing.getId();
    }

    // Ranks duplicate candidates so the "best" version wins the slot: published beats
    // unpublished, having an image beats not, matching the currently-queried category beats
    // not, and richer ingredients/instructions break remaining ties.
    private int qualityScore(Recipe recipe, String preferredCategory) {
        int score = 0;
        if (recipe.isPublished()) {
            score += 100;
        }
        if (hasText(recipe.getImageUrl())) {
            score += 20;
        }
        if (hasText(preferredCategory) && preferredCategory.equalsIgnoreCase(recipe.getCategory())) {
            score += 15;
        }
        score += Math.min(30, RecipeIngredientNormalizer.countIngredients(recipe.getIngredients()) * 3);
        score += Math.min(30, recipe.getInstructions() == null ? 0 : recipe.getInstructions().trim().length() / 80);
        return score;
    }

    private String normalizeDuplicateText(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
