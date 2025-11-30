package de.htwberlin.webtech.repo;

import de.htwberlin.webtech.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
}
