package de.htwberlin.webtech.recipe.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RecipeInstructionNormalizerTest {

    @Test
    void normalizeToList_should_replace_german_summary_with_smoothie_steps() {
        String summary = "Dieser Smoothie mit Blaubeeren, Kaki, Banane und Erdnussbutter eignet sich gut als schnelles Frühstück oder Getränk. "
                + "Das Rezept ergibt 2 Portionen. Eine Portion enthält 300 Kalorien. Quelle: Foodista.";

        List<String> steps = RecipeInstructionNormalizer.normalizeToList(
                summary,
                "Smoothie mit Blaubeeren",
                "breakfast",
                "Getränk",
                List.of("Banane", "Kaki", "Blaubeeren"),
                "de"
        );

        assertEquals(
                List.of(
                        "Obst und weitere Zutaten vorbereiten.",
                        "Alle Zutaten in einen Mixer geben.",
                        "Fein und cremig mixen.",
                        "Bei Bedarf mit etwas Flüssigkeit anpassen und sofort servieren."
                ),
                steps
        );
        assertFalse(steps.contains(summary));
    }

    @Test
    void normalizeToList_should_keep_numbered_real_instructions() {
        List<String> steps = RecipeInstructionNormalizer.normalizeToList(
                "1. Gemüse waschen.\n2. Zutaten schneiden.\n3. Alles servieren.",
                "Salat",
                "lunch",
                "Salat",
                List.of("Gemüse"),
                "de"
        );

        assertEquals(
                List.of("Gemüse waschen.", "Zutaten schneiden.", "Alles servieren."),
                steps
        );
    }

    @Test
    void normalizeToList_should_keep_english_real_instructions() {
        List<String> steps = RecipeInstructionNormalizer.normalizeToList(
                "Chop the onion. Heat the pan. Add the pasta and serve warm.",
                "Pasta",
                "dinner",
                "main course",
                List.of("onion", "pasta"),
                "en"
        );

        assertEquals(
                List.of("Chop the onion.", "Heat the pan.", "Add the pasta and serve warm."),
                steps
        );
    }

    @Test
    void normalizeToList_should_create_english_fallback_for_summary() {
        List<String> steps = RecipeInstructionNormalizer.normalizeToList(
                "This pasta recipe serves 2 and contains 450 calories per serving. Source: Foodista.",
                "Tomato Pasta",
                "dinner",
                "main course",
                List.of("pasta", "tomato"),
                "en"
        );

        assertEquals(
                List.of(
                        "Cook the pasta according to the package instructions.",
                        "Prepare the remaining ingredients.",
                        "Heat the ingredients in a pan or pot.",
                        "Combine the pasta with the sauce and serve."
                ),
                steps
        );
    }
}
