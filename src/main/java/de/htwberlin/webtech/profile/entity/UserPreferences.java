package de.htwberlin.webtech.profile.entity;

import de.htwberlin.webtech.user.entity.AppUser;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(
        name = "user_preferences",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_preferences_owner", columnNames = "owner_id")
)
public class UserPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_preference_likes", joinColumns = @JoinColumn(name = "preferences_id"))
    @Column(name = "value", nullable = false)
    private Set<String> likes = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_preference_dislikes", joinColumns = @JoinColumn(name = "preferences_id"))
    @Column(name = "value", nullable = false)
    private Set<String> dislikes = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_preference_allergies", joinColumns = @JoinColumn(name = "preferences_id"))
    @Column(name = "value", nullable = false)
    private Set<String> allergies = new LinkedHashSet<>();

    @Column(nullable = false)
    private boolean vegan;

    @Column(nullable = false)
    private boolean vegetarian;

    @Column(nullable = false)
    private boolean glutenFree;

    @Column(nullable = false)
    private boolean lactoseFree;

    @Column(nullable = false)
    private boolean highProtein;

    @Column(nullable = false)
    private boolean calorieConscious;

    @Column(nullable = false)
    private boolean budgetFriendly;

    private Integer maxPrepTimeMinutes;
    private Integer calorieGoal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserGoal goal = UserGoal.MAINTAIN;

    private Integer dailyCalorieTarget;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (goal == null) {
            goal = UserGoal.MAINTAIN;
        }
        if (dailyCalorieTarget == null && calorieGoal != null) {
            dailyCalorieTarget = calorieGoal;
        }
        if (calorieGoal == null && dailyCalorieTarget != null) {
            calorieGoal = dailyCalorieTarget;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AppUser getOwner() {
        return owner;
    }

    public void setOwner(AppUser owner) {
        this.owner = owner;
    }

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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
