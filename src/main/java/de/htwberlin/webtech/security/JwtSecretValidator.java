package de.htwberlin.webtech.security;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Optional;

@Startup
@ApplicationScoped
public class JwtSecretValidator {

    private static final Logger LOG = Logger.getLogger(JwtSecretValidator.class);
    static final int MIN_SECRET_LENGTH = 32;

    @ConfigProperty(name = "dishly.jwt.secret")
    Optional<String> jwtSecret;

    @PostConstruct
    void validateConfiguredSecret() {
        validate(jwtSecret.orElse(null));
    }

    static void validate(String secret) {
        if (secret == null || secret.isBlank()) {
            String message = "JWT_SECRET must be configured and must be at least 32 characters long.";
            LOG.error(message);
            throw new IllegalStateException(message);
        }
        if (secret.length() < MIN_SECRET_LENGTH) {
            String message = "JWT_SECRET must be at least 32 characters long.";
            LOG.error(message);
            throw new IllegalStateException(message);
        }
    }
}
