package de.htwberlin.webtech.restaurant.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GeoapifyResponse {

    private List<GeoapifyFeature> features = new ArrayList<>();

    public List<GeoapifyFeature> getFeatures() {
        return features;
    }

    public void setFeatures(List<GeoapifyFeature> features) {
        this.features = features == null ? new ArrayList<>() : features;
    }
}
