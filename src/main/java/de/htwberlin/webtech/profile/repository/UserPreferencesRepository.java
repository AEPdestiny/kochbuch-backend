package de.htwberlin.webtech.profile.repository;

import de.htwberlin.webtech.profile.entity.UserPreferences;
import de.htwberlin.webtech.user.entity.AppUser;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class UserPreferencesRepository implements PanacheRepository<UserPreferences> {

    public Optional<UserPreferences> findByOwner(AppUser owner) {
        return find("owner", owner).firstResultOptional();
    }
}
