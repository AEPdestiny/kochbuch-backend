package de.htwberlin.webtech.mealplan.dto;

import java.time.LocalDate;
import java.util.List;

public class MealPlanWeekResponse {

    private LocalDate weekStart;
    private LocalDate weekEnd;
    private List<MealPlanEntryResponse> entries;

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
}
