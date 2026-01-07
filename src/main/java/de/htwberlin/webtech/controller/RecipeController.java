package de.htwberlin.webtech.controller;

import de.htwberlin.webtech.model.Recipe;
import de.htwberlin.webtech.service.ExternalRecipeService;
import de.htwberlin.webtech.service.RecipeService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * REST-Controller für alle HTTP-Endpunkte.
 * Bietet CRUD-Operationen auf dem eigenen Recipe-Modell
 * und einen zusätzlichen Endpunkt für externe Rezepte.
 */
@RestController
@CrossOrigin(origins = {
        "http://localhost:5173",
        "https://webtech-frontend-odbd.onrender.com"
})
@RequestMapping("/recipes")
public class RecipeController {

    private final RecipeService service; // logik für die Verwaltung der eigenen Rezepte.
    private final ExternalRecipeService externalService; //Lädt Rezepte von der externen API und mapped sie auf das Modell.

    /**
     * Konstruktor der benötigten Services.
     *
     * @param service         Service für lokale Rezept-CRUD-Operationen
     * @param externalService Service zum Abruf externer Rezepte
     */
    public RecipeController(RecipeService service, ExternalRecipeService externalService) {
        this.service = service;
        this.externalService = externalService;
    }

    /**
     * Legt ein neues Rezept an.
     *
     * @param recipe Rezeptdaten aus dem Request-Body
     * @return das gespeicherte Rezept inklusive generierter ID
     */
    @PostMapping
    public Recipe create(@RequestBody Recipe recipe) {
        return service.create(recipe);
    }

    /**
     * Liefert alle gespeicherten Rezepte zurück.
     *
     * @return Liste aller Rezepte
     */
    @GetMapping
    public List<Recipe> getAll() {
        return service.findAll();
    }

    /**
     * Liefert nur Rezepte, die als veröffentlicht markiert sind.
     *
     * @return Liste veröffentlichter Rezepte
     */
    @GetMapping("/published")
    public List<Recipe> getPublished() {
        return service.findAllPublished();
    }

    /**
     * Holt ein einzelnes Rezept anhand seiner ID.
     *
     * @param id technische ID des gesuchten Rezepts
     * @return gefundenes Rezept oder Exception, falls nicht vorhanden
     */
    @GetMapping("/{id}")
    public Recipe getById(@PathVariable Long id) {
        return service.findById(id);
    }

    /**
     * Aktualisiert ein bestehendes Rezept.
     *
     * @param id     ID des zu aktualisierenden Rezepts
     * @param recipe neue Rezeptdaten
     * @return das aktualisierte Rezept
     */
    @PutMapping("/{id}")
    public Recipe update(@PathVariable Long id, @RequestBody Recipe recipe) {
        return service.update(id, recipe);
    }

    /**
     * Löscht ein Rezept dauerhaft aus der Datenbank.
     *
     * @param id ID des zu löschenden Rezepts
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
    /**
     * Lädt Rezepte von der externen API
     * und gibt sie im Recipe-Format zurück.
     *
     * @return Liste externer Rezepte als Recipe-Objekte
     */
    @GetMapping("/external")
    public List<Recipe> getExternal() {
        return externalService.fetchExternalRecipes();
    }
}
