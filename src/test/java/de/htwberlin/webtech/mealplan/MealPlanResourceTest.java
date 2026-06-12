package de.htwberlin.webtech.mealplan;

import de.htwberlin.webtech.mealplan.dto.MealPlanEntryRequest;
import de.htwberlin.webtech.mealplan.entity.MealPlan;
import de.htwberlin.webtech.mealplan.entity.MealSlot;
import de.htwberlin.webtech.mealplan.exception.MealPlanEntryNotFoundException;
import de.htwberlin.webtech.mealplan.service.MealPlanService;
import de.htwberlin.webtech.recipe.entity.Recipe;
import de.htwberlin.webtech.recipe.exception.RecipeNotFoundException;
import de.htwberlin.webtech.security.UserContext;
import de.htwberlin.webtech.shared.exception.ForbiddenException;
import de.htwberlin.webtech.shared.exception.UnauthorizedException;
import de.htwberlin.webtech.user.entity.AppUser;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@QuarkusTest
class MealPlanResourceTest {

    private MealPlanService mealPlanService;
    private UserContext userContext;

    @BeforeEach
    void setUp() {
        mealPlanService = mock(MealPlanService.class);
        userContext = mock(UserContext.class);
        QuarkusMock.installMockForType(mealPlanService, MealPlanService.class);
        QuarkusMock.installMockForType(userContext, UserContext.class);
    }

    @Test
    void getWeek_should_return_unauthorized_without_token() {
        doThrow(new UnauthorizedException("Missing or invalid Bearer token."))
                .when(userContext).requireUser(null);

        given()
                .when().get("/meal-plan/week")
                .then()
                .statusCode(401)
                .body("status", equalTo(401))
                .body("error", equalTo("Unauthorized"))
                .body("message", equalTo("Missing or invalid Bearer token."))
                .body("path", equalTo("/meal-plan/week"));
    }

    @Test
    void getWeek_should_return_week_for_user() {
        AppUser currentUser = user(1L);
        LocalDate monday = LocalDate.of(2026, 6, 1);
        LocalDate sunday = LocalDate.of(2026, 6, 7);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doReturn(monday).when(mealPlanService).normalizeWeekStart(LocalDate.of(2026, 6, 3));
        doReturn(List.of(mealPlan(currentUser, recipe(10L, currentUser), monday)))
                .when(mealPlanService).getWeek(currentUser, monday);

        given()
                .header("Authorization", "Bearer valid-token")
                .queryParam("startDate", "2026-06-03")
                .when().get("/meal-plan/week")
                .then()
                .statusCode(200)
                .body("weekStart", equalTo(monday.toString()))
                .body("weekEnd", equalTo(sunday.toString()))
                .body("entries", hasSize(1))
                .body("entries[0].plannedDate", equalTo(monday.toString()))
                .body("entries[0].recipe.title", equalTo("Pasta 10"));
    }

    @Test
    void getWeek_should_return_multiple_slots_for_same_day() {
        AppUser currentUser = user(1L);
        LocalDate monday = LocalDate.of(2026, 6, 1);
        MealPlan breakfast = mealPlan(currentUser, recipe(10L, currentUser), monday);
        breakfast.setMealSlot(MealSlot.BREAKFAST);
        MealPlan lunch = mealPlan(currentUser, recipe(11L, currentUser), monday);
        lunch.setId(2L);
        lunch.setMealSlot(MealSlot.LUNCH);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doReturn(monday).when(mealPlanService).normalizeWeekStart(null);
        doReturn(List.of(breakfast, lunch)).when(mealPlanService).getWeek(currentUser, monday);

        given()
                .header("Authorization", "Bearer valid-token")
                .when().get("/meal-plan/week")
                .then()
                .statusCode(200)
                .body("entries", hasSize(2))
                .body("entries[0].plannedDate", equalTo("2026-06-01"))
                .body("entries[0].mealSlot", equalTo("breakfast"))
                .body("entries[0].recipe.title", equalTo("Pasta 10"))
                .body("entries[1].plannedDate", equalTo("2026-06-01"))
                .body("entries[1].mealSlot", equalTo("lunch"))
                .body("entries[1].recipe.title", equalTo("Pasta 11"));
    }

    @Test
    void putDay_should_return_ok_for_own_recipe() {
        AppUser currentUser = user(1L);
        LocalDate date = LocalDate.of(2026, 6, 1);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doReturn(mealPlan(currentUser, recipe(10L, currentUser), date))
                .when(mealPlanService).setRecipeForDay(eq(currentUser), eq(date), any(MealPlanEntryRequest.class));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body("""
                        {
                          "recipeId": 10
                        }
                        """)
                .when().put("/meal-plan/days/2026-06-01")
                .then()
                .statusCode(200)
                .body("plannedDate", equalTo("2026-06-01"))
                .body("recipe.id", equalTo(10))
                .body("recipe.title", equalTo("Pasta 10"));
    }

