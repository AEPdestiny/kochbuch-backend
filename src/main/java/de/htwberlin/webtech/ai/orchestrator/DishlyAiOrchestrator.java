package de.htwberlin.webtech.ai.orchestrator;

import de.htwberlin.webtech.ai.client.GroqClient;
import de.htwberlin.webtech.ai.client.GroqClientException;
import de.htwberlin.webtech.ai.dto.AiChatRequest;
import de.htwberlin.webtech.ai.dto.AiChatResponse;
import de.htwberlin.webtech.ai.model.AiActionPlan;
import de.htwberlin.webtech.ai.model.AiConversationContext;
import de.htwberlin.webtech.ai.model.AiIntent;
import de.htwberlin.webtech.ai.model.AiIntentDetectionResult;
import de.htwberlin.webtech.ai.tools.AiShoppingListTool;
import de.htwberlin.webtech.ai.tools.AiShoppingListToolResult;
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
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Skeleton coordinator for Dishly AI. It currently preserves the existing chat
 * completion path, while defining the place where intent detection, validation,
 * tool execution and response composition will be added incrementally.
 */
@ApplicationScoped
public class DishlyAiOrchestrator {

    private static final int PANTRY_LIMIT = 30;
    private static final int SHOPPING_LIST_LIMIT = 30;
    private static final int OWN_RECIPE_LIMIT = 10;
    private static final int PUBLISHED_RECIPE_LIMIT = 15;
    private static final int FAVORITE_LIMIT = 15;
    private static final int INGREDIENT_SUMMARY_LIMIT = 140;
    private static final int HISTORY_LIMIT = 10;
    private static final int HISTORY_TURN_LIMIT = 500;

    private final GroqClient groqClient;
    private final UserPreferencesRepository preferencesRepository;
    private final PantryItemRepository pantryItemRepository;
    private final MealPlanRepository mealPlanRepository;
    private final ExternalRecipeFavoriteRepository favoriteRepository;
    private final ShoppingListItemRepository shoppingListItemRepository;
    private final RecipeRepository recipeRepository;
    private final AiIntentDetector intentDetector;
    private final AiShoppingListTool shoppingListTool;

    public DishlyAiOrchestrator(GroqClient groqClient,
                                UserPreferencesRepository preferencesRepository,
                                PantryItemRepository pantryItemRepository,
                                MealPlanRepository mealPlanRepository,
                                ExternalRecipeFavoriteRepository favoriteRepository,
                                ShoppingListItemRepository shoppingListItemRepository,
                                RecipeRepository recipeRepository,
                                AiIntentDetector intentDetector,
                                AiShoppingListTool shoppingListTool) {
        this.groqClient = groqClient;
        this.preferencesRepository = preferencesRepository;
        this.pantryItemRepository = pantryItemRepository;
        this.mealPlanRepository = mealPlanRepository;
        this.favoriteRepository = favoriteRepository;
        this.shoppingListItemRepository = shoppingListItemRepository;
        this.recipeRepository = recipeRepository;
        this.intentDetector = intentDetector;
        this.shoppingListTool = shoppingListTool;
    }

    public AiChatResponse answer(AppUser currentUser, String message, List<AiChatRequest.AiChatTurn> history) {
        return answer(currentUser, message, history, null);
    }

