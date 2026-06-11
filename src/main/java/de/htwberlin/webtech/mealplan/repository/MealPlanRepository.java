package de.htwberlin.webtech.mealplan.repository;

import de.htwberlin.webtech.mealplan.entity.MealPlan;
import de.htwberlin.webtech.mealplan.entity.MealSlot;
import de.htwberlin.webtech.user.entity.AppUser;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class MealPlanRepository implements PanacheRepository<MealPlan> {

    public List<MealPlan> findByOwnerAndPlannedDateBetween(AppUser owner, LocalDate weekStart, LocalDate weekEnd) {
        return list("owner = ?1 and plannedDate between ?2 and ?3 order by plannedDate, mealSlot", owner, weekStart, weekEnd);
    }

    public Optional<MealPlan> findByOwnerAndPlannedDate(AppUser owner, LocalDate plannedDate) {
        return findByOwnerAndPlannedDateAndMealSlot(owner, plannedDate, MealSlot.DINNER);
    }

    public Optional<MealPlan> findByOwnerAndPlannedDateAndMealSlot(AppUser owner, LocalDate plannedDate, MealSlot mealSlot) {
        return find("owner = ?1 and plannedDate = ?2 and (mealSlot = ?3 or (mealSlot is null and ?3 = ?4))",
                owner,
                plannedDate,
                mealSlot,
                MealSlot.DINNER
        ).firstResultOptional();
    }
}
