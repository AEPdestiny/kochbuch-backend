package de.htwberlin.webtech.pantry.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class PantryItemRequest {

    @NotBlank(message = "must not be blank")
    private String name;

    @DecimalMin(value = "0.0", inclusive = true, message = "must be greater than or equal to 0")
    @NotNull(message = "must not be null")
    private BigDecimal quantity;

    private String unit;
    private String category;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
