package de.htwberlin.webtech.mealplan;

import de.htwberlin.webtech.mealplan.entity.MealPlan;
import de.htwberlin.webtech.mealplan.entity.MealSlot;
import de.htwberlin.webtech.mealplan.repository.MealPlanRepository;
import de.htwberlin.webtech.recipe.PostgresDevServicesTestProfile;
import de.htwberlin.webtech.recipe.entity.Recipe;
import de.htwberlin.webtech.recipe.repository.RecipeRepository;
import de.htwberlin.webtech.user.entity.AppUser;
import de.htwberlin.webtech.user.entity.Role;
import de.htwberlin.webtech.user.repository.AppUserRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
@TestProfile(PostgresDevServicesTestProfile.class)
class MealPlanRepositoryIntegrationTest {

    @Inject
    MealPlanRepository repository;

    @Inject
    RecipeRepository recipeRepository;

    @Inject
    AppUserRepository userRepository;

    @Test
    @TestTransaction
    void should_save_custom_title_without_recipe() {
        AppUser owner = user("mealplan-custom-owner", "mealplan-custom-owner@example.com");
        userRepository.persistAndFlush(owner);
        LocalDate date = LocalDate.of(2026, 6, 8);

        for (MealSlot slot : MealSlot.values()) {
            MealPlan entry = mealPlan(owner, date, slot);
            entry.setCustomTitle("Lasagna Silvia " + slot.name());
            repository.persist(entry);
        }
        repository.flush();

        var entries = repository.findByOwnerAndPlannedDateBetween(owner, date, date);

        assertEquals(4, entries.size());
        for (MealPlan entry : entries) {
            assertNotNull(entry.getId());
            assertNull(entry.getRecipe());
            assertEquals("Lasagna Silvia " + entry.getMealSlot().name(), entry.getCustomTitle());
        }
    }

    @Test
    @TestTransaction
    void should_save_multiple_slots_for_same_day() {
        AppUser owner = user("mealplan-slots-owner", "mealplan-slots-owner@example.com");
        userRepository.persistAndFlush(owner);
        Recipe recipe = recipe("Owned Lunch", owner);
        recipeRepository.persistAndFlush(recipe);
        LocalDate date = LocalDate.of(2026, 6, 8);
        MealPlan breakfast = mealPlan(owner, date, MealSlot.BREAKFAST);
        breakfast.setCustomTitle("Free Breakfast");
        MealPlan lunch = mealPlan(owner, date, MealSlot.LUNCH);
        lunch.setRecipe(recipe);

        repository.persist(breakfast);
        repository.persist(lunch);
        repository.flush();

        var entries = repository.findByOwnerAndPlannedDateBetween(owner, date, date);

        assertEquals(2, entries.size());
        assertEquals(MealSlot.BREAKFAST, entries.get(0).getMealSlot());
        assertEquals(MealSlot.LUNCH, entries.get(1).getMealSlot());
        assertEquals("Free Breakfast", entries.get(0).getCustomTitle());
        assertEquals("Owned Lunch", entries.get(1).getRecipe().getTitle());
    }

    private MealPlan mealPlan(AppUser owner, LocalDate plannedDate, MealSlot mealSlot) {
        MealPlan entry = new MealPlan();
        entry.setOwner(owner);
        entry.setPlannedDate(plannedDate);
        entry.setMealSlot(mealSlot);
        return entry;
    }

    private Recipe recipe(String title, AppUser owner) {
        Recipe recipe = new Recipe(
                title,
                "",
                10,
                20,
                2,
                "easy",
                "Italian",
                4.5,
                "noodles",
                "cook",
                false,
                true
        );
        recipe.setOwner(owner);
        return recipe;
    }

    private AppUser user(String username, String email) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash("$2a$10$123456789012345678901uPj6z5oE0l3m6QmS1uJ9p4K7yJrB9xjG");
        user.setRole(Role.USER);
        return user;
    }
}
