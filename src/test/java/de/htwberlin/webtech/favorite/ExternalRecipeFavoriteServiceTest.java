package de.htwberlin.webtech.favorite;

import de.htwberlin.webtech.favorite.repository.ExternalRecipeFavoriteRepository;
import de.htwberlin.webtech.favorite.service.ExternalRecipeFavoriteService;
import de.htwberlin.webtech.user.entity.AppUser;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.Mockito.mock;
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
}
