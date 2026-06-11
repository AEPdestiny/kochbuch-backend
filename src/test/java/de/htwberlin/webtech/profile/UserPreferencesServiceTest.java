package de.htwberlin.webtech.profile;

import de.htwberlin.webtech.profile.dto.UserPreferencesRequest;
import de.htwberlin.webtech.profile.entity.UserPreferences;
import de.htwberlin.webtech.profile.mapper.UserPreferencesMapper;
import de.htwberlin.webtech.profile.repository.UserPreferencesRepository;
import de.htwberlin.webtech.profile.service.UserPreferencesService;
import de.htwberlin.webtech.user.entity.AppUser;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class UserPreferencesServiceTest {

    private final UserPreferencesRepository repository = mock(UserPreferencesRepository.class);
    private final UserPreferencesService underTest = new UserPreferencesService(repository, new UserPreferencesMapper());

    @Test
    void getOrCreate_should_create_defaults_when_missing() {
        AppUser owner = user(1L);
        doReturn(Optional.empty()).when(repository).findByOwner(owner);

        UserPreferences result = underTest.getOrCreate(owner);

        verify(repository).findByOwner(owner);
        verify(repository).persist(result);
        assertSame(owner, result.getOwner());
        assertTrue(result.getLikes().isEmpty());
    }

    @Test
    void update_should_reuse_existing_preferences() {
        AppUser owner = user(1L);
        UserPreferences existing = new UserPreferences();
        existing.setOwner(owner);
        doReturn(Optional.of(existing)).when(repository).findByOwner(owner);

        UserPreferences result = underTest.update(request(), owner);

        verify(repository).findByOwner(owner);
        assertSame(existing, result);
        assertEquals(Set.of("pasta", "curry"), result.getLikes());
        assertEquals(Set.of("nuts"), result.getAllergies());
        assertTrue(result.isVegan());
        assertTrue(result.isHighProtein());
        assertEquals(30, result.getMaxPrepTimeMinutes());
        assertEquals(2200, result.getCalorieGoal());
    }

    private UserPreferencesRequest request() {
        UserPreferencesRequest request = new UserPreferencesRequest();
        request.setLikes(new LinkedHashSet<>(Set.of(" pasta ", "", "curry")));
        request.setDislikes(new LinkedHashSet<>(Set.of("mushrooms")));
        request.setAllergies(new LinkedHashSet<>(Set.of("nuts")));
        request.setVegan(true);
        request.setHighProtein(true);
        request.setMaxPrepTimeMinutes(30);
        request.setCalorieGoal(2200);
        return request;
    }

    private AppUser user(Long id) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUsername("user-" + id);
        user.setEmail("user-" + id + "@example.com");
        user.setPasswordHash("hash");
        return user;
    }
}
