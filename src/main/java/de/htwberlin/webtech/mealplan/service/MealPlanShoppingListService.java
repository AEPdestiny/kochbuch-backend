package de.htwberlin.webtech.mealplan.service;

import de.htwberlin.webtech.mealplan.dto.MealPlanShoppingListItemResponse;
import de.htwberlin.webtech.mealplan.dto.MealPlanShoppingListResponse;
import de.htwberlin.webtech.mealplan.entity.MealPlan;
import de.htwberlin.webtech.mealplan.repository.MealPlanRepository;
import de.htwberlin.webtech.pantry.entity.PantryItem;
import de.htwberlin.webtech.pantry.repository.PantryItemRepository;
import de.htwberlin.webtech.recipe.entity.Recipe;
import de.htwberlin.webtech.recipe.service.RecipeIngredientNormalizer;
import de.htwberlin.webtech.shopping.entity.ShoppingListItem;
import de.htwberlin.webtech.shopping.repository.ShoppingListItemRepository;
import de.htwberlin.webtech.user.entity.AppUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Turns a week's meal plan into a shopping list: aggregates every planned recipe's
 * ingredients (summing quantities for the same ingredient across recipes), converts units
 * to a common base per {@link #unitFamily(String)}/{@link #canonicalUnit(String)} so e.g.
 * "500 g" and "0.5 kg" of the same ingredient combine correctly, then subtracts what the
 * user already has in their pantry. Entries the algorithm can't confidently parse or match
 * (freetext meal plan entries, unparsable quantities, unit families it can't compare) are
 * routed to the response's "needs review" list instead of silently guessing.
 */
@ApplicationScoped
public class MealPlanShoppingListService {

    private static final String CATEGORY = "Wochenplan";
    private static final Set<String> GRAM_UNITS = Set.of("g", "kg", "oz", "lb");
    private static final Set<String> MILLILITER_UNITS = Set.of("ml", "l");
    private static final Set<String> COUNT_UNITS = Set.of(
            "piece", "stk", "ei", "eier", "clove", "stalk", "slice", "package", "can", "bunch"
    );
    private static final Set<String> UNSAFE_COUNT_UNITS = Set.of(
            "tsp", "tbsp", "pinch", "cup"
    );
    private static final Set<String> ALL_UNITS = union(GRAM_UNITS, MILLILITER_UNITS, COUNT_UNITS, UNSAFE_COUNT_UNITS);

    private final MealPlanRepository mealPlanRepository;
    private final PantryItemRepository pantryItemRepository;
    private final ShoppingListItemRepository shoppingListItemRepository;

    public MealPlanShoppingListService(MealPlanRepository mealPlanRepository,
                                       PantryItemRepository pantryItemRepository,
                                       ShoppingListItemRepository shoppingListItemRepository) {
        this.mealPlanRepository = mealPlanRepository;
        this.pantryItemRepository = pantryItemRepository;
        this.shoppingListItemRepository = shoppingListItemRepository;
    }

    @Transactional
    public MealPlanShoppingListResponse createShoppingList(AppUser currentUser, LocalDate weekStart) {
        MealPlanShoppingListResponse response = new MealPlanShoppingListResponse();
        LocalDate weekEnd = weekStart.plusDays(6);
        List<MealPlan> entries = mealPlanRepository.findByOwnerAndPlannedDateBetween(currentUser, weekStart, weekEnd);
        List<PantryItem> pantryItems = pantryItemRepository.findByOwner(currentUser);
        List<ShoppingListItem> shoppingItems = shoppingListItemRepository.findByOwner(currentUser);

        Map<String, IngredientNeed> needs = collectNeeds(entries, response);
        for (IngredientNeed need : needs.values()) {
            handleNeed(currentUser, need, pantryItems, shoppingItems, response);
        }
        return response;
    }

    private Map<String, IngredientNeed> collectNeeds(List<MealPlan> entries, MealPlanShoppingListResponse response) {
        Map<String, IngredientNeed> needs = new LinkedHashMap<>();
        for (MealPlan entry : entries) {
            Recipe recipe = entry.getRecipe();
            if (recipe == null) {
                response.getNeedsReview().add(item(
                        entry.getCustomTitle(),
                        null,
                        null,
                        entry.getCustomTitle(),
                        entry.getCustomTitle(),
                        "Freitext-Eintrag hat keine strukturierten Zutaten."
                ));
                continue;
            }
            List<String> ingredients = RecipeIngredientNormalizer.normalizeToList(recipe.getIngredients());
            if (ingredients.isEmpty()) {
                response.getNeedsReview().add(item(
                        recipe.getTitle(),
                        null,
                        null,
                        recipe.getTitle(),
                        "",
                        "Rezept hat keine auswertbaren Zutaten."
                ));
                continue;
            }
            for (String ingredient : ingredients) {
                ParsedIngredient parsed = parseIngredient(ingredient, recipe.getTitle());
                if (parsed.name().isBlank()) {
                    continue;
                }
                String key = aggregateKey(parsed);
                needs.compute(key, (ignored, existing) -> merge(existing, parsed));
            }
        }
        return needs;
    }

    private void handleNeed(AppUser currentUser,
                            IngredientNeed need,
                            List<PantryItem> pantryItems,
                            List<ShoppingListItem> shoppingItems,
                            MealPlanShoppingListResponse response) {
        IngredientNeed remainingNeed = need;
        IngredientNeed lookupNeed = remainingNeed;
        // Name-only matching: if the same ingredient already exists on the shopping list
        // (regardless of unit format), skip it to prevent duplicates on re-runs.
        Optional<ShoppingListItem> existingShoppingItem = shoppingItems.stream()
                .filter(item -> equivalentName(item.getName(), lookupNeed.name()))
                .findFirst();
        if (existingShoppingItem.isPresent()) {
            response.getAlreadyOnShoppingList().add(toResponse(remainingNeed, "Bereits auf der Einkaufsliste."));
            return;
        }

        Optional<PantryItem> pantryMatch = pantryItems.stream()
                .filter(item -> equivalentName(item.getName(), lookupNeed.name()))
                .findFirst();
        if (pantryMatch.isPresent()) {
            PantryItem pantryItem = pantryMatch.get();
            MatchResult result = compareWithPantry(remainingNeed, pantryItem);
            if (result.status() == MatchStatus.COVERED) {
                response.getSkippedBecauseInPantry().add(toResponse(remainingNeed, "Bereits ausreichend im Vorrat."));
                return;
            }
            if (result.status() == MatchStatus.NEEDS_REVIEW) {
                response.getNeedsReview().add(toResponse(remainingNeed, result.reason()));
                return;
            }
            remainingNeed = remainingNeed.withQuantity(result.remainingQuantity());
        }

        ShoppingListItem item = new ShoppingListItem();
        item.setOwner(currentUser);
        item.setName(remainingNeed.name());
        item.setQuantity(remainingNeed.quantity());
        item.setUnit(remainingNeed.unit());
        item.setCategory(CATEGORY);
        item.setRecipeId("meal-plan");
        item.setRecipeTitle(remainingNeed.recipeTitle());
        item.setChecked(false);
        shoppingListItemRepository.persist(item);
        response.getAdded().add(toResponse(remainingNeed, "Zur Einkaufsliste hinzugefügt."));
    }

    private MatchResult compareWithPantry(IngredientNeed need, PantryItem pantryItem) {
        if (need.quantity() == null || pantryItem.getQuantity() == null) {
            return new MatchResult(MatchStatus.NEEDS_REVIEW, null, "Menge im Rezept oder Vorrat ist nicht eindeutig.");
        }
        if (!compatibleUnits(need.unit(), pantryItem.getUnit())) {
            return new MatchResult(MatchStatus.NEEDS_REVIEW, null, "Einheiten sind nicht sicher vergleichbar.");
        }
        BigDecimal needBase = toBaseQuantity(need.quantity(), need.unit());
        BigDecimal pantryBase = toBaseQuantity(pantryItem.getQuantity(), pantryItem.getUnit());
        if (needBase == null || pantryBase == null) {
            return new MatchResult(MatchStatus.NEEDS_REVIEW, null, "Einheiten sind nicht sicher vergleichbar.");
        }
        if (pantryBase.compareTo(needBase) >= 0) {
            return new MatchResult(MatchStatus.COVERED, null, "Bereits ausreichend im Vorrat.");
        }
        BigDecimal remainingBase = needBase.subtract(pantryBase);
        BigDecimal remaining = fromBaseQuantity(remainingBase, need.unit());
        return new MatchResult(MatchStatus.ADD_REMAINING, remaining, "Teilmenge fehlt.");
    }

    private IngredientNeed merge(IngredientNeed existing, ParsedIngredient parsed) {
        if (existing == null) {
            return new IngredientNeed(parsed.name(), parsed.quantity(), parsed.unit(), parsed.recipeTitle(), parsed.rawIngredient());
        }
        if (existing.quantity() != null && parsed.quantity() != null && Objects.equals(unitFamily(existing.unit()), unitFamily(parsed.unit()))) {
            BigDecimal existingBase = toBaseQuantity(existing.quantity(), existing.unit());
            BigDecimal parsedBase = toBaseQuantity(parsed.quantity(), parsed.unit());
            if (existingBase != null && parsedBase != null) {
                BigDecimal mergedQuantity = fromBaseQuantity(existingBase.add(parsedBase), existing.unit());
                return existing.withQuantity(mergedQuantity);
            }
        }
        return existing.withRawIngredient(existing.rawIngredient() + "\n" + parsed.rawIngredient());
    }

    private ParsedIngredient parseIngredient(String ingredient, String recipeTitle) {
        String cleaned = normalizeQuantityGlyphs(ingredient == null ? "" : ingredient.trim())
                .replaceFirst("^[\\s\\u2022\\u00B7\\u25CF*\\-\\u2013\\u2014.:)]+", "")
                .replaceAll("\\s+", " ");
        if (cleaned.isBlank()) {
            return new ParsedIngredient("", null, null, recipeTitle, ingredient);
        }
        String[] parts = cleaned.split(" ", 3);
        BigDecimal quantity = parseQuantity(parts[0]);
        if (quantity == null) {
            ParsedIngredient unitOnly = parseLeadingUnitWithoutQuantity(cleaned, recipeTitle, ingredient);
            if (!unitOnly.name().isBlank()) {
                return unitOnly;
            }
            return new ParsedIngredient(cleaned, null, null, recipeTitle, ingredient);
        }
        if (parts.length == 1) {
            return new ParsedIngredient(cleaned, null, null, recipeTitle, ingredient);
        }
        String possibleUnit = canonicalUnit(parts[1]);
        if (ALL_UNITS.contains(possibleUnit) && parts.length == 3) {
            return new ParsedIngredient(parts[2].trim(), quantity, possibleUnit, recipeTitle, ingredient);
        }
        String name = cleaned.substring(parts[0].length()).trim();
        return new ParsedIngredient(name, quantity, null, recipeTitle, ingredient);
    }

    private ParsedIngredient parseLeadingUnitWithoutQuantity(String cleaned, String recipeTitle, String rawIngredient) {
        String[] parts = cleaned.split(" ", 4);
        int index = 0;
        while (index < parts.length && isSizeDescriptor(parts[index])) {
            index++;
        }
        if (index < parts.length - 1) {
            String possibleUnit = canonicalUnit(parts[index]);
            if (ALL_UNITS.contains(possibleUnit)) {
                String name = String.join(" ", java.util.Arrays.copyOfRange(parts, index + 1, parts.length)).trim();
                return new ParsedIngredient(name, null, possibleUnit, recipeTitle, rawIngredient);
            }
        }
        return new ParsedIngredient("", null, null, recipeTitle, rawIngredient);
    }

    private boolean isSizeDescriptor(String value) {
        String normalized = normalizeName(value);
        return normalized.equals("large")
                || normalized.equals("small")
                || normalized.equals("medium")
                || normalized.equals("gross")
                || normalized.equals("klein")
                || normalized.equals("mittel");
    }

    private String normalizeQuantityGlyphs(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value
                .replace("\u00bc", "1/4")
                .replace("\u00bd", "1/2")
                .replace("\u00be", "3/4")
                .replace("\u2153", "1/3")
                .replace("\u2154", "2/3");
    }

    private BigDecimal parseQuantity(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().replace(',', '.');
        if (normalized.contains("/")) {
            String[] fraction = normalized.split("/");
            if (fraction.length == 2) {
                try {
                    BigDecimal numerator = new BigDecimal(fraction[0]);
                    BigDecimal denominator = new BigDecimal(fraction[1]);
                    if (denominator.compareTo(BigDecimal.ZERO) == 0) {
                        return null;
                    }
                    return numerator.divide(denominator, 3, RoundingMode.HALF_UP);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String aggregateKey(ParsedIngredient parsed) {
        return normalizeName(parsed.name()) + "|" + (unitFamily(parsed.unit()) == null ? "unknown" : unitFamily(parsed.unit()));
    }

    private boolean equivalentName(String left, String right) {
        String normalizedLeft = singularize(normalizeName(left));
        String normalizedRight = singularize(normalizeName(right));
        return normalizedLeft.equals(normalizedRight);
    }

    private String normalizeName(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace("ß", "ss")
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return normalized;
    }

    private String singularize(String value) {
        if (value.endsWith("en") && value.length() > 4) {
            return value.substring(0, value.length() - 2);
        }
        if (value.endsWith("s") && value.length() > 3) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private boolean compatibleUnits(String left, String right) {
        String leftFamily = unitFamily(left);
        String rightFamily = unitFamily(right);
        return leftFamily != null && leftFamily.equals(rightFamily);
    }

    private String unitFamily(String unit) {
        String canonical = canonicalUnit(unit);
        if (canonical == null) {
            return "count";
        }
        if (GRAM_UNITS.contains(canonical)) {
            return "mass";
        }
        if (MILLILITER_UNITS.contains(canonical)) {
            return "volume";
        }
        if (COUNT_UNITS.contains(canonical)) {
            return "count";
        }
        return null;
    }

    // Maps both English and German unit spellings (and common abbreviations) to one German
    // canonical form, so quantities entered in either language still aggregate together.
    // The frontend also stores German unit labels for its own suggestions (see
    // ingredientCategories.ts's UNIT_LABELS_DE) — this method is why that's safe to send back.
    private String canonicalUnit(String unit) {
        if (unit == null || unit.isBlank()) {
            return null;
        }
        String trimmed = unit.trim();
        if ("T".equals(trimmed)) {
            return "tbsp";
        }
        if ("t".equals(trimmed)) {
            return "tsp";
        }
        String normalized = normalizeName(unit);
        return switch (normalized) {
            case "gram", "grams" -> "g";
            case "liter", "liters", "litre", "litres" -> "l";
            case "stueck", "stuck", "stk", "piece", "pieces" -> "piece";
            case "essloeffel", "essloffel", "el", "tbsp", "tbsps", "tablespoon", "tablespoons" -> "tbsp";
            case "teeloeffel", "teeloffel", "tl", "tsp", "tsps", "teaspoon", "teaspoons" -> "tsp";
            case "prise", "prisen", "pinch", "pinches" -> "pinch";
            case "zehe", "zehen", "clove", "cloves" -> "clove";
            case "stiel", "stiele", "stalk", "stalks" -> "stalk";
            case "scheibe", "scheiben", "slice", "slices" -> "slice";
            case "dose", "dosen", "can", "cans" -> "can";
            case "tasse", "tassen", "cup", "cups" -> "cup";
            case "ounce", "ounces", "unze", "unzen", "oz" -> "oz";
            case "pound", "pounds", "pfund", "lb", "lbs" -> "lb";
            case "bund", "bunch", "bunches" -> "bunch";
            case "packung", "packungen", "paket", "pakete", "package", "packages", "pack", "packs" -> "package";
            default -> normalized;
        };
    }

    private BigDecimal toBaseQuantity(BigDecimal quantity, String unit) {
        String canonical = canonicalUnit(unit);
        if (canonical == null) {
            return quantity;
        }
        if ("kg".equals(canonical) || "l".equals(canonical)) {
            return quantity.multiply(BigDecimal.valueOf(1000));
        }
        if ("oz".equals(canonical)) {
            return quantity.multiply(BigDecimal.valueOf(28.3495));
        }
        if ("lb".equals(canonical)) {
            return quantity.multiply(BigDecimal.valueOf(453.59237));
        }
        if (GRAM_UNITS.contains(canonical) || MILLILITER_UNITS.contains(canonical) || COUNT_UNITS.contains(canonical)) {
            return quantity;
        }
        return null;
    }

    private BigDecimal fromBaseQuantity(BigDecimal quantity, String unit) {
        String canonical = canonicalUnit(unit);
        if ("kg".equals(canonical) || "l".equals(canonical)) {
            return quantity.divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP).stripTrailingZeros();
        }
        if ("oz".equals(canonical)) {
            return quantity.divide(BigDecimal.valueOf(28.3495), 3, RoundingMode.HALF_UP).stripTrailingZeros();
        }
        if ("lb".equals(canonical)) {
            return quantity.divide(BigDecimal.valueOf(453.59237), 3, RoundingMode.HALF_UP).stripTrailingZeros();
        }
        return quantity.stripTrailingZeros();
    }

    private MealPlanShoppingListItemResponse toResponse(IngredientNeed need, String reason) {
        return item(need.name(), need.quantity(), need.unit(), need.recipeTitle(), need.rawIngredient(), reason);
    }

    private MealPlanShoppingListItemResponse item(String name, BigDecimal quantity, String unit, String recipeTitle, String ingredient, String reason) {
        return new MealPlanShoppingListItemResponse(name, quantity, unit, recipeTitle, ingredient, reason);
    }

    @SafeVarargs
    private static Set<String> union(Set<String>... sets) {
        java.util.HashSet<String> result = new java.util.HashSet<>();
        for (Set<String> set : sets) {
            result.addAll(set);
        }
        return Set.copyOf(result);
    }

    private record ParsedIngredient(String name, BigDecimal quantity, String unit, String recipeTitle, String rawIngredient) {
    }

    private record IngredientNeed(String name, BigDecimal quantity, String unit, String recipeTitle, String rawIngredient) {
        IngredientNeed withQuantity(BigDecimal quantity) {
            return new IngredientNeed(name, quantity, unit, recipeTitle, rawIngredient);
        }

        IngredientNeed withRawIngredient(String rawIngredient) {
            return new IngredientNeed(name, quantity, unit, recipeTitle, rawIngredient);
        }
    }

    private enum MatchStatus {
        COVERED,
        ADD_REMAINING,
        NEEDS_REVIEW
    }

    private record MatchResult(MatchStatus status, BigDecimal remainingQuantity, String reason) {
    }
}
