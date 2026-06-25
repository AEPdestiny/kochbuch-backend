package de.htwberlin.webtech.mealplan.dto;

import java.util.ArrayList;
import java.util.List;

public class MealPlanShoppingListResponse {

    private List<MealPlanShoppingListItemResponse> added = new ArrayList<>();
    private List<MealPlanShoppingListItemResponse> skippedBecauseInPantry = new ArrayList<>();
    private List<MealPlanShoppingListItemResponse> needsReview = new ArrayList<>();
    private List<MealPlanShoppingListItemResponse> alreadyOnShoppingList = new ArrayList<>();

    public List<MealPlanShoppingListItemResponse> getAdded() {
        return added;
    }

    public void setAdded(List<MealPlanShoppingListItemResponse> added) {
        this.added = added;
    }

    public List<MealPlanShoppingListItemResponse> getSkippedBecauseInPantry() {
        return skippedBecauseInPantry;
    }

    public void setSkippedBecauseInPantry(List<MealPlanShoppingListItemResponse> skippedBecauseInPantry) {
        this.skippedBecauseInPantry = skippedBecauseInPantry;
    }

    public List<MealPlanShoppingListItemResponse> getNeedsReview() {
        return needsReview;
    }

    public void setNeedsReview(List<MealPlanShoppingListItemResponse> needsReview) {
        this.needsReview = needsReview;
    }

    public List<MealPlanShoppingListItemResponse> getAlreadyOnShoppingList() {
        return alreadyOnShoppingList;
    }

    public void setAlreadyOnShoppingList(List<MealPlanShoppingListItemResponse> alreadyOnShoppingList) {
        this.alreadyOnShoppingList = alreadyOnShoppingList;
    }
}
