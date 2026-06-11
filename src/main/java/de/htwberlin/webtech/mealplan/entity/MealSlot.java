package de.htwberlin.webtech.mealplan.entity;

public enum MealSlot {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK;

    public static MealSlot fromPath(String value) {
        if (value == null || value.isBlank()) {
            return DINNER;
        }
        try {
            return MealSlot.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("mealSlot must be breakfast, lunch, dinner or snack.");
        }
    }
}
