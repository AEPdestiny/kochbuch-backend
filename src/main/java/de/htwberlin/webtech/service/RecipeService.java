package de.htwberlin.webtech.service;

import de.htwberlin.webtech.model.Recipe;
import de.htwberlin.webtech.repo.RecipeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecipeService {

    @Autowired
    RecipeRepository repo;

    public RecipeService(RecipeRepository repo) {
        this.repo = repo;
    }

    public Recipe create(Recipe recipe) {
        return repo.save(recipe);
    }

    public List<Recipe> findAll() {
        return repo.findAll();
    }
}
