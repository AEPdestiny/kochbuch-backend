package de.htwberlin.webtech.profile.service;

import de.htwberlin.webtech.profile.dto.UserPreferencesRequest;
import de.htwberlin.webtech.profile.entity.UserPreferences;
import de.htwberlin.webtech.profile.mapper.UserPreferencesMapper;
import de.htwberlin.webtech.profile.repository.UserPreferencesRepository;
import de.htwberlin.webtech.user.entity.AppUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Reads and updates a user's profile preferences (diet, allergies, calorie goal, ...).
 * A user has no preferences row until first accessed; getOrCreate() lazily creates one
 * with defaults, so callers never have to handle a "not found" case for this entity.
 */
@ApplicationScoped
public class UserPreferencesService {

    private final UserPreferencesRepository repository;
    private final UserPreferencesMapper mapper;

    public UserPreferencesService(UserPreferencesRepository repository, UserPreferencesMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public UserPreferences getOrCreate(AppUser currentUser) {
        return repository.findByOwner(currentUser)
                .orElseGet(() -> createDefaults(currentUser));
    }

    @Transactional
    public UserPreferences update(UserPreferencesRequest request, AppUser currentUser) {
        UserPreferences preferences = getOrCreate(currentUser);
        mapper.updateEntity(preferences, request);
        return preferences;
    }

    private UserPreferences createDefaults(AppUser currentUser) {
        UserPreferences preferences = new UserPreferences();
        preferences.setOwner(currentUser);
        repository.persist(preferences);
        return preferences;
    }
}
