package de.htwberlin.webtech.recipe.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecipeIngredientNormalizerTest {

    @Test
    void normalizeToList_should_split_newline_separated_ingredients() {
        assertEquals(
                List.of("200 g Tomaten", "150 g Pasta", "1/2 TL Salz"),
                RecipeIngredientNormalizer.normalizeToList("200 g Tomaten\n150 g Pasta\r\n1/2 TL Salz")
        );
    }

    @Test
    void normalizeToList_should_split_legacy_zero_separators_and_remove_noise() {
        assertEquals(
                List.of("onion", "5 EL garlic", "butter", "grits", "shrimp"),
                RecipeIngredientNormalizer.normalizeToList("0 ml onion 0 5 EL garlic 0 ml butter 0 0 ml grits 0 g shrimp 0")
        );
    }

    @Test
    void normalizeToList_should_remove_empty_numeric_and_duplicate_ingredients() {
        assertEquals(
                List.of("200 g Reis", "Salz"),
                RecipeIngredientNormalizer.normalizeToList("0\n1\n200 g Reis\nSalz\nSalz")
        );
    }

    @Test
    void normalizeToList_should_keep_real_ingredients_from_recipe_1443() {
        String rawIngredients = """
                0 ml onion 0
                5 EL garlic 0 ml butter 0
                5 EL olive oil 2 g clam juice 0 ml wine 1 ml tomato 1 Prise kosher salt 1 Prise pepper 1 Prise chili flakes 0
                """;

        assertEquals(
                List.of(
                        "onion",
                        "5 EL garlic",
                        "butter",
                        "5 EL olive oil",
                        "2 g clam juice",
                        "wine",
                        "1 ml tomato",
                        "1 Prise kosher salt",
                        "1 Prise pepper",
                        "1 Prise chili flakes"
                ),
                RecipeIngredientNormalizer.normalizeToList(rawIngredients)
        );
    }

    @Test
    void normalizeToList_should_keep_real_ingredients_from_recipe_804() {
        String rawIngredients = """
                1
                3 strips bacon 1 servings barbecue sauce 0
                08 stick butter 0
                5 garlic cloves 0 ml grits 0 g shrimp 1 Prise salt 0 ml scallions 0 ml sharp cheddar cheese 0
                17 EL water
                """;

        assertEquals(
                List.of(
                        "3 strips bacon",
                        "barbecue sauce",
                        "08 stick butter",
                        "5 garlic cloves",
                        "grits",
                        "shrimp",
                        "1 Prise salt",
                        "scallions",
                        "sharp cheddar cheese",
                        "17 EL water"
                ),
                RecipeIngredientNormalizer.normalizeToList(rawIngredients)
        );
    }

    @Test
    void normalizeToList_should_split_live_recipe_1155_ingredient_block() {
        assertEquals(
                List.of(
                        "61 g geröstetes Kürbispüree",
                        "4 g Teffmehl",
                        "4 g Tapiokamehl",
                        "8 g Reismehl"
                ),
                RecipeIngredientNormalizer.normalizeToList(
                        "61 g geröstetes Kürbispüree 4 g Teffmehl 4 g Tapiokamehl 8 g Reismehl 0"
                )
        );
    }

    @Test
    void normalizeToList_should_split_long_pumpkin_ingredient_block() {
        assertEquals(
                List.of(
                        "02 TL gemahlene Nelken",
                        "1/4 EL Dattelzucker",
                        "1/6 EL Sonnenblumenöl",
                        "1/6 EL Leinsamenmehl",
                        "1/6 EL Wasser",
                        "19 ml Kokosmilch"
                ),
                RecipeIngredientNormalizer.normalizeToList(
                        "02 TL gemahlene Nelken 1/4 EL Dattelzucker 1/6 EL Sonnenblumenöl 1/6 EL Leinsamenmehl 1/6 EL Wasser 19 ml Kokosmilch 0"
                )
        );
    }

    @Test
    void normalizeToList_should_keep_valid_amounts_and_units() {
        assertEquals(
                List.of("120 ml Milch", "40 g Reis", "1/2 TL Salz", "3 strips bacon"),
                RecipeIngredientNormalizer.normalizeToList("120 ml Milch\n40 g Reis\n1/2 TL Salz\n3 strips bacon")
        );
    }

    @Test
    void normalizeToList_should_strip_leading_bullet_and_list_markers() {
        assertEquals(
                List.of(
                        "1/2 tbsp soy sauce",
                        "2 package shrimp flavored udon",
                        "scallion, finely cut",
                        "1/2 cup fish cake, thinly sliced flat",
                        "1 tsp minced garlic",
                        "2 eggs, poached"
                ),
                RecipeIngredientNormalizer.normalizeToList("""
                        \u20221/2 tbsp soy sauce
                        - 2 package shrimp flavored udon
                        \u2022scallion, finely cut
                        .1/2 cup fish cake, thinly sliced flat
                        .1 tsp minced garlic
                        \u20222 eggs, poached
                        """)
        );
    }

    @Test
    void normalizeToText_should_return_frontend_compatible_newline_text() {
        assertEquals(
                "200 g Tomaten\n150 g Pasta",
                RecipeIngredientNormalizer.normalizeToText("200 g Tomaten; 150 g Pasta")
        );
    }
}
