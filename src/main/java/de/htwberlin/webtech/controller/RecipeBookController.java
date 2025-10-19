package de.htwberlin.webtech.controller;

import de.htwberlin.webtech.model.Recipe;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RecipeBookController {

    @GetMapping("/")
    public ResponseEntity<List<Recipe>> home() {
        List<Recipe> data = List.of(
                new Recipe("1", "Pasta Carbonara", "Pasta, Eier, Speck, Käse, Pfeffer", true),
                new Recipe("2", "Tomatensuppe", "Tomaten, Zwiebeln, Knoblauch, Brühe", false)
        );
        return ResponseEntity.ok(data);
    }
}
