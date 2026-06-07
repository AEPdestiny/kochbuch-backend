package de.htwberlin.webtech.restaurant.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GeoapifyFeature {

    private GeoapifyProperties properties;
    private GeoapifyGeometry geometry;

    public GeoapifyProperties getProperties() {
        return properties;
    }

    public void setProperties(GeoapifyProperties properties) {
        this.properties = properties;
    }

    public GeoapifyGeometry getGeometry() {
        return geometry;
    }

    public void setGeometry(GeoapifyGeometry geometry) {
        this.geometry = geometry;
    }
}
