package de.htwberlin.webtech.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtSecretValidatorTest {

    @Test
    void valid_secret_should_be_accepted() {
        assertDoesNotThrow(() ->
                JwtSecretValidator.validate("production-secret-with-at-least-32-characters")
        );
    }

    @Test
    void missing_secret_should_be_rejected() {
        var thrown = assertThrows(IllegalStateException.class, () ->
                JwtSecretValidator.validate(null)
        );

        assertTrue(thrown.getMessage().contains("JWT_SECRET must be configured"));
    }

    @Test
    void short_secret_should_be_rejected() {
        var thrown = assertThrows(IllegalStateException.class, () ->
                JwtSecretValidator.validate("too-short")
        );

        assertTrue(thrown.getMessage().contains("at least 32 characters"));
    }
}
