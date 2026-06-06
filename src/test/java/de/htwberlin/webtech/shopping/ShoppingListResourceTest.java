package de.htwberlin.webtech.shopping;

import de.htwberlin.webtech.security.UserContext;
import de.htwberlin.webtech.shared.exception.ForbiddenException;
import de.htwberlin.webtech.shared.exception.UnauthorizedException;
import de.htwberlin.webtech.shopping.dto.ShoppingListItemRequest;
import de.htwberlin.webtech.shopping.entity.ShoppingListItem;
import de.htwberlin.webtech.shopping.exception.ShoppingListItemNotFoundException;
import de.htwberlin.webtech.shopping.service.ShoppingListService;
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
class ShoppingListResourceTest {

    private ShoppingListService shoppingListService;
    private UserContext userContext;

    @BeforeEach
    void setUp() {
        shoppingListService = mock(ShoppingListService.class);
        userContext = mock(UserContext.class);
        QuarkusMock.installMockForType(shoppingListService, ShoppingListService.class);
        QuarkusMock.installMockForType(userContext, UserContext.class);
    }

    @Test
    void getMine_should_return_unauthorized_without_token() {
        doThrow(new UnauthorizedException("Missing or invalid Bearer token."))
                .when(userContext).requireUser(null);

        given()
                .when().get("/shopping-list/items")
                .then()
                .statusCode(401)
                .body("status", equalTo(401))
                .body("error", equalTo("Unauthorized"))
                .body("message", equalTo("Missing or invalid Bearer token."))
                .body("path", equalTo("/shopping-list/items"));
    }

    @Test
    void getMine_should_return_only_current_users_items() {
        AppUser currentUser = user(1L);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doReturn(List.of(item("Tomatoes", currentUser))).when(shoppingListService).listMine(currentUser);

        given()
                .header("Authorization", "Bearer valid-token")
                .when().get("/shopping-list/items")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].name", equalTo("Tomatoes"));
    }

    @Test
    void create_should_return_created_for_current_user() {
        AppUser currentUser = user(1L);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doReturn(item("Tomatoes", currentUser)).when(shoppingListService).create(any(ShoppingListItemRequest.class), eq(currentUser));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body("""
                        {
                          "name": "Tomatoes",
                          "quantity": 3,
                          "unit": "piece",
                          "category": "Vegetables",
                          "checked": false
                        }
                        """)
                .when().post("/shopping-list/items")
                .then()
                .statusCode(201)
                .body("name", equalTo("Tomatoes"))
                .body("quantity", equalTo(3))
                .body("checked", equalTo(false));
    }

    @Test
    void update_should_return_ok_for_own_item() {
        AppUser currentUser = user(1L);
        ShoppingListItem updated = item("Cherry Tomatoes", currentUser);
        updated.setChecked(true);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doReturn(updated).when(shoppingListService).update(eq(1L), any(ShoppingListItemRequest.class), eq(currentUser));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body("""
                        {
                          "name": "Cherry Tomatoes",
                          "quantity": 5,
                          "unit": "piece",
                          "category": "Vegetables",
                          "checked": true
                        }
                        """)
                .when().put("/shopping-list/items/1")
                .then()
                .statusCode(200)
                .body("name", equalTo("Cherry Tomatoes"))
                .body("checked", equalTo(true));
    }

    @Test
    void update_should_return_forbidden_for_foreign_item() {
        AppUser currentUser = user(1L);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doThrow(new ForbiddenException("Only the shopping list item owner may access this shopping list item."))
                .when(shoppingListService).update(eq(2L), any(ShoppingListItemRequest.class), eq(currentUser));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body(validBody())
                .when().put("/shopping-list/items/2")
                .then()
                .statusCode(403)
                .body("status", equalTo(403))
                .body("error", equalTo("Forbidden"))
                .body("message", equalTo("Only the shopping list item owner may access this shopping list item."))
                .body("path", equalTo("/shopping-list/items/2"));
    }

    @Test
    void update_should_return_not_found_for_unknown_id() {
        AppUser currentUser = user(1L);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doThrow(new ShoppingListItemNotFoundException(99L))
                .when(shoppingListService).update(eq(99L), any(ShoppingListItemRequest.class), eq(currentUser));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body(validBody())
                .when().put("/shopping-list/items/99")
                .then()
                .statusCode(404)
                .body("status", equalTo(404))
                .body("error", equalTo("Not Found"))
                .body("message", equalTo("Shopping list item with ID 99 not found."))
                .body("path", equalTo("/shopping-list/items/99"));
    }

    @Test
    void delete_should_return_no_content_for_own_item() {
        AppUser currentUser = user(1L);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");

        given()
                .header("Authorization", "Bearer valid-token")
                .when().delete("/shopping-list/items/1")
                .then()
                .statusCode(204);

        verify(shoppingListService).delete(1L, currentUser);
    }

    @Test
    void delete_should_return_forbidden_for_foreign_item() {
        AppUser currentUser = user(1L);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doThrow(new ForbiddenException("Only the shopping list item owner may access this shopping list item."))
                .when(shoppingListService).delete(2L, currentUser);

        given()
                .header("Authorization", "Bearer valid-token")
                .when().delete("/shopping-list/items/2")
                .then()
                .statusCode(403)
                .body("status", equalTo(403))
                .body("error", equalTo("Forbidden"))
                .body("message", equalTo("Only the shopping list item owner may access this shopping list item."))
                .body("path", equalTo("/shopping-list/items/2"));
    }

    @Test
    void delete_should_return_not_found_for_unknown_id() {
        AppUser currentUser = user(1L);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doThrow(new ShoppingListItemNotFoundException(99L))
                .when(shoppingListService).delete(99L, currentUser);

        given()
                .header("Authorization", "Bearer valid-token")
                .when().delete("/shopping-list/items/99")
                .then()
                .statusCode(404)
                .body("status", equalTo(404))
                .body("error", equalTo("Not Found"))
                .body("message", equalTo("Shopping list item with ID 99 not found."))
                .body("path", equalTo("/shopping-list/items/99"));
    }

    private ShoppingListItem item(String name, AppUser owner) {
        ShoppingListItem item = new ShoppingListItem();
        item.setId(1L);
        item.setName(name);
        item.setQuantity(BigDecimal.valueOf(3));
        item.setUnit("piece");
        item.setCategory("Vegetables");
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

    private String validBody() {
        return """
                {
                  "name": "Tomatoes",
                  "quantity": 3,
                  "unit": "piece",
                  "category": "Vegetables",
                  "checked": false
                }
                """;
    }
}