    public AiChatResponse answer(AppUser currentUser, String message, List<AiChatRequest.AiChatTurn> history, String locale) {
        AiConversationContext context = buildConversationContext(currentUser, message, history, locale);
        AiIntentDetectionResult intent = intentDetector.detect(context.message(), context.history());
        AiChatResponse toolResponse = tryExecuteShoppingListTool(currentUser, context, intent);
        if (toolResponse != null) {
            return toolResponse;
        }
        String systemPrompt = """
                Du bist Dishly, ein smarter Koch-Assistent.
                Du hilfst Nutzern Rezepte zu finden, Mahlzeiten zu planen und Einkaeufe zu verwalten.
                Wenn du ein ad-hoc Gericht vorschlaegst und eine Einkaufslisten-Folgeaktion anbietest,
                schreibe immer eine eindeutige Zutatenzeile wie "Zutaten: ..." oder "Fehlende Zutaten: ...".
                Wenn du nummerierte Optionen anbietest, muessen die Nummern echte Dishly-Rezeptideen oder Dishly-Aktionen sein.
                Biete keine Restaurant-Suche als Standardoption an.
                Nutze nur die bereitgestellten Nutzerdaten.
                Use recipe titles and ingredients from the active UI locale.
                If active locale is de, prefer German recipe data with language=de.
                Do not output English recipe titles or English ingredients when German recipe candidates exist.
                Do not claim a recipe is from the catalog unless it is present in the provided candidate recipe context.
                If no suitable localized recipe exists, label the suggestion as a free recipe idea, not a saved Dishly recipe.
                Respektiere Allergien, Abneigungen und Ernaehrungsziele strikt.
                Erfinde keine Vorrats-, Einkaufslisten- oder Rezeptdaten.
                Behaupte nie, dass du App-Aktionen ausgefuehrt hast.
                Empfiehl stattdessen konkrete naechste Schritte, die der Nutzer selbst ausfuehren kann.
                Nutzerantworten wie "1", "2", "3", "ja", "oeffne das" oder "dieses Gericht" koennen sich auf vorherige Assistant-Optionen beziehen.
                Nutze den bereitgestellten Chatverlauf, um solche Bezuege aufzuloesen.
                Nutze den Abschnitt "Detected intent" als interne Orientierung fuer die wahrscheinliche Nutzerabsicht.
                Wenn "Detected intent" eine aufgeloeste Option enthaelt, antworte genau zu dieser Option und wechsle nicht zu einer anderen Funktion.
                Wenn der Bezug weiterhin mehrdeutig ist, frage kurz nach.
                Wenn eine erkannte Aktion noch nicht ausgefuehrt werden kann, sage kurz, dass du die Absicht verstanden hast, und frage nach fehlenden Angaben.
                Wenn Daten leer oder nicht verfuegbar sind, sage das ehrlich.
                Wenn eine echte Aktion nicht eindeutig moeglich ist, frage nach den fehlenden Angaben.
                Behaupte nie, dass Einkaufsliste, Wochenplan, Rezeptdetails oder Restaurants bereits geaendert/geoeffnet/gesucht wurden.
                Antworte kurz, konkret und in der Sprache des Nutzers, wenn sie erkennbar ist.
                """;
        String prompt = context.appContextSummary()
                + buildHistory(context.history())
                + buildDetectedIntent(intent)
                + "\n\nAktuelle Nutzerfrage: " + context.message();
        try {
            return new AiChatResponse(groqClient.complete(systemPrompt, prompt), true);
        } catch (GroqClientException exception) {
            if (exception.getMessage() != null && exception.getMessage().contains("GROQ_API_KEY")) {
                return new AiChatResponse("Dishly AI ist noch nicht konfiguriert. Setze GROQ_API_KEY im Backend, damit echte Antworten erzeugt werden.", false);
            }
            if (exception.getMessage() != null && exception.getMessage().contains("status 429")) {
                return new AiChatResponse("Dishly AI ist gerade kurz ausgelastet. Bitte versuche es gleich nochmal.", false);
            }
            return new AiChatResponse("Dishly AI konnte Groq gerade nicht erreichen: " + exception.getMessage(), false);
        }
    }

    private AiChatResponse tryExecuteShoppingListTool(AppUser currentUser, AiConversationContext context, AiIntentDetectionResult intent) {
        if (intent.primaryIntent() != AiIntent.ADD_TO_SHOPPING_LIST
                && !hasShoppingListFollowUpAction(intent)) {
            return null;
        }
        if (intent.confidence() < 0.85) {
            return null;
        }
        List<String> ingredients = extractIngredients(context.message(), context.history());
        if (ingredients.isEmpty()) {
            return new AiChatResponse("Welche konkreten Zutaten soll ich hinzufuegen? Schreib sie bitte z.B. so: Limette, Olivenoel, Salz.", true);
        }
        try {
            AiShoppingListToolResult result = shoppingListTool.addMissingIngredients(currentUser, ingredients);
            return new AiChatResponse(shoppingListToolMessage(result), true);
        } catch (RuntimeException exception) {
            return new AiChatResponse("Ich konnte die Zutaten gerade nicht zur Einkaufsliste hinzufuegen: " + exception.getMessage(), false);
        }
    }

