package de.htwberlin.webtech.user.repository;

import de.htwberlin.webtech.user.entity.AppUser;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class AppUserRepository implements PanacheRepository<AppUser> {

    public Optional<AppUser> findByEmail(String email) {
        return find("email", normalize(email)).firstResultOptional();
    }

    public Optional<AppUser> findByUsername(String username) {
        return find("username", username).firstResultOptional();
    }

    public boolean existsByEmail(String email) {
        return count("email", normalize(email)) > 0;
    }

    public boolean existsByUsername(String username) {
        return count("username", username) > 0;
    }

    private String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
