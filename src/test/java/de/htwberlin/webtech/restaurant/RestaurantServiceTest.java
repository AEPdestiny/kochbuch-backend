package de.htwberlin.webtech.restaurant;

import de.htwberlin.webtech.restaurant.client.GeoapifyClient;
import de.htwberlin.webtech.restaurant.client.GeoapifyClientException;
import de.htwberlin.webtech.restaurant.client.dto.GeoapifyFeature;
import de.htwberlin.webtech.restaurant.client.dto.GeoapifyGeometry;
import de.htwberlin.webtech.restaurant.client.dto.GeoapifyProperties;
import de.htwberlin.webtech.restaurant.client.dto.GeoapifyResponse;
import de.htwberlin.webtech.restaurant.dto.RestaurantResponse;
import de.htwberlin.webtech.restaurant.dto.RestaurantSearchRequest;
import de.htwberlin.webtech.restaurant.service.RestaurantService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class RestaurantServiceTest {

    private final GeoapifyClient geoapifyClient = mock(GeoapifyClient.class);
    private final RestaurantService underTest = new RestaurantService(geoapifyClient);

    @Test
    void search_should_map_geoapify_restaurants() {
        doReturn(response(feature("Pizza Place", "Pizza Street 1, Berlin", 850, 13.4052, 52.5201)))
                .when(geoapifyClient).searchRestaurants("pizza", 52.52, 13.405, 5000, 5);

        List<RestaurantResponse> result = underTest.search(request(" Pizza ", 52.52, 13.405));

        verify(geoapifyClient).searchRestaurants("pizza", 52.52, 13.405, 5000, 5);
        assertEquals(1, result.size());
        RestaurantResponse restaurant = result.getFirst();
        assertEquals("Pizza Place", restaurant.getName());
        assertEquals("Pizza Street 1, Berlin", restaurant.getAddress());
        assertEquals(850, restaurant.getDistanceMeters());
        assertEquals(52.5201, restaurant.getLatitude());
        assertEquals(13.4052, restaurant.getLongitude());
        assertEquals("https://www.google.com/maps/search/?api=1&query=52.5201,13.4052", restaurant.getGoogleMapsUrl());
    }

    @Test
    void search_should_normalize_pizza_recipe_title_to_pizza_search_term() {
        doReturn(response(feature("Pizza Napoli", "Pizza Street 1", 300, 13.4052, 52.5201)))
                .when(geoapifyClient).searchRestaurants("pizza", 52.52, 13.405, 5000, 5);

        List<RestaurantResponse> result = underTest.search(request("Pizza Margherita", 52.52, 13.405));

        verify(geoapifyClient).searchRestaurants("pizza", 52.52, 13.405, 5000, 5);
        assertEquals("Pizza Napoli", result.getFirst().getName());
    }

    @Test
    void search_should_normalize_sushi_recipe_title_to_sushi_search_term() {
        doReturn(response(feature("Sushi House", "Sushi Street 1", 300, 13.4052, 52.5201)))
                .when(geoapifyClient).searchRestaurants("sushi", 52.52, 13.405, 5000, 5);

        List<RestaurantResponse> result = underTest.search(request("Sushi Bowl", 52.52, 13.405));

        verify(geoapifyClient).searchRestaurants("sushi", 52.52, 13.405, 5000, 5);
        assertEquals("Sushi House", result.getFirst().getName());
    }

    @Test
    void search_should_prefer_matching_restaurants() {
        doReturn(response(
                feature("Generic Restaurant", "Main Street 1", 100, 13.4052, 52.5201),
                feature("Pizza Napoli", "Pizza Street 1", 500, 13.4062, 52.5211)
        )).when(geoapifyClient).searchRestaurants("pizza", 52.52, 13.405, 5000, 5);

        List<RestaurantResponse> result = underTest.search(request("Pizza Margherita", 52.52, 13.405));

        assertEquals("Pizza Napoli", result.getFirst().getName());
        assertEquals("Generic Restaurant", result.get(1).getName());
    }

    @Test
    void search_should_fallback_to_general_restaurants_when_recipe_search_has_no_matching_results() {
        doReturn(response(feature("Generic Restaurant", "Main Street 1", 100, 13.4052, 52.5201)))
                .when(geoapifyClient).searchRestaurants("sushi", 52.52, 13.405, 5000, 5);
        doReturn(response(feature("Nearby Restaurant", "Nearby Street 1", 200, 13.4062, 52.5211)))
                .when(geoapifyClient).searchRestaurants("", 52.52, 13.405, 5000, 5);

        List<RestaurantResponse> result = underTest.search(request("Sushi Bowl", 52.52, 13.405));

        verify(geoapifyClient).searchRestaurants("sushi", 52.52, 13.405, 5000, 5);
        verify(geoapifyClient).searchRestaurants("", 52.52, 13.405, 5000, 5);
        assertEquals(1, result.size());
        assertEquals("Nearby Restaurant", result.getFirst().getName());
    }

    @Test
    void search_should_return_empty_list_for_geoapify_error() {
        doThrow(new GeoapifyClientException("Geoapify API key is not configured."))
                .when(geoapifyClient).searchRestaurants("pizza", 52.52, 13.405, 5000, 5);

        List<RestaurantResponse> result = underTest.search(request("Pizza", 52.52, 13.405));

        verify(geoapifyClient).searchRestaurants("pizza", 52.52, 13.405, 5000, 5);
        verifyNoMoreInteractions(geoapifyClient);
        assertTrue(result.isEmpty());
    }

    @Test
    void search_should_skip_results_without_coordinates() {
        GeoapifyFeature feature = feature("No Coordinates", "Unknown", 100, 13.4052, 52.5201);
        feature.setGeometry(new GeoapifyGeometry());
        doReturn(response(feature)).when(geoapifyClient).searchRestaurants("pizza", 52.52, 13.405, 5000, 5);
        doReturn(response()).when(geoapifyClient).searchRestaurants("", 52.52, 13.405, 5000, 5);

        List<RestaurantResponse> result = underTest.search(request("Pizza", 52.52, 13.405));

        assertTrue(result.isEmpty());
    }

    private RestaurantSearchRequest request(String query, double latitude, double longitude) {
        RestaurantSearchRequest request = new RestaurantSearchRequest();
        request.setQuery(query);
        request.setLatitude(latitude);
        request.setLongitude(longitude);
        return request;
    }

    private GeoapifyResponse response(GeoapifyFeature... features) {
        GeoapifyResponse response = new GeoapifyResponse();
        response.setFeatures(List.of(features));
        return response;
    }

    private GeoapifyFeature feature(String name, String address, int distance, double longitude, double latitude) {
        GeoapifyProperties properties = new GeoapifyProperties();
        properties.setName(name);
        properties.setFormatted(address);
        properties.setDistance(distance);
        properties.setCategories(List.of("catering.restaurant"));

        GeoapifyGeometry geometry = new GeoapifyGeometry();
        geometry.setCoordinates(List.of(longitude, latitude));

        GeoapifyFeature feature = new GeoapifyFeature();
        feature.setProperties(properties);
        feature.setGeometry(geometry);
        return feature;
    }
}
