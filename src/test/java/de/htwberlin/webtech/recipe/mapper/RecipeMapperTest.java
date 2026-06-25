package de.htwberlin.webtech.recipe.mapper;

import de.htwberlin.webtech.recipe.dto.RecipeResponse;
import de.htwberlin.webtech.recipe.entity.Recipe;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeMapperTest {

    private final RecipeMapper underTest = new RecipeMapper();

    @Test
    void toResponse_should_include_normalized_ingredients_list() {
        Recipe recipe = new Recipe();
        recipe.setId(1155L);
        recipe.setTitle("Kürbisrezept");
        recipe.setIngredients("61 g geröstetes Kürbispüree 4 g Teffmehl 4 g Tapiokamehl 8 g Reismehl 0");
        recipe.setInstructions("1. Zutaten vorbereiten.\n2. Alles verrühren.\n3. Servieren.");

        RecipeResponse response = underTest.toResponse(recipe);

        assertEquals(
                List.of(
                        "61 g geröstetes Kürbispüree",
                        "4 g Teffmehl",
                        "4 g Tapiokamehl",
                        "8 g Reismehl"
                ),
                response.getIngredientsList()
        );
        assertEquals(
                "61 g geröstetes Kürbispüree\n4 g Teffmehl\n4 g Tapiokamehl\n8 g Reismehl",
                response.getIngredients()
        );
        assertEquals(
                List.of("Zutaten vorbereiten.", "Alles verrühren.", "Servieren."),
                response.getInstructionsList()
        );
        assertTrue(response.isHasRealInstructions());
    }
}
