package de.htwberlin.webtech.profile;

import de.htwberlin.webtech.profile.dto.UserPreferencesRequest;
import de.htwberlin.webtech.profile.entity.UserPreferences;
import de.htwberlin.webtech.profile.service.UserPreferencesService;
import de.htwberlin.webtech.security.UserContext;
import de.htwberlin.webtech.shared.exception.UnauthorizedException;
import de.htwberlin.webtech.user.entity.AppUser;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

@QuarkusTest
class UserPreferencesResourceTest {

    private UserPreferencesService service;
    private UserContext userContext;

    @BeforeEach
    void setUp() {
        service = mock(UserPreferencesService.class);
        userContext = mock(UserContext.class);
        QuarkusMock.installMockForType(service, UserPreferencesService.class);
        QuarkusMock.installMockForType(userContext, UserContext.class);
    }

    @Test
    void getPreferences_should_return_unauthorized_without_token() {
        doThrow(new UnauthorizedException("Missing or invalid Bearer token."))
                .when(userContext).requireUser(null);

        given()
                .when().get("/profile/preferences")
                .then()
                .statusCode(401)
                .body("status", equalTo(401))
                .body("path", equalTo("/profile/preferences"));
    }

    @Test
    void getPreferences_should_return_current_users_preferences() {
        AppUser currentUser = user(1L);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doReturn(preferences(currentUser)).when(service).getOrCreate(currentUser);

        given()
                .header("Authorization", "Bearer valid-token")
                .when().get("/profile/preferences")
                .then()
                .statusCode(200)
                .body("likes", hasItems("pasta"))
                .body("allergies", hasItems("nuts"))
                .body("vegan", equalTo(true))
                .body("maxPrepTimeMinutes", equalTo(30))
                .body("dailyCalorieTarget", equalTo(2600));
    }

    @Test
    void putPreferences_should_update_current_users_preferences() {
        AppUser currentUser = user(1L);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doReturn(preferences(currentUser)).when(service).update(any(UserPreferencesRequest.class), eq(currentUser));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body("""
                        {
                          "likes": ["pasta"],
                          "dislikes": ["mushrooms"],
                          "allergies": ["nuts"],
                          "vegan": true,
                          "highProtein": true,
                          "maxPrepTimeMinutes": 30,
                          "dailyCalorieTarget": 2600
                        }
                        """)
                .when().put("/profile/preferences")
                .then()
                .statusCode(200)
                .body("likes", hasItems("pasta"))
                .body("highProtein", equalTo(true))
                .body("dailyCalorieTarget", equalTo(2600));
    }

    @Test
    void putPreferences_should_reject_invalid_numbers() {
        AppUser currentUser = user(1L);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body("""
                        {
                          "maxPrepTimeMinutes": 0,
                          "calorieGoal": -1
                        }
                        """)
                .when().put("/profile/preferences")
                .then()
                .statusCode(400);
    }

    private UserPreferences preferences(AppUser owner) {
        UserPreferences preferences = new UserPreferences();
        preferences.setOwner(owner);
        preferences.setLikes(new LinkedHashSet<>(Set.of("pasta")));
        preferences.setDislikes(new LinkedHashSet<>(Set.of("mushrooms")));
        preferences.setAllergies(new LinkedHashSet<>(Set.of("nuts")));
        preferences.setVegan(true);
        preferences.setHighProtein(true);
        preferences.setMaxPrepTimeMinutes(30);
        preferences.setDailyCalorieTarget(2600);
        preferences.setCalorieGoal(2600);
        return preferences;
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
