package de.htwberlin.webtech.recipe.instructions;

import de.htwberlin.webtech.recipe.dto.InstructionSearchResult;
import de.htwberlin.webtech.recipe.entity.Recipe;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecipeInstructionSuggestionServiceTest {

    @Test
    void suggestFor_should_map_tavily_results_to_instruction_suggestions() {
        InstructionSearchClient client = mock(InstructionSearchClient.class);
        when(client.search(anyString())).thenReturn(List.of());
        when(client.search(contains("recipe instructions steps"))).thenReturn(List.of(
                new InstructionSearchResult(
                        "Pasta instructions",
                        "https://example.com/pasta",
                        "Prepare the pasta. Heat the sauce. Add the pasta and serve warm."
                )
        ));
        when(client.search(contains("Zubereitung Rezept Schritte"))).thenReturn(List.of());
        RecipeInstructionSuggestionService service = new RecipeInstructionSuggestionService(client);

        var response = service.suggestFor(recipe("Tomato Pasta"));

        assertTrue(response.isConfigured());
        assertFalse(response.isHasRealInstructions());
        assertEquals(1, response.getSuggestions().size());
        assertEquals("Pasta instructions", response.getSuggestions().getFirst().getSourceTitle());
        assertEquals("https://example.com/pasta", response.getSuggestions().getFirst().getSourceUrl());
        assertEquals(
                List.of("Prepare the pasta.", "Heat the sauce.", "Add the pasta and serve warm."),
                response.getSuggestions().getFirst().getSteps()
        );
    }

    @Test
    void suggestFor_should_return_controlled_response_when_key_is_missing() {
        InstructionSearchClient client = mock(InstructionSearchClient.class);
        when(client.search(contains("recipe instructions steps"))).thenThrow(new InstructionSearchNotConfiguredException());
        RecipeInstructionSuggestionService service = new RecipeInstructionSuggestionService(client);

        var response = service.suggestFor(recipe("Sushi Bowl"));

        assertFalse(response.isConfigured());
        assertEquals("Online-Suche ist aktuell nicht konfiguriert.", response.getMessage());
        assertTrue(response.getSuggestions().isEmpty());
    }

    @Test
    void suggestFor_should_return_controlled_response_when_tavily_fails() {
        InstructionSearchClient client = mock(InstructionSearchClient.class);
        when(client.search(contains("recipe instructions steps"))).thenThrow(new InstructionSearchClientException("timeout"));
        RecipeInstructionSuggestionService service = new RecipeInstructionSuggestionService(client);

        var response = service.suggestFor(recipe("Pasta"));

        assertTrue(response.isConfigured());
        assertEquals("Zubereitungsvorschläge konnten aktuell nicht geladen werden.", response.getMessage());
        assertTrue(response.getSuggestions().isEmpty());
    }

    @Test
    void suggestFor_should_not_search_when_recipe_already_has_real_instructions() {
        InstructionSearchClient client = mock(InstructionSearchClient.class);
        Recipe recipe = recipe("Salat");
        recipe.setInstructions("1. Gemüse waschen.\n2. Zutaten schneiden.\n3. Servieren.");
        RecipeInstructionSuggestionService service = new RecipeInstructionSuggestionService(client);

        var response = service.suggestFor(recipe);

        assertTrue(response.isHasRealInstructions());
        assertTrue(response.getSuggestions().isEmpty());
        assertEquals("Dieses Rezept hat bereits eine Zubereitung.", response.getMessage());
    }

    private Recipe recipe(String title) {
        Recipe recipe = new Recipe();
        recipe.setId(42L);
        recipe.setTitle(title);
        recipe.setLanguage("de");
        recipe.setSourceUrl("https://example.com/source");
        recipe.setSourceName("Example");
        recipe.setInstructions("Dieser Eintrag enthält nur Kalorien und Quelle: Foodista.");
        return recipe;
    }
}
