package de.htwberlin.webtech.favorite.dto;

import java.time.Instant;

public class ExternalRecipeFavoriteResponse {

    private Long id;
    private String externalRecipeId;
    private String externalTitle;
    private String externalImageUrl;
    private String externalSource;
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
