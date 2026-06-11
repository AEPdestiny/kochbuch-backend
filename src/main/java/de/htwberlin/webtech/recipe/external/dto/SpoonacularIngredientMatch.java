package de.htwberlin.webtech.recipe.external.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SpoonacularIngredientMatch {

    private Long id;
    private String title;
    private String image;
    private Integer usedIngredientCount;
    private Integer missedIngredientCount;
    private List<SpoonacularIngredient> usedIngredients;
    private List<SpoonacularIngredient> missedIngredients;

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

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Integer getUsedIngredientCount() {
        return usedIngredientCount;
    }

    public void setUsedIngredientCount(Integer usedIngredientCount) {
        this.usedIngredientCount = usedIngredientCount;
    }

    public Integer getMissedIngredientCount() {
        return missedIngredientCount;
    }

    public void setMissedIngredientCount(Integer missedIngredientCount) {
        this.missedIngredientCount = missedIngredientCount;
    }

    public List<SpoonacularIngredient> getUsedIngredients() {
        return usedIngredients;
    }

    public void setUsedIngredients(List<SpoonacularIngredient> usedIngredients) {
        this.usedIngredients = usedIngredients;
    }

    public List<SpoonacularIngredient> getMissedIngredients() {
        return missedIngredients;
    }

    public void setMissedIngredients(List<SpoonacularIngredient> missedIngredients) {
        this.missedIngredients = missedIngredients;
    }
}
