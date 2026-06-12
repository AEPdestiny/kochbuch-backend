package de.htwberlin.webtech.favorite.service;

import de.htwberlin.webtech.favorite.dto.ExternalRecipeFavoriteRequest;
import de.htwberlin.webtech.favorite.entity.ExternalRecipeFavorite;
import de.htwberlin.webtech.favorite.repository.ExternalRecipeFavoriteRepository;
import de.htwberlin.webtech.user.entity.AppUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class ExternalRecipeFavoriteService {

    private final ExternalRecipeFavoriteRepository repository;

    public ExternalRecipeFavoriteService(ExternalRecipeFavoriteRepository repository) {
        this.repository = repository;
    }

    public List<ExternalRecipeFavorite> listMine(AppUser owner) {
        return repository.findByOwner(owner);
    }

    @Transactional
    public ExternalRecipeFavorite add(AppUser owner, ExternalRecipeFavoriteRequest request) {
        String source = normalizeSource(request.getExternalSource());
        String externalRecipeId = requireText(request.getExternalRecipeId(), "externalRecipeId must not be blank.");
        String title = requireText(request.getExternalTitle(), "externalTitle must not be blank.");
        return repository.findByOwnerAndSourceAndExternalRecipeId(owner, source, externalRecipeId)
                .map(existing -> update(existing, title, request.getExternalImageUrl(), source))
                .orElseGet(() -> create(owner, externalRecipeId, title, request.getExternalImageUrl(), source));
    }

    @Transactional
    public void remove(AppUser owner, String source, String externalRecipeId) {
        repository.findByOwnerAndSourceAndExternalRecipeId(owner, normalizeSource(source), externalRecipeId)
                .ifPresent(repository::delete);
    }

    private ExternalRecipeFavorite create(AppUser owner, String externalRecipeId, String title, String imageUrl, String source) {
        ExternalRecipeFavorite favorite = new ExternalRecipeFavorite();
        favorite.setOwner(owner);
        favorite.setExternalRecipeId(externalRecipeId);
        favorite.setExternalTitle(title);
        favorite.setExternalImageUrl(blankToNull(imageUrl));
        favorite.setExternalSource(source);
        repository.persist(favorite);
        return favorite;
    }

    private ExternalRecipeFavorite update(ExternalRecipeFavorite favorite, String title, String imageUrl, String source) {
        favorite.setExternalTitle(title);
        favorite.setExternalImageUrl(blankToNull(imageUrl));
        favorite.setExternalSource(source);
        return favorite;
    }

    private String normalizeSource(String source) {
        return source == null || source.isBlank() ? "SPOONACULAR" : source.trim().toUpperCase();
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
