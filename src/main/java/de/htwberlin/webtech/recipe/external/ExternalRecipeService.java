package de.htwberlin.webtech.recipe.external;

import de.htwberlin.webtech.recipe.dto.ExternalRecipeDetailResponse;
import de.htwberlin.webtech.recipe.dto.ExternalRecipeIngredientResponse;
import de.htwberlin.webtech.recipe.dto.ExternalRecipeMatchResponse;
import de.htwberlin.webtech.recipe.dto.RecipeResponse;
import de.htwberlin.webtech.recipe.external.dto.SpoonacularIngredientMatch;
import de.htwberlin.webtech.recipe.external.dto.SpoonacularIngredient;
import de.htwberlin.webtech.recipe.external.dto.SpoonacularInstructionStep;
import de.htwberlin.webtech.recipe.external.dto.SpoonacularNutrient;
import de.htwberlin.webtech.recipe.external.dto.SpoonacularRecipe;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@ApplicationScoped
public class ExternalRecipeService {

    private static final Logger LOG = Logger.getLogger(ExternalRecipeService.class);
    private static final String DEFAULT_SEARCH = "pasta";
    private static final Duration CACHE_TTL = Duration.ofMinutes(15);
    private static final int MAX_CACHE_ENTRIES = 100;
    private static final String SOURCE = "spoonacular";
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]*>");

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

    public List<RecipeResponse> fetchExternalRecipes() {
        return fetchExternalRecipes(null);
    }

    public List<RecipeResponse> fetchExternalRecipes(String search) {
        return fetchExternalRecipes(search, null, null, null, null);
    }

    public List<RecipeResponse> fetchExternalRecipes(String search, String diet, String intolerances, Integer maxReadyTime, String type) {
        String query = normalizeSearch(search);
        String cacheKey = cacheKey(query, diet, intolerances, maxReadyTime, type);
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired(now())) {
            return cached.recipes();
        }

        try {
            List<RecipeResponse> recipes = client.searchRecipes(query, normalizeOptional(diet), normalizeOptional(intolerances), maxReadyTime, normalizeOptional(type)).stream()
                    .map(this::mapToListResponse)
                    .toList();
            cache.put(cacheKey, new CacheEntry(recipes, now()));
            evictOldestEntriesIfNecessary();
            return recipes;
        } catch (RuntimeException e) {
            if (cached != null) {
                LOG.warnf("Could not refresh external recipes from Spoonacular for search '%s': %s. Returning stale cache.", cacheKey, e.getMessage());
                return cached.recipes();
            }
            LOG.warnf("Could not fetch external recipes from Spoonacular for search '%s': %s. Returning empty list.", cacheKey, e.getMessage());
            return List.of();
        }
    }

    public Optional<ExternalRecipeDetailResponse> fetchExternalRecipeDetail(Long id) {
        try {
            return client.getRecipeInformation(id).map(this::mapToDetailResponse);
        } catch (RuntimeException e) {
            LOG.warnf("Could not fetch external recipe detail from Spoonacular for id '%s': %s.", id, e.getMessage());
            return Optional.empty();
        }
    }

    public List<ExternalRecipeMatchResponse> findRecipesByIngredients(List<String> ingredients) {
        List<String> normalized = ingredients == null ? List.of() : ingredients.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            return List.of();
        }

        try {
            return client.findByIngredients(normalized).stream()
                    .map(this::mapToMatchResponse)
                    .toList();
        } catch (RuntimeException e) {
            LOG.warnf("Could not fetch external recipes by ingredients from Spoonacular: %s. Returning empty list.", e.getMessage());
            return List.of();
        }
    }

    RecipeResponse mapToListResponse(SpoonacularRecipe recipe) {
        RecipeResponse response = new RecipeResponse();
        response.setId(recipe.getId());
        response.setExternalId(recipe.getId() == null ? null : recipe.getId().toString());
        response.setSource(SOURCE);
        response.setTitle(valueOrDefault(recipe.getTitle(), "External recipe"));
        response.setImageUrl(valueOrDefault(recipe.getImage(), ""));
        response.setPrepTimeMinutes(0);
        response.setCookTimeMinutes(valueOrZero(recipe.getReadyInMinutes()));
        response.setServings(valueOrZero(recipe.getServings()));
        response.setDifficulty("");
        response.setCategory(firstString(recipe.getDishTypes()));
        response.setRating(0.0);
        response.setIngredients(mapIngredientsText(recipe.getExtendedIngredients()));
        response.setInstructions(firstNonBlank(stripHtml(recipe.getInstructions()), stripHtml(recipe.getSummary()), "No instructions available."));
        response.setFavorite(false);
        response.setPublished(true);
        response.setCalories(findCalories(recipe));
        response.setSourceUrl(recipe.getSourceUrl());
        return response;
    }

    ExternalRecipeDetailResponse mapToDetailResponse(SpoonacularRecipe recipe) {
        ExternalRecipeDetailResponse response = new ExternalRecipeDetailResponse();
        response.setId(recipe.getId());
        response.setExternalId(recipe.getId() == null ? null : recipe.getId().toString());
        response.setSource(SOURCE);
        response.setTitle(valueOrDefault(recipe.getTitle(), "External recipe"));
        response.setImageUrl(valueOrDefault(recipe.getImage(), ""));
        response.setPrepTimeMinutes(0);
        response.setCookTimeMinutes(valueOrZero(recipe.getReadyInMinutes()));
        response.setReadyInMinutes(valueOrZero(recipe.getReadyInMinutes()));
        response.setServings(valueOrZero(recipe.getServings()));
        response.setCategory(firstString(recipe.getDishTypes()));
        response.setTags(mapTags(recipe));
        response.setCalories(findCalories(recipe));
        response.setIngredients(mapIngredients(recipe.getExtendedIngredients()));
        response.setInstructions(firstNonBlank(stripHtml(recipe.getInstructions()), stripHtml(recipe.getSummary()), ""));
        response.setSteps(mapSteps(recipe));
        response.setSourceUrl(recipe.getSourceUrl());
        return response;
    }

    ExternalRecipeMatchResponse mapToMatchResponse(SpoonacularIngredientMatch match) {
        ExternalRecipeMatchResponse response = new ExternalRecipeMatchResponse();
        response.setId(match.getId());
        response.setExternalId(match.getId() == null ? null : match.getId().toString());
        response.setSource(SOURCE);
        response.setTitle(valueOrDefault(match.getTitle(), "External recipe"));
        response.setImageUrl(valueOrDefault(match.getImage(), ""));
        response.setUsedIngredientCount(valueOrZero(match.getUsedIngredientCount()));
        response.setMissedIngredientCount(valueOrZero(match.getMissedIngredientCount()));
        response.setUsedIngredients(mapIngredientNames(match.getUsedIngredients()));
        response.setMissedIngredients(mapIngredientNames(match.getMissedIngredients()));
        return response;
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return DEFAULT_SEARCH;
        }
        return search.trim().toLowerCase();
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase();
    }

    private String cacheKey(String search, String diet, String intolerances, Integer maxReadyTime, String type) {
        return String.join("|",
                search,
                valueOrDefault(normalizeOptional(diet), ""),
                valueOrDefault(normalizeOptional(intolerances), ""),
                maxReadyTime == null ? "" : maxReadyTime.toString(),
                valueOrDefault(normalizeOptional(type), "")
        );
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

    private String mapIngredientsText(List<SpoonacularIngredient> ingredients) {
        return mapIngredients(ingredients).stream()
                .map(ingredient -> firstNonBlank(ingredient.getOriginal(), formatIngredient(ingredient), ingredient.getName()))
                .filter(value -> value != null && !value.isBlank())
                .toList()
                .stream()
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }

    private List<ExternalRecipeIngredientResponse> mapIngredients(List<SpoonacularIngredient> ingredients) {
        if (ingredients == null) {
            return List.of();
        }
        return ingredients.stream()
                .map(this::mapIngredient)
                .toList();
    }

    private List<String> mapIngredientNames(List<SpoonacularIngredient> ingredients) {
        if (ingredients == null) {
            return List.of();
        }
        return ingredients.stream()
                .map(ingredient -> firstNonBlank(ingredient.getName(), ingredient.getOriginal()))
                .filter(value -> value != null && !value.isBlank())
                .toList();
    }

    private ExternalRecipeIngredientResponse mapIngredient(SpoonacularIngredient ingredient) {
        ExternalRecipeIngredientResponse response = new ExternalRecipeIngredientResponse();
        response.setName(valueOrDefault(ingredient.getName(), firstNonBlank(ingredient.getOriginal(), "Ingredient")));
        response.setOriginal(firstNonBlank(ingredient.getOriginal(), formatIngredient(response)));
        response.setAmount(ingredient.getAmount());
        response.setUnit(ingredient.getUnit());
        return response;
    }

    private String formatIngredient(ExternalRecipeIngredientResponse ingredient) {
        String amount = ingredient.getAmount() == null ? "" : ingredient.getAmount().stripTrailingZeros().toPlainString();
        return (amount + " " + valueOrDefault(ingredient.getUnit(), "") + " " + valueOrDefault(ingredient.getName(), "")).trim();
    }

    private List<String> mapSteps(SpoonacularRecipe recipe) {
        if (recipe.getAnalyzedInstructions() == null) {
            return List.of();
        }
        return recipe.getAnalyzedInstructions().stream()
                .filter(instruction -> instruction.getSteps() != null)
                .flatMap(instruction -> instruction.getSteps().stream())
                .map(SpoonacularInstructionStep::getStep)
                .filter(step -> step != null && !step.isBlank())
                .map(String::trim)
                .toList();
    }

    private List<String> mapTags(SpoonacularRecipe recipe) {
        List<String> dishTypes = recipe.getDishTypes() == null ? List.of() : recipe.getDishTypes();
        List<String> diets = recipe.getDiets() == null ? List.of() : recipe.getDiets();
        return java.util.stream.Stream.concat(dishTypes.stream(), diets.stream())
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
    }

    private Integer findCalories(SpoonacularRecipe recipe) {
        if (recipe.getNutrition() == null || recipe.getNutrition().getNutrients() == null) {
            return null;
        }
        return recipe.getNutrition().getNutrients().stream()
                .filter(nutrient -> nutrient.getName() != null && nutrient.getName().equalsIgnoreCase("Calories"))
                .map(SpoonacularNutrient::getAmount)
                .filter(amount -> amount != null)
                .findFirst()
                .map(amount -> amount.setScale(0, RoundingMode.HALF_UP).intValue())
                .orElse(null);
    }

    private String firstString(List<String> values) {
        if (values == null) {
            return "";
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("");
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String stripHtml(String value) {
        if (value == null) {
            return null;
        }
        return HTML_TAG.matcher(value).replaceAll("").trim();
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

    private String firstNonBlank(String first, String second, String third) {
        String firstResult = firstNonBlank(first, second);
        if (!firstResult.isBlank()) {
            return firstResult;
        }
        return valueOrDefault(third, "");
    }

    private record CacheEntry(List<RecipeResponse> recipes, Instant cachedAt) {

        private boolean isExpired(Instant now) {
            return cachedAt.plus(CACHE_TTL).isBefore(now);
        }
    }
}
