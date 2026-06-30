package de.htwberlin.webtech.mealplan;

import de.htwberlin.webtech.mealplan.dto.MealPlanEntryRequest;
import de.htwberlin.webtech.mealplan.dto.MealPlanShoppingListItemResponse;
import de.htwberlin.webtech.mealplan.dto.MealPlanShoppingListResponse;
import de.htwberlin.webtech.mealplan.entity.MealPlan;
import de.htwberlin.webtech.mealplan.entity.MealSlot;
import de.htwberlin.webtech.mealplan.exception.MealPlanEntryNotFoundException;
import de.htwberlin.webtech.mealplan.service.MealPlanShoppingListService;
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
import java.math.BigDecimal;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
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
    private MealPlanShoppingListService shoppingListService;
    private UserContext userContext;

    @BeforeEach
    void setUp() {
        mealPlanService = mock(MealPlanService.class);
        shoppingListService = mock(MealPlanShoppingListService.class);
        userContext = mock(UserContext.class);
        QuarkusMock.installMockForType(mealPlanService, MealPlanService.class);
        QuarkusMock.installMockForType(shoppingListService, MealPlanShoppingListService.class);
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
        MealPlan snack = mealPlan(currentUser, null, monday);
        snack.setId(3L);
        snack.setMealSlot(MealSlot.SNACK);
        snack.setCustomTitle("Sushi frei");
        snack.setCaloriesSnapshot(480);
        snack.setImageUrlSnapshot("https://example.com/sushi.jpg");
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doReturn(monday).when(mealPlanService).normalizeWeekStart(null);
        doReturn(List.of(breakfast, lunch, snack)).when(mealPlanService).getWeek(currentUser, monday);

        given()
                .header("Authorization", "Bearer valid-token")
                .when().get("/meal-plan/week")
                .then()
                .statusCode(200)
                .body("entries", hasSize(3))
                .body("entries[0].plannedDate", equalTo("2026-06-01"))
                .body("entries[0].mealSlot", equalTo("breakfast"))
                .body("entries[0].recipe.title", equalTo("Pasta 10"))
                .body("entries[1].plannedDate", equalTo("2026-06-01"))
                .body("entries[1].mealSlot", equalTo("lunch"))
                .body("entries[1].recipe.title", equalTo("Pasta 11"))
                .body("entries[2].mealSlot", equalTo("snack"))
                .body("entries[2].customTitle", equalTo("Sushi frei"))
                .body("entries[2].calories", equalTo(480))
                .body("entries[2].caloriesSnapshot", equalTo(480))
                .body("entries[2].imageUrlSnapshot", equalTo("https://example.com/sushi.jpg"));
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
        LocalDate date = LocalDate.of(2026, 6, 1);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doThrow(new IllegalArgumentException("recipeId or customTitle must be provided."))
                .when(mealPlanService).setRecipeForDay(eq(currentUser), eq(date), any(MealPlanEntryRequest.class));

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
                .body("message", equalTo("recipeId or customTitle must be provided."));
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
    void putSlot_should_return_ok_for_custom_title() {
        AppUser currentUser = user(1L);
        LocalDate date = LocalDate.of(2026, 6, 1);
        MealPlan mealPlan = mealPlan(currentUser, null, date);
        mealPlan.setMealSlot(MealSlot.SNACK);
        mealPlan.setCustomTitle("Sushi frei");
        mealPlan.setCaloriesSnapshot(480);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doReturn(mealPlan)
                .when(mealPlanService).setRecipeForSlot(eq(currentUser), eq(date), eq(MealSlot.SNACK), any(MealPlanEntryRequest.class));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body("""
                        {
                          "customTitle": "Sushi frei",
                          "caloriesSnapshot": 480
                        }
                        """)
                .when().put("/meal-plan/days/2026-06-01/slots/snack")
                .then()
                .statusCode(200)
                .body("mealSlot", equalTo("snack"))
                .body("customTitle", equalTo("Sushi frei"))
                .body("calories", equalTo(480));
    }

    @Test
    void putSlot_should_return_ok_for_custom_title_without_calories() {
        AppUser currentUser = user(1L);
        LocalDate date = LocalDate.of(2026, 6, 1);
        MealPlan mealPlan = mealPlan(currentUser, null, date);
        mealPlan.setMealSlot(MealSlot.SNACK);
        mealPlan.setCustomTitle("Lasagne");
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doReturn(mealPlan)
                .when(mealPlanService).setRecipeForSlot(eq(currentUser), eq(date), eq(MealSlot.SNACK), any(MealPlanEntryRequest.class));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body("""
                        {
                          "customTitle": "Lasagne"
                        }
                        """)
                .when().put("/meal-plan/days/2026-06-01/slots/snack")
                .then()
                .statusCode(200)
                .body("customTitle", equalTo("Lasagne"))
                .body("calories", nullValue());
    }

    @Test
    void putSlot_should_return_bad_request_for_negative_caloriesSnapshot() {
        AppUser currentUser = user(1L);
        LocalDate date = LocalDate.of(2026, 6, 1);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doThrow(new IllegalArgumentException("caloriesSnapshot must be greater than or equal to 0."))
                .when(mealPlanService).setRecipeForSlot(eq(currentUser), eq(date), eq(MealSlot.SNACK), any(MealPlanEntryRequest.class));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body("""
                        {
                          "customTitle": "Sushi",
                          "caloriesSnapshot": -1
                        }
                        """)
                .when().put("/meal-plan/days/2026-06-01/slots/snack")
                .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("error", equalTo("Bad Request"))
                .body("message", equalTo("caloriesSnapshot must be greater than or equal to 0."))
                .body("path", equalTo("/meal-plan/days/2026-06-01/slots/snack"));
    }

    @Test
    void putSlot_should_return_ok_when_updating_calories_to_new_value() {
        AppUser currentUser = user(1L);
        LocalDate date = LocalDate.of(2026, 6, 1);
        MealPlan mealPlan = mealPlan(currentUser, null, date);
        mealPlan.setMealSlot(MealSlot.LUNCH);
        mealPlan.setCustomTitle("Pasta");
        mealPlan.setCaloriesSnapshot(750);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doReturn(mealPlan)
                .when(mealPlanService).setRecipeForSlot(eq(currentUser), eq(date), eq(MealSlot.LUNCH), any(MealPlanEntryRequest.class));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body("""
                        {
                          "customTitle": "Pasta",
                          "caloriesSnapshot": 750
                        }
                        """)
                .when().put("/meal-plan/days/2026-06-01/slots/lunch")
                .then()
                .statusCode(200)
                .body("calories", equalTo(750));
    }

    @Test
    void putSlot_should_return_ok_when_clearing_calories() {
        AppUser currentUser = user(1L);
        LocalDate date = LocalDate.of(2026, 6, 1);
        MealPlan mealPlan = mealPlan(currentUser, null, date);
        mealPlan.setMealSlot(MealSlot.LUNCH);
        mealPlan.setCustomTitle("Pasta");
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doReturn(mealPlan)
                .when(mealPlanService).setRecipeForSlot(eq(currentUser), eq(date), eq(MealSlot.LUNCH), any(MealPlanEntryRequest.class));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body("""
                        {
                          "customTitle": "Pasta"
                        }
                        """)
                .when().put("/meal-plan/days/2026-06-01/slots/lunch")
                .then()
                .statusCode(200)
                .body("calories", nullValue());
    }

    @Test
    void getWeek_should_include_day_and_week_calorie_totals() {
        AppUser currentUser = user(1L);
        LocalDate monday = LocalDate.of(2026, 6, 1);
        MealPlan freeText = mealPlan(currentUser, null, monday);
        freeText.setMealSlot(MealSlot.SNACK);
        freeText.setCustomTitle("Sushi frei");
        freeText.setCaloriesSnapshot(480);
        Recipe recipeWithCalories = recipe(10L, currentUser);
        recipeWithCalories.setCalories(520);
        MealPlan recipeEntry = mealPlan(currentUser, recipeWithCalories, monday);
        recipeEntry.setId(2L);
        recipeEntry.setMealSlot(MealSlot.DINNER);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doReturn(monday).when(mealPlanService).normalizeWeekStart(null);
        doReturn(List.of(freeText, recipeEntry)).when(mealPlanService).getWeek(currentUser, monday);

        given()
                .header("Authorization", "Bearer valid-token")
                .when().get("/meal-plan/week")
                .then()
                .statusCode(200)
                .body("caloriesByDate.'" + monday + "'", equalTo(1000))
                .body("totalCalories", equalTo(1000));
    }

    @Test
    void getWeek_should_not_count_entries_without_calories_in_totals() {
        AppUser currentUser = user(1L);
        LocalDate monday = LocalDate.of(2026, 6, 1);
        MealPlan noCalories = mealPlan(currentUser, null, monday);
        noCalories.setMealSlot(MealSlot.SNACK);
        noCalories.setCustomTitle("Unbekannt");
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doReturn(monday).when(mealPlanService).normalizeWeekStart(null);
        doReturn(List.of(noCalories)).when(mealPlanService).getWeek(currentUser, monday);

        given()
                .header("Authorization", "Bearer valid-token")
                .when().get("/meal-plan/week")
                .then()
                .statusCode(200)
                .body("caloriesByDate", not(hasKey(monday.toString())))
                .body("totalCalories", equalTo(0));
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

    @Test
    void moveEntry_should_return_updated_week_for_own_entry() {
        AppUser currentUser = user(1L);
        LocalDate tuesday = LocalDate.of(2026, 6, 2);
        LocalDate monday = LocalDate.of(2026, 6, 1);
        MealPlan moved = mealPlan(currentUser, recipe(10L, currentUser), tuesday);
        moved.setMealSlot(MealSlot.BREAKFAST);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doReturn(monday).when(mealPlanService).normalizeWeekStart(tuesday);
        doReturn(List.of(moved)).when(mealPlanService)
                .moveEntry(currentUser, 1L, tuesday, MealSlot.BREAKFAST, true);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body("""
                        {
                          "targetDate": "2026-06-02",
                          "targetSlot": "breakfast",
                          "swapIfOccupied": true
                        }
                        """)
                .when().patch("/meal-plan/entries/1/move")
                .then()
                .statusCode(200)
                .body("weekStart", equalTo("2026-06-01"))
                .body("weekEnd", equalTo("2026-06-07"))
                .body("entries", hasSize(1))
                .body("entries[0].plannedDate", equalTo("2026-06-02"))
                .body("entries[0].mealSlot", equalTo("breakfast"))
                .body("entries[0].recipe.id", equalTo(10));
    }

    @Test
    void moveEntry_should_preserve_free_text_entry_with_caloriesSnapshot() {
        AppUser currentUser = user(1L);
        LocalDate tuesday = LocalDate.of(2026, 6, 2);
        LocalDate monday = LocalDate.of(2026, 6, 1);
        MealPlan moved = mealPlan(currentUser, null, tuesday);
        moved.setMealSlot(MealSlot.BREAKFAST);
        moved.setCustomTitle("Sushi frei");
        moved.setCaloriesSnapshot(480);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doReturn(monday).when(mealPlanService).normalizeWeekStart(tuesday);
        doReturn(List.of(moved)).when(mealPlanService)
                .moveEntry(currentUser, 1L, tuesday, MealSlot.BREAKFAST, true);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body("""
                        {
                          "targetDate": "2026-06-02",
                          "targetSlot": "breakfast",
                          "swapIfOccupied": true
                        }
                        """)
                .when().patch("/meal-plan/entries/1/move")
                .then()
                .statusCode(200)
                .body("entries[0].customTitle", equalTo("Sushi frei"))
                .body("entries[0].calories", equalTo(480))
                .body("entries[0].mealSlot", equalTo("breakfast"))
                .body("entries[0].plannedDate", equalTo("2026-06-02"));
    }

    @Test
    void moveEntry_should_return_updated_week_for_recipe_on_different_day() {
        AppUser currentUser = user(1L);
        LocalDate thursday = LocalDate.of(2026, 6, 4);
        LocalDate monday = LocalDate.of(2026, 6, 1);
        MealPlan moved = mealPlan(currentUser, recipe(10L, currentUser), thursday);
        moved.setMealSlot(MealSlot.DINNER);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doReturn(monday).when(mealPlanService).normalizeWeekStart(thursday);
        doReturn(List.of(moved)).when(mealPlanService)
                .moveEntry(currentUser, 1L, thursday, MealSlot.DINNER, false);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body("""
                        {
                          "targetDate": "2026-06-04",
                          "targetSlot": "dinner",
                          "swapIfOccupied": false
                        }
                        """)
                .when().patch("/meal-plan/entries/1/move")
                .then()
                .statusCode(200)
                .body("weekStart", equalTo("2026-06-01"))
                .body("entries[0].plannedDate", equalTo("2026-06-04"))
                .body("entries[0].recipe.id", equalTo(10));
    }

    @Test
    void moveEntry_should_return_not_found_for_unknown_entry() {
        AppUser currentUser = user(1L);
        LocalDate monday = LocalDate.of(2026, 6, 1);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doReturn(monday).when(mealPlanService).normalizeWeekStart(LocalDate.of(2026, 6, 2));
        doThrow(new MealPlanEntryNotFoundException(99L))
                .when(mealPlanService).moveEntry(eq(currentUser), eq(99L), any(), any(), eq(true));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body("""
                        {
                          "targetDate": "2026-06-02",
                          "targetSlot": "dinner",
                          "swapIfOccupied": true
                        }
                        """)
                .when().patch("/meal-plan/entries/99/move")
                .then()
                .statusCode(404)
                .body("status", equalTo(404))
                .body("error", equalTo("Not Found"))
                .body("path", equalTo("/meal-plan/entries/99/move"));
    }

    @Test
    void moveEntry_should_return_forbidden_for_foreign_entry() {
        AppUser currentUser = user(1L);
        LocalDate monday = LocalDate.of(2026, 6, 1);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doReturn(monday).when(mealPlanService).normalizeWeekStart(LocalDate.of(2026, 6, 2));
        doThrow(new ForbiddenException("Only own meal plan entries can be moved."))
                .when(mealPlanService).moveEntry(eq(currentUser), eq(1L), any(), any(), eq(true));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body("""
                        {
                          "targetDate": "2026-06-02",
                          "targetSlot": "dinner",
                          "swapIfOccupied": true
                        }
                        """)
                .when().patch("/meal-plan/entries/1/move")
                .then()
                .statusCode(403)
                .body("status", equalTo(403))
                .body("message", equalTo("Only own meal plan entries can be moved."));
    }

    @Test
    void moveEntry_should_return_bad_request_for_missing_target() {
        AppUser currentUser = user(1L);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body("""
                        {
                          "targetSlot": "breakfast"
                        }
                        """)
                .when().patch("/meal-plan/entries/1/move")
                .then()
                .statusCode(400)
                .body("message", equalTo("targetDate and targetSlot must be provided."));
    }

    @Test
    void createShoppingListFromWeek_should_return_unauthorized_without_token() {
        doThrow(new UnauthorizedException("Missing or invalid Bearer token."))
                .when(userContext).requireUser(null);

        given()
                .contentType(ContentType.JSON)
                .when().post("/meal-plan/shopping-list")
                .then()
                .statusCode(401)
                .body("status", equalTo(401))
                .body("message", equalTo("Missing or invalid Bearer token."))
                .body("path", equalTo("/meal-plan/shopping-list"));

        verify(shoppingListService, never()).createShoppingList(any(), any());
    }

    @Test
    void createShoppingListFromWeek_should_return_transparent_result() {
        AppUser currentUser = user(1L);
        LocalDate monday = LocalDate.of(2026, 6, 1);
        MealPlanShoppingListResponse response = new MealPlanShoppingListResponse();
        response.getAdded().add(new MealPlanShoppingListItemResponse(
                "Tomaten",
                BigDecimal.valueOf(200),
                "g",
                "Pasta",
                "500 g Tomaten",
                "Zur Einkaufsliste hinzugefügt."
        ));
        response.getSkippedBecauseInPantry().add(new MealPlanShoppingListItemResponse(
                "Eier",
                BigDecimal.valueOf(2),
                "Stück",
                "Omelette",
                "2 Eier",
                "Bereits ausreichend im Vorrat."
        ));
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doReturn(monday).when(mealPlanService).normalizeWeekStart(LocalDate.of(2026, 6, 3));
        doReturn(response).when(shoppingListService).createShoppingList(currentUser, monday);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .queryParam("startDate", "2026-06-03")
                .when().post("/meal-plan/shopping-list")
                .then()
                .statusCode(200)
                .body("added", hasSize(1))
                .body("added[0].name", equalTo("Tomaten"))
                .body("added[0].quantity", equalTo(200))
                .body("added[0].unit", equalTo("g"))
                .body("skippedBecauseInPantry", hasSize(1))
                .body("skippedBecauseInPantry[0].name", equalTo("Eier"));
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
