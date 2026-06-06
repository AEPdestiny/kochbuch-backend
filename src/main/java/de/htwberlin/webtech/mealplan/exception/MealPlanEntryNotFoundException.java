package de.htwberlin.webtech.mealplan.exception;

import java.time.LocalDate;

public class MealPlanEntryNotFoundException extends RuntimeException {

    public MealPlanEntryNotFoundException(LocalDate plannedDate) {
        super("Meal plan entry for date " + plannedDate + " not found.");
    }
}
