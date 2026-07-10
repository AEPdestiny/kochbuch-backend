package de.htwberlin.webtech.recipe;

import de.htwberlin.webtech.recipe.entity.Recipe;
import de.htwberlin.webtech.recipe.exception.RecipeNotFoundException;
import de.htwberlin.webtech.recipe.external.ExternalRecipeService;
import de.htwberlin.webtech.recipe.dto.ExternalRecipeDetailResponse;
import de.htwberlin.webtech.recipe.dto.InstructionSearchResponse;
import de.htwberlin.webtech.recipe.dto.InstructionSearchResult;
import de.htwberlin.webtech.recipe.dto.RecipeInstructionSuggestion;
import de.htwberlin.webtech.recipe.dto.RecipeInstructionSuggestionResponse;
import de.htwberlin.webtech.recipe.dto.RecipeResponse;
import de.htwberlin.webtech.recipe.instructions.InstructionSearchService;
import de.htwberlin.webtech.recipe.instructions.RecipeInstructionSuggestionService;
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
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
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
    private InstructionSearchService instructionSearchService;
    private RecipeInstructionSuggestionService instructionSuggestionService;
    private UserContext userContext;

    @BeforeEach
    void setUp() {
        recipeService = mock(RecipeService.class);
        externalRecipeService = mock(ExternalRecipeService.class);
        instructionSearchService = mock(InstructionSearchService.class);
        instructionSuggestionService = mock(RecipeInstructionSuggestionService.class);
        userContext = mock(UserContext.class);
        QuarkusMock.installMockForType(recipeService, RecipeService.class);
        QuarkusMock.installMockForType(externalRecipeService, ExternalRecipeService.class);
        QuarkusMock.installMockForType(instructionSearchService, InstructionSearchService.class);
        QuarkusMock.installMockForType(instructionSuggestionService, RecipeInstructionSuggestionService.class);
        QuarkusMock.installMockForType(userContext, UserContext.class);
    }

    @Test
    void getAll_should_return_only_published_recipes() {
        doReturn(List.of(recipe("Pasta"), recipe("Soup"))).when(recipeService).findAllPublished("en", null);

        given()
                .queryParam("language", "en")
                .when().get("/recipes")
                .then()
                .statusCode(200)
                .body("$", hasSize(2))
                .body("[0].protein", equalTo(24.5F));

        verify(recipeService).findAllPublished("en", null);
    }

    @Test
    void getPublished_should_return_only_published_recipes() {
        doReturn(List.of(recipe("Cake"))).when(recipeService).findAllPublished("de", null);

        given()
                .queryParam("language", "de")
                .when().get("/recipes/published")
                .then()
                .statusCode(200)
                .body("$", hasSize(1));

        verify(recipeService).findAllPublished("de", null);
    }

    @Test
    void getPublished_should_pass_search_query() {
        doReturn(List.of(recipe("Sushi"))).when(recipeService).findAllPublished("de", "sushi");

        given()
                .queryParam("language", "de")
                .queryParam("search", "sushi")
                .when().get("/recipes/published")
                .then()
                .statusCode(200)
                .body("$", hasSize(1));

        verify(recipeService).findAllPublished("de", "sushi");
    }

    @Test
    void getPublished_should_return_normalized_ingredient_text() {
        Recipe recipe = recipe("Shrimp");
        recipe.setIngredients("0 ml onion 0 5 EL garlic 0 ml butter 0 1");
        recipe.setInstructions("1. Zutaten vorbereiten.\n2. Alles verrühren.\n3. Servieren.");
        doReturn(List.of(recipe)).when(recipeService).findAllPublished("en", "shrimp");

        given()
                .queryParam("language", "en")
                .queryParam("search", "shrimp")
                .when().get("/recipes/published")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].ingredients", equalTo("onion\n5 EL garlic\nbutter"))
                .body("[0].ingredientsList", equalTo(List.of("onion", "5 EL garlic", "butter")))
                .body("[0].instructionsList", equalTo(List.of("Zutaten vorbereiten.", "Alles verrühren.", "Servieren.")));
    }

    @Test
    void getPublished_should_include_owned_published_recipe_without_exposing_owner_actions() {
        Recipe publishedOwned = recipe("Community Pasta");
        publishedOwned.setOwner(user());
        publishedOwned.setLanguage("de");
        doReturn(List.of(publishedOwned)).when(recipeService).findAllPublished("de", "community");

        given()
                .queryParam("language", "de")
                .queryParam("search", "community")
                .when().get("/recipes/published")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].title", equalTo("Community Pasta"))
                .body("[0].published", equalTo(true))
                .body("[0].userCreated", equalTo(true))
                .body("[0].ownedByCurrentUser", equalTo(false));
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
                .body("[0].title", equalTo("Mine"))
                .body("[0].ownedByCurrentUser", equalTo(true));

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
        doReturn(List.of(externalRecipe("Ext"))).when(externalRecipeService).fetchExternalRecipes(null, null, null, null, null);

        given()
                .queryParam("language", "en")
                .when().get("/recipes/external")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].source", equalTo("spoonacular"));
    }

    @Test
    void getExternal_should_pass_search_query() {
        doReturn(List.of(externalRecipe("Pasta"))).when(externalRecipeService).fetchExternalRecipes("pasta", null, null, null, null);

        given()
                .queryParam("search", "pasta")
                .when().get("/recipes/external")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].title", equalTo("Pasta"));

        verify(externalRecipeService).fetchExternalRecipes("pasta", null, null, null, null);
    }

    @Test
    void getExternal_should_return_empty_for_non_english_language_without_calling_spoonacular() {
        given()
                .queryParam("language", "de")
                .when().get("/recipes/external")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    void getExternalById_should_return_detail() {
        ExternalRecipeDetailResponse detail = new ExternalRecipeDetailResponse();
        detail.setId(716429L);
        detail.setExternalId("716429");
        detail.setSource("spoonacular");
        detail.setTitle("Pasta");
        doReturn(Optional.of(detail)).when(externalRecipeService).fetchExternalRecipeDetail(716429L);

        given()
                .when().get("/recipes/external/716429")
                .then()
                .statusCode(200)
                .body("title", equalTo("Pasta"))
                .body("source", equalTo("spoonacular"));
    }

    @Test
    void getExternalById_should_return_not_found_when_missing() {
        doReturn(Optional.empty()).when(externalRecipeService).fetchExternalRecipeDetail(99L);

        given()
                .when().get("/recipes/external/99")
                .then()
                .statusCode(404)
                .body("status", equalTo(404))
                .body("error", equalTo("Not Found"));
    }

    @Test
    void searchInstructions_should_return_results() {
        InstructionSearchResponse response = new InstructionSearchResponse();
        response.setConfigured(true);
        response.setResults(List.of(new InstructionSearchResult(
                "Pasta instructions",
                "https://example.com/pasta",
                "Cook pasta."
        )));
        doReturn(response).when(instructionSearchService).search(any());

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "recipeTitle": "Pasta",
                          "sourceUrl": "https://example.com/source",
                          "sourceName": "Example"
                        }
                        """)
                .when().post("/recipes/instructions/search")
                .then()
                .statusCode(200)
                .body("configured", equalTo(true))
                .body("results", hasSize(1))
                .body("results[0].title", equalTo("Pasta instructions"))
                .body("results[0].url", equalTo("https://example.com/pasta"));
    }

    @Test
    void searchInstructions_should_return_bad_request_for_blank_title() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "recipeTitle": ""
                        }
                        """)
                .when().post("/recipes/instructions/search")
                .then()
                .statusCode(400)
                .body("message", equalTo("Validation failed: recipeTitle must not be blank"));
    }

    @Test
    void suggestInstructions_should_require_token() {
        doThrow(new UnauthorizedException("Missing or invalid Bearer token."))
                .when(userContext).requireUser(null);

        given()
                .contentType(ContentType.JSON)
                .when().post("/recipes/1/instruction-suggestions")
                .then()
                .statusCode(401)
                .body("status", equalTo(401))
                .body("error", equalTo("Unauthorized"));
    }

    @Test
    void suggestInstructions_should_return_recipe_specific_suggestions() {
        AppUser currentUser = user();
        Recipe recipe = recipe("Pasta");
        recipe.setId(1L);
        RecipeInstructionSuggestionResponse response = new RecipeInstructionSuggestionResponse();
        response.setRecipeId(1L);
        response.setConfigured(true);
        response.setHasRealInstructions(false);
        response.setSuggestions(List.of(new RecipeInstructionSuggestion(
                "Pasta instructions",
                "https://example.com/pasta",
                List.of("Prepare pasta.", "Cook sauce.", "Serve warm."),
                0.7,
                "Aus Websuche abgeleitete Vorschlagsquelle. Bitte vor dem Kochen prüfen."
        )));
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doReturn(recipe).when(recipeService).findVisibleById(1L, currentUser);
        doReturn(response).when(instructionSuggestionService).suggestFor(recipe);

        given()
                .header("Authorization", "Bearer valid-token")
                .contentType(ContentType.JSON)
                .when().post("/recipes/1/instruction-suggestions")
                .then()
                .statusCode(200)
                .body("configured", equalTo(true))
                .body("hasRealInstructions", equalTo(false))
                .body("suggestions", hasSize(1))
                .body("suggestions[0].sourceTitle", equalTo("Pasta instructions"))
                .body("suggestions[0].steps", equalTo(List.of("Prepare pasta.", "Cook sauce.", "Serve warm.")));

        verify(recipeService).findVisibleById(1L, currentUser);
        verify(instructionSuggestionService).suggestFor(recipe);
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
                .body("published", equalTo(false))
                .body("ownedByCurrentUser", equalTo(true));
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
        AppUser currentUser = user();
        Recipe created = recipe("Pasta");
        created.setOwner(currentUser);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doReturn(created).when(recipeService).create(any(), any());

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
                .body("title", equalTo("Pasta"))
                .body("published", equalTo(true))
                .body("ownedByCurrentUser", equalTo(true));
    }

    @Test
    void create_should_return_ok_without_calories() {
        AppUser currentUser = user();
        Recipe created = recipe("Pasta");
        created.setOwner(currentUser);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doReturn(created).when(recipeService).create(any(), any());

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
                .body("title", equalTo("Pasta"))
                .body("calories", nullValue());
    }

    @Test
    void create_should_save_and_return_calories_when_provided() {
        AppUser currentUser = user();
        Recipe created = recipe("Pasta");
        created.setOwner(currentUser);
        created.setCalories(480);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doReturn(created).when(recipeService).create(any(), any());

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
                          "published": true,
                          "calories": 480
                        }
                """)
                .when().post("/recipes")
                .then()
                .statusCode(201)
                .body("title", equalTo("Pasta"))
                .body("calories", equalTo(480));
    }

    @Test
    void create_should_return_bad_request_when_calories_is_negative() {
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
                          "published": true,
                          "calories": -5
                        }
                        """)
                .when().post("/recipes")
                .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("error", equalTo("Bad Request"))
                .body("message", equalTo("Validation failed: calories must be greater than or equal to 0"))
                .body("path", equalTo("/recipes"));
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
        AppUser currentUser = user();
        Recipe updated = recipe("Updated");
        updated.setOwner(currentUser);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doReturn(updated).when(recipeService).update(eq(1L), any(), any());

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
                .body("title", equalTo("Updated"))
                .body("ownedByCurrentUser", equalTo(true));
    }

    @Test
    void update_should_save_and_return_calories_when_provided() {
        AppUser currentUser = user();
        Recipe updated = recipe("Updated");
        updated.setOwner(currentUser);
        updated.setCalories(610);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doReturn(updated).when(recipeService).update(eq(1L), any(), any());

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
                          "published": true,
                          "calories": 610
                        }
                        """)
                .when().put("/recipes/1")
                .then()
                .statusCode(200)
                .body("title", equalTo("Updated"))
                .body("calories", equalTo(610));
    }

    @Test
    void update_should_clear_calories_when_omitted() {
        AppUser currentUser = user();
        Recipe updated = recipe("Updated");
        updated.setOwner(currentUser);
        updated.setCalories(null);
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doReturn(updated).when(recipeService).update(eq(1L), any(), any());

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
                .body("title", equalTo("Updated"))
                .body("calories", nullValue());
    }

    @Test
    void remove_favorite_should_not_require_recipe_payload() {
        AppUser currentUser = user();
        Recipe updated = recipe("Broken old favorite");
        updated.setOwner(currentUser);
        updated.setFavorite(false);
        updated.setIngredients("");
        updated.setInstructions("");
        doReturn(currentUser).when(userContext).requireUser("Bearer valid-token");
        doReturn(updated).when(recipeService).removeFavorite(1L, currentUser);

        given()
                .header("Authorization", "Bearer valid-token")
                .when().delete("/recipes/1/favorite")
                .then()
                .statusCode(204);

        verify(recipeService).removeFavorite(1L, currentUser);
    }

    @Test
    void update_should_return_bad_request_when_calories_is_negative() {
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
                          "published": true,
                          "calories": -1
                        }
                        """)
                .when().put("/recipes/1")
                .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("error", equalTo("Bad Request"))
                .body("message", equalTo("Validation failed: calories must be greater than or equal to 0"))
                .body("path", equalTo("/recipes/1"));
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
        Recipe recipe = new Recipe(title, "", 10, 20, 2,
                "easy", "Italian", 4.5,
                "noodles", "cook", false, true);
        recipe.setProtein(24.5);
        return recipe;
    }

    private RecipeResponse externalRecipe(String title) {
        RecipeResponse response = new RecipeResponse();
        response.setId(716429L);
        response.setExternalId("716429");
        response.setSource("spoonacular");
        response.setTitle(title);
        response.setImageUrl("");
        response.setIngredients("noodles");
        response.setInstructions("cook");
        response.setPublished(true);
        return response;
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
