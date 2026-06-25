package de.htwberlin.webtech.recipe.dto;

public class ImageUploadResponse {

    private String imageUrl;

    public ImageUploadResponse() {
    }

    public ImageUploadResponse(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
