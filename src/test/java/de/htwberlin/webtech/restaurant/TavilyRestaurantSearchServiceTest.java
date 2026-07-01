package de.htwberlin.webtech.restaurant;

import de.htwberlin.webtech.restaurant.client.TavilyRestaurantSearchClient;
import de.htwberlin.webtech.restaurant.client.TavilyRestaurantSearchClient.TavilyRestaurantResult;
import de.htwberlin.webtech.restaurant.dto.RestaurantResponse;
import de.htwberlin.webtech.restaurant.dto.TavilyRestaurantSearchResponse;
import de.htwberlin.webtech.restaurant.service.TavilyRestaurantSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class TavilyRestaurantSearchServiceTest {

    private final TavilyRestaurantSearchClient client = mock(TavilyRestaurantSearchClient.class);
    private final TavilyRestaurantSearchService underTest = new TavilyRestaurantSearchService(client);

    @BeforeEach
    void setUp() {
        doReturn(true).when(client).isConfigured();
    }

    // === Status tests ===

    @Test
    @DisplayName("search returns unavailable status when Tavily API key is not configured")
    void search_returns_unavailable_when_not_configured() {
        doReturn(false).when(client).isConfigured();

        TavilyRestaurantSearchResponse result = underTest.search("Pasta Carbonara", "Berlin");

        assertEquals("unavailable", result.getStatus());
        assertTrue(result.getResults().isEmpty());
    }

    @Test
    @DisplayName("search returns no_results status when all Tavily results are filtered out")
    void search_returns_no_results_when_all_filtered() {
        doReturn(List.of(
                new TavilyRestaurantResult("Hotel Mitte Berlin", "https://hotel.de", "Luxury hotel with spa.")
        )).when(client).search("Pasta Carbonara", "Berlin");

        TavilyRestaurantSearchResponse result = underTest.search("Pasta Carbonara", "Berlin");

        assertEquals("no_results", result.getStatus());
        assertTrue(result.getResults().isEmpty());
    }

    @Test
    @DisplayName("search returns ok status when at least one result passes the plausibility filter")
    void search_returns_ok_when_results_found() {
        doReturn(List.of(
                new TavilyRestaurantResult(
                        "Pasta Palace Berlin",
                        "https://pasta-palace.de",
                        "Best Pasta Carbonara in Berlin. Our restaurant menu."
                )
        )).when(client).search("Pasta Carbonara", "Berlin");

        TavilyRestaurantSearchResponse result = underTest.search("Pasta Carbonara", "Berlin");

        assertEquals("ok", result.getStatus());
        assertEquals(1, result.getResults().size());
    }

    // === Must match: distinctive title/word ===

    @Test
    @DisplayName("search accepts result when exact normalized recipe title appears in restaurant context")
    void search_accepts_exact_title_in_restaurant_context() {
        doReturn(List.of(
                new TavilyRestaurantResult(
                        "Il Ristorante",
                        "https://ilristorante.de",
                        "Best Pasta Carbonara in Berlin. Authentic Italian restaurant."
                )
        )).when(client).search("Pasta Carbonara", "Berlin");

        TavilyRestaurantSearchResponse result = underTest.search("Pasta Carbonara", "Berlin");

        assertEquals(1, result.getResults().size());
        assertEquals("Il Ristorante", result.getResults().getFirst().getName());
    }

    @Test
    @DisplayName("search accepts result when distinctive word 'Carbonara' appears as menu item in restaurant context")
    void search_accepts_distinctive_word_carbonara_in_restaurant_context() {
        doReturn(List.of(
                new TavilyRestaurantResult(
                        "Il Ristorante Berlin",
                        "https://ilristorante.de",
                        "Our menu features classic Carbonara. Restaurant in Berlin Mitte."
                )
        )).when(client).search("Pasta Carbonara", "Berlin");

        TavilyRestaurantSearchResponse result = underTest.search("Pasta Carbonara", "Berlin");

        assertEquals(1, result.getResults().size());
    }

    @Test
    @DisplayName("search accepts result when distinctive word 'Kimbap' appears in restaurant context")
    void search_accepts_distinctive_word_kimbap_in_restaurant_context() {
        doReturn(List.of(
                new TavilyRestaurantResult(
                        "Seoul Kitchen Berlin",
                        "https://seoul-kitchen.de",
                        "We serve Kimbap and other Korean dishes. Restaurant in Berlin."
                )
        )).when(client).search("Kimbap – koreanisches Sushi", "Berlin");

        TavilyRestaurantSearchResponse result = underTest.search("Kimbap – koreanisches Sushi", "Berlin");

        assertEquals(1, result.getResults().size());
    }

    // === Must filter: generic single words ===

    @Test
    @DisplayName("search filters result where only generic word 'sushi' matches for 'Sushi Bowl' recipe")
    void search_filters_generic_sushi_alone_for_sushi_bowl() {
        doReturn(List.of(
                new TavilyRestaurantResult(
                        "Sushi House Berlin",
                        "https://sushi-house.de",
                        "Authentic sushi restaurant in Berlin."
                )
        )).when(client).search("Sushi Bowl", "Berlin");

        TavilyRestaurantSearchResponse result = underTest.search("Sushi Bowl", "Berlin");

        assertEquals("no_results", result.getStatus());
        assertTrue(result.getResults().isEmpty());
    }

    @Test
    @DisplayName("search filters result where only generic word 'pasta' matches for 'Pasta Carbonara' recipe")
    void search_filters_generic_pasta_alone_for_pasta_carbonara() {
        doReturn(List.of(
                new TavilyRestaurantResult(
                        "Italian Restaurant Berlin",
                        "https://italian-berlin.de",
                        "Best pasta dishes in our Italian restaurant."
                )
        )).when(client).search("Pasta Carbonara", "Berlin");

        TavilyRestaurantSearchResponse result = underTest.search("Pasta Carbonara", "Berlin");

        assertEquals("no_results", result.getStatus());
        assertTrue(result.getResults().isEmpty());
    }

    @Test
    @DisplayName("search filters result where only generic word 'sushi' matches for 'Kimbap' recipe")
    void search_filters_generic_sushi_alone_for_kimbap_recipe() {
        doReturn(List.of(
                new TavilyRestaurantResult(
                        "Sushi Bar Berlin",
                        "https://sushi-bar.de",
                        "Fresh sushi restaurant in Berlin."
                )
        )).when(client).search("Kimbap – koreanisches Sushi", "Berlin");

        TavilyRestaurantSearchResponse result = underTest.search("Kimbap – koreanisches Sushi", "Berlin");

        assertEquals("no_results", result.getStatus());
        assertTrue(result.getResults().isEmpty());
    }

    @Test
    @DisplayName("search filters result that has no restaurant context (e.g. a recipe blog)")
    void search_filters_result_without_restaurant_context() {
        doReturn(List.of(
                new TavilyRestaurantResult(
                        "Pasta Carbonara Recipe",
                        "https://recipe-blog.de",
                        "How to make Pasta Carbonara at home. Classic Italian pasta."
                )
        )).when(client).search("Pasta Carbonara", "Berlin");

        TavilyRestaurantSearchResponse result = underTest.search("Pasta Carbonara", "Berlin");

        assertEquals("no_results", result.getStatus());
        assertTrue(result.getResults().isEmpty());
    }

    // === Helper method correctness ===

    @Test
    @DisplayName("search sets distanceMeters and coordinates to null for Tavily results")
    void search_sets_null_distance_and_coordinates() {
        doReturn(List.of(
                new TavilyRestaurantResult(
                        "Pasta Palace Berlin",
                        "https://pasta-palace.de",
                        "Pasta Carbonara on our menu. Restaurant in Berlin."
                )
        )).when(client).search("Pasta Carbonara", "Berlin");

        TavilyRestaurantSearchResponse result = underTest.search("Pasta Carbonara", "Berlin");

        RestaurantResponse restaurant = result.getResults().getFirst();
        assertNull(restaurant.getDistanceMeters());
        assertNull(restaurant.getLatitude());
        assertNull(restaurant.getLongitude());
    }

    @Test
    @DisplayName("search generates a Google Maps search URL containing the restaurant name and location")
    void search_generates_google_maps_url() {
        doReturn(List.of(
                new TavilyRestaurantResult(
                        "Pasta Palace Berlin",
                        "https://pasta-palace.de",
                        "Best Carbonara in Berlin. Restaurant menu."
                )
        )).when(client).search("Pasta Carbonara", "Berlin");

        TavilyRestaurantSearchResponse result = underTest.search("Pasta Carbonara", "Berlin");

        String url = result.getResults().getFirst().getGoogleMapsUrl();
        assertTrue(url.startsWith("https://www.google.com/maps/search/?api=1&query="));
        assertTrue(url.contains("Berlin"));
    }

    @Test
    @DisplayName("search extracts restaurant name from title before ' - ' separator")
    void search_extracts_name_before_dash_separator() {
        doReturn(List.of(
                new TavilyRestaurantResult(
                        "Kebab King - Döner und mehr in Berlin",
                        "https://kebab-king.de",
                        "Best Döner Kebab restaurant in Berlin."
                )
        )).when(client).search("Döner Kebab", "Berlin");

        TavilyRestaurantSearchResponse result = underTest.search("Döner Kebab", "Berlin");

        assertEquals("Kebab King", result.getResults().getFirst().getName());
    }
}
