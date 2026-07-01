package de.htwberlin.webtech.restaurant.dto;

import java.util.List;

public class TavilyRestaurantSearchResponse {

    private String status;
    private List<RestaurantResponse> results;
    private String resolvedLocation;
    private String searchMode;

    public TavilyRestaurantSearchResponse(String status, List<RestaurantResponse> results) {
        this.status = status;
        this.results = results;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<RestaurantResponse> getResults() { return results; }
    public void setResults(List<RestaurantResponse> results) { this.results = results; }

    public String getResolvedLocation() { return resolvedLocation; }
    public void setResolvedLocation(String resolvedLocation) { this.resolvedLocation = resolvedLocation; }

    /** "exact" when results match the exact dish, "suggestions" for general cuisine fallback, null otherwise. */
    public String getSearchMode() { return searchMode; }
    public void setSearchMode(String searchMode) { this.searchMode = searchMode; }
}
