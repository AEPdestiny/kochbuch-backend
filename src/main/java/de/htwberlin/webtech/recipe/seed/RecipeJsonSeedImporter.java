package de.htwberlin.webtech.recipe.seed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.htwberlin.webtech.recipe.entity.Recipe;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class RecipeJsonSeedImporter {

    private static final Logger LOG = Logger.getLogger(RecipeJsonSeedImporter.class);
    private static final List<String> LANGUAGES = List.of("en", "de", "tr", "ar", "zh", "ru", "pl", "ja");
    private static final int MAX_TITLE_LENGTH = 240;
    private static final List<CategoryFile> CATEGORY_FILES = List.of(
            new CategoryFile("breakfast.json", "breakfast"),
            new CategoryFile("lunch.json", "lunch"),
            new CategoryFile("dinner.json", "dinner"),
            new CategoryFile("snacks.json", "snack")
    );

    private final RecipeSeedPersistence persistence;
    private final ObjectMapper objectMapper;

    @Inject
    public RecipeJsonSeedImporter(RecipeSeedPersistence persistence, ObjectMapper objectMapper) {
        this.persistence = persistence;
        this.objectMapper = objectMapper;
    }

    void importRecipes(@Observes StartupEvent event) {
        if (LaunchMode.current() == LaunchMode.TEST) {
            return;
        }

        int imported = seedFiles().stream()
                .mapToInt(file -> importFile(file.fileNames(), file.category(), file.language()))
                .sum();

        if (imported > 0) {
            LOG.infof("Imported %d recipe seed entries.", imported);
        }
    }

    List<SeedFile> seedFiles() {
        List<SeedFile> files = new ArrayList<>();

        for (String language : LANGUAGES) {
            for (CategoryFile categoryFile : CATEGORY_FILES) {
                files.add(new SeedFile(
                        List.of("recipes/" + language + "/" + categoryFile.fileName()),
                        categoryFile.category(),
                        language
                ));
            }
        }

        files.add(new SeedFile(List.of("recipesbreakfast.json", "recipes_breakfast.json", "recipes_en_breakfast.json"), "breakfast", "en"));
        files.add(new SeedFile(List.of("recipeslunch.json", "recipes_lunch.json", "recipes_en_lunch.json"), "lunch", "en"));
        files.add(new SeedFile(List.of("recipesDinner.json", "recipes_dinner.json", "recipes_en_dinner.json"), "dinner", "en"));
        files.add(new SeedFile(List.of("recipesSnack.json", "recipes_snacks.json", "recipes_en_snacks.json"), "snack", "en"));

        for (String language : LANGUAGES) {
            if (!"en".equals(language)) {
                files.add(new SeedFile(List.of("recipes_" + language + "_breakfast.json"), "breakfast", language));
                files.add(new SeedFile(List.of("recipes_" + language + "_lunch.json"), "lunch", language));
                files.add(new SeedFile(List.of("recipes_" + language + "_dinner.json"), "dinner", language));
                files.add(new SeedFile(List.of("recipes_" + language + "_snacks.json"), "snack", language));
            }
        }
        return files;
    }

    int importFile(SeedFile seedFile) {
        return importFile(seedFile.fileNames(), seedFile.category(), seedFile.language());
    }

    private int importFile(List<String> fileNames, String category, String language) {
        for (String fileName : fileNames) {
            try (InputStream stream = openResource(fileName)) {
                if (stream != null) {
                    return importFile(fileName, category, language);
                }
            } catch (Exception exception) {
                LOG.warnf("Could not check recipe seed file '%s': %s", fileName, exception.getMessage());
            }
        }
        LOG.debugf("No recipe seed file found for category '%s'. Tried: %s", category, fileNames);
        return 0;
    }

    private int importFile(String fileName, String category, String language) {
        try (InputStream stream = openResource(fileName)) {
            if (stream == null) {
                LOG.debugf("Recipe seed file '%s' not found. Skipping.", fileName);
                return 0;
            }

            JsonNode root = objectMapper.readTree(stream);
            JsonNode recipes = recipesNode(root);
            if (!recipes.isArray()) {
                LOG.warnf("Recipe seed file '%s' does not contain an array. Skipping.", fileName);
                return 0;
            }

            int imported = 0;
            for (JsonNode node : recipes) {
                Recipe recipe = mapRecipe(node, category, language, fileName);
                if (recipe == null) {
                    continue;
                }
                try {
                    if (persistence.upsertSeedRecipe(recipe)) {
                        imported++;
                    }
                } catch (Exception exception) {
                    LOG.warnf(
                            "Could not import recipe '%s' from seed file '%s': %s",
                            recipe.getTitle(),
                            fileName,
                            exception.getMessage()
                    );
                }
            }
            return imported;
        } catch (Exception exception) {
            LOG.warnf("Could not import recipe seed file '%s': %s", fileName, exception.getMessage());
            return 0;
        }
    }

    private InputStream openResource(String fileName) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream stream = loader.getResourceAsStream(fileName);
        if (stream != null) {
            return stream;
        }
        return loader.getResourceAsStream("recipes/" + fileName);
    }

    private JsonNode recipesNode(JsonNode root) {
        if (root == null) {
            return objectMapper.createArrayNode();
        }
        if (root.isArray()) {
            return root;
        }
        if (root.has("recipes")) {
            return root.get("recipes");
        }
        if (root.has("results")) {
            return root.get("results");
        }
        return objectMapper.createArrayNode();
    }

    private Recipe mapRecipe(JsonNode node, String category, String language, String fileName) {
        String title = firstText(node, "title", "name");
        if (title.isBlank()) {
            return null;
        }
        String recipeLanguage = firstText(node, "language", "lang");
        if (recipeLanguage.isBlank() || !language.equalsIgnoreCase(recipeLanguage.trim())) {
            LOG.warnf(
                    "Skipping recipe seed id='%s' title='%s' from '%s': language '%s' does not match expected '%s'.",
                    firstText(node, "id"),
                    title,
                    fileName,
                    recipeLanguage.isBlank() ? "<missing>" : recipeLanguage,
                    language
            );
            return null;
        }

        String ingredients = ingredientsText(node);
        String instructions = instructionsText(node);
        Recipe recipe = new Recipe();
        recipe.setExternalId(firstText(node, "id"));
        recipe.setTitle(truncate(title, MAX_TITLE_LENGTH));
        recipe.setImageUrl(firstImageUrl(node));
        recipe.setPrepTimeMinutes(firstInt(node, 0, "prepTimeMinutes", "prepTime", "preparationMinutes"));
        recipe.setCookTimeMinutes(firstInt(node, firstInt(node, 0, "readyInMinutes"), "cookTimeMinutes", "cookTime", "cookingMinutes"));
        recipe.setServings(firstInt(node, 1, "servings"));
        recipe.setDifficulty(firstText(node, "difficulty"));
        recipe.setCategory(category);
        recipe.setLanguage(recipeLanguage.trim().toLowerCase());
        recipe.setRating(firstDouble(node, 0.0, "rating"));
        recipe.setIngredients(ingredients);
        recipe.setInstructions(instructions);
        recipe.setCalories(firstNonNull(firstNullableInt(node, "calories", "kcal"), caloriesFromNutrition(node)));
        recipe.setProtein(firstNonNull(firstNullableDouble(node, "protein", "proteinGrams"), proteinFromNutrition(node)));
        recipe.setAlcohol(nutrientFromNutrition(node, "Alcohol"));
        recipe.setAlcoholPercent(nutrientFromNutrition(node, "Alcohol %"));
        recipe.setSourceUrl(firstText(node, "sourceUrl", "spoonacularSourceUrl"));
        recipe.setSourceName(firstText(node, "sourceName", "creditsText"));
        recipe.setDishTypes(joinValues(node.get("dishTypes")));
        recipe.setDiets(joinValues(node.get("diets")));
        recipe.setVegetarian(firstBoolean(node, "vegetarian"));
        recipe.setVegan(firstBoolean(node, "vegan"));
        recipe.setGlutenFree(firstBoolean(node, "glutenFree"));
        recipe.setDairyFree(firstBoolean(node, "dairyFree"));
        recipe.setFavorite(false);
        recipe.setPublished(true);
        return recipe;
    }

    private JsonNode firstNode(JsonNode node, String... names) {
        for (String name : names) {
            if (node.has(name)) {
                return node.get(name);
            }
        }
        return null;
    }

    private String ingredientsText(JsonNode node) {
        String ingredients = joinIngredientValues(firstNode(node, "ingredients", "extendedIngredients"));
        if (ingredients != null && !ingredients.isBlank() && !"[]".equals(ingredients.trim())) {
            return ingredients;
        }
        return joinIngredientValues(node.path("nutrition").path("ingredients"));
    }

    private String instructionsText(JsonNode node) {
        String analyzed = analyzedInstructionsText(node.path("analyzedInstructions"));
        if (!analyzed.isBlank()) {
            return analyzed;
        }
        String direct = stripHtml(firstText(node, "instructions"));
        if (!direct.isBlank()) {
            return direct;
        }
        String structured = stripHtml(joinValues(firstNode(node, "instructions")));
        if (!structured.isBlank()) {
            return structured;
        }
        return stripHtml(firstText(node, "summary"));
    }

    private String analyzedInstructionsText(JsonNode analyzedInstructions) {
        if (!analyzedInstructions.isArray()) {
            return "";
        }
        List<String> steps = new ArrayList<>();
        for (JsonNode instructionGroup : analyzedInstructions) {
            JsonNode groupSteps = instructionGroup.path("steps");
            if (!groupSteps.isArray()) {
                continue;
            }
            for (JsonNode step : groupSteps) {
                String text = firstText(step, "step", "text");
                if (!text.isBlank()) {
                    steps.add(text);
                }
            }
        }
        if (steps.isEmpty()) {
            return "";
        }
        List<String> numbered = new ArrayList<>();
        for (int index = 0; index < steps.size(); index++) {
            numbered.add((index + 1) + ". " + steps.get(index));
        }
        return String.join("\n", numbered);
    }

    private String firstText(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && !value.isNull() && !value.asText("").isBlank()) {
                return value.asText().trim();
            }
        }
        return "";
    }

    private String firstImageUrl(JsonNode node) {
        String direct = firstText(node, "imageUrl", "image");
        if (!direct.isBlank()) {
            return direct;
        }
        JsonNode images = node.get("images");
        if (images != null && images.isArray() && !images.isEmpty()) {
            return images.get(0).asText("").trim();
        }
        return "";
    }

    private int firstInt(JsonNode node, int defaultValue, String... names) {
        Integer value = firstNullableInt(node, names);
        return value == null ? defaultValue : value;
    }

    private Integer firstNullableInt(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && value.canConvertToInt()) {
                return (int) Math.round(value.asDouble());
            }
        }
        return null;
    }

    private Double firstNullableDouble(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && value.isNumber()) {
                return value.asDouble();
            }
        }
        return null;
    }

    private Integer firstNonNull(Integer first, Integer second) {
        return first == null ? second : first;
    }

    private Double firstNonNull(Double first, Double second) {
        return first == null ? second : first;
    }

    private Integer caloriesFromNutrition(JsonNode node) {
        JsonNode nutrients = node.path("nutrition").path("nutrients");
        if (!nutrients.isArray()) {
            return null;
        }
        for (JsonNode nutrient : nutrients) {
            if ("calories".equalsIgnoreCase(nutrient.path("name").asText())) {
                JsonNode amount = nutrient.get("amount");
                if (amount != null && amount.isNumber()) {
                    return (int) Math.round(amount.asDouble());
                }
            }
        }
        return null;
    }

    private Double proteinFromNutrition(JsonNode node) {
        return nutrientFromNutrition(node, "Protein");
    }

    private Double nutrientFromNutrition(JsonNode node, String nutrientName) {
        JsonNode nutrients = node.path("nutrition").path("nutrients");
        if (!nutrients.isArray()) {
            return null;
        }
        for (JsonNode nutrient : nutrients) {
            if (nutrientName.equalsIgnoreCase(nutrient.path("name").asText())) {
                JsonNode amount = nutrient.get("amount");
                if (amount != null && amount.isNumber()) {
                    return amount.asDouble();
                }
            }
        }
        return null;
    }

    private boolean firstBoolean(JsonNode node, String name) {
        JsonNode value = node.get(name);
        return value != null && value.asBoolean(false);
    }

    private String stripHtml(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength).trim();
    }

    private double firstDouble(JsonNode node, double defaultValue, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && value.isNumber()) {
                return value.asDouble();
            }
        }
        return defaultValue;
    }

    private String joinValues(JsonNode value) {
        if (value == null || value.isNull()) {
            return "";
        }
        if (value.isTextual()) {
            return value.asText().trim();
        }
        if (value.isArray()) {
            List<String> parts = new ArrayList<>();
            value.forEach(child -> {
                String mapped = joinValues(child);
                if (!mapped.isBlank()) {
                    parts.add(mapped);
                }
            });
            return String.join(", ", parts);
        }
        if (value.isObject()) {
            String ingredient = ingredientText(value);
            if (!ingredient.isBlank()) {
                return ingredient;
            }
            String text = firstText(value, "step", "text", "title");
            if (!text.isBlank()) {
                return text;
            }
        }
        return value.asText("").trim();
    }

    private String joinIngredientValues(JsonNode value) {
        if (value == null || value.isNull()) {
            return "";
        }
        if (value.isArray()) {
            List<String> parts = new ArrayList<>();
            value.forEach(child -> {
                String mapped = child.isObject() ? ingredientText(child) : joinValues(child);
                if (!mapped.isBlank()) {
                    parts.add(mapped);
                }
            });
            return String.join("\n", parts);
        }
        if (value.isObject()) {
            return ingredientText(value);
        }
        return joinValues(value);
    }

    private String ingredientText(JsonNode value) {
        String original = firstText(value, "original", "originalString");
        String metric = metricIngredientText(value);
        if (!original.isBlank() && !shouldPreferMetric(original, metric)) {
            return stripHtml(original);
        }
        if (!metric.isBlank()) {
            return metric;
        }
        String name = firstText(value, "name");
        if (name.isBlank()) {
            return "";
        }
        String unit = firstText(value, "unit", "unitShort", "unitLong");
        String amount = formattedAmount(value.get("amount"), unit);
        unit = normalizedUnit(unit);
        if (isTinyTeaspoon(value.get("amount"), unit)) {
            return "1 Prise " + name;
        }
        return List.of(amount, unit, name).stream()
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" "));
    }

    private String metricIngredientText(JsonNode value) {
        JsonNode metric = value.path("measures").path("metric");
        if (metric.isMissingNode() || metric.isNull()) {
            return "";
        }
        String name = firstText(value, "name");
        if (name.isBlank()) {
            return "";
        }
        String unit = firstText(metric, "unitShort", "unitLong", "unit");
        JsonNode amountNode = metric.get("amount");
        if (isTinyTeaspoon(amountNode, unit)) {
            return "1 Prise " + name;
        }
        String amount = formattedAmount(amountNode, unit);
        unit = normalizedUnit(unit);
        return List.of(amount, unit, name).stream()
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" "));
    }

    private boolean shouldPreferMetric(String original, String metric) {
        if (metric == null || metric.isBlank()) {
            return false;
        }
        String normalized = original.toLowerCase();
        return normalized.matches(".*\\b(ounce|ounces|oz|pound|pounds|lb|lbs|teaspoon|teaspoons|tablespoon|tablespoons|tbsp|tsp|cup|cups)\\b.*")
                || normalized.matches(".*\\b0\\.0[0-9]+\\b.*")
                || normalized.matches(".*\\b[0-9]+\\.[0-9]{2,}\\b.*");
    }

    private boolean isTinyTeaspoon(JsonNode amountNode, String unit) {
        String normalizedUnit = normalizedUnit(unit);
        return amountNode != null
                && amountNode.isNumber()
                && amountNode.asDouble() > 0
                && amountNode.asDouble() < 0.15
                && "TL".equals(normalizedUnit);
    }

    private String normalizedUnit(String unit) {
        if (unit == null || unit.isBlank()) {
            return "";
        }
        return switch (unit.trim().toLowerCase()) {
            case "teaspoon", "teaspoons", "tsp" -> "TL";
            case "tablespoon", "tablespoons", "tbsp" -> "EL";
            case "ounce", "ounces", "oz" -> "g";
            case "pound", "pounds", "lb", "lbs" -> "g";
            case "cup", "cups" -> "ml";
            default -> unit.trim();
        };
    }

    private String formattedAmount(JsonNode value, String unit) {
        if (value == null || value.isNull()) {
            return "";
        }
        if (!value.isNumber()) {
            return value.asText("").trim();
        }
        double number = value.asDouble();
        String normalizedUnit = normalizedUnit(unit);
        if ("g".equals(normalizedUnit) || "ml".equals(normalizedUnit)) {
            return String.valueOf(Math.round(number));
        }
        if (number == Math.rint(number)) {
            return String.valueOf((long) number);
        }
        return BigDecimal.valueOf(number)
                .setScale(number < 1 ? 2 : 1, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString()
                .replace(".", ",");
    }

    private String numericText(JsonNode value) {
        if (value == null || value.isNull()) {
            return "";
        }
        if (value.isNumber()) {
            double number = value.asDouble();
            if (number == Math.rint(number)) {
                return String.valueOf((long) number);
            }
            return BigDecimal.valueOf(number).stripTrailingZeros().toPlainString();
        }
        return value.asText("").trim();
    }

    record SeedFile(List<String> fileNames, String category, String language) {
    }

    record CategoryFile(String fileName, String category) {
    }
}
