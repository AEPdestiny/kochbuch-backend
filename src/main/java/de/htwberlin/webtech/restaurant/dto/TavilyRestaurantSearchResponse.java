package de.htwberlin.webtech.restaurant.dto;

import java.util.List;

public class TavilyRestaurantSearchResponse {

    private String status;
    private List<RestaurantResponse> results;

    public TavilyRestaurantSearchResponse(String status, List<RestaurantResponse> results) {
        this.status = status;
        this.results = results;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<RestaurantResponse> getResults() {
        return results;
    }

    public void setResults(List<RestaurantResponse> results) {
        this.results = results;
    }
}
