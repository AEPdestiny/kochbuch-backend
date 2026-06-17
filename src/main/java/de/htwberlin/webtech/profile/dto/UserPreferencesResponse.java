package de.htwberlin.webtech.profile.dto;

import de.htwberlin.webtech.profile.entity.UserGoal;

import java.util.LinkedHashSet;
import java.util.Set;

public class UserPreferencesResponse {

    private Set<String> likes = new LinkedHashSet<>();
    private Set<String> dislikes = new LinkedHashSet<>();
    private Set<String> allergies = new LinkedHashSet<>();
    private boolean vegan;
    private boolean vegetarian;
    private boolean glutenFree;
    private boolean lactoseFree;
    private boolean highProtein;
    private boolean calorieConscious;
    private boolean budgetFriendly;
    private Integer maxPrepTimeMinutes;
    private Integer calorieGoal;
    private UserGoal goal = UserGoal.MAINTAIN;
    private Integer dailyCalorieTarget;

    public Set<String> getLikes() {
        return likes;
    }

    public void setLikes(Set<String> likes) {
        this.likes = likes == null ? new LinkedHashSet<>() : likes;
    }

    public Set<String> getDislikes() {
        return dislikes;
    }

    public void setDislikes(Set<String> dislikes) {
        this.dislikes = dislikes == null ? new LinkedHashSet<>() : dislikes;
    }

    public Set<String> getAllergies() {
        return allergies;
    }

    public void setAllergies(Set<String> allergies) {
        this.allergies = allergies == null ? new LinkedHashSet<>() : allergies;
    }

    public boolean isVegan() {
        return vegan;
    }

    public void setVegan(boolean vegan) {
        this.vegan = vegan;
    }

    public boolean isVegetarian() {
        return vegetarian;
    }

    public void setVegetarian(boolean vegetarian) {
        this.vegetarian = vegetarian;
    }

    public boolean isGlutenFree() {
        return glutenFree;
    }

    public void setGlutenFree(boolean glutenFree) {
        this.glutenFree = glutenFree;
    }

    public boolean isLactoseFree() {
        return lactoseFree;
    }

    public void setLactoseFree(boolean lactoseFree) {
        this.lactoseFree = lactoseFree;
    }

    public boolean isHighProtein() {
        return highProtein;
    }

    public void setHighProtein(boolean highProtein) {
        this.highProtein = highProtein;
    }

    public boolean isCalorieConscious() {
        return calorieConscious;
    }

    public void setCalorieConscious(boolean calorieConscious) {
        this.calorieConscious = calorieConscious;
    }

    public boolean isBudgetFriendly() {
        return budgetFriendly;
    }

    public void setBudgetFriendly(boolean budgetFriendly) {
        this.budgetFriendly = budgetFriendly;
    }

    public Integer getMaxPrepTimeMinutes() {
        return maxPrepTimeMinutes;
    }

    public void setMaxPrepTimeMinutes(Integer maxPrepTimeMinutes) {
        this.maxPrepTimeMinutes = maxPrepTimeMinutes;
    }

    public Integer getCalorieGoal() {
        return calorieGoal;
    }

    public void setCalorieGoal(Integer calorieGoal) {
        this.calorieGoal = calorieGoal;
    }

    public UserGoal getGoal() {
        return goal == null ? UserGoal.MAINTAIN : goal;
    }

    public void setGoal(UserGoal goal) {
        this.goal = goal == null ? UserGoal.MAINTAIN : goal;
    }

    public Integer getDailyCalorieTarget() {
        return dailyCalorieTarget;
    }

    public void setDailyCalorieTarget(Integer dailyCalorieTarget) {
        this.dailyCalorieTarget = dailyCalorieTarget;
    }
}
