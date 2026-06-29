package de.htwberlin.webtech.recipe.mapper;

import de.htwberlin.webtech.recipe.dto.RecipeRequest;
import de.htwberlin.webtech.recipe.dto.RecipeResponse;
import de.htwberlin.webtech.recipe.entity.Recipe;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeMapperTest {

    private final RecipeMapper underTest = new RecipeMapper();

    @Test
    void toResponse_should_include_calories_when_present() {
        Recipe recipe = baseRecipe();
        recipe.setCalories(450);

        RecipeResponse response = underTest.toResponse(recipe);

        assertEquals(450, response.getCalories());
    }

    @Test
    void toResponse_should_return_null_calories_when_not_set() {
        Recipe recipe = baseRecipe();

        RecipeResponse response = underTest.toResponse(recipe);

        assertNull(response.getCalories());
    }

    @Test
    void toEntity_should_set_calories_from_request() {
        RecipeRequest request = baseRequest();
        request.setCalories(320);

        Recipe recipe = underTest.toEntity(request);

        assertEquals(320, recipe.getCalories());
    }

    @Test
    void toEntity_should_leave_calories_null_when_request_omits_it() {
        RecipeRequest request = baseRequest();

        Recipe recipe = underTest.toEntity(request);

        assertNull(recipe.getCalories());
    }

    @Test
    void updateEntity_from_request_should_overwrite_existing_calories_with_new_value() {
        Recipe existing = baseRecipe();
        existing.setCalories(200);
        RecipeRequest request = baseRequest();
        request.setCalories(550);

        underTest.updateEntity(existing, request);

        assertEquals(550, existing.getCalories());
    }

    @Test
    void updateEntity_from_request_should_clear_calories_when_request_has_none() {
        Recipe existing = baseRecipe();
        existing.setCalories(200);
        RecipeRequest request = baseRequest();

        underTest.updateEntity(existing, request);

        assertNull(existing.getCalories());
    }

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

    private Recipe baseRecipe() {
        Recipe recipe = new Recipe();
        recipe.setTitle("Pasta");
        recipe.setIngredients("noodles");
        recipe.setInstructions("cook");
        return recipe;
    }

    private RecipeRequest baseRequest() {
        RecipeRequest request = new RecipeRequest();
        request.setTitle("Pasta");
        request.setIngredients("noodles");
        request.setInstructions("cook");
        return request;
    }
}
