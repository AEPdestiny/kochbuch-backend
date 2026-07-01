package de.htwberlin.webtech.restaurant;

import de.htwberlin.webtech.restaurant.client.GeoapifyClient;
import de.htwberlin.webtech.restaurant.client.GeoapifyClientException;
import de.htwberlin.webtech.restaurant.client.TavilyRestaurantSearchClient;
import de.htwberlin.webtech.restaurant.client.TavilyRestaurantSearchClient.TavilyRestaurantResult;
import de.htwberlin.webtech.restaurant.client.dto.GeoapifyFeature;
import de.htwberlin.webtech.restaurant.client.dto.GeoapifyGeometry;
import de.htwberlin.webtech.restaurant.client.dto.GeoapifyProperties;
import de.htwberlin.webtech.restaurant.client.dto.GeoapifyResponse;
import de.htwberlin.webtech.restaurant.dto.RestaurantResponse;
import de.htwberlin.webtech.restaurant.dto.TavilyRestaurantSearchResponse;
import de.htwberlin.webtech.restaurant.service.TavilyRestaurantSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class TavilyRestaurantSearchServiceTest {

    private final TavilyRestaurantSearchClient client = mock(TavilyRestaurantSearchClient.class);
    private final GeoapifyClient geoapifyClient = mock(GeoapifyClient.class);
    private final TavilyRestaurantSearchService underTest = new TavilyRestaurantSearchService(client, geoapifyClient);

    @BeforeEach
    void setUp() {
        doReturn(true).when(client).isConfigured();
    }

    // === Status tests ===

    @Test
    @DisplayName("search returns unavailable status when Tavily API key is not configured")
    void search_returns_unavailable_when_not_configured() {
        doReturn(false).when(client).isConfigured();

        TavilyRestaurantSearchResponse result = underTest.search("Pasta Carbonara", "Berlin", null, null);

        assertEquals("unavailable", result.getStatus());
        assertTrue(result.getResults().isEmpty());
    }

    @Test
    @DisplayName("search returns no_results status when all Tavily results are filtered out")
    void search_returns_no_results_when_all_filtered() {
        doReturn(List.of(
                new TavilyRestaurantResult("Hotel Mitte Berlin", "https://hotel.de", "Luxury hotel with spa.")
        )).when(client).search("Pasta Carbonara", "Berlin");

        TavilyRestaurantSearchResponse result = underTest.search("Pasta Carbonara", "Berlin", null, null);

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

        TavilyRestaurantSearchResponse result = underTest.search("Pasta Carbonara", "Berlin", null, null);

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

        TavilyRestaurantSearchResponse result = underTest.search("Pasta Carbonara", "Berlin", null, null);

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

        TavilyRestaurantSearchResponse result = underTest.search("Pasta Carbonara", "Berlin", null, null);

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

        TavilyRestaurantSearchResponse result = underTest.search("Kimbap – koreanisches Sushi", "Berlin", null, null);

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

        TavilyRestaurantSearchResponse result = underTest.search("Sushi Bowl", "Berlin", null, null);

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

        TavilyRestaurantSearchResponse result = underTest.search("Pasta Carbonara", "Berlin", null, null);

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

        TavilyRestaurantSearchResponse result = underTest.search("Kimbap – koreanisches Sushi", "Berlin", null, null);

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

        TavilyRestaurantSearchResponse result = underTest.search("Pasta Carbonara", "Berlin", null, null);

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

        TavilyRestaurantSearchResponse result = underTest.search("Pasta Carbonara", "Berlin", null, null);

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

        TavilyRestaurantSearchResponse result = underTest.search("Pasta Carbonara", "Berlin", null, null);

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

        TavilyRestaurantSearchResponse result = underTest.search("Döner Kebab", "Berlin", null, null);

        assertEquals("Kebab King", result.getResults().getFirst().getName());
    }

    // === Restaurant-name extraction: new tests ===

    @Test
    @DisplayName("filters result when Tavily title is the recipe dish name and URL is an aggregator")
    void filters_when_title_is_dish_name_and_url_is_aggregator() {
        doReturn(List.of(
                new TavilyRestaurantResult(
                        "Miesmuscheln in Beurre Blanc",
                        "https://instagram.com/post/abc123",
                        "Frische Miesmuscheln in Beurre Blanc. Unser Restaurant. Speisekarte online."
                )
        )).when(client).search("Miesmuscheln in Beurre Blanc", "München");

        TavilyRestaurantSearchResponse result = underTest.search("Miesmuscheln in Beurre Blanc", "München", null, null);

        assertEquals("no_results", result.getStatus());
        assertTrue(result.getResults().isEmpty());
    }

    @Test
    @DisplayName("filters result when Tavily title contains hashtags (social media snippet)")
    void filters_when_title_contains_hashtag() {
        doReturn(List.of(
                new TavilyRestaurantResult(
                        "von unserer neuen Sommerspeisekarte 🌞 #ammersee ...",
                        "https://instagram.com/post/xyz",
                        "Miesmuscheln in Beurre Blanc auf der Karte. Speisekarte. Restaurant am See."
                )
        )).when(client).search("Miesmuscheln in Beurre Blanc", "München");

        TavilyRestaurantSearchResponse result = underTest.search("Miesmuscheln in Beurre Blanc", "München", null, null);

        assertEquals("no_results", result.getStatus());
        assertTrue(result.getResults().isEmpty());
    }

    @Test
    @DisplayName("filters result when Tavily title is the generic page title 'Speisekarte'")
    void filters_when_title_is_generic_speisekarte() {
        doReturn(List.of(
                new TavilyRestaurantResult(
                        "Speisekarte",
                        "https://tripadvisor.de/restaurant/abc",
                        "Pasta Carbonara auf der Speisekarte. Restaurant in Berlin."
                )
        )).when(client).search("Pasta Carbonara", "Berlin");

        TavilyRestaurantSearchResponse result = underTest.search("Pasta Carbonara", "Berlin", null, null);

        assertEquals("no_results", result.getStatus());
        assertTrue(result.getResults().isEmpty());
    }

    @Test
    @DisplayName("filters result when Tavily title is the generic page title 'Restaurant menu'")
    void filters_when_title_is_restaurant_menu() {
        doReturn(List.of(
                new TavilyRestaurantResult(
                        "Restaurant menu",
                        "https://tripadvisor.com/restaurant/456",
                        "Pasta Carbonara in our restaurant menu. Dining in Berlin."
                )
        )).when(client).search("Pasta Carbonara", "Berlin");

        TavilyRestaurantSearchResponse result = underTest.search("Pasta Carbonara", "Berlin", null, null);

        assertEquals("no_results", result.getStatus());
        assertTrue(result.getResults().isEmpty());
    }

    @Test
    @DisplayName("extracts restaurant name before ' - ' and discards generic page suffix like 'Speisekarte'")
    void extracts_restaurant_name_before_dash_discards_generic_suffix() {
        doReturn(List.of(
                new TavilyRestaurantResult(
                        "ARME RITTER BERLIN - Speisekarte",
                        "https://arme-ritter.de/karte",
                        "Pasta Carbonara auf der Karte. Restaurant in Berlin Mitte."
                )
        )).when(client).search("Pasta Carbonara", "Berlin");

        TavilyRestaurantSearchResponse result = underTest.search("Pasta Carbonara", "Berlin", null, null);

        assertEquals(1, result.getResults().size());
        assertEquals("ARME RITTER BERLIN", result.getResults().getFirst().getName());
    }

    @Test
    @DisplayName("extracts first segment as restaurant name from multi-part title separated by ' | '")
    void extracts_restaurant_name_from_pipe_separated_title() {
        doReturn(List.of(
                new TavilyRestaurantResult(
                        "Trattoria Mario | Pasta Carbonara | Menu",
                        "https://trattoria-mario.de",
                        "Authentic Carbonara. Italian restaurant in Berlin."
                )
        )).when(client).search("Pasta Carbonara", "Berlin");

        TavilyRestaurantSearchResponse result = underTest.search("Pasta Carbonara", "Berlin", null, null);

        assertEquals(1, result.getResults().size());
        assertEquals("Trattoria Mario", result.getResults().getFirst().getName());
    }

    @Test
    @DisplayName("derives restaurant name from URL domain when title is the dish name")
    void derives_name_from_url_domain_when_title_is_dish_name() {
        doReturn(List.of(
                new TavilyRestaurantResult(
                        "Pasta Carbonara",
                        "https://trattoria-mario.de/menu/carbonara",
                        "Authentic Carbonara. Italian restaurant and menu."
                )
        )).when(client).search("Pasta Carbonara", "Berlin");

        TavilyRestaurantSearchResponse result = underTest.search("Pasta Carbonara", "Berlin", null, null);

        assertEquals(1, result.getResults().size());
        assertEquals("Trattoria Mario", result.getResults().getFirst().getName());
    }

    @Test
    @DisplayName("filters Instagram result when title starts with a sentence starter word")
    void filters_instagram_result_when_title_is_sentence_snippet() {
        doReturn(List.of(
                new TavilyRestaurantResult(
                        "von unserer neuen Sommerspeisekarte",
                        "https://instagram.com/restaurant_xyz",
                        "Pasta Carbonara jetzt auf unserer Speisekarte. Restaurant in Berlin."
                )
        )).when(client).search("Pasta Carbonara", "Berlin");

        TavilyRestaurantSearchResponse result = underTest.search("Pasta Carbonara", "Berlin", null, null);

        assertEquals("no_results", result.getStatus());
        assertTrue(result.getResults().isEmpty());
    }

    // === Content-based restaurant name extraction ===

    @Test
    @DisplayName("extracts 'Restaurant Fischer Ammersee' from content when title is a snippet with hashtag")
    void extracts_restaurant_name_from_content_when_title_is_snippet() {
        doReturn(List.of(
                new TavilyRestaurantResult(
                        "von unserer neuen Sommerspeisekarte #sommer ...",
                        "https://instagram.com/fischerammersee",
                        "Restaurant Fischer Ammersee serviert Miesmuscheln in Beurre Blanc. Reservierungen online."
                )
        )).when(client).search("Miesmuscheln in Beurre Blanc", "München");

        TavilyRestaurantSearchResponse result = underTest.search("Miesmuscheln in Beurre Blanc", "München", null, null);

        assertEquals(1, result.getResults().size());
        assertEquals("Restaurant Fischer Ammersee", result.getResults().getFirst().getName());
    }

    @Test
    @DisplayName("extracts 'Trattoria Mario' from content when title is the dish name and URL is aggregator")
    void extracts_trattoria_from_content_when_title_is_dish_name_and_url_is_aggregator() {
        doReturn(List.of(
                new TavilyRestaurantResult(
                        "Pasta Carbonara",
                        "https://instagram.com/trattoria_mario",
                        "Trattoria Mario serviert Pasta Carbonara. Restaurant in Berlin."
                )
        )).when(client).search("Pasta Carbonara", "Berlin");

        TavilyRestaurantSearchResponse result = underTest.search("Pasta Carbonara", "Berlin", null, null);

        assertEquals(1, result.getResults().size());
        assertEquals("Trattoria Mario", result.getResults().getFirst().getName());
    }

    @Test
    @DisplayName("extracts 'Café Am See' from content including uppercase preposition")
    void extracts_cafe_name_with_preposition_from_content() {
        doReturn(List.of(
                new TavilyRestaurantResult(
                        "Miesmuscheln in Beurre Blanc - Speisekarte",
                        "https://instagram.com/cafe_am_see",
                        "Café Am See bietet Miesmuscheln in Beurre Blanc täglich frisch. Menu."
                )
        )).when(client).search("Miesmuscheln in Beurre Blanc", "München");

        TavilyRestaurantSearchResponse result = underTest.search("Miesmuscheln in Beurre Blanc", "München", null, null);

        assertEquals(1, result.getResults().size());
        assertEquals("Café Am See", result.getResults().getFirst().getName());
    }

    @Test
    @DisplayName("extracts 'Gasthaus Zur Post' from content when URL is aggregator")
    void extracts_gasthaus_name_from_content() {
        doReturn(List.of(
                new TavilyRestaurantResult(
                        "Wiener Schnitzel - Speisekarte",
                        "https://instagram.com/gasthaus_zur_post",
                        "Gasthaus Zur Post serviert Wiener Schnitzel. Öffnungszeiten täglich ab 11 Uhr."
                )
        )).when(client).search("Wiener Schnitzel", "Wien");

        TavilyRestaurantSearchResponse result = underTest.search("Wiener Schnitzel", "Wien", null, null);

        assertEquals(1, result.getResults().size());
        assertEquals("Gasthaus Zur Post", result.getResults().getFirst().getName());
    }

    @Test
    @DisplayName("filters result when content has only hashtags and no restaurant name pattern")
    void filters_when_content_has_only_hashtags_and_no_restaurant_name() {
        doReturn(List.of(
                new TavilyRestaurantResult(
                        "von unserer neuen Sommerspeisekarte",
                        "https://instagram.com/post/abc",
                        "#ammersee #mittagessen Miesmuscheln in Beurre Blanc frisch zubereitet. Speisekarte."
                )
        )).when(client).search("Miesmuscheln in Beurre Blanc", "München");

        TavilyRestaurantSearchResponse result = underTest.search("Miesmuscheln in Beurre Blanc", "München", null, null);

        assertEquals("no_results", result.getStatus());
        assertTrue(result.getResults().isEmpty());
    }

    @Test
    @DisplayName("filters result when content has only dish name and Speisekarte without a restaurant name")
    void filters_when_content_has_dish_name_and_speisekarte_but_no_restaurant_name() {
        doReturn(List.of(
                new TavilyRestaurantResult(
                        "von unserer neuen Sommerspeisekarte",
                        "https://instagram.com/post/xyz",
                        "Miesmuscheln in Beurre Blanc auf unserer Speisekarte. Täglich frisch zubereitet."
                )
        )).when(client).search("Miesmuscheln in Beurre Blanc", "München");

        TavilyRestaurantSearchResponse result = underTest.search("Miesmuscheln in Beurre Blanc", "München", null, null);

        assertEquals("no_results", result.getStatus());
        assertTrue(result.getResults().isEmpty());
    }

    @Test
    @DisplayName("filters result when content says 'restaurant menu' without a concrete restaurant name")
    void filters_when_content_says_restaurant_menu_without_concrete_name() {
        doReturn(List.of(
                new TavilyRestaurantResult(
                        "Speisekarte",
                        "https://tripadvisor.com/restaurant/456",
                        "Pasta Carbonara in our restaurant menu. Great dining in Berlin."
                )
        )).when(client).search("Pasta Carbonara", "Berlin");

        TavilyRestaurantSearchResponse result = underTest.search("Pasta Carbonara", "Berlin", null, null);

        assertEquals("no_results", result.getStatus());
        assertTrue(result.getResults().isEmpty());
    }

    @Test
    @DisplayName("filters result when content has similar but non-matching dish (plausibility check)")
    void filters_when_content_has_similar_but_not_matching_dish() {
        doReturn(List.of(
                new TavilyRestaurantResult(
                        "Muscheln in Weißweinsauce",
                        "https://restaurant-am-see.de/menu",
                        "Restaurant Fischer Ammersee serviert Muscheln in Weißweinsauce. Menu."
                )
        )).when(client).search("Miesmuscheln in Beurre Blanc", "München");

        TavilyRestaurantSearchResponse result = underTest.search("Miesmuscheln in Beurre Blanc", "München", null, null);

        assertEquals("no_results", result.getStatus());
        assertTrue(result.getResults().isEmpty());
    }

    // === Geoapify enrichment ===

    @Test
    @DisplayName("enriches result with address and distance when user coordinates are provided and Geoapify returns a match")
    void enriches_result_with_geoapify_when_user_coords_present() {
        doReturn(List.of(
                new TavilyRestaurantResult(
                        "Pasta Palace Berlin",
                        "https://pasta-palace.de",
                        "Best Carbonara in Berlin. Restaurant menu."
                )
        )).when(client).search("Pasta Carbonara", "Berlin");
        doReturn(geoapifyResponse(52.5201, 13.4052, "Pasta Palace, Musterstraße 1, Berlin"))
                .when(geoapifyClient).searchRestaurants(anyString(), anyDouble(), anyDouble(), anyInt(), anyInt());

        TavilyRestaurantSearchResponse result = underTest.search("Pasta Carbonara", "Berlin", 52.52, 13.405);

        RestaurantResponse restaurant = result.getResults().getFirst();
        assertEquals("Pasta Palace Berlin", restaurant.getName());
        assertEquals("Pasta Palace, Musterstraße 1, Berlin", restaurant.getAddress());
        assertNotNull(restaurant.getDistanceMeters());
        assertTrue(restaurant.getDistanceMeters() > 0);
        assertNotNull(restaurant.getLatitude());
        assertNotNull(restaurant.getLongitude());
    }

    @Test
    @DisplayName("returns result without Geoapify data when user coordinates are absent")
    void returns_result_without_geoapify_when_no_user_coords() {
        doReturn(List.of(
                new TavilyRestaurantResult(
                        "Pasta Palace Berlin",
                        "https://pasta-palace.de",
                        "Pasta Carbonara on our menu. Restaurant in Berlin."
                )
        )).when(client).search("Pasta Carbonara", "Berlin");

        TavilyRestaurantSearchResponse result = underTest.search("Pasta Carbonara", "Berlin", null, null);

        RestaurantResponse restaurant = result.getResults().getFirst();
        assertNull(restaurant.getDistanceMeters());
        assertNull(restaurant.getLatitude());
        assertNull(restaurant.getLongitude());
    }

    @Test
    @DisplayName("keeps result when Geoapify throws GeoapifyClientException (API key not configured)")
    void keeps_result_when_geoapify_not_configured() {
        doReturn(List.of(
                new TavilyRestaurantResult(
                        "Pasta Palace Berlin",
                        "https://pasta-palace.de",
                        "Best Carbonara in Berlin. Restaurant menu."
                )
        )).when(client).search("Pasta Carbonara", "Berlin");
        doThrow(new GeoapifyClientException("Geoapify API key not configured"))
                .when(geoapifyClient).searchRestaurants(any(), anyDouble(), anyDouble(), anyInt(), anyInt());

        TavilyRestaurantSearchResponse result = underTest.search("Pasta Carbonara", "Berlin", 52.52, 13.405);

        assertEquals("ok", result.getStatus());
        assertEquals(1, result.getResults().size());
        assertNull(result.getResults().getFirst().getDistanceMeters());
    }

    @Test
    @DisplayName("generates route Maps URL when user coords and restaurant coords are both available")
    void generates_route_maps_url_when_all_coords_available() {
        doReturn(List.of(
                new TavilyRestaurantResult(
                        "Pasta Palace Berlin",
                        "https://pasta-palace.de",
                        "Best Carbonara in Berlin. Restaurant menu."
                )
        )).when(client).search("Pasta Carbonara", "Berlin");
        doReturn(geoapifyResponse(52.5201, 13.4052, "Pasta Palace, Berlin"))
                .when(geoapifyClient).searchRestaurants(anyString(), anyDouble(), anyDouble(), anyInt(), anyInt());

        TavilyRestaurantSearchResponse result = underTest.search("Pasta Carbonara", "Berlin", 52.52, 13.405);

        String url = result.getResults().getFirst().getGoogleMapsUrl();
        assertTrue(url.startsWith("https://www.google.com/maps/dir/?api=1&origin="));
        assertTrue(url.contains("destination="));
    }

    @Test
    @DisplayName("generates search Maps URL when no coordinates are available")
    void generates_search_maps_url_when_no_coords() {
        doReturn(List.of(
                new TavilyRestaurantResult(
                        "Pasta Palace Berlin",
                        "https://pasta-palace.de",
                        "Best Carbonara in Berlin. Restaurant menu."
                )
        )).when(client).search("Pasta Carbonara", "Berlin");

        TavilyRestaurantSearchResponse result = underTest.search("Pasta Carbonara", "Berlin", null, null);

        String url = result.getResults().getFirst().getGoogleMapsUrl();
        assertTrue(url.startsWith("https://www.google.com/maps/search/?api=1&query="));
        assertTrue(url.contains("Berlin"));
    }

    // === Helper ===

    private GeoapifyResponse geoapifyResponse(double lat, double lon, String formatted) {
        GeoapifyGeometry geometry = new GeoapifyGeometry();
        geometry.setCoordinates(List.of(lon, lat));

        GeoapifyProperties properties = new GeoapifyProperties();
        properties.setFormatted(formatted);

        GeoapifyFeature feature = new GeoapifyFeature();
        feature.setGeometry(geometry);
        feature.setProperties(properties);

        GeoapifyResponse response = new GeoapifyResponse();
        response.setFeatures(List.of(feature));
        return response;
    }
}
