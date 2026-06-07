package de.htwberlin.webtech.restaurant.service;

import de.htwberlin.webtech.restaurant.client.GeoapifyClient;
import de.htwberlin.webtech.restaurant.client.GeoapifyClientException;
import de.htwberlin.webtech.restaurant.client.dto.GeoapifyFeature;
import de.htwberlin.webtech.restaurant.client.dto.GeoapifyGeometry;
import de.htwberlin.webtech.restaurant.client.dto.GeoapifyProperties;
import de.htwberlin.webtech.restaurant.client.dto.GeoapifyResponse;
import de.htwberlin.webtech.restaurant.dto.RestaurantSearchRequest;
import de.htwberlin.webtech.restaurant.dto.RestaurantResponse;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@ApplicationScoped
public class RestaurantService {

    private static final Logger LOG = Logger.getLogger(RestaurantService.class);
    private static final int SEARCH_RADIUS_METERS = 5000;
    private static final int SEARCH_LIMIT = 5;
    private static final Map<String, List<String>> FOOD_KEYWORDS = Map.ofEntries(
            Map.entry("pizza", List.of("pizza", "italian", "italienisch", "pizzeria")),
            Map.entry("sushi", List.of("sushi", "japanese", "japanisch")),
            Map.entry("pasta", List.of("pasta", "carbonara", "bolognese", "italian", "italienisch")),
            Map.entry("burger", List.of("burger", "hamburger")),
            Map.entry("kebab", List.of("döner", "doener", "doner", "kebab")),
            Map.entry("curry", List.of("curry", "indian", "indisch")),
            Map.entry("tacos", List.of("taco", "tacos", "mexican", "mexikanisch"))
    );

    private final GeoapifyClient geoapifyClient;

    public RestaurantService(GeoapifyClient geoapifyClient) {
        this.geoapifyClient = geoapifyClient;
    }

    public List<RestaurantResponse> search(RestaurantSearchRequest request) {
        String searchTerm = searchTerm(request.getQuery());
        try {
            List<RankedRestaurant> rankedRestaurants = fetchRanked(
                    searchTerm,
                    request.getLatitude(),
                    request.getLongitude()
            );

            boolean hasMatchingResult = rankedRestaurants.stream()
                    .anyMatch(restaurant -> restaurant.score() > 0);
            if (searchTerm.isBlank() || hasMatchingResult) {
                return rankedRestaurants.stream()
                        .sorted(Comparator.comparingInt(RankedRestaurant::score).reversed()
                                .thenComparing(restaurant -> restaurant.response().getDistanceMeters()))
                        .map(RankedRestaurant::response)
                        .toList();
            }

            return fetchRanked("", request.getLatitude(), request.getLongitude()).stream()
                    .map(RankedRestaurant::response)
                    .toList();
        } catch (GeoapifyClientException exception) {
            LOG.warnf("Geoapify restaurant search failed: %s", exception.getMessage());
            return List.of();
        }
    }

    private List<RankedRestaurant> fetchRanked(String searchTerm, double latitude, double longitude) {
        GeoapifyResponse response = geoapifyClient.searchRestaurants(
                searchTerm,
                latitude,
                longitude,
                SEARCH_RADIUS_METERS,
                SEARCH_LIMIT
        );
        return response.getFeatures().stream()
                .map(feature -> toRankedResponse(feature, searchTerm))
                .filter(Objects::nonNull)
                .toList();
    }

    private RankedRestaurant toRankedResponse(GeoapifyFeature feature, String searchTerm) {
        if (feature == null || feature.getGeometry() == null || feature.getProperties() == null) {
            return null;
        }

        Coordinates coordinates = coordinates(feature.getGeometry());
        if (coordinates == null) {
            return null;
        }

        GeoapifyProperties properties = feature.getProperties();
        RestaurantResponse response = new RestaurantResponse();
        response.setName(valueOrDefault(properties.getName(), "Restaurant"));
        response.setAddress(valueOrDefault(properties.getFormatted(), valueOrDefault(properties.getAddressLine1(), "")));
        response.setDistanceMeters(properties.getDistance() == null ? 0 : properties.getDistance());
        response.setLatitude(coordinates.latitude());
        response.setLongitude(coordinates.longitude());
        response.setGoogleMapsUrl(googleMapsUrl(coordinates.latitude(), coordinates.longitude()));
        return new RankedRestaurant(response, score(properties, searchTerm));
    }

    private String searchTerm(String query) {
        String normalized = normalize(query);
        return FOOD_KEYWORDS.entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(normalized::contains))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(normalized);
    }

    private int score(GeoapifyProperties properties, String searchTerm) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return 0;
        }
        List<String> aliases = new ArrayList<>();
        aliases.add(searchTerm);
        aliases.addAll(FOOD_KEYWORDS.getOrDefault(searchTerm, List.of()));
        String haystack = normalize(String.join(" ",
                valueOrDefault(properties.getName(), ""),
                valueOrDefault(properties.getFormatted(), ""),
                valueOrDefault(properties.getAddressLine1(), ""),
                String.join(" ", properties.getCategories())
        ));
        return aliases.stream().anyMatch(alias -> haystack.contains(normalize(alias))) ? 1 : 0;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private Coordinates coordinates(GeoapifyGeometry geometry) {
        List<Double> coordinates = geometry.getCoordinates();
        if (coordinates == null || coordinates.size() < 2 || coordinates.get(0) == null || coordinates.get(1) == null) {
            return null;
        }
        return new Coordinates(coordinates.get(1), coordinates.get(0));
    }

    private String googleMapsUrl(double latitude, double longitude) {
        return "https://www.google.com/maps/search/?api=1&query=" + latitude + "," + longitude;
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private record Coordinates(double latitude, double longitude) {
    }

    private record RankedRestaurant(RestaurantResponse response, int score) {
    }
}
