package de.htwberlin.webtech.shopping.mapper;

import de.htwberlin.webtech.shopping.dto.ShoppingListItemRequest;
import de.htwberlin.webtech.shopping.dto.ShoppingListItemResponse;
import de.htwberlin.webtech.shopping.entity.ShoppingListItem;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ShoppingListItemMapper {

    public ShoppingListItem toEntity(ShoppingListItemRequest request) {
        ShoppingListItem item = new ShoppingListItem();
        updateEntity(item, request);
        return item;
    }

    public ShoppingListItemResponse toResponse(ShoppingListItem item) {
        ShoppingListItemResponse response = new ShoppingListItemResponse();
        response.setId(item.getId());
        response.setName(item.getName());
        response.setQuantity(item.getQuantity());
        response.setUnit(item.getUnit());
        response.setCategory(item.getCategory());
        response.setRecipeId(item.getRecipeId());
        response.setRecipeTitle(item.getRecipeTitle());
        response.setChecked(item.isChecked());
        response.setCreatedAt(item.getCreatedAt());
        response.setUpdatedAt(item.getUpdatedAt());
        return response;
    }

    public List<ShoppingListItemResponse> toResponseList(List<ShoppingListItem> items) {
        return items.stream()
                .map(this::toResponse)
                .toList();
    }

    public void updateEntity(ShoppingListItem item, ShoppingListItemRequest request) {
        item.setName(request.getName());
        item.setQuantity(request.getQuantity());
        item.setUnit(request.getUnit());
        item.setCategory(request.getCategory());
        item.setRecipeId(request.getRecipeId());
        item.setRecipeTitle(request.getRecipeTitle());
        item.setChecked(request.isChecked());
    }
}
