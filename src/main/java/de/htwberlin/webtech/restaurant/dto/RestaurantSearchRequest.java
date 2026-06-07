package de.htwberlin.webtech.restaurant.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class RestaurantSearchRequest {

    @NotBlank(message = "must not be blank")
    private String query;

    @NotNull(message = "must not be null")
    @DecimalMin(value = "-90.0", message = "must be greater than or equal to -90")
    @DecimalMax(value = "90.0", message = "must be less than or equal to 90")
    private Double latitude;

    @NotNull(message = "must not be null")
    @DecimalMin(value = "-180.0", message = "must be greater than or equal to -180")
    @DecimalMax(value = "180.0", message = "must be less than or equal to 180")
    private Double longitude;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}
