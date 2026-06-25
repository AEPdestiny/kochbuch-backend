package de.htwberlin.webtech.recipe.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class RecipeIngredientNormalizer {

    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
    private static final Pattern STANDALONE_ZERO_SEPARATOR = Pattern.compile("\\s+0\\s+");
    private static final Pattern SIMPLE_VALUE = Pattern.compile("^[01](?:[,.]0+)?$");
    private static final Pattern LEADING_ZERO_UNIT = Pattern.compile("^(?:0(?:[,.]0+)?\\s*)?(?:ml|g|kg|l|el|tl|tbsp|tsp|cup|cups|serving|servings)\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern LEADING_SERVINGS = Pattern.compile("^\\d+(?:[,.]\\d+)?\\s+servings?\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMMA_INGREDIENT_BOUNDARY = Pattern.compile(",\\s*(?=(?:\\d|\\d/|[\\u00BC\\u00BD\\u00BE]|\\p{Lu}))");
    private static final String AMOUNT_PATTERN = "(?:\\d+(?:[,.]\\d+)?|\\d+/\\d+|[\\u00BC\\u00BD\\u00BE])";
    private static final String DIRECT_UNIT_PATTERN =
            "ml|g|kg|l|el|tl|tbsp|tsp|cups?|servings?|strips?|cloves?|sticks?|prise|prisen|zehe|zehen|stück|stueck|scheiben?|dose|dosen|bund|handvoll";
    private static final String TRAILING_UNIT_PATTERN =
            "cloves?|zehen?|sticks?|strips?|scheiben?";
    private static final Pattern EMBEDDED_AMOUNT_BOUNDARY = Pattern.compile(
            "(?<=\\p{L})\\s+(?=" + AMOUNT_PATTERN + "\\s+"
                    + "(?:(?i:" + DIRECT_UNIT_PATTERN + ")\\b|\\p{Lu}|\\p{L}+\\s+(?i:" + TRAILING_UNIT_PATTERN + ")\\b))"
    );

    private RecipeIngredientNormalizer() {
    }

    public static String normalizeToText(String value) {
        return String.join("\n", normalizeToList(value));
    }

    public static List<String> normalizeToList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        List<String> rawParts = splitRaw(value);
        Set<String> seen = new LinkedHashSet<>();
        List<String> normalized = new ArrayList<>();

        for (String rawPart : rawParts) {
            String cleaned = cleanIngredient(rawPart);
            if (cleaned.isBlank() || isNoise(cleaned)) {
                continue;
            }
            String key = cleaned.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
            if (seen.add(key)) {
                normalized.add(cleaned);
            }
        }

        return normalized;
    }

    public static int countIngredients(String value) {
        return normalizeToList(value).size();
    }

    private static List<String> splitRaw(String value) {
        String normalized = value
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");

        if (normalized.contains("\n")) {
            return splitEmbeddedBoundaries(splitByRegex(normalized, "\\n+"));
        }
        if (normalized.contains(";")) {
            return splitEmbeddedBoundaries(splitByRegex(normalized, "\\s*;\\s*"));
        }
        if (STANDALONE_ZERO_SEPARATOR.matcher(normalized).find()) {
            return splitEmbeddedBoundaries(splitByRegex(normalized, "\\s+0\\s+"));
        }
        if (COMMA_INGREDIENT_BOUNDARY.matcher(normalized).find()) {
            return splitEmbeddedBoundaries(List.of(COMMA_INGREDIENT_BOUNDARY.split(normalized)));
        }
        return splitEmbeddedBoundaries(List.of(normalized));
    }

    private static List<String> splitByRegex(String value, String regex) {
        String[] parts = value.split(regex);
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            result.add(part);
        }
        return result;
    }

    private static List<String> splitEmbeddedBoundaries(List<String> values) {
        List<String> result = new ArrayList<>();
        for (String value : values) {
            String[] parts = EMBEDDED_AMOUNT_BOUNDARY.split(value);
            for (String part : parts) {
                result.add(part);
            }
        }
        return result;
    }

    private static String cleanIngredient(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = HTML_TAG.matcher(value).replaceAll(" ")
                .replaceAll("\\s+", " ")
                .trim();
        cleaned = cleaned.replaceAll("^[,;:.\\-\\s]+", "").replaceAll("[,;:.\\-\\s]+$", "").trim();
        cleaned = LEADING_SERVINGS.matcher(cleaned).replaceFirst("").trim();
        cleaned = LEADING_ZERO_UNIT.matcher(cleaned).replaceFirst("").trim();
        cleaned = cleaned.replaceAll("\\s+[01](?:[,.]0+)?$", "").trim();
        return cleaned.replaceAll("\\s+", " ").trim();
    }

    private static boolean isNoise(String value) {
        if (SIMPLE_VALUE.matcher(value).matches()) {
            return true;
        }
        return value.chars().noneMatch(Character::isLetter);
    }
}
