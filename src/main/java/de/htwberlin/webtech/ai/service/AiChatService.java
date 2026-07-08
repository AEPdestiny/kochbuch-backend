package de.htwberlin.webtech.ai.service;

import de.htwberlin.webtech.ai.client.GroqClient;
import de.htwberlin.webtech.ai.client.GroqClientException;
import de.htwberlin.webtech.ai.dto.AiChatResponse;
import de.htwberlin.webtech.favorite.repository.ExternalRecipeFavoriteRepository;
import de.htwberlin.webtech.mealplan.entity.MealPlan;
import de.htwberlin.webtech.mealplan.repository.MealPlanRepository;
import de.htwberlin.webtech.pantry.repository.PantryItemRepository;
import de.htwberlin.webtech.profile.entity.UserPreferences;
import de.htwberlin.webtech.profile.repository.UserPreferencesRepository;
import de.htwberlin.webtech.recipe.entity.Recipe;
import de.htwberlin.webtech.recipe.repository.RecipeRepository;
import de.htwberlin.webtech.shopping.entity.ShoppingListItem;
import de.htwberlin.webtech.shopping.repository.ShoppingListItemRepository;
import de.htwberlin.webtech.user.entity.AppUser;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Answers a chat message via the Groq LLM, grounded in the current user's own data.
 * The frontend contract stays intentionally small: the backend derives trusted context
 * from the authenticated user instead of requiring the client to send personal data.
 */
@ApplicationScoped
public class AiChatService {

    private static final int PANTRY_LIMIT = 30;
    private static final int SHOPPING_LIST_LIMIT = 30;
    private static final int OWN_RECIPE_LIMIT = 10;
    private static final int PUBLISHED_RECIPE_LIMIT = 15;
    private static final int FAVORITE_LIMIT = 15;
    private static final int INGREDIENT_SUMMARY_LIMIT = 140;

    private final GroqClient groqClient;
    private final UserPreferencesRepository preferencesRepository;
    private final PantryItemRepository pantryItemRepository;
    private final MealPlanRepository mealPlanRepository;
    private final ExternalRecipeFavoriteRepository favoriteRepository;
    private final ShoppingListItemRepository shoppingListItemRepository;
    private final RecipeRepository recipeRepository;

    public AiChatService(GroqClient groqClient,
                         UserPreferencesRepository preferencesRepository,
                         PantryItemRepository pantryItemRepository,
                         MealPlanRepository mealPlanRepository,
                         ExternalRecipeFavoriteRepository favoriteRepository,
                         ShoppingListItemRepository shoppingListItemRepository,
                         RecipeRepository recipeRepository) {
        this.groqClient = groqClient;
        this.preferencesRepository = preferencesRepository;
        this.pantryItemRepository = pantryItemRepository;
        this.mealPlanRepository = mealPlanRepository;
        this.favoriteRepository = favoriteRepository;
        this.shoppingListItemRepository = shoppingListItemRepository;
        this.recipeRepository = recipeRepository;
    }

