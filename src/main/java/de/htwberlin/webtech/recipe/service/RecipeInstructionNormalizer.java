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
        return realInstructionSteps(clean(instructions));
    }

    public static boolean hasRealInstructions(String instructions) {
        return !realInstructionSteps(clean(instructions)).isEmpty();
    }

    public static List<String> extractRealStepsFromSnippet(String snippet) {
        return realInstructionSteps(clean(snippet));
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
}
