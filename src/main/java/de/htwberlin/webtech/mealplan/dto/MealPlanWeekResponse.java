package de.htwberlin.webtech.mealplan.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class MealPlanWeekResponse {

    private LocalDate weekStart;
    private LocalDate weekEnd;
    private List<MealPlanEntryResponse> entries;
    private Map<String, Integer> caloriesByDate;
    private int totalCalories;

    public LocalDate getWeekStart() {
        return weekStart;
    }

    public void setWeekStart(LocalDate weekStart) {
        this.weekStart = weekStart;
    }

    public LocalDate getWeekEnd() {
        return weekEnd;
    }

    public void setWeekEnd(LocalDate weekEnd) {
        this.weekEnd = weekEnd;
    }

    public List<MealPlanEntryResponse> getEntries() {
        return entries;
    }

    public void setEntries(List<MealPlanEntryResponse> entries) {
        this.entries = entries;
    }

    public Map<String, Integer> getCaloriesByDate() {
        return caloriesByDate;
    }

    public void setCaloriesByDate(Map<String, Integer> caloriesByDate) {
        this.caloriesByDate = caloriesByDate;
    }

    public int getTotalCalories() {
        return totalCalories;
    }

    public void setTotalCalories(int totalCalories) {
        this.totalCalories = totalCalories;
    }
}
