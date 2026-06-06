package de.htwberlin.webtech.recipe.external.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TheMealDbMeal {

    private String idMeal;
    private String strMeal;
    private String strMealThumb;
    private String strCategory;
    private String strArea;
    private String strInstructions;
    private final Map<String, String> numberedFields = new HashMap<>();

    @JsonAnySetter
    public void putNumberedField(String name, Object value) {
        if (name.startsWith("strIngredient") || name.startsWith("strMeasure")) {
            numberedFields.put(name, value == null ? "" : value.toString());
        }
    }

    public String getIngredient(int number) {
        return numberedFields.get("strIngredient" + number);
    }

    public String getMeasure(int number) {
        return numberedFields.get("strMeasure" + number);
    }

    public String getIdMeal() {
        return idMeal;
    }

    public void setIdMeal(String idMeal) {
        this.idMeal = idMeal;
    }

    public String getStrMeal() {
        return strMeal;
    }

    public void setStrMeal(String strMeal) {
        this.strMeal = strMeal;
    }

    public String getStrMealThumb() {
        return strMealThumb;
    }

    public void setStrMealThumb(String strMealThumb) {
        this.strMealThumb = strMealThumb;
    }

    public String getStrCategory() {
        return strCategory;
    }

    public void setStrCategory(String strCategory) {
        this.strCategory = strCategory;
    }

    public String getStrArea() {
        return strArea;
    }

    public void setStrArea(String strArea) {
        this.strArea = strArea;
    }

    public String getStrInstructions() {
        return strInstructions;
    }

    public void setStrInstructions(String strInstructions) {
        this.strInstructions = strInstructions;
    }
}
