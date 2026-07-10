package de.htwberlin.webtech.favorite;

import de.htwberlin.webtech.favorite.entity.ExternalRecipeFavorite;
import de.htwberlin.webtech.favorite.repository.ExternalRecipeFavoriteRepository;
import de.htwberlin.webtech.favorite.service.ExternalRecipeFavoriteService;
import de.htwberlin.webtech.user.entity.AppUser;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExternalRecipeFavoriteServiceTest {

    @Test
    void remove_normalizes_source_before_deleting_external_favorite() {
        ExternalRecipeFavoriteRepository repository = mock(ExternalRecipeFavoriteRepository.class);
        ExternalRecipeFavoriteService service = new ExternalRecipeFavoriteService(repository);
        AppUser owner = new AppUser();

        when(repository.findByOwnerAndSourceAndExternalRecipeId(owner, "SPOONACULAR", "716429"))
                .thenReturn(Optional.empty());

        service.remove(owner, "spoonacular", "716429");

        verify(repository).findByOwnerAndSourceAndExternalRecipeId(owner, "SPOONACULAR", "716429");
    }

    @Test
    void removeById_deletes_broken_external_favorite_for_owner() {
        ExternalRecipeFavoriteRepository repository = mock(ExternalRecipeFavoriteRepository.class);
        ExternalRecipeFavoriteService service = new ExternalRecipeFavoriteService(repository);
        AppUser owner = user(1L);
        ExternalRecipeFavorite favorite = favorite(owner);

        when(repository.findByIdOptional(10L)).thenReturn(Optional.of(favorite));

        service.removeById(owner, 10L);

        verify(repository).findByIdOptional(10L);
        verify(repository).delete(favorite);
    }

    @Test
    void removeById_does_not_delete_foreign_external_favorite() {
        ExternalRecipeFavoriteRepository repository = mock(ExternalRecipeFavoriteRepository.class);
        ExternalRecipeFavoriteService service = new ExternalRecipeFavoriteService(repository);
        AppUser owner = user(1L);
        ExternalRecipeFavorite favorite = favorite(user(2L));

        when(repository.findByIdOptional(10L)).thenReturn(Optional.of(favorite));

        service.removeById(owner, 10L);

        verify(repository).findByIdOptional(10L);
        verify(repository, never()).delete(favorite);
    }

    private ExternalRecipeFavorite favorite(AppUser owner) {
        ExternalRecipeFavorite favorite = new ExternalRecipeFavorite();
        favorite.setOwner(owner);
        favorite.setExternalRecipeId("");
        favorite.setExternalTitle("");
        favorite.setExternalSource("SPOONACULAR");
        return favorite;
    }

    private AppUser user(Long id) {
        AppUser owner = new AppUser();
        owner.setId(id);
        return owner;
    }
}
