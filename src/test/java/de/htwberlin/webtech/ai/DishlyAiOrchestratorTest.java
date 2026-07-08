package de.htwberlin.webtech.ai;

import de.htwberlin.webtech.ai.client.GroqClient;
import de.htwberlin.webtech.ai.client.GroqClientException;
import de.htwberlin.webtech.ai.dto.AiChatRequest;
import de.htwberlin.webtech.ai.dto.AiChatResponse;
import de.htwberlin.webtech.ai.orchestrator.AiIntentDetector;
import de.htwberlin.webtech.ai.orchestrator.DishlyAiOrchestrator;
import de.htwberlin.webtech.ai.tools.AiShoppingListTool;
import de.htwberlin.webtech.ai.tools.AiShoppingListToolResult;
import de.htwberlin.webtech.favorite.repository.ExternalRecipeFavoriteRepository;
import de.htwberlin.webtech.mealplan.repository.MealPlanRepository;
import de.htwberlin.webtech.pantry.repository.PantryItemRepository;
import de.htwberlin.webtech.profile.repository.UserPreferencesRepository;
import de.htwberlin.webtech.recipe.entity.Recipe;
import de.htwberlin.webtech.recipe.repository.RecipeRepository;
import de.htwberlin.webtech.shopping.entity.ShoppingListItem;
import de.htwberlin.webtech.shopping.repository.ShoppingListItemRepository;
import de.htwberlin.webtech.user.entity.AppUser;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class DishlyAiOrchestratorTest {

    private final GroqClient groqClient = mock(GroqClient.class);
    private final UserPreferencesRepository preferencesRepository = mock(UserPreferencesRepository.class);
    private final PantryItemRepository pantryItemRepository = mock(PantryItemRepository.class);
    private final MealPlanRepository mealPlanRepository = mock(MealPlanRepository.class);
    private final ExternalRecipeFavoriteRepository favoriteRepository = mock(ExternalRecipeFavoriteRepository.class);
    private final ShoppingListItemRepository shoppingListItemRepository = mock(ShoppingListItemRepository.class);
    private final RecipeRepository recipeRepository = mock(RecipeRepository.class);
    private final AiShoppingListTool shoppingListTool = mock(AiShoppingListTool.class);
    private final DishlyAiOrchestrator underTest = new DishlyAiOrchestrator(
            groqClient,
            preferencesRepository,
            pantryItemRepository,
            mealPlanRepository,
            favoriteRepository,
            shoppingListItemRepository,
            recipeRepository,
            new AiIntentDetector(),
            shoppingListTool
    );

    @Test
    void answer_should_call_groq_with_real_context() {
        AppUser user = user();
        stubEmptyContext(user);
        doReturn("Nutze Pasta aus deinem Vorrat.").when(groqClient).complete(any(), contains("Aktuelle Nutzerfrage: Was soll ich kochen?"));

        AiChatResponse response = underTest.answer(user, "Was soll ich kochen?", List.of());

        assertTrue(response.isConfigured());
        assertEquals("Nutze Pasta aus deinem Vorrat.", response.getMessage());
        verify(groqClient).complete(any(), contains("Current week meal plan"));
    }

    @Test
    void answer_should_include_shopping_list_own_recipes_and_published_recipes() {
        AppUser user = user();
        stubEmptyContext(user);
        doReturn(List.of(shoppingItem("Tomaten", "2", "Stueck", false)))
                .when(shoppingListItemRepository).findByOwner(user);
        doReturn(List.of(recipe("Familien-Pasta", "dinner", 620, 28.0, "Nudeln, Tomaten, Basilikum")))
                .when(recipeRepository).findByOwner(user);
        doReturn(List.of(recipe("Dishly Bowl", "lunch", 510, 22.5, "Reis, Bohnen, Mais")))
                .when(recipeRepository).findRandomPublished(45);
        doReturn("Antwort").when(groqClient).complete(any(), any());

        underTest.answer(user, "Was passt zu meiner Einkaufsliste?", List.of());

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(groqClient).complete(any(), promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertTrue(prompt.contains("Shopping list"));
        assertTrue(prompt.contains("Tomaten (2 Stueck) [offen]"));
        assertTrue(prompt.contains("Own recipes"));
        assertTrue(prompt.contains("Familien-Pasta"));
        assertTrue(prompt.contains("Zutaten=Nudeln, Tomaten, Basilikum"));
        assertTrue(prompt.contains("Dishly recipe catalog"));
        assertTrue(prompt.contains("Dishly Bowl"));
    }

    @Test
    void answer_should_include_limited_chat_history_for_follow_up_messages() {
        AppUser user = user();
        stubEmptyContext(user);
        doReturn("Antwort").when(groqClient).complete(any(), any());

        underTest.answer(user, "2", List.of(
                turn("user", "Was soll ich kochen?"),
                turn("assistant", "Ich empfehle Dishly Pasta. Moechtest du (1) Details oder (2) Restaurant?")
        ));

        ArgumentCaptor<String> systemPromptCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(groqClient).complete(systemPromptCaptor.capture(), promptCaptor.capture());
        String systemPrompt = systemPromptCaptor.getValue();
        String prompt = promptCaptor.getValue();
        assertTrue(systemPrompt.contains("Nutzerantworten wie \"1\", \"2\", \"3\""));
        assertTrue(prompt.contains("Bisheriger Chatverlauf"));
        assertTrue(prompt.contains("User: Was soll ich kochen?"));
        assertTrue(prompt.contains("Assistant: Ich empfehle Dishly Pasta."));
        assertTrue(prompt.contains("Detected intent"));
        assertTrue(prompt.contains("primaryIntent=FOLLOW_UP_SELECTION"));
        assertTrue(prompt.contains("plannedActions=type=FIND_RESTAURANT"));
        assertTrue(prompt.contains("Aktuelle Nutzerfrage: 2"));
    }

    @Test
    void answer_should_describe_empty_shopping_list_and_empty_recipes_honestly() {
        AppUser user = user();
        stubEmptyContext(user);
        doReturn("Antwort").when(groqClient).complete(any(), any());

        underTest.answer(user, "Was weiss Dishly ueber mich?", List.of());

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(groqClient).complete(any(), promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertTrue(prompt.contains("Einkaufsliste ist leer"));
        assertTrue(prompt.contains("keine eigenen Rezepte gespeichert"));
        assertTrue(prompt.contains("keine passenden Dishly-Rezepte verfuegbar"));
    }

    @Test
    void answer_should_return_honest_fallback_when_groq_is_missing() {
        AppUser user = user();
        stubEmptyContext(user);
        doThrow(new GroqClientException("GROQ_API_KEY is not configured.")).when(groqClient).complete(any(), any());

        AiChatResponse response = underTest.answer(user, "Hallo", List.of());

        assertFalse(response.isConfigured());
        assertTrue(response.getMessage().contains("GROQ_API_KEY"));
    }

    @Test
    void answer_should_return_controlled_groq_error_message() {
        AppUser user = user();
        stubEmptyContext(user);
        doThrow(new GroqClientException("Groq request failed with status 401.")).when(groqClient).complete(any(), any());

        AiChatResponse response = underTest.answer(user, "Hallo", List.of());

        assertFalse(response.isConfigured());
        assertTrue(response.getMessage().contains("Groq request failed with status 401."));
    }

    @Test
    void answer_should_execute_shopping_list_follow_up_selection_when_ingredients_are_clear() {
        AppUser user = user();
        stubEmptyContext(user);
        List<String> ingredients = List.of("Avocado", "Reis", "Limette");
        doReturn(new AiShoppingListToolResult(List.of("Limette"), List.of("Avocado", "Reis"), List.of()))
                .when(shoppingListTool).addMissingIngredients(user, ingredients);

        AiChatResponse response = underTest.answer(user, "1", List.of(
                turn("assistant", "Ich empfehle eine Bowl. Zutaten: Avocado, Reis, Limette. Moechtest du (1) die Zutaten zur Einkaufsliste hinzufuegen?")
        ));

        assertTrue(response.isConfigured());
        assertTrue(response.getMessage().contains("Limette"));
        assertTrue(response.getMessage().contains("Avocado und Reis"));
        verify(shoppingListTool).addMissingIngredients(user, ingredients);
        verifyNoInteractions(groqClient);
    }

    @Test
    void answer_should_execute_natural_shopping_list_command_when_ingredients_are_clear() {
        AppUser user = user();
        stubEmptyContext(user);
        List<String> ingredients = List.of("Tomaten", "Basilikum");
        doReturn(new AiShoppingListToolResult(ingredients, List.of(), List.of()))
                .when(shoppingListTool).addMissingIngredients(user, ingredients);

        AiChatResponse response = underTest.answer(user, "fuege es zur Einkaufsliste hinzu", List.of(
                turn("assistant", "Gute Idee: Pasta. Zutaten: Tomaten, Basilikum.")
        ));

        assertTrue(response.isConfigured());
        assertTrue(response.getMessage().contains("Tomaten und Basilikum"));
        verify(shoppingListTool).addMissingIngredients(user, ingredients);
        verifyNoInteractions(groqClient);
    }

    @Test
    void answer_should_execute_turkish_shopping_list_command_when_ingredients_are_clear() {
        AppUser user = user();
        stubEmptyContext(user);
        List<String> ingredients = List.of("Nohut", "Limon");
        doReturn(new AiShoppingListToolResult(ingredients, List.of(), List.of()))
                .when(shoppingListTool).addMissingIngredients(user, ingredients);

        AiChatResponse response = underTest.answer(user, "bunu alisveris listesine ekle", List.of(
                turn("assistant", "Malzemeler: Nohut, Limon.")
        ));

        assertTrue(response.isConfigured());
        assertTrue(response.getMessage().contains("Nohut und Limon"));
        verify(shoppingListTool).addMissingIngredients(user, ingredients);
        verifyNoInteractions(groqClient);
    }

    @Test
    void answer_should_ask_clarification_for_shopping_list_intent_without_clear_ingredients() {
        AppUser user = user();
        stubEmptyContext(user);

        AiChatResponse response = underTest.answer(user, "fuege es zur Einkaufsliste hinzu", List.of());

        assertTrue(response.isConfigured());
        assertTrue(response.getMessage().contains("Welche Zutaten"));
        verifyNoInteractions(shoppingListTool);
        verifyNoInteractions(groqClient);
    }

    @Test
    void answer_should_not_claim_success_when_shopping_list_tool_fails() {
        AppUser user = user();
        stubEmptyContext(user);
        List<String> ingredients = List.of("Limette");
        doThrow(new RuntimeException("persist failed"))
                .when(shoppingListTool).addMissingIngredients(user, ingredients);

        AiChatResponse response = underTest.answer(user, "fuege es zur Einkaufsliste hinzu", List.of(
                turn("assistant", "Zutaten: Limette.")
        ));

        assertFalse(response.isConfigured());
        assertTrue(response.getMessage().contains("konnte"));
        assertTrue(response.getMessage().contains("persist failed"));
        verifyNoInteractions(groqClient);
    }

    @Test
    void answer_should_not_execute_meal_plan_intent_in_shopping_list_step() {
        AppUser user = user();
        stubEmptyContext(user);
        doReturn("Ich brauche dafuer noch eine Bestaetigung.").when(groqClient).complete(any(), any());

        AiChatResponse response = underTest.answer(user, "mach es morgen abend rein", List.of(
                turn("assistant", "Zutaten: Limette.")
        ));

        assertTrue(response.isConfigured());
        assertEquals("Ich brauche dafuer noch eine Bestaetigung.", response.getMessage());
        verifyNoInteractions(shoppingListTool);
        verify(groqClient).complete(any(), any());
    }

    private void stubEmptyContext(AppUser user) {
        doReturn(Optional.empty()).when(preferencesRepository).findByOwner(user);
        doReturn(List.of()).when(pantryItemRepository).findByOwner(user);
        doReturn(List.of()).when(favoriteRepository).findByOwner(user);
        doReturn(List.of()).when(shoppingListItemRepository).findByOwner(user);
        doReturn(List.of()).when(recipeRepository).findByOwner(user);
        doReturn(List.of()).when(recipeRepository).findRandomPublished(45);
        doReturn(List.of()).when(mealPlanRepository).findByOwnerAndPlannedDateBetween(any(), any(LocalDate.class), any(LocalDate.class));
    }

    private ShoppingListItem shoppingItem(String name, String quantity, String unit, boolean checked) {
        ShoppingListItem item = new ShoppingListItem();
        item.setName(name);
        item.setQuantity(new BigDecimal(quantity));
        item.setUnit(unit);
        item.setChecked(checked);
        return item;
    }

    private Recipe recipe(String title, String category, Integer calories, Double protein, String ingredients) {
        Recipe recipe = new Recipe();
        recipe.setTitle(title);
        recipe.setCategory(category);
        recipe.setCalories(calories);
        recipe.setProtein(protein);
        recipe.setIngredients(ingredients);
        recipe.setPublished(true);
        return recipe;
    }

    private AiChatRequest.AiChatTurn turn(String role, String text) {
        AiChatRequest.AiChatTurn turn = new AiChatRequest.AiChatTurn();
        turn.setRole(role);
        turn.setText(text);
        return turn;
    }

    private AppUser user() {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setUsername("produuser");
        user.setEmail("user@example.com");
        user.setPasswordHash("hash");
        return user;
    }
}
