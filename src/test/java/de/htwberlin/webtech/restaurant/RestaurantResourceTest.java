package de.htwberlin.webtech.restaurant;

import de.htwberlin.webtech.restaurant.dto.RestaurantResponse;
import de.htwberlin.webtech.restaurant.dto.RestaurantSearchRequest;
import de.htwberlin.webtech.restaurant.service.RestaurantService;
import de.htwberlin.webtech.security.UserContext;
import de.htwberlin.webtech.shared.exception.UnauthorizedException;
import de.htwberlin.webtech.user.entity.AppUser;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@QuarkusTest
class RestaurantResourceTest {

    private RestaurantService restaurantService;
    private UserContext userContext;

    @BeforeEach
    void setUp() {
        restaurantService = mock(RestaurantService.class);
        userContext = mock(UserContext.class);
        QuarkusMock.installMockForType(restaurantService, RestaurantService.class);
        QuarkusMock.installMockForType(userContext, UserContext.class);
    }

    @Test
    void search_should_return_unauthorized_without_token() {
        doThrow(new UnauthorizedException("Missing or invalid Bearer token."))
                .when(userContext).requireUser(null);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "query": "Pizza",
                          "latitude": 52.52,
                          "longitude": 13.405
                        }
                        """)
                .when().post("/restaurants/search")
                .then()
                .statusCode(401)
                .body("message", equalTo("Missing or invalid Bearer token."))
                .body("path", equalTo("/restaurants/search"));
    }

    @Test
    void search_should_return_bad_request_for_blank_query() {
        AppUser currentUser = user(1L);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body("""
                        {
                          "query": "",
                          "latitude": 52.52,
                          "longitude": 13.405
                        }
                        """)
                .when().post("/restaurants/search")
                .then()
                .statusCode(400)
                .body("message", equalTo("Validation failed: query must not be blank"));

        verify(restaurantService, never()).search(any());
    }

    @Test
    void search_should_return_bad_request_for_invalid_latitude() {
        AppUser currentUser = user(1L);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body("""
                        {
                          "query": "Pizza",
                          "latitude": 120,
                          "longitude": 13.405
                        }
                        """)
                .when().post("/restaurants/search")
                .then()
                .statusCode(400)
                .body("message", equalTo("Validation failed: latitude must be less than or equal to 90"));

        verify(restaurantService, never()).search(any());
    }

    @Test
    void search_should_return_bad_request_for_invalid_longitude() {
        AppUser currentUser = user(1L);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body("""
                        {
                          "query": "Pizza",
                          "latitude": 52.52,
                          "longitude": 220
                        }
                        """)
                .when().post("/restaurants/search")
                .then()
                .statusCode(400)
                .body("message", equalTo("Validation failed: longitude must be less than or equal to 180"));

        verify(restaurantService, never()).search(any());
    }

    @Test
    void search_should_return_restaurants_for_valid_request() {
        AppUser currentUser = user(1L);
        RestaurantResponse restaurant = restaurant();
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doReturn(List.of(restaurant)).when(restaurantService).search(any(RestaurantSearchRequest.class));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body("""
                        {
                          "query": "Pizza",
                          "latitude": 52.52,
                          "longitude": 13.405
                        }
                        """)
                .when().post("/restaurants/search")
                .then()
                .statusCode(200)
                .body("", hasSize(1))
                .body("[0].name", equalTo("Pasta Place"))
                .body("[0].address", equalTo("Pasta Street 1, Berlin"))
                .body("[0].distanceMeters", equalTo(850))
                .body("[0].googleMapsUrl", equalTo("https://www.google.com/maps/search/?api=1&query=52.5201,13.4052"))
                .body("[0].latitude", equalTo(52.5201f))
                .body("[0].longitude", equalTo(13.4052f));
    }

    private RestaurantResponse restaurant() {
        RestaurantResponse restaurant = new RestaurantResponse();
        restaurant.setName("Pasta Place");
        restaurant.setAddress("Pasta Street 1, Berlin");
        restaurant.setDistanceMeters(850);
        restaurant.setGoogleMapsUrl("https://www.google.com/maps/search/?api=1&query=52.5201,13.4052");
        restaurant.setLatitude(52.5201);
        restaurant.setLongitude(13.4052);
        return restaurant;
    }

    private AppUser user(Long id) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUsername("user-" + id);
        user.setEmail("user-" + id + "@example.com");
        user.setPasswordHash("hash");
        return user;
    }
}
