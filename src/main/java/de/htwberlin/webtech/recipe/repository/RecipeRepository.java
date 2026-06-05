package de.htwberlin.webtech.recipe.repository;

import de.htwberlin.webtech.recipe.entity.Recipe;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class RecipeRepository implements PanacheRepository<Recipe> {

    public List<Recipe> findPublished() {
        return list("published", true);
    }
}
