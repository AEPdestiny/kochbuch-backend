package de.htwberlin.webtech.auth;

import de.htwberlin.webtech.auth.dto.AuthResponse;
import de.htwberlin.webtech.auth.dto.LoginRequest;
import de.htwberlin.webtech.auth.dto.RegisterRequest;
import de.htwberlin.webtech.auth.service.AuthService;
import de.htwberlin.webtech.shared.exception.ConflictException;
import de.htwberlin.webtech.shared.exception.UnauthorizedException;
import de.htwberlin.webtech.user.dto.UserResponse;
import de.htwberlin.webtech.user.entity.Role;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.smallrye.jwt.build.Jwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

@QuarkusTest
class AuthResourceTest {

    private static final String JWT_SECRET = "dishly-smart-test-secret-please-change-in-production";
    private static final String JWT_ISSUER = "dishly-smart";

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        QuarkusMock.installMockForType(authService, AuthService.class);
    }

    @Test
    void register_should_return_created_with_token() {
        doReturn(authResponse()).when(authService).register(any(RegisterRequest.class));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "username": "salma",
                          "email": "salma@example.com",
                          "password": "supersecret"
                        }
                        """)
                .when().post("/auth/register")
                .then()
                .statusCode(201)
                .body("tokenType", equalTo("Bearer"))
                .body("expiresIn", equalTo(3600))
                .body("accessToken", not(blankOrNullString()))
                .body("user.email", equalTo("salma@example.com"));
    }

    @Test
    void register_should_return_conflict_when_email_exists() {
        doThrow(new ConflictException("Email is already registered."))
                .when(authService).register(any(RegisterRequest.class));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "username": "salma",
                          "email": "salma@example.com",
                          "password": "supersecret"
                        }
                        """)
                .when().post("/auth/register")
                .then()
                .statusCode(409)
                .body("status", equalTo(409))
                .body("error", equalTo("Conflict"))
                .body("message", equalTo("Email is already registered."))
                .body("path", equalTo("/auth/register"));
    }

    @Test
    void login_should_return_ok_with_token() {
        doReturn(authResponse()).when(authService).login(any(LoginRequest.class));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "email": "salma@example.com",
                          "password": "supersecret"
                        }
                        """)
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .body("tokenType", equalTo("Bearer"))
                .body("expiresIn", equalTo(3600))
                .body("accessToken", not(blankOrNullString()))
                .body("user.email", equalTo("salma@example.com"));
    }

    @Test
    void login_should_return_unauthorized_when_password_is_wrong() {
        doThrow(new UnauthorizedException("Invalid email or password."))
                .when(authService).login(any(LoginRequest.class));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "email": "salma@example.com",
                          "password": "wrong-password"
                        }
                        """)
                .when().post("/auth/login")
                .then()
                .statusCode(401)
                .body("status", equalTo(401))
                .body("error", equalTo("Unauthorized"))
                .body("message", equalTo("Invalid email or password."))
                .body("path", equalTo("/auth/login"));
    }

    @Test
    void me_should_return_unauthorized_without_token() {
        given()
                .when().get("/auth/me")
                .then()
                .statusCode(401);
    }

    @Test
    void me_should_return_current_user_with_valid_token() {
        doReturn(userResponse()).when(authService).currentUser("salma@example.com");

        given()
                .header("Authorization", "Bearer " + validToken())
                .when().get("/auth/me")
                .then()
                .statusCode(200)
                .body("email", equalTo("salma@example.com"))
                .body("username", equalTo("salma"))
                .body("role", equalTo("USER"));
    }

    private AuthResponse authResponse() {
        return new AuthResponse("test-token", "Bearer", 3600, userResponse());
    }

    private UserResponse userResponse() {
        UserResponse user = new UserResponse();
        user.setId(1L);
        user.setUsername("salma");
        user.setEmail("salma@example.com");
        user.setRole(Role.USER);
        user.setCreatedAt(Instant.parse("2026-06-05T12:00:00Z"));
        return user;
    }

    private String validToken() {
        Instant now = Instant.now();
        return Jwt.issuer(JWT_ISSUER)
                .subject("1")
                .upn("salma@example.com")
                .preferredUserName("salma")
                .groups(Set.of("USER"))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .signWithSecret(JWT_SECRET);
    }
}
