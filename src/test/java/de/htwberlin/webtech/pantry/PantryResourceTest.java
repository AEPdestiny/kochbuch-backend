package de.htwberlin.webtech.pantry;

import de.htwberlin.webtech.pantry.dto.PantryItemRequest;
import de.htwberlin.webtech.pantry.entity.PantryItem;
import de.htwberlin.webtech.pantry.exception.PantryItemNotFoundException;
import de.htwberlin.webtech.pantry.service.PantryService;
import de.htwberlin.webtech.security.UserContext;
import de.htwberlin.webtech.shared.exception.ForbiddenException;
import de.htwberlin.webtech.shared.exception.UnauthorizedException;
import de.htwberlin.webtech.user.entity.AppUser;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@QuarkusTest
class PantryResourceTest {

    private PantryService pantryService;
    private UserContext userContext;

    @BeforeEach
    void setUp() {
        pantryService = mock(PantryService.class);
        userContext = mock(UserContext.class);
        QuarkusMock.installMockForType(pantryService, PantryService.class);
        QuarkusMock.installMockForType(userContext, UserContext.class);
    }

    @Test
    void getMine_should_return_unauthorized_without_token() {
        doThrow(new UnauthorizedException("Missing or invalid Bearer token."))
                .when(userContext).requireUser(null);

        given()
                .when().get("/pantry/items")
                .then()
                .statusCode(401)
                .body("status", equalTo(401))
                .body("error", equalTo("Unauthorized"))
                .body("message", equalTo("Missing or invalid Bearer token."))
                .body("path", equalTo("/pantry/items"));
    }

    @Test
    void getMine_should_return_only_current_users_items() {
        AppUser currentUser = user(1L);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doReturn(List.of(item("Rice", currentUser))).when(pantryService).listMine(currentUser);

        given()
                .header("Authorization", "Bearer valid-token")
                .when().get("/pantry/items")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].name", equalTo("Rice"));
    }

    @Test
    void create_should_return_created_for_current_user() {
        AppUser currentUser = user(1L);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doReturn(item("Rice", currentUser)).when(pantryService).create(any(PantryItemRequest.class), eq(currentUser));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body("""
                        {
                          "name": "Rice",
                          "quantity": 2,
                          "unit": "kg",
                          "category": "Grains"
                        }
                        """)
                .when().post("/pantry/items")
                .then()
                .statusCode(201)
                .body("name", equalTo("Rice"))
                .body("quantity", equalTo(2));
    }

    @Test
    void update_should_return_ok_for_own_item() {
        AppUser currentUser = user(1L);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doReturn(item("Updated Rice", currentUser)).when(pantryService).update(eq(1L), any(PantryItemRequest.class), eq(currentUser));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body("""
                        {
                          "name": "Updated Rice",
                          "quantity": 3,
                          "unit": "kg",
                          "category": "Grains"
                        }
                        """)
                .when().put("/pantry/items/1")
                .then()
                .statusCode(200)
                .body("name", equalTo("Updated Rice"));
    }

    @Test
    void update_should_return_forbidden_for_foreign_item() {
        AppUser currentUser = user(1L);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doThrow(new ForbiddenException("Only the pantry item owner may access this pantry item."))
                .when(pantryService).update(eq(2L), any(PantryItemRequest.class), eq(currentUser));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body("""
                        {
                          "name": "Updated Rice",
                          "quantity": 3,
                          "unit": "kg",
                          "category": "Grains"
                        }
                        """)
                .when().put("/pantry/items/2")
                .then()
                .statusCode(403)
                .body("status", equalTo(403))
                .body("error", equalTo("Forbidden"))
                .body("message", equalTo("Only the pantry item owner may access this pantry item."))
                .body("path", equalTo("/pantry/items/2"));
    }

    @Test
    void update_should_return_not_found_for_unknown_id() {
        AppUser currentUser = user(1L);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doThrow(new PantryItemNotFoundException(99L))
                .when(pantryService).update(eq(99L), any(PantryItemRequest.class), eq(currentUser));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body("""
                        {
                          "name": "Updated Rice",
                          "quantity": 3,
                          "unit": "kg",
                          "category": "Grains"
                        }
                        """)
                .when().put("/pantry/items/99")
                .then()
                .statusCode(404)
                .body("status", equalTo(404))
                .body("error", equalTo("Not Found"))
                .body("message", equalTo("Pantry item with ID 99 not found."))
                .body("path", equalTo("/pantry/items/99"));
    }

    @Test
    void delete_should_return_no_content_for_own_item() {
        AppUser currentUser = user(1L);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");

        given()
                .header("Authorization", "Bearer valid-token")
                .when().delete("/pantry/items/1")
                .then()
                .statusCode(204);

        verify(pantryService).delete(1L, currentUser);
    }

    @Test
    void delete_should_return_forbidden_for_foreign_item() {
        AppUser currentUser = user(1L);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doThrow(new ForbiddenException("Only the pantry item owner may access this pantry item."))
                .when(pantryService).delete(2L, currentUser);

        given()
                .header("Authorization", "Bearer valid-token")
                .when().delete("/pantry/items/2")
                .then()
                .statusCode(403)
                .body("status", equalTo(403))
                .body("error", equalTo("Forbidden"))
                .body("message", equalTo("Only the pantry item owner may access this pantry item."))
                .body("path", equalTo("/pantry/items/2"));
    }

    @Test
    void delete_should_return_not_found_for_unknown_id() {
        AppUser currentUser = user(1L);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doThrow(new PantryItemNotFoundException(99L))
                .when(pantryService).delete(99L, currentUser);

        given()
                .header("Authorization", "Bearer valid-token")
                .when().delete("/pantry/items/99")
                .then()
                .statusCode(404)
                .body("status", equalTo(404))
                .body("error", equalTo("Not Found"))
                .body("message", equalTo("Pantry item with ID 99 not found."))
                .body("path", equalTo("/pantry/items/99"));
    }

    private PantryItem item(String name, AppUser owner) {
        PantryItem item = new PantryItem();
        item.setId(1L);
        item.setName(name);
        item.setQuantity(BigDecimal.valueOf(2));
        item.setUnit("kg");
        item.setCategory("Grains");
        item.setOwner(owner);
        return item;
    }

    private AppUser user(Long id) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUsername("salma");
        user.setEmail("salma@example.com");
        user.setPasswordHash("hash");
        return user;
    }
}
