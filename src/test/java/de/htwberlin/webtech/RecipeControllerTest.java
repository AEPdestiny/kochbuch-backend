package de.htwberlin.webtech;

import de.htwberlin.webtech.controller.RecipeController;
import de.htwberlin.webtech.service.ExternalRecipeService;
import de.htwberlin.webtech.service.RecipeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Web-Layer-Test für den RecipeController. */
@WebMvcTest(RecipeController.class)
class RecipeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecipeService recipeService;

    @MockitoBean
    private ExternalRecipeService externalRecipeService;

    /**Testet, dass GET /recipes eine 200 zurückgibt und genau zwei Rezepte im JSON-Array stehen. */
    @Test
    void getAll_should_return_ok() throws Exception {
        // given
        var r1 = new de.htwberlin.webtech.model.Recipe("Pasta", "", 10, 20, 2,
                "easy", "Italian", 4.5, "noodles", "cook", false, true);
        var r2 = new de.htwberlin.webtech.model.Recipe("Soup", "", 5, 15, 1,
                "easy", "German", 4.0, "water", "boil", false, true);

        org.mockito.Mockito
                .doReturn(java.util.List.of(r1, r2))
                .when(recipeService).findAll();

        // when + then
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/recipes"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.length()").value(2));
    }

    /** Prüft GET /recipes/published (Status 200 und genau ein veröffentlichtes Rezept). */
    @Test
    void getPublished_should_return_ok() throws Exception {
        // given
        var published = new de.htwberlin.webtech.model.Recipe("Cake", "", 0, 0, 0,
                "", "", 0.0, "", "", false, true);

        org.mockito.Mockito
                .doReturn(java.util.List.of(published))
                .when(recipeService).findAllPublished();

        // when + then
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/recipes/published"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.length()").value(1));
    }

    /** Prüft GET /recipes/external mit externen Rezept. */
    @Test
    void getExternal_should_return_ok() throws Exception {
        // given
        var ext = new de.htwberlin.webtech.model.Recipe("Ext", "", 0, 0, 0,
                "", "", 0.0, "", "", false, true);

        org.mockito.Mockito
                .doReturn(java.util.List.of(ext))
                .when(externalRecipeService).fetchExternalRecipes();

        // when + then
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/recipes/external"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.length()").value(1));
    }

    /** Prüft GET /recipes/{id} für den Erfolgsfall. */
    @Test
    void getById_should_return_ok() throws Exception {
        // given
        var recipe = new de.htwberlin.webtech.model.Recipe("Pasta", "", 10, 20, 2,
                "easy", "Italian", 4.5, "noodles", "cook", false, true);

        org.mockito.Mockito
                .doReturn(recipe)
                .when(recipeService).findById(1L);

        // when + then
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/recipes/1"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.title").value("Pasta"));
    }

    /** Prüft POST /recipes mit einem gültigen JSON-Body. */
    @Test
    void create_should_return_ok() throws Exception {
        // given
        var recipe = new de.htwberlin.webtech.model.Recipe(
                "Pasta", "", 10, 20, 2,
                "easy", "Italian", 4.5,
                "noodles", "cook", false, true
        );

        org.mockito.Mockito
                .doReturn(recipe)
                .when(recipeService).create(org.mockito.Mockito.any());

        var json = """
        {
          "title": "Pasta",
          "imageUrl": "",
          "prepTimeMinutes": 10,
          "cookTimeMinutes": 20,
          "servings": 2,
          "difficulty": "easy",
          "category": "Italian",
          "rating": 4.5,
          "ingredients": "noodles",
          "instructions": "cook",
          "favorite": false,
          "published": true
        }
        """;

        // when + then
        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/recipes")
                                .contentType("application/json")
                                .content(json)
                )
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.title").value("Pasta"));
    }

    /** Prüft PUT /recipes/{id} und ob der neue Titel im Response-JSON landet. */
    @Test
    void update_should_return_ok() throws Exception {
        // given
        var updated = new de.htwberlin.webtech.model.Recipe(
                "Updated", "", 10, 20, 2,
                "easy", "Italian", 4.5,
                "noodles", "cook", false, true
        );

        org.mockito.Mockito
                .doReturn(updated)
                .when(recipeService).update(org.mockito.Mockito.eq(1L), org.mockito.Mockito.any());

        var json = """
        {
          "title": "Updated",
          "imageUrl": "",
          "prepTimeMinutes": 10,
          "cookTimeMinutes": 20,
          "servings": 2,
          "difficulty": "easy",
          "category": "Italian",
          "rating": 4.5,
          "ingredients": "noodles",
          "instructions": "cook",
          "favorite": false,
          "published": true
        }
        """;

        // when + then
        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .put("/recipes/1")
                                .contentType("application/json")
                                .content(json)
                )
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.title").value("Updated"));
    }

    /** Prüft DELETE /recipes/{id} und dass der Service mit der richtigen ID aufgerufen wird. */
    @Test
    void delete_should_return_ok() throws Exception {
        // when + then
        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .delete("/recipes/1")
                )
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

        org.mockito.Mockito.verify(recipeService).delete(1L);
    }
}
