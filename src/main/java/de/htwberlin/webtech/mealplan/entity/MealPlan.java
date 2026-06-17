package de.htwberlin.webtech.mealplan.entity;

import de.htwberlin.webtech.recipe.entity.Recipe;
import de.htwberlin.webtech.user.entity.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uk_meal_plan_owner_planned_date_slot", columnNames = {"owner_id", "planned_date", "meal_slot"})
})
public class MealPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "recipe_id", nullable = true)
    private Recipe recipe;

    @Column(name = "custom_title", length = 160)
    private String customTitle;

    @Column(name = "calories_snapshot")
    private Integer caloriesSnapshot;

    @Column(name = "protein_snapshot")
    private Double proteinSnapshot;

    @Column(name = "image_url_snapshot", length = 1000)
    private String imageUrlSnapshot;

    @Column(name = "external_recipe_id", length = 100)
    private String externalRecipeId;

    @Column(name = "external_source", length = 80)
    private String externalSource;

    @Column(name = "planned_date", nullable = false)
    private LocalDate plannedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_slot")
    private MealSlot mealSlot = MealSlot.DINNER;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (mealSlot == null) {
            mealSlot = MealSlot.DINNER;
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

    public Recipe getRecipe() {
        return recipe;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }

    public String getCustomTitle() {
        return customTitle;
    }

    public void setCustomTitle(String customTitle) {
        this.customTitle = customTitle;
    }

    public Integer getCaloriesSnapshot() {
        return caloriesSnapshot;
    }

    public void setCaloriesSnapshot(Integer caloriesSnapshot) {
        this.caloriesSnapshot = caloriesSnapshot;
    }

    public Double getProteinSnapshot() {
        return proteinSnapshot;
    }

    public void setProteinSnapshot(Double proteinSnapshot) {
        this.proteinSnapshot = proteinSnapshot;
    }

    public String getImageUrlSnapshot() {
        return imageUrlSnapshot;
    }

    public void setImageUrlSnapshot(String imageUrlSnapshot) {
        this.imageUrlSnapshot = imageUrlSnapshot;
    }

    public String getExternalRecipeId() {
        return externalRecipeId;
    }

    public void setExternalRecipeId(String externalRecipeId) {
        this.externalRecipeId = externalRecipeId;
    }

    public String getExternalSource() {
        return externalSource;
    }

    public void setExternalSource(String externalSource) {
        this.externalSource = externalSource;
    }

    public LocalDate getPlannedDate() {
        return plannedDate;
    }

    public void setPlannedDate(LocalDate plannedDate) {
        this.plannedDate = plannedDate;
    }

    public MealSlot getMealSlot() {
        return mealSlot == null ? MealSlot.DINNER : mealSlot;
    }

    public void setMealSlot(MealSlot mealSlot) {
        this.mealSlot = mealSlot == null ? MealSlot.DINNER : mealSlot;
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
