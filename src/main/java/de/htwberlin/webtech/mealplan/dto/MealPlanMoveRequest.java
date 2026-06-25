package de.htwberlin.webtech.mealplan.dto;

import java.time.LocalDate;

public class MealPlanMoveRequest {

    private LocalDate targetDate;
    private String targetSlot;
    private boolean swapIfOccupied = true;

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public String getTargetSlot() {
        return targetSlot;
    }

    public void setTargetSlot(String targetSlot) {
        this.targetSlot = targetSlot;
    }

    public boolean isSwapIfOccupied() {
        return swapIfOccupied;
    }

    public void setSwapIfOccupied(boolean swapIfOccupied) {
        this.swapIfOccupied = swapIfOccupied;
    }
}
