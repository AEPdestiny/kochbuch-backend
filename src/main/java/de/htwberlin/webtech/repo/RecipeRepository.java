package de.htwberlin.webtech.repo;

import de.htwberlin.webtech.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**Repository für die Recipe-Entität.*/
public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    /**
     * Liefert alle Rezepte, die als veröffentlicht markiert sind.
     *
     * @return Liste veröffentlichter Rezepte
     */
    List<Recipe> findByPublishedTrue();
}
