package de.htwberlin.webtech.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Entität für ein Rezept.
 * Wird direkt in der Datenbank als Tabelle gespeichert.
 */
@Entity
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; //automatisch generierte ID
    private String title; //Titel des Rezeptes
    private String imageUrl; //URL zu einem Rezeptbild
    private int prepTimeMinutes; //Vorbereitungszeit
    private int cookTimeMinutes; //Koch-Backzeit
    private int servings; //Anzahl der Portionen, für die das Rezept gedacht ist
    private String difficulty; //Schwierigkeitsgrad des Rezeptes
    private String category; //Kategorie oder Küche
    private double rating; //Bewertungsdurchschnitt
    private String ingredients; //Zutaten
    private String instructions; //Schritt-für-Schritt-Anleitung
    private boolean favorite; //Kennzeichnet, ob das Rezept vom Nutzer als Favorit markiert wurde
    private boolean published; //Gibt an, ob das Rezept öffentlich im Frontend angezeigt werden soll


    public Recipe() {
    }
    //Konstruktor
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
    //Getter und Setter
    public Long getId() {
        return id; }

    public String getTitle() {
        return title; }

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
}
