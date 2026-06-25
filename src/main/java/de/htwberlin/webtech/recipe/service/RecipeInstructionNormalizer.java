package de.htwberlin.webtech.recipe.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class RecipeInstructionNormalizer {

    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
    private static final Pattern NUMBERED_STEP_BOUNDARY = Pattern.compile("(?m)(?:^|\\n)\\s*\\d+[.)]\\s*");
    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("(?<=[.!?])\\s+");

    private static final List<String> GERMAN_ACTION_WORDS = List.of(
            "schneiden", "waschen", "erhitzen", "kochen", "backen", "braten", "mixen",
            "verrühren", "verruehren", "hinzufügen", "hinzufuegen", "servieren",
            "vorbereiten", "geben", "vermengen", "abschmecken", "würzen", "wuerzen",
            "köcheln", "koecheln", "garen", "anrichten", "pürieren", "puerieren"
    );
    private static final List<String> ENGLISH_ACTION_WORDS = List.of(
            "chop", "wash", "heat", "cook", "bake", "fry", "mix", "stir", "add",
            "serve", "blend", "prepare", "combine", "season", "simmer", "boil"
    );
    private static final List<String> SUMMARY_MARKERS = List.of(
            "eignet sich", "passt gut als", "das rezept ergibt", "eine portion enthält",
            "eine portion enthaelt", "pro portion", "die zubereitung dauert", "quelle:",
            "kalorien", "protein", "fett", "requires around", "serves", "contains",
            "source:", "ready in", "per serving", "calories", "fat"
    );

    private RecipeInstructionNormalizer() {
    }

    public static List<String> normalizeToList(
            String instructions,
            String title,
            String category,
            String dishTypes,
            List<String> ingredients,
            String language
    ) {
        String cleaned = clean(instructions);
        List<String> realSteps = realInstructionSteps(cleaned);
        if (!realSteps.isEmpty()) {
            return realSteps;
        }
        return fallbackSteps(title, category, dishTypes, ingredients, language);
    }

    private static List<String> realInstructionSteps(String value) {
        if (value.isBlank() || isPlaceholder(value) || isLikelySummary(value)) {
            return List.of();
        }

        List<String> numbered = splitNumberedSteps(value);
        if (numbered.size() > 1 && containsActionWord(String.join(" ", numbered))) {
            return numbered;
        }

        List<String> lines = splitLines(value);
        if (lines.size() > 1 && actionWordCount(lines) >= 2) {
            return lines;
        }

        List<String> sentences = splitSentences(value);
        if (sentences.size() > 1 && actionWordCount(sentences) >= 2) {
            return sentences;
        }

        if (containsActionWord(value) && !hasSummaryMarker(value)) {
            return List.of(value);
        }

        return List.of();
    }

    private static List<String> splitNumberedSteps(String value) {
        String[] parts = NUMBERED_STEP_BOUNDARY.split(value);
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            String step = cleanStep(part);
            if (!step.isBlank()) {
                result.add(step);
            }
        }
        return result;
    }

    private static List<String> splitLines(String value) {
        String[] parts = value.split("\\n+");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            String step = cleanStep(part);
            if (!step.isBlank()) {
                result.add(step);
            }
        }
        return result;
    }

    private static List<String> splitSentences(String value) {
        String[] parts = SENTENCE_BOUNDARY.split(value);
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            String step = cleanStep(part);
            if (!step.isBlank()) {
                result.add(step);
            }
        }
        return result;
    }

    private static int actionWordCount(List<String> values) {
        int count = 0;
        for (String value : values) {
            if (containsActionWord(value)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isLikelySummary(String value) {
        return hasSummaryMarker(value) && !containsActionWord(value);
    }

    private static boolean hasSummaryMarker(String value) {
        String normalized = normalize(value);
        return SUMMARY_MARKERS.stream().anyMatch(normalized::contains);
    }

    private static boolean containsActionWord(String value) {
        String normalized = normalize(value);
        return GERMAN_ACTION_WORDS.stream().anyMatch(normalized::contains)
                || ENGLISH_ACTION_WORDS.stream().anyMatch(normalized::contains);
    }

    private static boolean isPlaceholder(String value) {
        String normalized = normalize(value);
        return normalized.isBlank()
                || normalized.equals("keine anleitung angegeben.")
                || normalized.equals("keine zubereitung angegeben.")
                || normalized.equals("no instructions provided.")
                || normalized.equals("no instructions available.");
    }

    private static List<String> fallbackSteps(
            String title,
            String category,
            String dishTypes,
            List<String> ingredients,
            String language
    ) {
        String context = normalize(String.join(" ",
                nullToBlank(title),
                nullToBlank(category),
                nullToBlank(dishTypes),
                String.join(" ", ingredients == null ? List.of() : ingredients)
        ));
        boolean german = isGerman(language);

        if (containsAny(context, "smoothie", "getränk", "getraenk", "drink", "beverage")) {
            return german
                    ? List.of(
                    "Obst und weitere Zutaten vorbereiten.",
                    "Alle Zutaten in einen Mixer geben.",
                    "Fein und cremig mixen.",
                    "Bei Bedarf mit etwas Flüssigkeit anpassen und sofort servieren."
            )
                    : List.of(
                    "Prepare the fruit and remaining ingredients.",
                    "Add all ingredients to a blender.",
                    "Blend until smooth and creamy.",
                    "Adjust with a little liquid if needed and serve immediately."
            );
        }

        if (containsAny(context, "salat", "salad")) {
            return german
                    ? List.of(
                    "Zutaten waschen und vorbereiten.",
                    "Zutaten in mundgerechte Stücke schneiden.",
                    "Alles in einer Schüssel vermengen.",
                    "Abschmecken und servieren."
            )
                    : List.of(
                    "Wash and prepare the ingredients.",
                    "Cut the ingredients into bite-sized pieces.",
                    "Combine everything in a bowl.",
                    "Season to taste and serve."
            );
        }

        if (containsAny(context, "suppe", "soup")) {
            return german
                    ? List.of(
                    "Gemüse und weitere Zutaten vorbereiten.",
                    "Zutaten in einem Topf erhitzen.",
                    "Köcheln lassen, bis alles gar ist.",
                    "Nach Geschmack würzen und servieren."
            )
                    : List.of(
                    "Prepare the vegetables and remaining ingredients.",
                    "Heat the ingredients in a pot.",
                    "Simmer until everything is cooked.",
                    "Season to taste and serve."
            );
        }

        if (containsAny(context, "pasta", "nudel", "spaghetti", "macaroni")) {
            return german
                    ? List.of(
                    "Pasta nach Packungsanleitung kochen.",
                    "Weitere Zutaten vorbereiten.",
                    "Zutaten in einer Pfanne oder einem Topf erhitzen.",
                    "Pasta mit der Sauce vermengen und servieren."
            )
                    : List.of(
                    "Cook the pasta according to the package instructions.",
                    "Prepare the remaining ingredients.",
                    "Heat the ingredients in a pan or pot.",
                    "Combine the pasta with the sauce and serve."
            );
        }

        if (containsAny(context, "pfannkuchen", "pancake", "muffin", "brot", "bread", "cake", "gebacken", "baked")) {
            return german
                    ? List.of(
                    "Zutaten vorbereiten und Backofen oder Pfanne vorheizen.",
                    "Trockene und feuchte Zutaten getrennt vorbereiten.",
                    "Alles zu einem Teig verrühren.",
                    "Backen oder ausbacken, bis das Gericht gar ist."
            )
                    : List.of(
                    "Prepare the ingredients and preheat the oven or pan.",
                    "Prepare dry and wet ingredients separately.",
                    "Mix everything into a batter or dough.",
                    "Bake or cook until done."
            );
        }

        return german
                ? List.of(
                "Zutaten vorbereiten.",
                "Zutaten nach Rezeptart garen oder vermengen.",
                "Nach Geschmack würzen.",
                "Anrichten und servieren."
        )
                : List.of(
                "Prepare the ingredients.",
                "Cook or combine the ingredients according to the recipe type.",
                "Season to taste.",
                "Plate and serve."
        );
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isGerman(String language) {
        return language != null && language.toLowerCase(Locale.ROOT).startsWith("de");
    }

    private static String clean(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return HTML_TAG.matcher(value).replaceAll(" ")
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replaceAll("[ \\t]+", " ")
                .trim();
    }

    private static String cleanStep(String value) {
        return clean(value)
                .replaceAll("^\\s*\\d+[.)]\\s*", "")
                .trim();
    }

    private static String normalize(String value) {
        return clean(value).toLowerCase(Locale.ROOT);
    }

    private static String nullToBlank(String value) {
        return value == null ? "" : value;
    }
}
