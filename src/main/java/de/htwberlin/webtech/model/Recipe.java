package de.htwberlin.webtech.model;

public class Recipe {
    private String id;
    private String title;
    private String ingredients;
    private boolean favorite;

    public Recipe() {}

    public Recipe(String id, String title, String ingredients, boolean favorite) {
        this.id = id;
        this.title = title;
        this.ingredients = ingredients;
        this.favorite = favorite;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getIngredients() { return ingredients; }
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }

    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }
}