    private boolean hasShoppingListFollowUpAction(AiIntentDetectionResult intent) {
        return intent.plannedActions().stream()
                .anyMatch(plan -> plan.type() == de.htwberlin.webtech.ai.model.AiActionType.ADD_INGREDIENTS_TO_SHOPPING_LIST);
    }

    private String shoppingListToolMessage(AiShoppingListToolResult result) {
        StringBuilder message = new StringBuilder();
        if (!result.changedAnything() && (!result.skippedPantryItems().isEmpty() || !result.skippedShoppingListItems().isEmpty())) {
            message.append("Ich habe nichts neu hinzugefuegt, weil ");
            if (!result.skippedPantryItems().isEmpty()) {
                message.append(joinNames(result.skippedPantryItems()))
                        .append(result.skippedPantryItems().size() == 1 ? " bereits im Vorrat ist" : " bereits im Vorrat sind");
            }
            if (!result.skippedPantryItems().isEmpty() && !result.skippedShoppingListItems().isEmpty()) {
                message.append(" und ");
            }
            if (!result.skippedShoppingListItems().isEmpty()) {
                message.append(joinNames(result.skippedShoppingListItems()))
                        .append(result.skippedShoppingListItems().size() == 1 ? " schon auf deiner Einkaufsliste steht" : " schon auf deiner Einkaufsliste stehen");
            }
            return message.append(".").toString();
        }
        if (result.changedAnything()) {
            message.append("Erledigt. Ich habe ")
                    .append(joinNames(result.addedItems()))
                    .append(" zur Einkaufsliste hinzugefuegt.");
        } else {
            message.append("Ich habe nichts neu hinzugefuegt.");
        }
        if (!result.skippedPantryItems().isEmpty()) {
            message.append(" ")
                    .append(joinNames(result.skippedPantryItems()))
                    .append(result.skippedPantryItems().size() == 1 ? " hast du bereits im Vorrat." : " hast du bereits im Vorrat.");
        }
        if (!result.skippedShoppingListItems().isEmpty()) {
            message.append(" ")
                    .append(joinNames(result.skippedShoppingListItems()))
                    .append(result.skippedShoppingListItems().size() == 1 ? " steht bereits auf deiner Einkaufsliste." : " stehen bereits auf deiner Einkaufsliste.");
        }
        return message.toString();
    }

