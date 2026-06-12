package de.htwberlin.webtech.mealplan.service;

import de.htwberlin.webtech.mealplan.dto.MealPlanEntryRequest;
import de.htwberlin.webtech.mealplan.entity.MealPlan;
import de.htwberlin.webtech.mealplan.entity.MealSlot;
import de.htwberlin.webtech.mealplan.exception.MealPlanEntryNotFoundException;
import de.htwberlin.webtech.mealplan.repository.MealPlanRepository;
import de.htwberlin.webtech.recipe.entity.Recipe;
import de.htwberlin.webtech.recipe.exception.RecipeNotFoundException;
import de.htwberlin.webtech.recipe.repository.RecipeRepository;
import de.htwberlin.webtech.shared.exception.ForbiddenException;
import de.htwberlin.webtech.user.entity.AppUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@ApplicationScoped
public class MealPlanService {

    private final MealPlanRepository mealPlanRepository;
    private final RecipeRepository recipeRepository;

    public MealPlanService(MealPlanRepository mealPlanRepository, RecipeRepository recipeRepository) {
        this.mealPlanRepository = mealPlanRepository;
        this.recipeRepository = recipeRepository;
    }

    public List<MealPlan> getWeek(AppUser currentUser, LocalDate startDate) {
        LocalDate weekStart = normalizeWeekStart(startDate);
        LocalDate weekEnd = weekStart.plusDays(6);
        return mealPlanRepository.findByOwnerAndPlannedDateBetween(currentUser, weekStart, weekEnd);
    }

    @Transactional
    public MealPlan setRecipeForDay(AppUser currentUser, LocalDate plannedDate, MealPlanEntryRequest request) {
        return setRecipeForSlot(currentUser, plannedDate, MealSlot.DINNER, request);
    }

    @Transactional
    public MealPlan setRecipeForSlot(AppUser currentUser, LocalDate plannedDate, MealSlot mealSlot, MealPlanEntryRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("recipeId or customTitle must be provided.");
        }
        Recipe recipe = null;
        String customTitle = cleanCustomTitle(request.getCustomTitle());
        if (request.getRecipeId() == null && customTitle == null) {
            throw new IllegalArgumentException("recipeId or customTitle must be provided.");
        }
        if (request.getRecipeId() != null) {
            recipe = recipeRepository.findById(request.getRecipeId());
            if (recipe == null) {
                throw new RecipeNotFoundException(request.getRecipeId());
            }
            ensureRecipeOwner(recipe, currentUser);
            customTitle = null;
        }

        MealPlan mealPlan = findEntry(currentUser, plannedDate, mealSlot)
                .orElseGet(() -> {
                    MealPlan created = new MealPlan();
                    created.setOwner(currentUser);
                    created.setPlannedDate(plannedDate);
                    created.setMealSlot(mealSlot);
                    return created;
                });
        mealPlan.setRecipe(recipe);
        mealPlan.setCustomTitle(customTitle);
        mealPlan.setMealSlot(mealSlot);
        if (mealPlan.getId() == null) {
            mealPlanRepository.persist(mealPlan);
        }
        return mealPlan;
    }

    @Transactional
    public void deleteForDay(AppUser currentUser, LocalDate plannedDate) {
        deleteForSlot(currentUser, plannedDate, MealSlot.DINNER);
    }

    @Transactional
    public void deleteForSlot(AppUser currentUser, LocalDate plannedDate, MealSlot mealSlot) {
        MealPlan mealPlan = findEntry(currentUser, plannedDate, mealSlot)
                .orElseThrow(() -> new MealPlanEntryNotFoundException(plannedDate));
        mealPlanRepository.delete(mealPlan);
    }

    public LocalDate normalizeWeekStart(LocalDate date) {
        LocalDate baseDate = date == null ? LocalDate.now() : date;
        return baseDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private void ensureRecipeOwner(Recipe recipe, AppUser currentUser) {
        if (recipe.getOwner() == null || currentUser == null || currentUser.getId() == null
                || !currentUser.getId().equals(recipe.getOwner().getId())) {
            throw new ForbiddenException("Only own recipes can be planned.");
        }
    }

    private String cleanCustomTitle(String customTitle) {
        if (customTitle == null || customTitle.isBlank()) {
            return null;
        }
        String value = customTitle.trim();
        if (value.length() > 160) {
            throw new IllegalArgumentException("customTitle must be at most 160 characters.");
        }
        return value;
    }

    private java.util.Optional<MealPlan> findEntry(AppUser currentUser, LocalDate plannedDate, MealSlot mealSlot) {
        if (mealSlot == MealSlot.DINNER) {
            return mealPlanRepository.findByOwnerAndPlannedDate(currentUser, plannedDate);
        }
        return mealPlanRepository.findByOwnerAndPlannedDateAndMealSlot(currentUser, plannedDate, mealSlot);
    }
}
