package de.htwberlin.webtech.recipe.seed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.htwberlin.webtech.recipe.entity.Recipe;
import de.htwberlin.webtech.recipe.repository.RecipeRepository;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class RecipeJsonSeedImporter {

    private static final Logger LOG = Logger.getLogger(RecipeJsonSeedImporter.class);
    private static final List<String> LANGUAGES = List.of("en", "de", "tr", "ar", "zh", "ru", "pl", "ja");
    private static final List<CategoryFile> CATEGORY_FILES = List.of(
            new CategoryFile("breakfast.json", "breakfast"),
            new CategoryFile("lunch.json", "lunch"),
            new CategoryFile("dinner.json", "dinner"),
            new CategoryFile("snacks.json", "snack")
    );

    private final RecipeRepository repository;
    private final ObjectMapper objectMapper;

    public RecipeJsonSeedImporter(RecipeRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
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
                Recipe recipe = mapRecipe(node, category, language);
                if (recipe == null) {
                    continue;
                }
                if (repository.findByTitleAndCategoryAndLanguage(recipe.getTitle(), recipe.getCategory(), recipe.getLanguage()).isPresent()) {
                    continue;
                }
                repository.persist(recipe);
                imported++;
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

    private Recipe mapRecipe(JsonNode node, String category, String language) {
        String title = firstText(node, "title", "name");
        if (title.isBlank()) {
            return null;
        }

        String ingredients = joinValues(firstNode(node, "ingredients", "extendedIngredients"));
        String instructions = joinValues(firstNode(node, "instructions", "steps", "analyzedInstructions"));
        if (ingredients.isBlank()) {
            ingredients = "Keine Zutaten angegeben.";
        }
        if (instructions.isBlank()) {
            instructions = "Keine Anleitung angegeben.";
        }

        Recipe recipe = new Recipe();
        recipe.setTitle(title);
        recipe.setImageUrl(firstText(node, "imageUrl", "image"));
        recipe.setPrepTimeMinutes(firstInt(node, 0, "prepTimeMinutes", "prepTime"));
        recipe.setCookTimeMinutes(firstInt(node, firstInt(node, 0, "readyInMinutes"), "cookTimeMinutes", "cookTime"));
        recipe.setServings(firstInt(node, 1, "servings"));
        recipe.setDifficulty(firstText(node, "difficulty"));
        recipe.setCategory(category);
        recipe.setLanguage(language);
        recipe.setRating(firstDouble(node, 0.0, "rating"));
        recipe.setIngredients(ingredients);
        recipe.setInstructions(instructions);
        recipe.setCalories(firstNonNull(firstNullableInt(node, "calories", "kcal"), caloriesFromNutrition(node)));
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

    private String firstText(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && !value.isNull() && !value.asText("").isBlank()) {
                return value.asText().trim();
            }
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

    private Integer firstNonNull(Integer first, Integer second) {
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
            String original = firstText(value, "original", "step", "text", "name", "title");
            if (!original.isBlank()) {
                return original;
            }
        }
        return value.asText("").trim();
    }

    record SeedFile(List<String> fileNames, String category, String language) {
    }

    record CategoryFile(String fileName, String category) {
    }
}