    public AiChatResponse answer(AppUser currentUser, String message) {
        String systemPrompt = """
                Du bist Dishly, ein smarter Koch-Assistent.
                Du hilfst Nutzern Rezepte zu finden, Mahlzeiten zu planen und Einkaeufe zu verwalten.
                Wenn ein Nutzer beschreibt was er essen moechte, schlage ein Rezept vor und frage:
                Moechtest du
                (1) die Zutaten zur Einkaufsliste hinzufuegen,
                (2) ein Restaurant finden,
                (3) es zum Wochenplan hinzufuegen?
                Nutze nur die bereitgestellten Nutzerdaten.
                Respektiere Allergien, Abneigungen und Ernaehrungsziele strikt.
                Erfinde keine Vorrats-, Einkaufslisten- oder Rezeptdaten.
                Behaupte nie, dass du App-Aktionen ausgefuehrt hast.
                Empfiehl stattdessen konkrete naechste Schritte, die der Nutzer selbst ausfuehren kann.
                Wenn Daten leer oder nicht verfuegbar sind, sage das ehrlich.
                Wenn eine echte Aktion nicht eindeutig moeglich ist, frage nach den fehlenden Angaben.
                Antworte kurz, konkret und auf Deutsch.
                """;
        String prompt = buildContext(currentUser) + "\n\nNutzerfrage: " + message;
        try {
            return new AiChatResponse(groqClient.complete(systemPrompt, prompt), true);
        } catch (GroqClientException exception) {
            if (exception.getMessage() != null && exception.getMessage().contains("GROQ_API_KEY")) {
                return new AiChatResponse("Dishly AI ist noch nicht konfiguriert. Setze GROQ_API_KEY im Backend, damit echte Antworten erzeugt werden.", false);
            }
            return new AiChatResponse("Dishly AI konnte Groq gerade nicht erreichen: " + exception.getMessage(), false);
        }
    }

    private String buildContext(AppUser currentUser) {
        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        UserPreferences preferences = preferencesRepository.findByOwner(currentUser).orElse(null);
        String pantry = pantryItemRepository.findByOwner(currentUser).stream()
                .map(item -> item.getName() + quantity(item.getQuantity(), item.getUnit()))
                .limit(PANTRY_LIMIT)
                .collect(Collectors.joining(", "));
        String mealPlan = mealPlanRepository.findByOwnerAndPlannedDateBetween(currentUser, weekStart, weekEnd).stream()
                .map(this::mealPlanLine)
                .collect(Collectors.joining("; "));
        String shoppingList = shoppingListItemRepository.findByOwner(currentUser).stream()
                .limit(SHOPPING_LIST_LIMIT)
                .map(this::shoppingListLine)
                .collect(Collectors.joining(", "));
        String ownRecipes = recipeRepository.findByOwner(currentUser).stream()
                .limit(OWN_RECIPE_LIMIT)
                .map(this::recipeSummary)
                .collect(Collectors.joining("; "));
        String publishedRecipes = recipeRepository.findRandomPublished(PUBLISHED_RECIPE_LIMIT * 3).stream()
                .filter(recipe -> matchesPreferences(recipe, preferences))
                .limit(PUBLISHED_RECIPE_LIMIT)
                .map(this::recipeSummary)
                .collect(Collectors.joining("; "));
        String favorites = favoriteRepository.findByOwner(currentUser).stream()
                .map(favorite -> favorite.getExternalTitle())
                .limit(FAVORITE_LIMIT)
                .collect(Collectors.joining(", "));

        return """
                Kontext fuer Dishly AI:
                Nutzer: %s

                User profile:
                %s

                Pantry:
                %s

                Current week meal plan:
                %s

                Shopping list:
                %s

                Own recipes:
                %s

                Dishly recipe catalog:
                %s

                Favorites:
                %s

                Safety rules:
                - Allergien und Abneigungen haben Vorrang vor Rezeptvorschlaegen.
                - Keine Vorrats-, Einkaufslisten- oder Rezeptdaten erfinden.
                - Keine App-Aktionen behaupten; nur konkrete naechste Schritte empfehlen.
                - Leere oder fehlende Daten klar benennen.
                """.formatted(
                currentUser.getUsername(),
                preferences == null ? "nicht ausgefuellt" : preferencesText(preferences),
                pantry.isBlank() ? "keine Vorratsdaten" : pantry,
                mealPlan.isBlank() ? "keine geplanten Mahlzeiten" : mealPlan,
                shoppingList.isBlank() ? "Einkaufsliste ist leer" : shoppingList,
                ownRecipes.isBlank() ? "keine eigenen Rezepte gespeichert" : ownRecipes,
                publishedRecipes.isBlank() ? "keine passenden Dishly-Rezepte verfuegbar" : publishedRecipes,
                favorites.isBlank() ? "keine externen Favoriten" : favorites
        );
    }

