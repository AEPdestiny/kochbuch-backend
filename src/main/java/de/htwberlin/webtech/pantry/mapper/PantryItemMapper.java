package de.htwberlin.webtech.pantry.mapper;

import de.htwberlin.webtech.pantry.dto.PantryItemRequest;
import de.htwberlin.webtech.pantry.dto.PantryItemResponse;
import de.htwberlin.webtech.pantry.entity.PantryItem;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class PantryItemMapper {

    public PantryItem toEntity(PantryItemRequest request) {
        PantryItem item = new PantryItem();
        updateEntity(item, request);
        return item;
    }

    public PantryItemResponse toResponse(PantryItem item) {
        PantryItemResponse response = new PantryItemResponse();
        response.setId(item.getId());
        response.setName(item.getName());
        response.setQuantity(item.getQuantity());
        response.setUnit(item.getUnit());
        response.setCategory(item.getCategory());
        response.setCreatedAt(item.getCreatedAt());
        response.setUpdatedAt(item.getUpdatedAt());
        return response;
    }

    public List<PantryItemResponse> toResponseList(List<PantryItem> items) {
        return items.stream()
                .map(this::toResponse)
                .toList();
    }

    public void updateEntity(PantryItem item, PantryItemRequest request) {
        item.setName(request.getName());
        item.setQuantity(request.getQuantity());
        item.setUnit(request.getUnit());
        item.setCategory(request.getCategory());
    }
}
