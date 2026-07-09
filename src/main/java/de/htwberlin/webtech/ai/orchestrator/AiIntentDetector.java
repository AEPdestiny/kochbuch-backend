package de.htwberlin.webtech.ai.orchestrator;

import de.htwberlin.webtech.ai.dto.AiChatRequest;
import de.htwberlin.webtech.ai.model.AiActionPlan;
import de.htwberlin.webtech.ai.model.AiActionType;
import de.htwberlin.webtech.ai.model.AiDetectedLanguage;
import de.htwberlin.webtech.ai.model.AiIntent;
import de.htwberlin.webtech.ai.model.AiIntentDetectionResult;
import de.htwberlin.webtech.mealplan.entity.MealSlot;
import jakarta.enterprise.context.ApplicationScoped;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class AiIntentDetector {

    private static final Pattern NUMERIC_SELECTION = Pattern.compile("\\b([123])\\b");
    private static final Pattern NUMBERED_OPTION = Pattern.compile("(?:^|\\s|\\()([123])\\)?\\s*(?:=|:|-)?\\s*([^()]+?)(?=\\s*\\(?[123]\\)?\\s*(?:=|:|-)?\\s+|$)");

    public AiIntentDetectionResult detect(String message, List<AiChatRequest.AiChatTurn> history) {
        String normalized = normalize(message);
        AiDetectedLanguage language = detectLanguage(message, normalized);
        if (normalized.isBlank()) {
            return new AiIntentDetectionResult(language, normalized, AiIntent.UNKNOWN, List.of(), 0.0, false, null);
        }

        List<Integer> selections = selectedNumbers(normalized);
        if (!selections.isEmpty() && previousAssistantOfferedNumberedOptions(history)) {
            List<ResolvedOption> resolvedOptions = resolveSelectedOptions(selections, history);
            List<AiActionPlan> plans = resolvedOptions.stream()
                    .map(option -> planForOptionText(option.text()))
                    .filter(plan -> plan != null)
                    .toList();
            String normalizedSelection = resolvedOptions.isEmpty()
                    ? normalized
                    : resolvedOptions.stream()
                    .map(option -> "Der Nutzer hat Option " + option.number() + " gewaehlt: " + option.text())
                    .collect(java.util.stream.Collectors.joining("; "));
            return new AiIntentDetectionResult(
                    language,
                    normalizedSelection,
                    AiIntent.FOLLOW_UP_SELECTION,
                    plans,
                    0.95,
                    !plans.isEmpty() && needsClarification(plans),
                    clarificationFor(plans)
            );
        }

        if (isShoppingListConfirmation(normalized) && previousAssistantProvidedShoppingIngredients(history)) {
            AiActionPlan plan = action(AiActionType.ADD_INGREDIENTS_TO_SHOPPING_LIST, 0.89, null, null);
            return result(language, normalized, AiIntent.ADD_TO_SHOPPING_LIST, List.of(plan), false, null, 0.89);
        }

        if ((containsAny(normalized, "einkaufsliste", "shopping list")
                && containsAny(normalized, "fuge", "fuege", "hinzu", "hinzufugen", "add", "put", "setz", "setze", "zutaten"))
                || containsAll(normalized, "zutaten", "hinzu")
                || containsAll(normalized, "ingredients", "add")
                || containsAny(normalized, "alisveris listesi", "alisveris listesine")
                || containsAll(normalized, "add", "shopping")
                || containsAll(normalized, "ekle", "alisveris")) {
            AiActionPlan plan = action(AiActionType.ADD_INGREDIENTS_TO_SHOPPING_LIST, 0.86, null, null);
            return result(language, normalized, AiIntent.ADD_TO_SHOPPING_LIST, List.of(plan), false, null, 0.86);
        }

        if (isMealPlanIntent(normalized)) {
            MealSlot slot = detectMealSlot(normalized);
            LocalDate date = detectTargetDate(normalized);
            boolean needsClarification = slot == null || date == null;
            AiActionPlan plan = action(AiActionType.ADD_RECIPE_TO_MEAL_PLAN, 0.82, date, slot);
            return result(
                    language,
                    normalized,
                    AiIntent.ADD_TO_MEAL_PLAN,
                    List.of(plan),
                    needsClarification,
                    needsClarification ? mealPlanClarification(slot, date) : null,
                    0.82
            );
        }

        if (containsAny(normalized, "details", "detail", "rezept offnen", "recipe details", "open recipe", "davon", "dieses gericht")) {
            AiActionPlan plan = action(AiActionType.OPEN_RECIPE, 0.78, null, null);
            return result(language, normalized, AiIntent.OPEN_RECIPE_DETAILS, List.of(plan), true, "Welches Rezept soll ich genau oeffnen?", 0.78);
        }

        if (containsAny(normalized, "restaurant", "lokal", "essen gehen", "nearby restaurant", "find restaurant")) {
            AiActionPlan plan = action(AiActionType.FIND_RESTAURANT, 0.84, null, null);
            return result(language, normalized, AiIntent.FIND_RESTAURANT, List.of(plan), true, "Fuer welchen Ort soll ich Restaurants suchen?", 0.84);
        }

        if (containsAny(normalized, "ja", "ok", "okay", "mach das", "ok mach", "evet", "tamam", "yes")) {
            return new AiIntentDetectionResult(
                    language,
                    normalized,
                    AiIntent.CLARIFICATION_RESPONSE,
                    List.of(),
                    0.62,
                    true,
                    "Worauf soll ich das anwenden?"
            );
        }

        return AiIntentDetectionResult.question(language, normalized);
    }

    private AiIntentDetectionResult result(AiDetectedLanguage language,
                                           String normalized,
                                           AiIntent intent,
                                           List<AiActionPlan> plans,
                                           boolean needsClarification,
                                           String clarificationQuestion,
                                           double confidence) {
        return new AiIntentDetectionResult(language, normalized, intent, plans, confidence, needsClarification, clarificationQuestion);
    }

    private AiActionPlan planForOptionText(String optionText) {
        String normalized = normalize(optionText);
        if (containsAny(normalized, "einkaufsliste", "shopping list", "zutaten hinzufugen", "zutaten hinzufuegen", "alisveris")) {
            return action(AiActionType.ADD_INGREDIENTS_TO_SHOPPING_LIST, 0.95, null, null);
        }
        if (containsAny(normalized, "wochenplan", "meal plan", "planen", "einplanen")) {
            return action(AiActionType.ADD_RECIPE_TO_MEAL_PLAN, 0.95, null, null);
        }
        if (containsAny(normalized, "restaurant", "lokal", "essen gehen", "find restaurant")) {
            return action(AiActionType.FIND_RESTAURANT, 0.95, null, null);
        }
        if (containsAny(normalized, "details", "detail", "rezept offnen", "recipe details")) {
            return action(AiActionType.OPEN_RECIPE, 0.90, null, null);
        }
        return null;
    }

    private AiActionPlan action(AiActionType type, double confidence, LocalDate targetDate, MealSlot mealSlot) {
        return new AiActionPlan(
                type,
                confidence,
                null,
                null,
                null,
                targetDate,
                mealSlot,
                List.of(),
                null,
                true
        );
    }

    private List<Integer> selectedNumbers(String normalized) {
        Matcher matcher = NUMERIC_SELECTION.matcher(normalized);
        Set<Integer> selections = new LinkedHashSet<>();
        while (matcher.find()) {
            selections.add(Integer.parseInt(matcher.group(1)));
        }
        return new ArrayList<>(selections);
    }

    private List<ResolvedOption> resolveSelectedOptions(List<Integer> selections, List<AiChatRequest.AiChatTurn> history) {
        String lastOptionsText = lastAssistantTextWithNumberedOptions(history);
        if (lastOptionsText.isBlank()) {
            return List.of();
        }
        java.util.Map<Integer, String> options = parseNumberedOptions(lastOptionsText);
        return selections.stream()
                .filter(options::containsKey)
                .map(selection -> new ResolvedOption(selection, options.get(selection)))
                .toList();
    }

    private String lastAssistantTextWithNumberedOptions(List<AiChatRequest.AiChatTurn> history) {
        if (history == null || history.isEmpty()) {
            return "";
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            AiChatRequest.AiChatTurn turn = history.get(i);
            if (turn == null || !"assistant".equalsIgnoreCase(turn.getRole()) || turn.getText() == null) {
                continue;
            }
            if (previousAssistantOfferedNumberedOptions(List.of(turn))) {
                return turn.getText();
            }
        }
        return "";
    }

    private java.util.Map<Integer, String> parseNumberedOptions(String text) {
        String compact = text.replaceAll("\\s+", " ").trim();
        Matcher matcher = NUMBERED_OPTION.matcher(compact);
        java.util.Map<Integer, String> options = new java.util.LinkedHashMap<>();
        while (matcher.find()) {
            int number = Integer.parseInt(matcher.group(1));
            String option = matcher.group(2)
                    .replaceAll("(?i)\\b(?:moechtest|mochtest|willst|soll ich|oder)\\b", "")
                    .replaceAll("[?.!]+$", "")
                    .replaceAll("\\s+", " ")
                    .trim();
            if (!option.isBlank()) {
                options.put(number, option);
            }
        }
        return options;
    }

    private boolean isShoppingListConfirmation(String normalized) {
        return containsAny(normalized,
                "alle",
                "alles",
                "ja",
                "ok",
                "okay",
                "nimm alle",
                "fuge alle hinzu",
                "fuege alle hinzu",
                "hinzufugen",
                "add all",
                "yes",
                "evet",
                "tamam");
    }

    private boolean previousAssistantProvidedShoppingIngredients(List<AiChatRequest.AiChatTurn> history) {
        if (history == null || history.isEmpty()) {
            return false;
        }
        return history.stream()
                .filter(turn -> turn != null && "assistant".equalsIgnoreCase(turn.getRole()) && turn.getText() != null)
                .map(AiChatRequest.AiChatTurn::getText)
                .map(this::normalize)
                .anyMatch(text -> containsAny(text,
                        "zutaten",
                        "fehlende zutaten",
                        "folgende zutaten hinzufugen",
                        "du benotigst",
                        "brauchst du",
                        "could add",
                        "ingredients",
                        "malzemeler"));
    }

    private boolean previousAssistantOfferedNumberedOptions(List<AiChatRequest.AiChatTurn> history) {
        if (history == null || history.isEmpty()) {
            return false;
        }
        return history.stream()
                .filter(turn -> turn != null && "assistant".equalsIgnoreCase(turn.getRole()) && turn.getText() != null)
                .map(AiChatRequest.AiChatTurn::getText)
                .map(this::normalize)
                .anyMatch(text -> text.contains("(1)") || text.contains("1)") || text.contains(" 1 ")
                        || text.contains("(2)") || text.contains("2)") || text.contains(" 2 ")
                        || text.contains("(3)") || text.contains("3)") || text.contains(" 3 "));
    }

    private record ResolvedOption(int number, String text) {
    }

    private boolean isMealPlanIntent(String normalized) {
        return containsAny(normalized,
                "wochenplan",
                "meal plan",
                "plan",
                "plana",
                "rein",
                "morgen abend",
                "morge abnd",
                "yarin aksam",
                "yarin plana");
    }

    private MealSlot detectMealSlot(String normalized) {
        if (containsAny(normalized, "fruhstuck", "breakfast", "kahvalti")) {
            return MealSlot.BREAKFAST;
        }
        if (containsAny(normalized, "mittag", "lunch", "ogle", "oglen")) {
            return MealSlot.LUNCH;
        }
        if (containsAny(normalized, "abend", "abnd", "dinner", "aksam")) {
            return MealSlot.DINNER;
        }
        if (containsAny(normalized, "snack", "zwischenmahlzeit")) {
            return MealSlot.SNACK;
        }
        return null;
    }

    private LocalDate detectTargetDate(String normalized) {
        LocalDate today = LocalDate.now();
        if (containsAny(normalized, "morgen", "morge", "tomorrow", "yarin")) {
            return today.plusDays(1);
        }
        if (containsAny(normalized, "heute", "today", "bugun")) {
            return today;
        }
        DayOfWeek dayOfWeek = detectDayOfWeek(normalized);
        if (dayOfWeek != null) {
            return today.with(TemporalAdjusters.nextOrSame(dayOfWeek));
        }
        return null;
    }

    private DayOfWeek detectDayOfWeek(String normalized) {
        if (containsAny(normalized, "montag", "monday", "pazartesi")) {
            return DayOfWeek.MONDAY;
        }
        if (containsAny(normalized, "dienstag", "tuesday", "sali")) {
            return DayOfWeek.TUESDAY;
        }
        if (containsAny(normalized, "mittwoch", "wednesday", "carsamba")) {
            return DayOfWeek.WEDNESDAY;
        }
        if (containsAny(normalized, "donnerstag", "thursday", "persembe")) {
            return DayOfWeek.THURSDAY;
        }
        if (containsAny(normalized, "freitag", "friday", "cuma")) {
            return DayOfWeek.FRIDAY;
        }
        if (containsAny(normalized, "samstag", "saturday", "cumartesi")) {
            return DayOfWeek.SATURDAY;
        }
        if (containsAny(normalized, "sonntag", "sunday", "pazar")) {
            return DayOfWeek.SUNDAY;
        }
        return null;
    }

    private boolean needsClarification(List<AiActionPlan> plans) {
        return plans.stream().anyMatch(plan -> plan.type() == AiActionType.ADD_RECIPE_TO_MEAL_PLAN);
    }

    private String clarificationFor(List<AiActionPlan> plans) {
        if (needsClarification(plans)) {
            return "Fuer welchen Tag und welche Mahlzeit soll ich das planen?";
        }
        if (plans.stream().anyMatch(plan -> plan.type() == AiActionType.FIND_RESTAURANT)) {
            return "Fuer welchen Ort soll ich Restaurants suchen?";
        }
        return null;
    }

    private String mealPlanClarification(MealSlot slot, LocalDate date) {
        if (slot == null && date == null) {
            return "Fuer welchen Tag und welche Mahlzeit soll ich das planen?";
        }
        if (slot == null) {
            return "Fuer welche Mahlzeit soll ich das planen?";
        }
        return "Fuer welchen Tag soll ich das planen?";
    }

    private AiDetectedLanguage detectLanguage(String original, String normalized) {
        if (original != null && original.matches(".*\\p{InArabic}.*")) {
            return AiDetectedLanguage.AR;
        }
        boolean turkish = containsAny(normalized, "alisveris", "ekle", "yarin", "aksam", "plana", "bunu", "tamam", "evet");
        boolean english = containsAny(normalized, "add", "shopping", "meal plan", "details", "restaurant", "tomorrow", "dinner");
        boolean german = containsAny(normalized, "einkaufsliste", "wochenplan", "morgen", "abend", "details", "restaurant", "mach", "fuge");
        int matches = (turkish ? 1 : 0) + (english ? 1 : 0) + (german ? 1 : 0);
        if (matches > 1) {
            return AiDetectedLanguage.MIXED;
        }
        if (turkish) {
            return AiDetectedLanguage.TR;
        }
        if (english) {
            return AiDetectedLanguage.EN;
        }
        if (german) {
            return AiDetectedLanguage.DE;
        }
        return AiDetectedLanguage.UNKNOWN;
    }

    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAll(String value, String first, String second) {
        return value.contains(first) && value.contains(second);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String replaced = value
                .replace('ı', 'i')
                .replace('İ', 'I')
                .replace('ğ', 'g')
                .replace('Ğ', 'G')
                .replace('ş', 's')
                .replace('Ş', 'S')
                .replace('ç', 'c')
                .replace('Ç', 'C')
                .replace('ö', 'o')
                .replace('Ö', 'O')
                .replace('ü', 'u')
                .replace('Ü', 'U')
                .replace('ä', 'a')
                .replace('Ä', 'A')
                .replace('ß', 's');
        return Normalizer.normalize(replaced, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^\\p{L}\\p{N}()]+", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}