    private String preferencesText(UserPreferences preferences) {
        return "Kalorienziel=" + preferences.getDailyCalorieTarget()
                + ", vegan=" + preferences.isVegan()
                + ", vegetarisch=" + preferences.isVegetarian()
                + ", glutenfrei=" + preferences.isGlutenFree()
                + ", laktosefrei=" + preferences.isLactoseFree()
                + ", proteinreich=" + preferences.isHighProtein()
                + ", kalorienarm=" + preferences.isCalorieConscious()
                + ", Allergien=" + preferences.getAllergies()
                + ", Vorlieben=" + preferences.getLikes()
                + ", Abneigungen=" + preferences.getDislikes();
    }

    private String mealPlanLine(MealPlan entry) {
        Recipe recipe = entry.getRecipe();
        String title = recipe != null ? recipe.getTitle() : entry.getCustomTitle();
        String nutrition = recipe != null
                ? nutrition(recipe.getCalories(), recipe.getProtein())
                : nutrition(entry.getCaloriesSnapshot(), entry.getProteinSnapshot());
        return entry.getPlannedDate() + " " + entry.getMealSlot().name().toLowerCase(Locale.ROOT) + ": " + fallback(title, "unbenannte Mahlzeit") + nutrition;
    }

    private String shoppingListLine(ShoppingListItem item) {
        String status = item.isChecked() ? "erledigt" : "offen";
        return item.getName() + quantity(item.getQuantity(), item.getUnit()) + " [" + status + "]";
    }

    private String recipeSummary(Recipe recipe) {
        return fallback(recipe.getTitle(), "unbenanntes Rezept")
                + optional("Kategorie", recipe.getCategory(), 60)
                + nutrition(recipe.getCalories(), recipe.getProtein())
                + optional("Zutaten", recipe.getIngredients(), INGREDIENT_SUMMARY_LIMIT);
    }

    private boolean matchesPreferences(Recipe recipe, UserPreferences preferences) {
        if (preferences == null) {
            return true;
        }
        if (preferences.isVegan() && !recipe.isVegan()) {
            return false;
        }
        if (preferences.isVegetarian() && !recipe.isVegetarian() && !recipe.isVegan()) {
            return false;
        }
        if (preferences.isGlutenFree() && !recipe.isGlutenFree()) {
            return false;
        }
        if (preferences.isLactoseFree() && !recipe.isDairyFree()) {
            return false;
        }
        String searchable = (fallback(recipe.getTitle(), "") + " "
                + fallback(recipe.getIngredients(), "") + " "
                + fallback(recipe.getCategory(), "")).toLowerCase(Locale.ROOT);
        return preferences.getAllergies().stream()
                .filter(allergy -> allergy != null && !allergy.isBlank())
                .noneMatch(allergy -> searchable.contains(allergy.toLowerCase(Locale.ROOT)));
    }

    private String nutrition(Number calories, Number protein) {
        String caloriesText = calories == null ? "" : calories + " kcal";
        String proteinText = protein == null ? "" : protein + " g Protein";
        if (caloriesText.isBlank() && proteinText.isBlank()) {
            return "";
        }
        if (proteinText.isBlank()) {
            return " (" + caloriesText + ")";
        }
        if (caloriesText.isBlank()) {
            return " (" + proteinText + ")";
        }
        return " (" + caloriesText + ", " + proteinText + ")";
    }

    private String optional(String label, String value, int maxLength) {
        String normalized = shortText(value, maxLength);
        return normalized.isBlank() ? "" : ", " + label + "=" + normalized;
    }

    private String shortText(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength - 1).trim() + "...";
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String quantity(Object quantity, String unit) {
        if (quantity == null) {
            return "";
        }
        return " (" + quantity + (unit == null || unit.isBlank() ? "" : " " + unit) + ")";
    }
}
