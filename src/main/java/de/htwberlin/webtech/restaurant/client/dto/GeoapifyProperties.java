package de.htwberlin.webtech.restaurant.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GeoapifyProperties {

    private String name;
    private String formatted;
    private Integer distance;
    private List<String> categories = new ArrayList<>();

    @JsonProperty("address_line1")
    private String addressLine1;

    private String city;
    private String town;
    private String village;
    private String county;
    private String country;

    @JsonProperty("country_code")
    private String countryCode;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFormatted() {
        return formatted;
    }

    public void setFormatted(String formatted) {
        this.formatted = formatted;
    }

    public Integer getDistance() {
        return distance;
    }

    public void setDistance(Integer distance) {
        this.distance = distance;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories == null ? new ArrayList<>() : categories;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getTown() { return town; }
    public void setTown(String town) { this.town = town; }

    public String getVillage() { return village; }
    public void setVillage(String village) { this.village = village; }

    public String getCounty() { return county; }
    public void setCounty(String county) { this.county = county; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
}
