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

import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class RestaurantService {

    private static final Logger LOG = Logger.getLogger(RestaurantService.class);
    private static final int SEARCH_RADIUS_METERS = 5000;
    private static final int SEARCH_LIMIT = 5;

    private final GeoapifyClient geoapifyClient;

    public RestaurantService(GeoapifyClient geoapifyClient) {
        this.geoapifyClient = geoapifyClient;
    }

    public List<RestaurantResponse> search(RestaurantSearchRequest request) {
        try {
            GeoapifyResponse response = geoapifyClient.searchRestaurants(
                    request.getQuery().trim(),
                    request.getLatitude(),
                    request.getLongitude(),
                    SEARCH_RADIUS_METERS,
                    SEARCH_LIMIT
            );
            return response.getFeatures().stream()
                    .map(this::toResponse)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (GeoapifyClientException exception) {
            LOG.warnf("Geoapify restaurant search failed: %s", exception.getMessage());
            return List.of();
        }
    }

    private RestaurantResponse toResponse(GeoapifyFeature feature) {
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
        return response;
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
}
