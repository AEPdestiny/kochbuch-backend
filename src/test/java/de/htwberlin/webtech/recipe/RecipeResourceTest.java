package de.htwberlin.webtech.recipe;

import de.htwberlin.webtech.recipe.entity.Recipe;
import de.htwberlin.webtech.recipe.exception.RecipeNotFoundException;
import de.htwberlin.webtech.recipe.service.ExternalRecipeService;
import de.htwberlin.webtech.recipe.service.RecipeService;
import de.htwberlin.webtech.security.UserContext;
import de.htwberlin.webtech.shared.exception.ForbiddenException;
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
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@QuarkusTest
class RecipeResourceTest {

    private RecipeService recipeService;
    private ExternalRecipeService externalRecipeService;
    private UserContext userContext;

    @BeforeEach
    void setUp() {
        recipeService = mock(RecipeService.class);
        externalRecipeService = mock(ExternalRecipeService.class);
        userContext = mock(UserContext.class);
        QuarkusMock.installMockForType(recipeService, RecipeService.class);
        QuarkusMock.installMockForType(externalRecipeService, ExternalRecipeService.class);
        QuarkusMock.installMockForType(userContext, UserContext.class);
    }

    @Test
    void getAll_should_return_only_published_recipes() {
        doReturn(List.of(recipe("Pasta"), recipe("Soup"))).when(recipeService).findAll();

        given()
                .when().get("/recipes")
                .then()
                .statusCode(200)
                .body("$", hasSize(2));

        verify(recipeService).findAll();
    }

    @Test
    void getPublished_should_return_only_published_recipes() {
        doReturn(List.of(recipe("Cake"))).when(recipeService).findAllPublished();

        given()
                .when().get("/recipes/published")
                .then()
                .statusCode(200)
                .body("$", hasSize(1));

        verify(recipeService).findAllPublished();
    }

    @Test
    void getMine_should_return_unauthorized_without_token() {
        doThrow(new UnauthorizedException("Missing or invalid Bearer token."))
                .when(userContext).requireUser(null);

        given()
                .when().get("/recipes/mine")
                .then()
                .statusCode(401)
                .body("status", equalTo(401))
                .body("error", equalTo("Unauthorized"))
                .body("message", equalTo("Missing or invalid Bearer token."))
                .body("path", equalTo("/recipes/mine"));
    }

    @Test
    void getMine_should_return_ok_with_token() {
        AppUser currentUser = user();
        Recipe mine = recipe("Mine");
        mine.setOwner(currentUser);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doReturn(List.of(mine)).when(recipeService).findMine(currentUser);

        given()
                .header("Authorization", "Bearer valid-token")
                .when().get("/recipes/mine")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].title", equalTo("Mine"));

