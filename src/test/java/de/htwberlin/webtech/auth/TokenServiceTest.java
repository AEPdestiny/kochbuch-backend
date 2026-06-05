package de.htwberlin.webtech.auth;

import de.htwberlin.webtech.auth.service.TokenService;
import de.htwberlin.webtech.user.entity.AppUser;
import de.htwberlin.webtech.user.entity.Role;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.keys.HmacKey;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class TokenServiceTest {

    private static final String JWT_SECRET = "dishly-smart-test-secret-please-change-in-production";

    @Inject
    TokenService tokenService;

    @Test
    void should_create_token_with_expected_claims_and_expiration() throws Exception {
        String token = tokenService.createAccessToken(user());

        assertNotNull(token);
        assertFalse(token.isBlank());

        JwtClaims claims = verifiedClaims(token);

        assertEquals("dishly-smart", claims.getIssuer());
        assertEquals("7", claims.getSubject());
        assertEquals("salma@example.com", claims.getStringClaimValue("upn"));
        assertEquals("salma", claims.getStringClaimValue("preferred_username"));
        assertTrue(claims.getStringListClaimValue("groups").contains("USER"));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpirationTime());

        long lifetimeSeconds = claims.getExpirationTime().getValue() - claims.getIssuedAt().getValue();
        assertEquals(3600, lifetimeSeconds, 5);
        assertTrue(claims.getExpirationTime().isAfter(NumericDate.now()));
    }

    private JwtClaims verifiedClaims(String token) throws Exception {
        JsonWebSignature signature = new JsonWebSignature();
        signature.setCompactSerialization(token);
        signature.setKey(new HmacKey(JWT_SECRET.getBytes(StandardCharsets.UTF_8)));
        signature.setAlgorithmConstraints(new AlgorithmConstraints(
                AlgorithmConstraints.ConstraintType.WHITELIST,
                AlgorithmIdentifiers.HMAC_SHA256
        ));
        assertTrue(signature.verifySignature());
        return JwtClaims.parse(signature.getPayload());
    }

    private AppUser user() {
        AppUser user = new AppUser();
        user.setId(7L);
        user.setUsername("salma");
        user.setEmail("salma@example.com");
        user.setRole(Role.USER);
        return user;
    }
}
