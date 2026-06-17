package de.htwberlin.webtech.ai;

import de.htwberlin.webtech.ai.dto.AiChatResponse;
import de.htwberlin.webtech.ai.service.AiChatService;
import de.htwberlin.webtech.security.UserContext;
import de.htwberlin.webtech.shared.exception.UnauthorizedException;
import de.htwberlin.webtech.user.entity.AppUser;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

@QuarkusTest
class AiChatResourceTest {

    private AiChatService service;
    private UserContext userContext;

    @BeforeEach
    void setUp() {
        service = mock(AiChatService.class);
        userContext = mock(UserContext.class);
        QuarkusMock.installMockForType(service, AiChatService.class);
        QuarkusMock.installMockForType(userContext, UserContext.class);
    }

    @Test
    void chat_should_require_token() {
        doThrow(new UnauthorizedException("Missing or invalid Bearer token."))
                .when(userContext).requireUser(null);

        given()
                .contentType(ContentType.JSON)
                .body("{\"message\":\"Hallo\"}")
                .when().post("/ai/chat")
                .then()
                .statusCode(401);
    }

    @Test
    void chat_should_reject_blank_message() {
        AppUser user = user();
        doReturn(user).when(userContext).requireUser("Bearer valid-token");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body("{\"message\":\"\"}")
                .when().post("/ai/chat")
                .then()
                .statusCode(400);
    }

    @Test
    void chat_should_return_answer() {
        AppUser user = user();
        doReturn(user).when(userContext).requireUser("Bearer valid-token");
        doReturn(new AiChatResponse("Antwort", true)).when(service).answer(user, "Hallo");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-token")
                .body("{\"message\":\"Hallo\"}")
                .when().post("/ai/chat")
                .then()
                .statusCode(200)
                .body("message", equalTo("Antwort"))
                .body("configured", equalTo(true));
    }

    private AppUser user() {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setUsername("produuser");
        user.setEmail("user@example.com");
        user.setPasswordHash("hash");
        return user;
    }
}
