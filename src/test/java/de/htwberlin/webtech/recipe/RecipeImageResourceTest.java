package de.htwberlin.webtech.recipe;

import de.htwberlin.webtech.recipe.image.SupabaseStorageException;
import de.htwberlin.webtech.recipe.image.SupabaseStorageService;
import de.htwberlin.webtech.security.UserContext;
import de.htwberlin.webtech.shared.exception.UnauthorizedException;
import de.htwberlin.webtech.user.entity.AppUser;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@QuarkusTest
class RecipeImageResourceTest {

    private UserContext userContext;
    private SupabaseStorageService storageService;

    @BeforeEach
    void setUp() {
        userContext = mock(UserContext.class);
        storageService = mock(SupabaseStorageService.class);
        QuarkusMock.installMockForType(userContext, UserContext.class);
        QuarkusMock.installMockForType(storageService, SupabaseStorageService.class);
    }

    @Test
    void uploadImage_should_return_unauthorized_without_token() {
        doThrow(new UnauthorizedException("Missing or invalid Bearer token."))
                .when(userContext).requireUser(null);

        given()
                .multiPart("file", "recipe.png", imageBytes(), "image/png")
                .when().post("/recipes/images")
                .then()
                .statusCode(401)
                .body("message", equalTo("Missing or invalid Bearer token."))
                .body("path", equalTo("/recipes/images"));

        verify(storageService, never()).upload(any(Path.class), any(), any());
    }

    @Test
    void uploadImage_should_return_bad_request_without_file() {
        doReturn(user()).when(userContext).requireUser("Bearer valid-token");

        given()
                .header("Authorization", "Bearer valid-token")
                .multiPart("description", "missing file")
                .when().post("/recipes/images")
                .then()
                .statusCode(400)
                .body("message", equalTo("Image file is required."));

        verify(storageService, never()).upload(any(Path.class), any(), any());
    }

    @Test
    void uploadImage_should_return_bad_request_for_wrong_content_type() {
        doReturn(user()).when(userContext).requireUser("Bearer valid-token");

        given()
                .header("Authorization", "Bearer valid-token")
                .multiPart("file", "notes.txt", "not an image".getBytes(), "text/plain")
                .when().post("/recipes/images")
                .then()
                .statusCode(400)
                .body("message", equalTo("Only JPEG, PNG and WebP images are supported."));

        verify(storageService, never()).upload(any(Path.class), any(), any());
    }

    @Test
    void uploadImage_should_return_bad_request_for_too_large_file() {
        doReturn(user()).when(userContext).requireUser("Bearer valid-token");

        given()
                .header("Authorization", "Bearer valid-token")
                .multiPart("file", "large.png", new byte[5 * 1024 * 1024 + 1], "image/png")
                .when().post("/recipes/images")
                .then()
                .statusCode(400)
                .body("message", equalTo("Image file must not exceed 5 MB."));

        verify(storageService, never()).upload(any(Path.class), any(), any());
    }

    @Test
    void uploadImage_should_upload_valid_image_and_return_image_url() {
        doReturn(user()).when(userContext).requireUser("Bearer valid-token");
        doReturn("https://example.supabase.co/storage/v1/object/public/recipe-images/recipes/1/image.png")
                .when(storageService).upload(any(Path.class), any(), eq("image/png"));

        given()
                .header("Authorization", "Bearer valid-token")
                .multiPart("file", "recipe.png", imageBytes(), "image/png")
                .when().post("/recipes/images")
                .then()
                .statusCode(200)
                .body("imageUrl", equalTo("https://example.supabase.co/storage/v1/object/public/recipe-images/recipes/1/image.png"));

        verify(storageService).upload(any(Path.class), org.mockito.ArgumentMatchers.startsWith("recipes/1/"), eq("image/png"));
    }

    @Test
    void uploadImage_should_return_bad_gateway_when_supabase_configuration_is_missing() {
        doReturn(user()).when(userContext).requireUser("Bearer valid-token");
        doThrow(new SupabaseStorageException("Supabase URL is not configured."))
                .when(storageService).upload(any(Path.class), any(), eq("image/jpeg"));

        given()
                .header("Authorization", "Bearer valid-token")
                .multiPart("file", "recipe.jpg", imageBytes(), "image/jpeg")
                .when().post("/recipes/images")
                .then()
                .statusCode(502)
                .body("message", equalTo("Supabase URL is not configured."));
    }

    private byte[] imageBytes() {
        return new byte[] { 1, 2, 3, 4 };
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
