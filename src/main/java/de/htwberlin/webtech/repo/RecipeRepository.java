package de.htwberlin.webtech.repo;

import de.htwberlin.webtech.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    List<Recipe> findByPublishedTrue();
}
