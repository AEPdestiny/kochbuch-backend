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
            validateSnapshotValues(request);
        } else {
            validateSnapshotValues(request);
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
        applySnapshots(mealPlan, recipe, request);
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

    @Transactional
    public List<MealPlan> moveEntry(AppUser currentUser,
                                    Long entryId,
                                    LocalDate targetDate,
                                    MealSlot targetSlot,
                                    boolean swapIfOccupied) {
        if (entryId == null || targetDate == null || targetSlot == null) {
            throw new IllegalArgumentException("targetDate and targetSlot must be provided.");
        }

        MealPlan source = mealPlanRepository.findByIdOptional(entryId)
                .orElseThrow(() -> new MealPlanEntryNotFoundException(entryId));
        ensureMealPlanOwner(source, currentUser);

        LocalDate sourceDate = source.getPlannedDate();
        MealSlot sourceSlot = source.getMealSlot();
        if (sourceDate.equals(targetDate) && sourceSlot == targetSlot) {
            return getWeek(currentUser, targetDate);
        }

        MealPlan target = findEntry(currentUser, targetDate, targetSlot)
                .filter(entry -> !entry.getId().equals(source.getId()))
                .orElse(null);

        if (target == null) {
            source.setPlannedDate(targetDate);
            source.setMealSlot(targetSlot);
        } else {
            if (!swapIfOccupied) {
                throw new IllegalArgumentException("Target meal plan slot is already occupied.");
            }
            MealPlanContent sourceContent = MealPlanContent.from(source);
            MealPlanContent targetContent = MealPlanContent.from(target);
            sourceContent.applyTo(target);
            targetContent.applyTo(source);
        }

        return getWeek(currentUser, targetDate);
    }

    public LocalDate normalizeWeekStart(LocalDate date) {
        LocalDate baseDate = date == null ? LocalDate.now() : date;
        return baseDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private void ensureRecipeOwner(Recipe recipe, AppUser currentUser) {
        if (recipe.getOwner() == null) {
            return;
        }
        if (currentUser == null || currentUser.getId() == null
                || !currentUser.getId().equals(recipe.getOwner().getId())) {
            throw new ForbiddenException("Only own recipes can be planned.");
        }
    }

    private void ensureMealPlanOwner(MealPlan mealPlan, AppUser currentUser) {
        if (mealPlan.getOwner() == null || currentUser == null || currentUser.getId() == null
                || !currentUser.getId().equals(mealPlan.getOwner().getId())) {
            throw new ForbiddenException("Only own meal plan entries can be moved.");
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

    private void validateSnapshotValues(MealPlanEntryRequest request) {
        if (request.getCaloriesSnapshot() != null && request.getCaloriesSnapshot() < 0) {
            throw new IllegalArgumentException("caloriesSnapshot must be greater than or equal to 0.");
        }
        if (request.getProteinSnapshot() != null && request.getProteinSnapshot() < 0) {
            throw new IllegalArgumentException("proteinSnapshot must be greater than or equal to 0.");
        }
    }

    private void applySnapshots(MealPlan mealPlan, Recipe recipe, MealPlanEntryRequest request) {
        if (recipe != null) {
            mealPlan.setCaloriesSnapshot(null);
            mealPlan.setProteinSnapshot(null);
            mealPlan.setImageUrlSnapshot(null);
            mealPlan.setExternalRecipeId(null);
            mealPlan.setExternalSource(null);
            return;
        }
        mealPlan.setCaloriesSnapshot(request.getCaloriesSnapshot());
        mealPlan.setProteinSnapshot(request.getProteinSnapshot());
        mealPlan.setImageUrlSnapshot(cleanOptionalText(request.getImageUrlSnapshot(), 1000, "imageUrlSnapshot"));
        mealPlan.setExternalRecipeId(cleanOptionalText(request.getExternalRecipeId(), 100, "externalRecipeId"));
        mealPlan.setExternalSource(cleanOptionalText(request.getExternalSource(), 80, "externalSource"));
    }

    private String cleanOptionalText(String value, int maxLength, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be at most " + maxLength + " characters.");
        }
        return trimmed;
    }

    private java.util.Optional<MealPlan> findEntry(AppUser currentUser, LocalDate plannedDate, MealSlot mealSlot) {
        if (mealSlot == MealSlot.DINNER) {
            return mealPlanRepository.findByOwnerAndPlannedDate(currentUser, plannedDate);
        }
        return mealPlanRepository.findByOwnerAndPlannedDateAndMealSlot(currentUser, plannedDate, mealSlot);
    }

    private record MealPlanContent(Recipe recipe,
                                   String customTitle,
                                   Integer caloriesSnapshot,
                                   Double proteinSnapshot,
                                   String imageUrlSnapshot,
                                   String externalRecipeId,
                                   String externalSource) {
        static MealPlanContent from(MealPlan mealPlan) {
            return new MealPlanContent(
                    mealPlan.getRecipe(),
                    mealPlan.getCustomTitle(),
                    mealPlan.getCaloriesSnapshot(),
                    mealPlan.getProteinSnapshot(),
                    mealPlan.getImageUrlSnapshot(),
                    mealPlan.getExternalRecipeId(),
                    mealPlan.getExternalSource()
            );
        }

        void applyTo(MealPlan mealPlan) {
            mealPlan.setRecipe(recipe);
            mealPlan.setCustomTitle(customTitle);
            mealPlan.setCaloriesSnapshot(caloriesSnapshot);
            mealPlan.setProteinSnapshot(proteinSnapshot);
            mealPlan.setImageUrlSnapshot(imageUrlSnapshot);
            mealPlan.setExternalRecipeId(externalRecipeId);
            mealPlan.setExternalSource(externalSource);
        }
    }
}
