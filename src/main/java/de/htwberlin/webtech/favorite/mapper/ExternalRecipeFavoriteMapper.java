package de.htwberlin.webtech.favorite.mapper;

import de.htwberlin.webtech.favorite.dto.ExternalRecipeFavoriteResponse;
import de.htwberlin.webtech.favorite.entity.ExternalRecipeFavorite;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ExternalRecipeFavoriteMapper {

    public ExternalRecipeFavoriteResponse toResponse(ExternalRecipeFavorite favorite) {
        ExternalRecipeFavoriteResponse response = new ExternalRecipeFavoriteResponse();
        response.setId(favorite.getId());
        response.setExternalRecipeId(favorite.getExternalRecipeId());
        response.setExternalTitle(favorite.getExternalTitle());
        response.setExternalImageUrl(favorite.getExternalImageUrl());
        response.setExternalSource(favorite.getExternalSource());
        response.setCreatedAt(favorite.getCreatedAt());
        return response;
    }

    public List<ExternalRecipeFavoriteResponse> toResponseList(List<ExternalRecipeFavorite> favorites) {
        return favorites.stream().map(this::toResponse).toList();
    }
}
