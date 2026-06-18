package de.htwberlin.webtech.recipe.entity;

import de.htwberlin.webtech.user.entity.AppUser;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Entity
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank(message = "must not be blank")
    @Column(length = 500)
    private String title;
    @Column(length = 1000)
    private String imageUrl;
    @Min(value = 0, message = "must be greater than or equal to 0")
    private int prepTimeMinutes;
    @Min(value = 0, message = "must be greater than or equal to 0")
    private int cookTimeMinutes;
    @Min(value = 0, message = "must be greater than or equal to 0")
    private int servings;
    private String difficulty;
    private String category;
    @Min(value = 0, message = "must be greater than or equal to 0")
    @Max(value = 5, message = "must be less than or equal to 5")
    private double rating;
    @Column(columnDefinition = "TEXT")
    private String ingredients;
    @Column(columnDefinition = "TEXT")
    private String instructions;
    private boolean favorite;
    private boolean published;
    private Integer calories;
    private Double protein;
    private String language = "en";
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private AppUser owner;

    public Recipe() {
    }

    public Recipe(String title,
                  String imageUrl,
                  int prepTimeMinutes,
                  int cookTimeMinutes,
                  int servings,
                  String difficulty,
                  String category,
                  double rating,
                  String ingredients,
                  String instructions,
                  boolean favorite,
                  boolean published) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.prepTimeMinutes = prepTimeMinutes;
        this.cookTimeMinutes = cookTimeMinutes;
        this.servings = servings;
        this.difficulty = difficulty;
        this.category = category;
        this.rating = rating;
        this.ingredients = ingredients;
        this.instructions = instructions;
        this.favorite = favorite;
        this.published = published;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getPrepTimeMinutes() {
        return prepTimeMinutes;
    }

    public void setPrepTimeMinutes(int prepTimeMinutes) {
        this.prepTimeMinutes = prepTimeMinutes;
    }

    public int getCookTimeMinutes() {
        return cookTimeMinutes;
    }

    public void setCookTimeMinutes(int cookTimeMinutes) {
        this.cookTimeMinutes = cookTimeMinutes;
    }

    public int getServings() {
        return servings;
    }

    public void setServings(int servings) {
        this.servings = servings;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getIngredients() {
        return ingredients;
    }

    public void setIngredients(String ingredients) {
        this.ingredients = ingredients;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public Integer getCalories() {
        return calories;
    }

    public void setCalories(Integer calories) {
        this.calories = calories;
    }

    public Double getProtein() {
        return protein;
    }

    public void setProtein(Double protein) {
        this.protein = protein;
    }

    public String getLanguage() {
        return language == null || language.isBlank() ? "en" : language;
    }

    public void setLanguage(String language) {
        this.language = language == null || language.isBlank() ? "en" : language.trim().toLowerCase();
    }

    public AppUser getOwner() {
        return owner;
    }

    public void setOwner(AppUser owner) {
        this.owner = owner;
    }
}
