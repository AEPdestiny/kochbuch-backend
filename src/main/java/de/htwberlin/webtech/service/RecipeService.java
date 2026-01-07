package de.htwberlin.webtech.service;

import de.htwberlin.webtech.model.Recipe;
import de.htwberlin.webtech.repo.RecipeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

/** Service für die Verwaltung der Rezepte.*/
@Service
public class RecipeService {

    @Autowired
    RecipeRepository repo;

    public RecipeService(RecipeRepository repo) {
        this.repo = repo;
    }
    /**
     * Legt ein neues Rezept an und führt Validierungen durch.
     *
     * @param recipe neues Rezept aus dem Controller
     * @return gespeichertes Rezept aus der Datenbank
     */
    public Recipe create(Recipe recipe) {
        if (recipe.getTitle() == null || recipe.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title cannot be empty.");
        }
        if (recipe.getIngredients() == null || recipe.getIngredients().isBlank()) {
            throw new IllegalArgumentException("Ingredients cannot be empty.");
        }
        if (recipe.getInstructions() == null || recipe.getInstructions().isBlank()) {
            throw new IllegalArgumentException("Instructions cannot be empty.");
        }
        // Falls das Rezept nicht als veröffentlicht markiert wurde, bleibt es intern
        if (!recipe.isPublished()) {
            recipe.setPublished(false);
        }
        return repo.save(recipe);
    }
    /**
     * Liefert alle gespeicherten Rezepte zurück.
     *
     * @return Liste aller Rezepte
     */
    public List<Recipe> findAll() {
        return repo.findAll();
    }
    /**
     * Liefert nur veröffentlichte Rezepte zurück.
     *
     * @return Liste veröffentlichter Rezepte
     */
    public List<Recipe> findAllPublished() {
        return repo.findByPublishedTrue();
    }
    /**
     * Sucht ein Rezept anhand seiner ID.
     * Wirft eine IllegalArgumentException, wenn kein Eintrag gefunden wird.
     *
     * @param id technische ID des gesuchten Rezepts
     * @return gefundenes Rezept
     */
    public Recipe findById(Long id) {
        return repo.findById(id).orElseThrow(() ->
                new IllegalArgumentException("Recipe with ID " + id + " not found."));
    }
    /**
     * Aktualisiert ein bestehendes Rezept mit neuen Werten.
     *
     * @param id      ID des zu aktualisierenden Rezepts
     * @param updated Objekt mit den neuen Rezeptdaten
     * @return gespeichertes, aktualisiertes Rezept
     */
    public Recipe update(Long id, Recipe updated) {
        Recipe existing = findById(id); // Bestehendes Rezept laden oder Fehler werfen

        // Alle relevanten Felder vom Update-Objekt übernehmen
        existing.setTitle(updated.getTitle());
        existing.setImageUrl(updated.getImageUrl());
        existing.setPrepTimeMinutes(updated.getPrepTimeMinutes());
        existing.setCookTimeMinutes(updated.getCookTimeMinutes());
        existing.setServings(updated.getServings());
        existing.setDifficulty(updated.getDifficulty());
        existing.setCategory(updated.getCategory());
        existing.setRating(updated.getRating());
        existing.setIngredients(updated.getIngredients());
        existing.setInstructions(updated.getInstructions());
        existing.setFavorite(updated.isFavorite());
        existing.setPublished(updated.isPublished());

        return repo.save(existing);
    }
    /**
     * Löscht ein Rezept dauerhaft anhand seiner ID.
     *
     * @param id ID des zu löschenden Rezepts
     */
    public void delete(Long id) {
        repo.deleteById(id);
    }
}
