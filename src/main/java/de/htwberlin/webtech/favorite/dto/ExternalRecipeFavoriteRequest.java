package de.htwberlin.webtech.favorite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ExternalRecipeFavoriteRequest {

    @NotBlank
    @Size(max = 80)
    private String externalRecipeId;

    @NotBlank
    @Size(max = 255)
    private String externalTitle;

    @Size(max = 1000)
    private String externalImageUrl;

    @Size(max = 40)
    private String externalSource = "SPOONACULAR";

    public String getExternalRecipeId() {
        return externalRecipeId;
    }

    public void setExternalRecipeId(String externalRecipeId) {
        this.externalRecipeId = externalRecipeId;
    }

    public String getExternalTitle() {
        return externalTitle;
    }

    public void setExternalTitle(String externalTitle) {
        this.externalTitle = externalTitle;
    }

    public String getExternalImageUrl() {
        return externalImageUrl;
    }

    public void setExternalImageUrl(String externalImageUrl) {
        this.externalImageUrl = externalImageUrl;
    }

    public String getExternalSource() {
        return externalSource;
    }

    public void setExternalSource(String externalSource) {
        this.externalSource = externalSource;
    }
}
