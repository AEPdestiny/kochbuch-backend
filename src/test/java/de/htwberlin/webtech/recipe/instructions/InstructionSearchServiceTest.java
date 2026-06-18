package de.htwberlin.webtech.recipe.instructions;

import de.htwberlin.webtech.recipe.dto.InstructionSearchRequest;
import de.htwberlin.webtech.recipe.dto.InstructionSearchResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InstructionSearchServiceTest {

    @Test
    void search_should_return_tavily_results() {
        InstructionSearchClient client = mock(InstructionSearchClient.class);
        when(client.search(contains("recipe instructions"))).thenReturn(List.of(
                new InstructionSearchResult("Pizza instructions", "https://example.com/pizza", "Bake it.")
        ));
        when(client.search(contains("Zubereitung Rezept"))).thenReturn(List.of());
        InstructionSearchService service = new InstructionSearchService(client);

        var response = service.search(request("Pizza Margherita"));

        assertTrue(response.isConfigured());
        assertEquals(1, response.getResults().size());
        assertEquals("Pizza instructions", response.getResults().get(0).getTitle());
        assertEquals("https://example.com/pizza", response.getResults().get(0).getUrl());
        assertTrue(response.getGoogleSearchUrl().contains("Pizza+Margherita"));
    }

    @Test
    void search_should_return_controlled_response_when_key_is_missing() {
        InstructionSearchClient client = mock(InstructionSearchClient.class);
        when(client.search(contains("recipe instructions"))).thenThrow(new InstructionSearchNotConfiguredException());
        InstructionSearchService service = new InstructionSearchService(client);

        var response = service.search(request("Sushi Bowl"));

        assertFalse(response.isConfigured());
        assertEquals("Online-Suche ist aktuell nicht konfiguriert.", response.getMessage());
        assertTrue(response.getResults().isEmpty());
        assertTrue(response.getGoogleSearchUrl().contains("Sushi+Bowl"));
    }

    @Test
    void search_should_return_controlled_response_when_tavily_fails() {
        InstructionSearchClient client = mock(InstructionSearchClient.class);
        when(client.search(contains("recipe instructions"))).thenThrow(new InstructionSearchClientException("timeout"));
        InstructionSearchService service = new InstructionSearchService(client);

        var response = service.search(request("Pasta"));

        assertTrue(response.isConfigured());
        assertEquals("Online-Suche konnte aktuell nicht durchgeführt werden.", response.getMessage());
        assertTrue(response.getResults().isEmpty());
    }

    private InstructionSearchRequest request(String title) {
        InstructionSearchRequest request = new InstructionSearchRequest();
        request.setRecipeTitle(title);
        request.setSourceName("Dishly");
        return request;
    }
}
