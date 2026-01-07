package de.htwberlin.webtech;

import de.htwberlin.webtech.model.Recipe;
import de.htwberlin.webtech.repo.RecipeRepository;
import de.htwberlin.webtech.service.RecipeService;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

/**Unit-Tests für den RecipeService mit Mockito.*/
@ExtendWith(MockitoExtension.class)
class RecipeServiceTest implements WithAssertions {

    @Mock
    private RecipeRepository repo;

    @InjectMocks
    private RecipeService underTest;

    @Test
    @DisplayName("findAll should delegate to repository")
    void findAll_should_delegate_to_repository() {
        // given: : Repository liefert zwei Beispiel-Rezepte zurück
        var r1 = new Recipe("Pasta", "", 10, 20, 2,
                "easy", "Italian", 4.5,
                "noodles", "cook", false, true);
        var r2 = new Recipe("Soup", "", 5, 15, 1,
                "easy", "German", 4.0,
                "water", "boil", false, true);

        doReturn(java.util.List.of(r1, r2)).when(repo).findAll();

        // when:Service-Methode aufrufen
        var result = underTest.findAll();

        // then: Service delegiert an das Repository und gibt zwei Elemente zurück
        verify(repo).findAll();
        Mockito.verifyNoMoreInteractions(repo);
        assertThat(result).hasSize(2);
    }
    @Test
    @DisplayName("findAllPublished should call repo.findByPublishedTrue")
    void findAllPublished_should_call_findByPublishedTrue() {
        // given: Repository liefert genau ein veröffentlichtes Rezept
        var published = new Recipe("Cake", "", 0, 0, 0,
                "", "", 0.0, "", "", false, true);

        doReturn(java.util.List.of(published)).when(repo).findByPublishedTrue();

        // when
        var result = underTest.findAllPublished();

        // then:Service ruft die spezialisierte Repository-Methode auf
        verify(repo).findByPublishedTrue();
        Mockito.verifyNoMoreInteractions(repo);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).isPublished()).isTrue();
    }
    @Test
    @DisplayName("create should throw IllegalArgumentException when title is empty")
    void create_should_throw_when_title_empty() {
        // given: Rezept ohne Titel
        var recipe = new Recipe("", "", 0, 0, 0,
                "", "", 0.0, "ing", "instr", false, true);

        // when: Service-Methodenaufruf und Exception abfangen
        var thrown = org.assertj.core.api.Assertions.catchThrowable(
                () -> underTest.create(recipe)
        );

        // then: Validierung schlägt an, Repository wird nicht genutzt
        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Title");
        Mockito.verifyNoMoreInteractions(repo);
    }
    @Test
    @DisplayName("findById should throw when recipe not found")
    void findById_should_throw_when_not_found() {
        // given: Repository findet zu der ID keinen Eintrag
        Long id = 42L;
        doReturn(java.util.Optional.empty()).when(repo).findById(id);

        // when
        var thrown = org.assertj.core.api.Assertions.catchThrowable(
                () -> underTest.findById(id)
        );

        // then: Service wirft eine sprechende Exception
        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Recipe with ID " + id);
        verify(repo).findById(id);
        Mockito.verifyNoMoreInteractions(repo);
    }

    @Test
    @DisplayName("delete should delegate to repo.deleteById")
    void delete_should_delegate_to_repository() {
        // when:Service-Methode aufrufen
        underTest.delete(5L);

        // then:Service ruft die passende Repository-Methode mit der ID auf
        verify(repo).deleteById(5L);
        Mockito.verifyNoMoreInteractions(repo);
    }
    @Test
    @DisplayName("update should copy fields and save")
    void update_should_copy_fields_and_save() {
        // given: bestehendes Rezept in der Datenbank
        var existing = new Recipe("Old", "", 1, 2, 1,
                "easy", "OldCat", 1.0, "old", "old", false, false);
        existing.setPublished(false);
        doReturn(java.util.Optional.of(existing)).when(repo).findById(1L);

        // neue Werte, die übernommen werden sollen
        var updated = new Recipe("New", "", 10, 20, 2,
                "medium", "NewCat", 4.0, "new", "new", true, true);
        doReturn(updated).when(repo).save(existing);

        // when: Update ausführen
        var result = underTest.update(1L, updated);

        // then: Service lädt, überschreibt Felder und speichert über das Repository
        verify(repo).findById(1L);
        verify(repo).save(existing);
        Mockito.verifyNoMoreInteractions(repo);
        assertThat(result.getTitle()).isEqualTo("New");
        assertThat(result.isFavorite()).isTrue();
    }
}
