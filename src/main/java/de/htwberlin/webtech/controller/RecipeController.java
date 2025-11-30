package de.htwberlin.webtech.controller;

import de.htwberlin.webtech.model.Recipe;
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

    public RecipeController(RecipeService service) {
        this.service = service;
    }

    @PostMapping
    public Recipe create(@RequestBody Recipe recipe) {
        return service.create(recipe);
    }

    @GetMapping
    public List<Recipe> getAll() {
        return service.findAll();
    }
}
