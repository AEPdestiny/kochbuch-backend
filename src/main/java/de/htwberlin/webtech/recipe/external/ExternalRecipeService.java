package de.htwberlin.webtech.recipe.external;

import de.htwberlin.webtech.recipe.entity.Recipe;
import de.htwberlin.webtech.recipe.external.dto.TheMealDbMeal;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ExternalRecipeService {

    private static final Logger LOG = Logger.getLogger(ExternalRecipeService.class);
    private static final String DEFAULT_SEARCH = "pasta";
    private static final Duration CACHE_TTL = Duration.ofMinutes(15);
    private static final int MAX_CACHE_ENTRIES = 100;

    private final ExternalRecipeClient client;
    private final Clock clock;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Inject
    public ExternalRecipeService(ExternalRecipeClient client) {
        this(client, Clock.systemUTC());
    }

    ExternalRecipeService(ExternalRecipeClient client, Clock clock) {
        this.client = client;
        this.clock = clock;
    }

    public List<Recipe> fetchExternalRecipes() {
        return fetchExternalRecipes(null);
    }

    public List<Recipe> fetchExternalRecipes(String search) {
        String query = normalizeSearch(search);
        CacheEntry cached = cache.get(query);
        if (cached != null && !cached.isExpired(now())) {
            return cached.recipes();
        }

        try {
            List<Recipe> recipes = client.searchMeals(query).stream()
                    .map(this::mapToRecipe)
                    .toList();
            cache.put(query, new CacheEntry(recipes, now()));
            evictOldestEntriesIfNecessary();
            return recipes;
        } catch (RuntimeException e) {
            if (cached != null) {
                LOG.warnf("Could not refresh external recipes from TheMealDB for search '%s': %s. Returning stale cache.", query, e.getMessage());
                return cached.recipes();
            }
            LOG.warnf("Could not fetch external recipes from TheMealDB for search '%s': %s. Returning empty list.", query, e.getMessage());
            return List.of();
        }
    }

    Recipe mapToRecipe(TheMealDbMeal meal) {
        return new Recipe(
                valueOrDefault(meal.getStrMeal(), "External recipe"),
                valueOrDefault(meal.getStrMealThumb(), ""),
                0,
                0,
                0,
                "",
                firstNonBlank(meal.getStrCategory(), meal.getStrArea()),
                0.0,
                mapIngredients(meal),
                valueOrDefault(meal.getStrInstructions(), "No instructions available."),
                false,
                true
        );
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return DEFAULT_SEARCH;
        }
        return search.trim().toLowerCase();
    }

    private Instant now() {
        return clock.instant();
    }

    private void evictOldestEntriesIfNecessary() {
        if (cache.size() <= MAX_CACHE_ENTRIES) {
            return;
        }

        cache.entrySet().stream()
                .min(Comparator.comparing(entry -> entry.getValue().cachedAt()))
                .map(Map.Entry::getKey)
                .ifPresent(cache::remove);
    }

    private String mapIngredients(TheMealDbMeal meal) {
        List<String> ingredients = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            String ingredient = meal.getIngredient(i);
            if (ingredient == null || ingredient.isBlank()) {
                continue;
            }

            String measure = meal.getMeasure(i);
            if (measure == null || measure.isBlank()) {
                ingredients.add(ingredient.trim());
            } else {
                ingredients.add(measure.trim() + " " + ingredient.trim());
            }
        }
        return String.join(", ", ingredients);
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return "";
    }

    private record CacheEntry(List<Recipe> recipes, Instant cachedAt) {

        private boolean isExpired(Instant now) {
            return cachedAt.plus(CACHE_TTL).isBefore(now);
        }
    }
}
