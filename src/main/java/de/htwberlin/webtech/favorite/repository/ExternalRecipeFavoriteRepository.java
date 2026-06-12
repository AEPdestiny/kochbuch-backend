package de.htwberlin.webtech.favorite.repository;

import de.htwberlin.webtech.favorite.entity.ExternalRecipeFavorite;
import de.htwberlin.webtech.user.entity.AppUser;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ExternalRecipeFavoriteRepository implements PanacheRepository<ExternalRecipeFavorite> {

    public List<ExternalRecipeFavorite> findByOwner(AppUser owner) {
        return list("owner = ?1 order by createdAt desc", owner);
    }

    public Optional<ExternalRecipeFavorite> findByOwnerAndSourceAndExternalRecipeId(AppUser owner, String source, String externalRecipeId) {
        return find("owner = ?1 and externalSource = ?2 and externalRecipeId = ?3", owner, source, externalRecipeId)
                .firstResultOptional();
    }
}
