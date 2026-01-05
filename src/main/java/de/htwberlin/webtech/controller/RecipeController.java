package de.htwberlin.webtech.controller;

import de.htwberlin.webtech.model.Recipe;
import de.htwberlin.webtech.service.ExternalRecipeService;
import de.htwberlin.webtech.service.RecipeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = {
        "http://localhost:5173",
        "https://webtech-frontend-odbd.onrender.com"
})
@RequestMapping("/recipes")
public class RecipeController {

    private final RecipeService service;
    private final ExternalRecipeService externalService;


    public RecipeController(RecipeService service, ExternalRecipeService externalService) {
        this.service = service;
        this.externalService = externalService;
    }

    @PostMapping
    public Recipe create(@RequestBody Recipe recipe) {
        return service.create(recipe);
    }

    @GetMapping
    public List<Recipe> getAll() {
        return service.findAll();
    }

    @GetMapping("/published")
    public List<Recipe> getPublished() {
        return service.findAllPublished();
    }

    @GetMapping("/{id}")
    public Recipe getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public Recipe update(@PathVariable Long id, @RequestBody Recipe recipe) {
        return service.update(id, recipe);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/external")
    public List<Recipe> getExternal() {
        return externalService.fetchExternalRecipes();
    }
}
