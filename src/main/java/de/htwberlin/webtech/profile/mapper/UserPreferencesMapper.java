package de.htwberlin.webtech.profile.mapper;

import de.htwberlin.webtech.profile.dto.UserPreferencesRequest;
import de.htwberlin.webtech.profile.dto.UserPreferencesResponse;
import de.htwberlin.webtech.profile.entity.UserPreferences;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class UserPreferencesMapper {

    public UserPreferencesResponse toResponse(UserPreferences preferences) {
        UserPreferencesResponse response = new UserPreferencesResponse();
        response.setLikes(new LinkedHashSet<>(preferences.getLikes()));
        response.setDislikes(new LinkedHashSet<>(preferences.getDislikes()));
        response.setAllergies(new LinkedHashSet<>(preferences.getAllergies()));
        response.setVegan(preferences.isVegan());
        response.setVegetarian(preferences.isVegetarian());
        response.setGlutenFree(preferences.isGlutenFree());
        response.setLactoseFree(preferences.isLactoseFree());
        response.setHighProtein(preferences.isHighProtein());
        response.setCalorieConscious(preferences.isCalorieConscious());
        response.setBudgetFriendly(false);
        response.setMaxPrepTimeMinutes(preferences.getMaxPrepTimeMinutes());
        Integer dailyTarget = preferences.getDailyCalorieTarget() != null
                ? preferences.getDailyCalorieTarget()
                : preferences.getCalorieGoal();
        response.setCalorieGoal(dailyTarget);
        response.setDailyCalorieTarget(dailyTarget);
        return response;
    }

    public void updateEntity(UserPreferences preferences, UserPreferencesRequest request) {
        preferences.setLikes(cleanValues(request.getLikes()));
        preferences.setDislikes(cleanValues(request.getDislikes()));
        preferences.setAllergies(cleanValues(request.getAllergies()));
        preferences.setVegan(request.isVegan());
        preferences.setVegetarian(!request.isVegan() && request.isVegetarian());
        preferences.setGlutenFree(request.isGlutenFree());
        preferences.setLactoseFree(request.isLactoseFree());
        preferences.setHighProtein(request.isHighProtein());
        preferences.setCalorieConscious(request.isCalorieConscious());
        preferences.setBudgetFriendly(false);
        preferences.setMaxPrepTimeMinutes(null);
        Integer dailyTarget = request.getDailyCalorieTarget() != null
                ? request.getDailyCalorieTarget()
                : request.getCalorieGoal();
        if (dailyTarget == null) {
            dailyTarget = 2200;
        }
        preferences.setDailyCalorieTarget(dailyTarget);
        preferences.setCalorieGoal(dailyTarget);
    }

    private Set<String> cleanValues(Set<String> values) {
        if (values == null) {
            return new LinkedHashSet<>();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
