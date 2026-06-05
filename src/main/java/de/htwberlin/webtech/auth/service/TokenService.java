package de.htwberlin.webtech.auth.service;

import de.htwberlin.webtech.user.entity.AppUser;
import de.htwberlin.webtech.shared.exception.UnauthorizedException;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.keys.HmacKey;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;

@ApplicationScoped
public class TokenService {

    @ConfigProperty(name = "dishly.jwt.secret")
    String jwtSecret;

    @ConfigProperty(name = "dishly.jwt.issuer")
    String issuer;

    @ConfigProperty(name = "dishly.jwt.expires-in-seconds")
    long expiresInSeconds;

    public String createAccessToken(AppUser user) {
        Instant now = Instant.now();
        return Jwt.issuer(issuer)
                .subject(String.valueOf(user.getId()))
                .upn(user.getEmail())
                .preferredUserName(user.getUsername())
                .groups(Set.of(user.getRole().name()))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiresInSeconds))
                .signWithSecret(jwtSecret);
    }

    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public String emailFromBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing or invalid Bearer token.");
        }
        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if (token.isBlank()) {
            throw new UnauthorizedException("Missing or invalid Bearer token.");
        }
        try {
            JwtClaims claims = verify(token);
            if (!issuer.equals(claims.getIssuer())) {
                throw new UnauthorizedException("Missing or invalid Bearer token.");
            }
            if (claims.getExpirationTime() == null || claims.getExpirationTime().isBefore(NumericDate.now())) {
                throw new UnauthorizedException("Missing or invalid Bearer token.");
            }
            return claims.getStringClaimValue("upn");
        } catch (Exception e) {
            throw new UnauthorizedException("Missing or invalid Bearer token.");
        }
    }

    private JwtClaims verify(String token) throws Exception {
        JsonWebSignature signature = new JsonWebSignature();
        signature.setCompactSerialization(token);
        signature.setKey(new HmacKey(jwtSecret.getBytes(StandardCharsets.UTF_8)));
        signature.setAlgorithmConstraints(new AlgorithmConstraints(
                AlgorithmConstraints.ConstraintType.WHITELIST,
                AlgorithmIdentifiers.HMAC_SHA256
        ));
        if (!signature.verifySignature()) {
            throw new UnauthorizedException("Missing or invalid Bearer token.");
        }
        return JwtClaims.parse(signature.getPayload());
    }
}