    private List<String> extractIngredients(String message, List<AiChatRequest.AiChatTurn> history) {
        List<String> fromDirectUserCommand = extractIngredientsFromUserCommand(message);
        if (!fromDirectUserCommand.isEmpty()) {
            return fromDirectUserCommand;
        }
        List<String> fromUserMessage = extractIngredientsFromText(message);
        if (!fromUserMessage.isEmpty()) {
            return fromUserMessage;
        }
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            AiChatRequest.AiChatTurn turn = history.get(i);
            if (turn == null || !"assistant".equalsIgnoreCase(turn.getRole())) {
                continue;
            }
            List<String> ingredients = extractIngredientsFromText(turn.getText());
            if (!ingredients.isEmpty()) {
                return ingredients;
            }
        }
        return List.of();
    }

    private List<String> extractIngredientsFromUserCommand(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        List<java.util.regex.Pattern> patterns = List.of(
                java.util.regex.Pattern.compile("(?iu)(?:kannst du(?: mir)?\\s+)?(.+?)\\s+(?:in meine|in die|auf die|zur)\\s+einkaufsliste\\s+(?:packen|setzen|setze|hinzufuegen|hinzufügen|tun)"),
                java.util.regex.Pattern.compile("(?iu)(?:fuege|füge|packe|setz(?:e)?)\\s+(.+?)\\s+(?:hinzu|auf die einkaufsliste|in die einkaufsliste|in meine einkaufsliste)"),
                java.util.regex.Pattern.compile("(?iu)ich\\s+brauche\\s+(.+?)(?:\\s+auf der einkaufsliste|\\s+in der einkaufsliste|[?.!]|$)"),
                java.util.regex.Pattern.compile("(?iu)ich\\s+habe\\s+(.+?)\\s+nicht\\s+im\\s+vorrat"),
                java.util.regex.Pattern.compile("(?iu)(.+?)\\s+fehlt(?:\\s+mir)?(?:[?.!]|$)")
        );
        for (java.util.regex.Pattern pattern : patterns) {
            java.util.regex.Matcher matcher = pattern.matcher(normalized);
            if (matcher.find()) {
                List<String> ingredients = splitIngredientText(matcher.group(1)).stream()
                        .filter(this::isConcreteIngredient)
                        .toList();
                if (!ingredients.isEmpty()) {
                    return ingredients;
                }
            }
        }
        return List.of();
    }

    private boolean isConcreteIngredient(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return !normalized.isBlank()
                && !normalized.contains("einkaufsliste")
                && !List.of("es", "das", "dies", "diese", "die", "alle", "alles").contains(normalized);
    }

    private List<String> extractIngredientsFromText(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(?iu)(?:fehlende\\s+zutaten|zutaten|ingredients|malzemeler|f.r\\s+das\\s+rezept\\s+brauchst\\s+du|fuer\\s+das\\s+rezept\\s+brauchst\\s+du|du\\s+ben.tigst|du\\s+benoetigst|folgende\\s+zutaten\\s+hinzuf.gen|folgende\\s+zutaten\\s+hinzufuegen|could\\s+add)\\s*[:\\-]?\\s*([^.!?\\n]+)"
        );
        java.util.regex.Matcher matcher = pattern.matcher(normalized);
        if (!matcher.find()) {
            return List.of();
        }
        String ingredientText = matcher.group(1)
                .replaceAll("(?iu)\\b(?:m.chtest|moechtest|mochtest|willst|soll ich|would you|ister misin)\\b.*$", "");
        return splitIngredientText(ingredientText);
    }

    private List<String> splitIngredientText(String ingredientText) {
        if (ingredientText == null || ingredientText.isBlank()) {
            return List.of();
        }
        String cleaned = ingredientText
                .replaceAll("(?iu)\\b(?:nach\\s+geschmack|zum\\s+braten|ich\\s+habe\\s+es\\s+nicht\\s+im\\s+vorrat)\\b", "")
                .replaceAll("\\?.*$", "")
                .trim();
        return java.util.Arrays.stream(cleaned.split("\\s*,\\s*|\\s*&\\s*|\\s+und\\s+|\\s+and\\s+|\\s+ve\\s+"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .filter(value -> value.length() <= 60)
                .toList();
    }

    private String joinNames(List<String> names) {
        if (names.isEmpty()) {
            return "";
        }
        if (names.size() == 1) {
            return names.getFirst();
        }
        return String.join(", ", names.subList(0, names.size() - 1)) + " und " + names.getLast();
    }

    private String buildDetectedIntent(AiIntentDetectionResult intent) {
        String plans = intent.plannedActions().isEmpty()
                ? "keine"
                : intent.plannedActions().stream()
                .map(this::actionPlanLine)
                .collect(Collectors.joining("; "));
        return """

                Detected intent:
                language=%s
                normalizedRequest=%s
                primaryIntent=%s
                confidence=%.2f
                plannedActions=%s
                needsClarification=%s
                clarificationQuestion=%s
                executionStatus=not_executed
                """.formatted(
                intent.detectedLanguage(),
                fallback(intent.normalizedUserRequest(), "unbekannt"),
                intent.primaryIntent(),
                intent.confidence(),
                plans,
                intent.needsClarification(),
                fallback(intent.clarificationQuestion(), "keine")
        );
    }

    private String actionPlanLine(AiActionPlan plan) {
        return "type=" + plan.type()
                + ", confidence=" + String.format(Locale.ROOT, "%.2f", plan.confidence())
                + ", targetDate=" + (plan.targetDate() == null ? "unknown" : plan.targetDate())
                + ", mealSlot=" + (plan.mealSlot() == null ? "unknown" : plan.mealSlot())
                + ", requiresConfirmation=" + plan.requiresConfirmation();
    }

    private AiConversationContext buildConversationContext(AppUser currentUser, String message, List<AiChatRequest.AiChatTurn> history, String locale) {
        String normalizedLocale = normalizeLocale(locale);
        return new AiConversationContext(currentUser, message, history == null ? List.of() : history, normalizedLocale, buildContext(currentUser, normalizedLocale));
    }

    private String buildContext(AppUser currentUser, String locale) {
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
        List<Recipe> localizedPublishedCandidates = localizedPublishedCandidates(locale, preferences);
        String publishedRecipes = localizedPublishedCandidates.stream()
                .map(this::recipeSummary)
                .collect(Collectors.joining("; "));
        String favorites = favoriteRepository.findByOwner(currentUser).stream()
                .map(favorite -> favorite.getExternalTitle())
                .limit(FAVORITE_LIMIT)
                .collect(Collectors.joining(", "));

        return """
                Kontext fuer Dishly AI:
                Nutzer: %s
                Active UI locale: %s

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
                - Nutze Rezepttitel und Zutaten passend zur aktiven UI-Sprache.
                - Bei locale=de keine englischen Katalog-Rezepttitel oder englischen Zutaten ausgeben, wenn deutsche Kandidaten vorhanden sind.
                - Bezeichne nur Rezepte aus "Dishly recipe catalog" als gespeicherte Dishly-Katalogrezepte.
                - Wenn keine lokalisierten Katalogkandidaten vorhanden sind, kennzeichne Vorschlaege als freie Rezeptidee.
                - Keine Vorrats-, Einkaufslisten- oder Rezeptdaten erfinden.
                - Keine App-Aktionen behaupten; nur konkrete naechste Schritte empfehlen.
                - Leere oder fehlende Daten klar benennen.
                """.formatted(
                currentUser.getUsername(),
                fallback(locale, "unknown"),
                preferences == null ? "nicht ausgefuellt" : preferencesText(preferences),
                pantry.isBlank() ? "keine Vorratsdaten" : pantry,
                mealPlan.isBlank() ? "keine geplanten Mahlzeiten" : mealPlan,
                shoppingList.isBlank() ? "Einkaufsliste ist leer" : shoppingList,
                ownRecipes.isBlank() ? "keine eigenen Rezepte gespeichert" : ownRecipes,
                publishedRecipes.isBlank() ? localizedCatalogEmptyText(locale) : publishedRecipes,
                favorites.isBlank() ? "keine externen Favoriten" : favorites
        );
    }

    private List<Recipe> localizedPublishedCandidates(String locale, UserPreferences preferences) {
        if (locale == null || locale.isBlank()) {
            return recipeRepository.findRandomPublished(PUBLISHED_RECIPE_LIMIT * 3).stream()
                    .filter(recipe -> matchesPreferences(recipe, preferences))
                    .limit(PUBLISHED_RECIPE_LIMIT)
                    .toList();
        }
        return recipeRepository.findRandomPublishedByLanguage(locale, PUBLISHED_RECIPE_LIMIT * 3).stream()
                .filter(recipe -> matchesPreferences(recipe, preferences))
                .limit(PUBLISHED_RECIPE_LIMIT)
                .toList();
    }

    private String localizedCatalogEmptyText(String locale) {
        if (locale == null || locale.isBlank()) {
            return "keine passenden Dishly-Rezepte verfuegbar";
        }
        return "keine passenden lokalisierten Dishly-Rezepte fuer locale=" + locale + " verfuegbar";
    }

    private String normalizeLocale(String locale) {
        if (locale == null || locale.isBlank()) {
            return null;
        }
        String normalized = locale.trim().toLowerCase(Locale.ROOT).split("[-_]")[0];
        return normalized.matches("[a-z]{2}") ? normalized : null;
    }

    private String buildHistory(List<AiChatRequest.AiChatTurn> history) {
        if (history == null || history.isEmpty()) {
            return "\n\nBisheriger Chatverlauf: keiner";
        }
        String turns = history.stream()
                .filter(turn -> turn != null && turn.getText() != null && !turn.getText().isBlank())
                .skip(Math.max(0, history.size() - HISTORY_LIMIT))
                .map(this::historyLine)
                .collect(Collectors.joining("\n"));
        if (turns.isBlank()) {
            return "\n\nBisheriger Chatverlauf: keiner";
        }
        return "\n\nBisheriger Chatverlauf:\n" + turns;
    }

    private String historyLine(AiChatRequest.AiChatTurn turn) {
        String role = "assistant".equalsIgnoreCase(turn.getRole()) ? "Assistant" : "User";
        return role + ": " + shortText(turn.getText(), HISTORY_TURN_LIMIT);
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
