package de.htwberlin.webtech.favorite;

import de.htwberlin.webtech.favorite.dto.ExternalRecipeFavoriteRequest;
import de.htwberlin.webtech.favorite.entity.ExternalRecipeFavorite;
import de.htwberlin.webtech.favorite.service.ExternalRecipeFavoriteService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@QuarkusTest
class ExternalRecipeFavoriteResourceTest {

    private ExternalRecipeFavoriteService service;
    private UserContext userContext;

    @BeforeEach
    void setUp() {
        service = mock(ExternalRecipeFavoriteService.class);
        userContext = mock(UserContext.class);
        QuarkusMock.installMockForType(service, ExternalRecipeFavoriteService.class);
        QuarkusMock.installMockForType(userContext, UserContext.class);
    }

    @Test
    void list_should_return_unauthorized_without_token() {
        doThrow(new UnauthorizedException("Missing or invalid Bearer token."))
                .when(userContext).requireUser(null);

        given()
                .when().get("/favorites/external")
                .then()
                .statusCode(401);
    }

    @Test
    void list_should_return_external_favorites() {
        AppUser user = user();
        doReturn(user).when(userContext).requireUser("Bearer token");
        doReturn(List.of(favorite("716429", "Pasta"))).when(service).listMine(user);

        given()
                .header("Authorization", "Bearer token")
                .when().get("/favorites/external")
                .then()
                .statusCode(200)
                .body("", hasSize(1))
                .body("[0].externalRecipeId", equalTo("716429"))
                .body("[0].externalTitle", equalTo("Pasta"))
                .body("[0].externalSource", equalTo("SPOONACULAR"));
    }

    @Test
    void add_should_save_external_favorite() {
        AppUser user = user();
        doReturn(user).when(userContext).requireUser("Bearer token");
        doReturn(favorite("716429", "Pasta")).when(service).add(eq(user), any(ExternalRecipeFavoriteRequest.class));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer token")
                .body("""
                        {
                          "externalRecipeId": "716429",
                          "externalTitle": "Pasta",
                          "externalImageUrl": "https://example.com/pasta.jpg",
                          "externalSource": "SPOONACULAR"
                        }
                        """)
                .when().post("/favorites/external")
                .then()
                .statusCode(200)
                .body("externalRecipeId", equalTo("716429"))
                .body("externalTitle", equalTo("Pasta"));
    }

    @Test
    void remove_should_delete_external_favorite() {
        AppUser user = user();
        doReturn(user).when(userContext).requireUser("Bearer token");

        given()
                .header("Authorization", "Bearer token")
                .when().delete("/favorites/external/SPOONACULAR/716429")
                .then()
                .statusCode(204);

        verify(service).remove(user, "SPOONACULAR", "716429");
    }

    @Test
    void removeById_should_delete_external_favorite_without_external_fields() {
        AppUser user = user();
        doReturn(user).when(userContext).requireUser("Bearer token");

        given()
                .header("Authorization", "Bearer token")
                .when().delete("/favorites/external/1")
                .then()
                .statusCode(204);

        verify(service).removeById(user, 1L);
    }

    private ExternalRecipeFavorite favorite(String id, String title) {
        ExternalRecipeFavorite favorite = new ExternalRecipeFavorite();
        favorite.setId(1L);
        favorite.setExternalRecipeId(id);
        favorite.setExternalTitle(title);
        favorite.setExternalSource("SPOONACULAR");
        return favorite;
    }

    private AppUser user() {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setUsername("favorite-user");
        user.setEmail("favorite@example.com");
        return user;
    }
}
