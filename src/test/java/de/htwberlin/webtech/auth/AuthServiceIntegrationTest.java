package de.htwberlin.webtech.auth;

import de.htwberlin.webtech.auth.dto.LoginRequest;
import de.htwberlin.webtech.auth.dto.RegisterRequest;
import de.htwberlin.webtech.auth.service.AuthService;
import de.htwberlin.webtech.recipe.PostgresDevServicesTestProfile;
import de.htwberlin.webtech.security.PasswordService;
import de.htwberlin.webtech.shared.exception.ConflictException;
import de.htwberlin.webtech.shared.exception.UnauthorizedException;
import de.htwberlin.webtech.user.repository.AppUserRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestProfile(PostgresDevServicesTestProfile.class)
class AuthServiceIntegrationTest {

    @Inject
    AuthService authService;

    @Inject
    AppUserRepository userRepository;

    @Inject
    PasswordService passwordService;

    @Test
    void register_should_persist_app_user() {
        authService.register(registerRequest("register-user", "register-user@example.com", "supersecret"));

        var user = userRepository.findByEmail("register-user@example.com");

        assertTrue(user.isPresent());
        assertEquals("register-user", user.get().getUsername());
        assertNotNull(user.get().getCreatedAt());
    }

    @Test
    void register_should_store_bcrypt_hash_instead_of_plain_password() {
        authService.register(registerRequest("hash-user", "hash-user@example.com", "supersecret"));

        var user = userRepository.findByEmail("hash-user@example.com").orElseThrow();

        assertNotNull(user.getPasswordHash());
        assertFalse("supersecret".equals(user.getPasswordHash()));
        assertTrue(user.getPasswordHash().startsWith("$2"));
        assertTrue(passwordService.matches("supersecret", user.getPasswordHash()));
    }

    @Test
    void register_should_return_auth_response_with_token() {
        var response = authService.register(registerRequest("token-user", "token-user@example.com", "supersecret"));

        assertNotNull(response.getAccessToken());
        assertFalse(response.getAccessToken().isBlank());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600, response.getExpiresIn());
        assertEquals("token-user@example.com", response.getUser().getEmail());
    }

    @Test
    void login_should_succeed_with_correct_credentials() {
        authService.register(registerRequest("login-user", "login-user@example.com", "supersecret"));

        var response = authService.login(loginRequest("login-user@example.com", "supersecret"));

        assertNotNull(response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("login-user@example.com", response.getUser().getEmail());
    }

    @Test
    void login_should_fail_with_wrong_password() {
        authService.register(registerRequest("wrong-password-user", "wrong-password-user@example.com", "supersecret"));

        assertThrows(
                UnauthorizedException.class,
                () -> authService.login(loginRequest("wrong-password-user@example.com", "wrong-password"))
        );
    }

    @Test
    void register_should_fail_when_email_already_exists() {
        authService.register(registerRequest("email-conflict-user-1", "email-conflict@example.com", "supersecret"));

        assertThrows(
                ConflictException.class,
                () -> authService.register(registerRequest("email-conflict-user-2", "email-conflict@example.com", "supersecret"))
        );
    }

    @Test
    void register_should_fail_when_username_already_exists() {
        authService.register(registerRequest("username-conflict", "username-conflict-1@example.com", "supersecret"));

        assertThrows(
                ConflictException.class,
                () -> authService.register(registerRequest("username-conflict", "username-conflict-2@example.com", "supersecret"))
        );
    }

    private RegisterRequest registerRequest(String username, String email, String password) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }
}