        verify(recipeService).findMine(currentUser);
    }

    @Test
    void getMine_should_return_only_current_users_recipes() {
        AppUser currentUser = user();
        Recipe mine = recipe("Mine");
        mine.setOwner(currentUser);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doReturn(List.of(mine)).when(recipeService).findMine(currentUser);

        given()
                .header("Authorization", "Bearer valid-token")
                .when().get("/recipes/mine")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].title", equalTo("Mine"));
    }

    @Test
    void getExternal_should_return_ok() {
        doReturn(List.of(recipe("Ext"))).when(externalRecipeService).fetchExternalRecipes();

        given()
                .when().get("/recipes/external")
                .then()
                .statusCode(200)
                .body("$", hasSize(1));
    }

    @Test
    void getById_should_return_published_recipe_without_token() {
        doReturn(recipe("Pasta")).when(recipeService).findVisibleById(1L, null);

        given()
                .when().get("/recipes/1")
                .then()
                .statusCode(200)
                .body("title", equalTo("Pasta"));
    }

    @Test
    void getById_should_return_not_found_for_private_recipe_without_token() {
        doThrow(new RecipeNotFoundException(2L))
                .when(recipeService).findVisibleById(2L, null);

        given()
                .when().get("/recipes/2")
                .then()
                .statusCode(404)
                .body("status", equalTo(404))
                .body("error", equalTo("Not Found"))
                .body("message", equalTo("Recipe with ID 2 not found."))
                .body("path", equalTo("/recipes/2"));
    }

    @Test
    void getById_should_return_private_recipe_for_owner_token() {
        AppUser owner = user();
        Recipe privateRecipe = recipe("Private");
        privateRecipe.setPublished(false);
        privateRecipe.setOwner(owner);
        doReturn(owner).when(userContext).currentUserOrNull("Bearer owner-token");
        doReturn(privateRecipe).when(recipeService).findVisibleById(3L, owner);

        given()
                .header("Authorization", "Bearer owner-token")
                .when().get("/recipes/3")
                .then()
                .statusCode(200)
                .body("title", equalTo("Private"))
                .body("published", equalTo(false));
    }

    @Test
    void getById_should_return_not_found_for_private_recipe_with_foreign_token() {
        AppUser foreignUser = user();
        foreignUser.setId(2L);
        doReturn(foreignUser).when(userContext).currentUserOrNull("Bearer foreign-token");
        doThrow(new RecipeNotFoundException(4L))
                .when(recipeService).findVisibleById(4L, foreignUser);

        given()
                .header("Authorization", "Bearer foreign-token")
                .when().get("/recipes/4")
                .then()
                .statusCode(404)
                .body("status", equalTo(404))
                .body("error", equalTo("Not Found"))
                .body("message", equalTo("Recipe with ID 4 not found."))
                .body("path", equalTo("/recipes/4"));
    }

    @Test
    void create_should_return_ok() {
        doReturn(user()).when(userContext).requireUser("Bearer valid-token");
        doReturn(recipe("Pasta")).when(recipeService).create(any(), any());

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body("""
                        {
                          "title": "Pasta",
                          "imageUrl": "",
                          "prepTimeMinutes": 10,
                          "cookTimeMinutes": 20,
                          "servings": 2,
                          "difficulty": "easy",
                          "category": "Italian",
                          "rating": 4.5,
                          "ingredients": "noodles",
                          "instructions": "cook",
                          "favorite": false,
                          "published": true
                        }
                """)
                .when().post("/recipes")
                .then()
                .statusCode(201)
                .body("title", equalTo("Pasta"));
    }

    @Test
    void create_should_return_unauthorized_without_token() {
        doThrow(new UnauthorizedException("Missing or invalid Bearer token."))
                .when(userContext).requireUser(null);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "Pasta",
                          "imageUrl": "",
                          "prepTimeMinutes": 10,
                          "cookTimeMinutes": 20,
                          "servings": 2,
                          "difficulty": "easy",
                          "category": "Italian",
                          "rating": 4.5,
                          "ingredients": "noodles",
                          "instructions": "cook",
                          "favorite": false,
                          "published": true
                        }
                        """)
                .when().post("/recipes")
                .then()
                .statusCode(401)
                .body("status", equalTo(401))
                .body("error", equalTo("Unauthorized"))
                .body("message", equalTo("Missing or invalid Bearer token."))
                .body("path", equalTo("/recipes"));
    }

    @Test
    void update_should_return_ok() {
        doReturn(user()).when(userContext).requireUser("Bearer valid-token");
        doReturn(recipe("Updated")).when(recipeService).update(eq(1L), any(), any());

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body("""
                        {
                          "title": "Updated",
                          "imageUrl": "",
                          "prepTimeMinutes": 10,
                          "cookTimeMinutes": 20,
                          "servings": 2,
                          "difficulty": "easy",
                          "category": "Italian",
                          "rating": 4.5,
                          "ingredients": "noodles",
                          "instructions": "cook",
                          "favorite": false,
                          "published": true
                        }
                        """)
                .when().put("/recipes/1")
                .then()
                .statusCode(200)
                .body("title", equalTo("Updated"));
    }

    @Test
    void update_should_return_unauthorized_without_token() {
        doThrow(new UnauthorizedException("Missing or invalid Bearer token."))
                .when(userContext).requireUser(null);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "Updated",
                          "imageUrl": "",
                          "prepTimeMinutes": 10,
                          "cookTimeMinutes": 20,
                          "servings": 2,
                          "difficulty": "easy",
                          "category": "Italian",
                          "rating": 4.5,
                          "ingredients": "noodles",
                          "instructions": "cook",
                          "favorite": false,
                          "published": true
                        }
                        """)
                .when().put("/recipes/1")
                .then()
                .statusCode(401)
                .body("status", equalTo(401))
                .body("error", equalTo("Unauthorized"))
                .body("message", equalTo("Missing or invalid Bearer token."))
                .body("path", equalTo("/recipes/1"));
    }

    @Test
    void update_should_return_forbidden_for_foreign_recipe() {
        doReturn(user()).when(userContext).requireUser("Bearer valid-token");
        doThrow(new ForbiddenException("Only the recipe owner may access this recipe."))
                .when(recipeService).update(eq(1L), any(), any());

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body("""
                        {
                          "title": "Updated",
                          "imageUrl": "",
                          "prepTimeMinutes": 10,
                          "cookTimeMinutes": 20,
                          "servings": 2,
                          "difficulty": "easy",
                          "category": "Italian",
                          "rating": 4.5,
                          "ingredients": "noodles",
                          "instructions": "cook",
                          "favorite": false,
                          "published": true
                        }
                        """)
                .when().put("/recipes/1")
                .then()
                .statusCode(403)
                .body("status", equalTo(403))
                .body("error", equalTo("Forbidden"))
                .body("message", equalTo("Only the recipe owner may access this recipe."))
                .body("path", equalTo("/recipes/1"));
    }

    @Test
    void update_should_return_forbidden_for_legacy_recipe_without_owner() {
        doReturn(user()).when(userContext).requireUser("Bearer valid-token");
        doThrow(new ForbiddenException("Only the recipe owner may access this recipe."))
                .when(recipeService).update(eq(2L), any(), any());

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body("""
                        {
                          "title": "Updated",
                          "imageUrl": "",
                          "prepTimeMinutes": 10,
                          "cookTimeMinutes": 20,
                          "servings": 2,
                          "difficulty": "easy",
                          "category": "Italian",
                          "rating": 4.5,
                          "ingredients": "noodles",
                          "instructions": "cook",
                          "favorite": false,
                          "published": true
                        }
                        """)
                .when().put("/recipes/2")
                .then()
                .statusCode(403)
                .body("status", equalTo(403))
                .body("error", equalTo("Forbidden"))
                .body("message", equalTo("Only the recipe owner may access this recipe."))
                .body("path", equalTo("/recipes/2"));
    }

    @Test
    void delete_should_return_ok() {
        doReturn(user()).when(userContext).requireUser("Bearer valid-token");

        given()
                .header("Authorization", "Bearer valid-token")
                .when().delete("/recipes/1")
                .then()
                .statusCode(204);

        verify(recipeService).delete(eq(1L), any());
    }

    @Test
    void delete_should_return_unauthorized_without_token() {
        doThrow(new UnauthorizedException("Missing or invalid Bearer token."))
                .when(userContext).requireUser(null);

        given()
                .when().delete("/recipes/1")
                .then()
                .statusCode(401)
                .body("status", equalTo(401))
                .body("error", equalTo("Unauthorized"))
                .body("message", equalTo("Missing or invalid Bearer token."))
                .body("path", equalTo("/recipes/1"));
    }

    @Test
    void delete_should_return_forbidden_for_foreign_recipe() {
        doReturn(user()).when(userContext).requireUser("Bearer valid-token");
        doThrow(new ForbiddenException("Only the recipe owner may access this recipe."))
                .when(recipeService).delete(eq(1L), any());

        given()
                .header("Authorization", "Bearer valid-token")
                .when().delete("/recipes/1")
                .then()
                .statusCode(403)
                .body("status", equalTo(403))
                .body("error", equalTo("Forbidden"))
                .body("message", equalTo("Only the recipe owner may access this recipe."))
                .body("path", equalTo("/recipes/1"));
    }

    @Test
    void delete_should_return_forbidden_for_legacy_recipe_without_owner() {
        doReturn(user()).when(userContext).requireUser("Bearer valid-token");
        doThrow(new ForbiddenException("Only the recipe owner may access this recipe."))
                .when(recipeService).delete(eq(2L), any());

        given()
                .header("Authorization", "Bearer valid-token")
                .when().delete("/recipes/2")
                .then()
                .statusCode(403)
                .body("status", equalTo(403))
                .body("error", equalTo("Forbidden"))
                .body("message", equalTo("Only the recipe owner may access this recipe."))
                .body("path", equalTo("/recipes/2"));
    }

    @Test
    void getById_should_return_not_found_when_recipe_missing() {
        doThrow(new RecipeNotFoundException(99L))
                .when(recipeService).findVisibleById(99L, null);

        given()
                .when().get("/recipes/99")
                .then()
                .statusCode(404)
                .body("status", equalTo(404))
                .body("error", equalTo("Not Found"))
                .body("message", equalTo("Recipe with ID 99 not found."))
                .body("path", equalTo("/recipes/99"));
    }

    @Test
    void update_should_return_not_found_when_recipe_missing() {
        doReturn(user()).when(userContext).requireUser("Bearer valid-token");
        doThrow(new RecipeNotFoundException(99L))
                .when(recipeService).update(eq(99L), any(), any());

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body("""
                        {
                          "title": "Updated",
                          "imageUrl": "",
                          "prepTimeMinutes": 10,
                          "cookTimeMinutes": 20,
                          "servings": 2,
                          "difficulty": "easy",
                          "category": "Italian",
                          "rating": 4.5,
                          "ingredients": "noodles",
                          "instructions": "cook",
                          "favorite": false,
                          "published": true
                        }
                        """)
                .when().put("/recipes/99")
                .then()
                .statusCode(404)
                .body("status", equalTo(404))
                .body("error", equalTo("Not Found"))
                .body("message", equalTo("Recipe with ID 99 not found."))
                .body("path", equalTo("/recipes/99"));
    }

    @Test
    void delete_should_return_not_found_when_recipe_missing() {
        doReturn(user()).when(userContext).requireUser("Bearer valid-token");
        doThrow(new RecipeNotFoundException(99L))
                .when(recipeService).delete(eq(99L), any());

        given()
                .header("Authorization", "Bearer valid-token")
                .when().delete("/recipes/99")
                .then()
                .statusCode(404)
                .body("status", equalTo(404))
                .body("error", equalTo("Not Found"))
                .body("message", equalTo("Recipe with ID 99 not found."))
                .body("path", equalTo("/recipes/99"));
    }

    @Test
    void create_should_return_bad_request_when_title_is_blank() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "",
                          "imageUrl": "",
                          "prepTimeMinutes": 10,
                          "cookTimeMinutes": 20,
                          "servings": 2,
                          "difficulty": "easy",
                          "category": "Italian",
                          "rating": 4.5,
                          "ingredients": "noodles",
                          "instructions": "cook",
                          "favorite": false,
                          "published": true
                        }
                        """)
                .when().post("/recipes")
                .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("error", equalTo("Bad Request"))
                .body("message", equalTo("Validation failed: title must not be blank"))
                .body("path", equalTo("/recipes"));
    }

    @Test
    void create_should_return_bad_request_when_required_fields_are_missing() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "imageUrl": "",
                          "prepTimeMinutes": 10,
                          "cookTimeMinutes": 20,
                          "servings": 2,
                          "difficulty": "easy",
                          "category": "Italian",
                          "rating": 4.5,
                          "favorite": false,
                          "published": true
                        }
                        """)
                .when().post("/recipes")
                .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("error", equalTo("Bad Request"))
                .body("message", startsWith("Validation failed:"))
                .body("path", equalTo("/recipes"));
    }

    @Test
    void update_should_return_bad_request_when_recipe_data_invalid() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "Updated",
                          "imageUrl": "",
                          "prepTimeMinutes": -1,
                          "cookTimeMinutes": 20,
                          "servings": 2,
                          "difficulty": "easy",
                          "category": "Italian",
                          "rating": 4.5,
                          "ingredients": "noodles",
                          "instructions": "cook",
                          "favorite": false,
                          "published": true
                        }
                        """)
                .when().put("/recipes/1")
                .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("error", equalTo("Bad Request"))
                .body("message", equalTo("Validation failed: prepTimeMinutes must be greater than or equal to 0"))
                .body("path", equalTo("/recipes/1"));
    }

    private Recipe recipe(String title) {
        return new Recipe(title, "", 10, 20, 2,
                "easy", "Italian", 4.5,
                "noodles", "cook", false, true);
    }

    private AppUser user() {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setUsername("salma");
        user.setEmail("salma@example.com");
        user.setPasswordHash("hash");
        return user;
    }
}