    @Test
    void putDay_should_return_forbidden_for_foreign_recipe() {
        AppUser currentUser = user(1L);
        LocalDate date = LocalDate.of(2026, 6, 1);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doThrow(new ForbiddenException("Only own recipes can be planned."))
                .when(mealPlanService).setRecipeForDay(eq(currentUser), eq(date), any(MealPlanEntryRequest.class));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body("""
                        {
                          "recipeId": 20
                        }
                        """)
                .when().put("/meal-plan/days/2026-06-01")
                .then()
                .statusCode(403)
                .body("message", equalTo("Only own recipes can be planned."))
                .body("path", equalTo("/meal-plan/days/2026-06-01"));
    }

    @Test
    void putDay_should_return_not_found_for_unknown_recipe() {
        AppUser currentUser = user(1L);
        LocalDate date = LocalDate.of(2026, 6, 1);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doThrow(new RecipeNotFoundException(99L))
                .when(mealPlanService).setRecipeForDay(eq(currentUser), eq(date), any(MealPlanEntryRequest.class));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body("""
                        {
                          "recipeId": 99
                        }
                        """)
                .when().put("/meal-plan/days/2026-06-01")
                .then()
                .statusCode(404)
                .body("message", equalTo("Recipe with ID 99 not found."));
    }

    @Test
    void putDay_should_return_bad_request_for_invalid_date() {
        AppUser currentUser = user(1L);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body("""
                        {
                          "recipeId": 10
                        }
                        """)
                .when().put("/meal-plan/days/not-a-date")
                .then()
                .statusCode(400)
                .body("message", equalTo("Date must use ISO format YYYY-MM-DD."));
    }

    @Test
    void putDay_should_return_bad_request_for_missing_recipe_id() {
        AppUser currentUser = user(1L);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body("""
                        {
                          "recipeId": null
                        }
                        """)
                .when().put("/meal-plan/days/2026-06-01")
                .then()
                .statusCode(400)
                .body("message", equalTo("Validation failed: recipeId must not be null"));

        verify(mealPlanService, never()).setRecipeForDay(any(), any(), any());
    }

    @Test
    void putSlot_should_return_ok_for_own_recipe() {
        AppUser currentUser = user(1L);
        LocalDate date = LocalDate.of(2026, 6, 1);
        MealPlan mealPlan = mealPlan(currentUser, recipe(10L, currentUser), date);
        mealPlan.setMealSlot(MealSlot.BREAKFAST);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doReturn(mealPlan)
                .when(mealPlanService).setRecipeForSlot(eq(currentUser), eq(date), eq(MealSlot.BREAKFAST), any(MealPlanEntryRequest.class));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body("""
                        {
                          "recipeId": 10
                        }
                        """)
                .when().put("/meal-plan/days/2026-06-01/slots/breakfast")
                .then()
                .statusCode(200)
                .body("mealSlot", equalTo("breakfast"))
                .body("recipe.id", equalTo(10));
    }

    @Test
    void putSlot_should_return_bad_request_for_invalid_slot() {
        AppUser currentUser = user(1L);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body("""
                        {
                          "recipeId": 10
                        }
                        """)
                .when().put("/meal-plan/days/2026-06-01/slots/brunch")
                .then()
                .statusCode(400)
                .body("message", equalTo("mealSlot must be breakfast, lunch, dinner or snack."));
    }

    @Test
    void deleteDay_should_return_no_content_for_own_entry() {
        AppUser currentUser = user(1L);
        LocalDate date = LocalDate.of(2026, 6, 1);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");

        given()
                .header("Authorization", "Bearer valid-token")
                .when().delete("/meal-plan/days/2026-06-01")
                .then()
                .statusCode(204);

        verify(mealPlanService).deleteForDay(currentUser, date);
    }

    @Test
    void deleteDay_should_return_not_found_for_missing_entry() {
        AppUser currentUser = user(1L);
        LocalDate date = LocalDate.of(2026, 6, 1);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doThrow(new MealPlanEntryNotFoundException(date)).when(mealPlanService).deleteForDay(currentUser, date);

        given()
                .header("Authorization", "Bearer valid-token")
                .when().delete("/meal-plan/days/2026-06-01")
                .then()
                .statusCode(404)
                .body("message", equalTo("Meal plan entry for date 2026-06-01 not found."));
    }

    @Test
    void deleteSlot_should_return_no_content_for_own_entry() {
        AppUser currentUser = user(1L);
        LocalDate date = LocalDate.of(2026, 6, 1);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");

        given()
                .header("Authorization", "Bearer valid-token")
                .when().delete("/meal-plan/days/2026-06-01/slots/snack")
                .then()
                .statusCode(204);

        verify(mealPlanService).deleteForSlot(currentUser, date, MealSlot.SNACK);
    }

    private MealPlan mealPlan(AppUser owner, Recipe recipe, LocalDate plannedDate) {
        MealPlan mealPlan = new MealPlan();
        mealPlan.setId(1L);
        mealPlan.setOwner(owner);
        mealPlan.setRecipe(recipe);
        mealPlan.setPlannedDate(plannedDate);
        return mealPlan;
    }

    private Recipe recipe(Long id, AppUser owner) {
        Recipe recipe = new Recipe("Pasta " + id, "", 10, 20, 2,
                "easy", "Italian", 4.5, "noodles", "cook", false, true);
        recipe.setId(id);
        recipe.setOwner(owner);
        return recipe;
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
