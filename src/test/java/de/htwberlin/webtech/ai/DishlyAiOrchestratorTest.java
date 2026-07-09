package de.htwberlin.webtech.ai;

import de.htwberlin.webtech.ai.client.GroqClient;
import de.htwberlin.webtech.ai.client.GroqClientException;
import de.htwberlin.webtech.ai.dto.AiChatRequest;
import de.htwberlin.webtech.ai.dto.AiChatResponse;
import de.htwberlin.webtech.ai.orchestrator.AiIntentDetector;
import de.htwberlin.webtech.ai.orchestrator.DishlyAiOrchestrator;
import de.htwberlin.webtech.ai.tools.AiMealPlanTool;
import de.htwberlin.webtech.ai.tools.AiMealPlanToolResult;
import de.htwberlin.webtech.ai.tools.AiShoppingListTool;
import de.htwberlin.webtech.ai.tools.AiShoppingListToolResult;
import de.htwberlin.webtech.favorite.repository.ExternalRecipeFavoriteRepository;
import de.htwberlin.webtech.mealplan.entity.MealSlot;
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
    private final AiMealPlanTool mealPlanTool = mock(AiMealPlanTool.class);
    private final DishlyAiOrchestrator underTest = new DishlyAiOrchestrator(
            groqClient,
            preferencesRepository,
            pantryItemRepository,
            mealPlanRepository,
            favoriteRepository,
            shoppingListItemRepository,
            recipeRepository,
            new AiIntentDetector(),
            shoppingListTool,
            mealPlanTool
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
    void answer_should_use_german_published_recipe_candidates_for_german_locale() {
        AppUser user = user();
        stubEmptyContext(user);
        doReturn(List.of(recipe("Leek And Pea Frittata", "dinner", 430, 21.0, "leek, peas, parsley")))
                .when(recipeRepository).findRandomPublished(45);
        doReturn(List.of(recipe("Lauch-Erbsen-Frittata", "abendessen", 430, 21.0, "Lauch, Erbsen, Petersilie", "de")))
                .when(recipeRepository).findRandomPublishedByLanguage("de", 45);
        doReturn("Antwort").when(groqClient).complete(any(), any());

        underTest.answer(user, "Was kann ich heute mit meinem Vorrat kochen?", List.of(), "de-DE");

        ArgumentCaptor<String> systemPromptCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(groqClient).complete(systemPromptCaptor.capture(), promptCaptor.capture());
        String systemPrompt = systemPromptCaptor.getValue();
        String prompt = promptCaptor.getValue();
        assertTrue(prompt.contains("Active UI locale: de"));
        assertTrue(prompt.contains("Lauch-Erbsen-Frittata"));
        assertTrue(prompt.contains("Zutaten=Lauch, Erbsen, Petersilie"));
        assertFalse(prompt.contains("Leek And Pea Frittata"));
        assertFalse(prompt.contains("leek, peas, parsley"));
        assertTrue(systemPrompt.contains("If active locale is de, prefer German recipe data with language=de."));
        assertTrue(systemPrompt.contains("Do not output English recipe titles or English ingredients when German recipe candidates exist."));
        assertTrue(systemPrompt.contains("Do not claim a recipe is from the catalog unless it is present in the provided candidate recipe context."));
    }

    @Test
    void answer_should_mark_missing_localized_catalog_when_no_german_candidates_exist() {
        AppUser user = user();
        stubEmptyContext(user);
        doReturn(List.of(recipe("Leek And Pea Frittata", "dinner", 430, 21.0, "leek, peas, parsley")))
                .when(recipeRepository).findRandomPublished(45);
        doReturn(List.of()).when(recipeRepository).findRandomPublishedByLanguage("de", 45);
        doReturn("Antwort").when(groqClient).complete(any(), any());

        underTest.answer(user, "Was kann ich kochen?", List.of(), "de");

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(groqClient).complete(any(), promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertTrue(prompt.contains("keine passenden lokalisierten Dishly-Rezepte fuer locale=de verfuegbar"));
        assertFalse(prompt.contains("Leek And Pea Frittata"));
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
        assertTrue(systemPrompt.contains("Zutaten: ..."));
        assertTrue(systemPrompt.contains("Fehlende Zutaten: ..."));
        assertTrue(prompt.contains("Bisheriger Chatverlauf"));
        assertTrue(prompt.contains("User: Was soll ich kochen?"));
        assertTrue(prompt.contains("Assistant: Ich empfehle Dishly Pasta."));
        assertTrue(prompt.contains("Detected intent"));
        assertTrue(prompt.contains("primaryIntent=FOLLOW_UP_SELECTION"));
        assertTrue(prompt.contains("plannedActions=type=FIND_RESTAURANT"));
        assertTrue(prompt.contains("Aktuelle Nutzerfrage: 2"));
    }

    @Test
    void answer_should_resolve_numeric_recipe_option_without_restaurant_action() {
        AppUser user = user();
        stubEmptyContext(user);
        doReturn("Hier ist das Rezept fuer Milchreis-Pfannkuchen.").when(groqClient).complete(any(), any());

        AiChatResponse response = underTest.answer(user, "2", List.of(
                turn("assistant", """
                        Was passt zu deinem Vorrat?
                        (1) ein einfaches Ei-Rezept
                        (2) ein Rezept fuer Milchreis-Pfannkuchen
                        (3) ein anderes Gericht
                        """)
        ));

        ArgumentCaptor<String> systemPromptCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(groqClient).complete(systemPromptCaptor.capture(), promptCaptor.capture());
        String systemPrompt = systemPromptCaptor.getValue();
        String prompt = promptCaptor.getValue();
        assertEquals("Hier ist das Rezept fuer Milchreis-Pfannkuchen.", response.getMessage());
        assertTrue(systemPrompt.contains("Biete keine Restaurant-Suche als Standardoption an."));
        assertTrue(prompt.contains("primaryIntent=FOLLOW_UP_SELECTION"));
        assertTrue(prompt.contains("Der Nutzer hat Option 2 gewaehlt"));
        assertTrue(prompt.contains("Milchreis-Pfannkuchen"));
        assertTrue(prompt.contains("plannedActions=keine"));
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
    void answer_should_return_friendly_rate_limit_message() {
        AppUser user = user();
        stubEmptyContext(user);
        doThrow(new GroqClientException("Groq request failed with status 429.")).when(groqClient).complete(any(), any());

        AiChatResponse response = underTest.answer(user, "Hallo", List.of());

        assertFalse(response.isConfigured());
        assertEquals("Dishly AI ist gerade kurz ausgelastet. Bitte versuche es gleich nochmal.", response.getMessage());
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
    void answer_should_not_extract_instruction_text_after_ingredient_line_and_should_ask_for_optional_choice() {
        AppUser user = user();
        stubEmptyContext(user);
        List<String> pantryIngredients = List.of("Milchreis", "Milch", "Tomate");
        doReturn(new AiShoppingListToolResult(List.of(), pantryIngredients, List.of()))
                .when(shoppingListTool).addMissingIngredients(user, pantryIngredients);

        AiChatResponse response = underTest.answer(user, "3", List.of(
                turn("assistant", """
                        Zutaten: Milchreis, Milch, Tomate, eventuell Zucker oder Suessstoff nach Geschmack

                        Um den Milchreis-Pudding zuzubereiten, koennen Sie den Milchreis mit Milch und einem wenig Zucker oder Suessstoff nach Geschmack kochen.
                        1. Rezeptdetails anzeigen
                        2. Weitere Rezeptideen erhalten
                        3. Einkaufsliste fuer fehlende Zutaten erstellen
                        """)
        ));

        assertTrue(response.isConfigured());
        assertEquals("Optional fehlen Zucker und Suessstoff. Was soll ich hinzufuegen?", response.getMessage());
        verify(shoppingListTool).addMissingIngredients(user, pantryIngredients);
        verifyNoInteractions(groqClient);
    }

    @Test
    void answer_should_prefer_missing_ingredients_line() {
        AppUser user = user();
        stubEmptyContext(user);
        List<String> ingredients = List.of("Salz", "Pfeffer");
        doReturn(new AiShoppingListToolResult(ingredients, List.of(), List.of()))
                .when(shoppingListTool).addMissingIngredients(user, ingredients);

        AiChatResponse response = underTest.answer(user, "1", List.of(
                turn("assistant", """
                        Verfuegbare Zutaten: Milchreis, Milch, Tomate
                        Fehlende Zutaten: Salz, Pfeffer
                        Zutaten: Milchreis, Milch, Tomate, Salz, Pfeffer
                        1. Einkaufsliste fuer fehlende Zutaten erstellen
                        """)
        ));

        assertTrue(response.isConfigured());
        assertTrue(response.getMessage().contains("Salz und Pfeffer"));
        verify(shoppingListTool).addMissingIngredients(user, ingredients);
        verifyNoInteractions(groqClient);
    }

    @Test
    void answer_should_not_mutate_when_missing_ingredients_are_none() {
        AppUser user = user();
        stubEmptyContext(user);

        AiChatResponse response = underTest.answer(user, "1", List.of(
                turn("assistant", """
                        Verfuegbare Zutaten: Milchreis, Milch, Tomate
                        Fehlende Zutaten: keine
                        1. Einkaufsliste fuer fehlende Zutaten erstellen
                        """)
        ));

        assertTrue(response.isConfigured());
        assertEquals("Ich habe nichts hinzugefuegt, weil keine fehlenden Zutaten angegeben sind.", response.getMessage());
        verifyNoInteractions(shoppingListTool);
        verifyNoInteractions(groqClient);
    }

    @Test
    void answer_should_extract_following_ingredients_add_pattern_for_numeric_selection() {
        AppUser user = user();
        stubEmptyContext(user);
        List<String> ingredients = List.of("Olivenoel", "Salz", "Pfeffer");
        doReturn(new AiShoppingListToolResult(ingredients, List.of(), List.of()))
                .when(shoppingListTool).addMissingIngredients(user, ingredients);

        AiChatResponse response = underTest.answer(user, "1", List.of(
                turn("assistant", "Folgende Zutaten hinzufuegen: Olivenoel, Salz, Pfeffer. Moechtest du (1) die Zutaten zur Einkaufsliste hinzufuegen?")
        ));

        assertTrue(response.isConfigured());
        assertTrue(response.getMessage().contains("Olivenoel, Salz und Pfeffer"));
        verify(shoppingListTool).addMissingIngredients(user, ingredients);
        verifyNoInteractions(groqClient);
    }

    @Test
    void answer_should_treat_all_as_shopping_list_follow_up_when_previous_message_has_ingredients() {
        AppUser user = user();
        stubEmptyContext(user);
        List<String> ingredients = List.of("Olivenoel", "Salz", "Pfeffer");
        doReturn(new AiShoppingListToolResult(ingredients, List.of(), List.of()))
                .when(shoppingListTool).addMissingIngredients(user, ingredients);

        AiChatResponse response = underTest.answer(user, "alle", List.of(
                turn("assistant", "Folgende Zutaten hinzufuegen: Olivenoel, Salz, Pfeffer.")
        ));

        assertTrue(response.isConfigured());
        assertTrue(response.getMessage().contains("Olivenoel, Salz und Pfeffer"));
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
    void answer_should_execute_direct_shopping_list_command_from_user_message() {
        AppUser user = user();
        stubEmptyContext(user);
        List<String> ingredients = List.of("Salz", "Pfeffer");
        doReturn(new AiShoppingListToolResult(ingredients, List.of(), List.of()))
                .when(shoppingListTool).addMissingIngredients(user, ingredients);

        AiChatResponse response = underTest.answer(user, "kannst du mir Salz und Pfeffer nach geschmack in meine einkaufsliste packen? ich habe es nicht im vorrat", List.of());

        assertTrue(response.isConfigured());
        assertTrue(response.getMessage().contains("Salz und Pfeffer"));
        verify(shoppingListTool).addMissingIngredients(user, ingredients);
        verifyNoInteractions(groqClient);
    }

    @Test
    void answer_should_extract_salt_and_pepper_without_taste_suffix() {
        AppUser user = user();
        stubEmptyContext(user);
        List<String> ingredients = List.of("Salz", "Pfeffer");
        doReturn(new AiShoppingListToolResult(ingredients, List.of(), List.of()))
                .when(shoppingListTool).addMissingIngredients(user, ingredients);

        AiChatResponse response = underTest.answer(user, "fuege Salz und Pfeffer nach Geschmack hinzu", List.of());

        assertTrue(response.isConfigured());
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

        AiChatResponse response = underTest.answer(user, "mach Einkaufsliste", List.of());

        assertTrue(response.isConfigured());
        assertTrue(response.getMessage().contains("Welche konkreten Zutaten"));
        assertTrue(response.getMessage().contains("Limette, Olivenoel, Salz"));
        verifyNoInteractions(shoppingListTool);
        verifyNoInteractions(groqClient);
    }

    @Test
    void answer_should_report_when_all_ingredients_are_already_available_or_listed() {
        AppUser user = user();
        stubEmptyContext(user);
        List<String> ingredients = List.of("Avocado", "Reis", "Limette");
        doReturn(new AiShoppingListToolResult(List.of(), List.of("Avocado", "Reis"), List.of("Limette")))
                .when(shoppingListTool).addMissingIngredients(user, ingredients);

        AiChatResponse response = underTest.answer(user, "1", List.of(
                turn("assistant", "Zutaten: Avocado, Reis, Limette. Moechtest du (1) die Zutaten zur Einkaufsliste hinzufuegen?")
        ));

        assertTrue(response.isConfigured());
        assertTrue(response.getMessage().contains("Ich habe nichts neu hinzugefuegt, weil"));
        assertTrue(response.getMessage().contains("Avocado und Reis bereits im Vorrat sind"));
        assertTrue(response.getMessage().contains("Limette schon auf deiner Einkaufsliste steht"));
        verify(shoppingListTool).addMissingIngredients(user, ingredients);
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
    void answer_should_execute_meal_plan_command_with_previous_recipe_idea() {
        AppUser user = user();
        stubEmptyContext(user);
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        doReturn(new AiMealPlanToolResult(true, false, "Tomaten-Ei-Omelett", null, tomorrow, MealSlot.DINNER))
                .when(mealPlanTool).addToMealPlan(user, tomorrow, MealSlot.DINNER, null, "Tomaten-Ei-Omelett");

        AiChatResponse response = underTest.answer(user, "fuege es morgen Abend zum Wochenplan hinzu", List.of(
                turn("assistant", "Eine moegliche Idee waere Tomaten-Ei-Omelett.")
        ));

        assertTrue(response.isConfigured());
        assertTrue(response.getMessage().contains("Tomaten-Ei-Omelett"));
        assertTrue(response.getMessage().contains("morgen Abend"));
        verify(mealPlanTool).addToMealPlan(user, tomorrow, MealSlot.DINNER, null, "Tomaten-Ei-Omelett");
        verifyNoInteractions(shoppingListTool);
        verifyNoInteractions(groqClient);
    }

    @Test
    void answer_should_execute_short_tomorrow_dinner_with_previous_recipe_idea() {
        AppUser user = user();
        stubEmptyContext(user);
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        doReturn(new AiMealPlanToolResult(true, false, "Tomaten-Ei-Omelett", null, tomorrow, MealSlot.DINNER))
                .when(mealPlanTool).addToMealPlan(user, tomorrow, MealSlot.DINNER, null, "Tomaten-Ei-Omelett");

        AiChatResponse response = underTest.answer(user, "morgen Abend", List.of(
                turn("assistant", "Eine moegliche Idee waere Tomaten-Ei-Omelett.")
        ));

        assertTrue(response.isConfigured());
        assertTrue(response.getMessage().contains("Tomaten-Ei-Omelett"));
        verify(mealPlanTool).addToMealPlan(user, tomorrow, MealSlot.DINNER, null, "Tomaten-Ei-Omelett");
        verifyNoInteractions(groqClient);
    }

    @Test
    void answer_should_execute_numeric_recipe_option_for_tomorrow_dinner() {
        AppUser user = user();
        stubEmptyContext(user);
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        doReturn(new AiMealPlanToolResult(true, false, "Milchreis-Pudding", null, tomorrow, MealSlot.DINNER))
                .when(mealPlanTool).addToMealPlan(user, tomorrow, MealSlot.DINNER, null, "Milchreis-Pudding");

        AiChatResponse response = underTest.answer(user, "2 morgen Abend", List.of(
                turn("assistant", """
                        1. Omelett
                        2. Milchreis-Pudding
                        3. Glasnudelsalat
                        """)
        ));

        assertTrue(response.isConfigured());
        assertTrue(response.getMessage().contains("Milchreis-Pudding"));
        verify(mealPlanTool).addToMealPlan(user, tomorrow, MealSlot.DINNER, null, "Milchreis-Pudding");
        verifyNoInteractions(groqClient);
    }

    @Test
    void answer_should_ask_for_date_and_slot_when_meal_plan_command_is_incomplete() {
        AppUser user = user();
        stubEmptyContext(user);

        AiChatResponse response = underTest.answer(user, "zum Wochenplan hinzufuegen", List.of(
                turn("assistant", "Eine moegliche Idee waere Tomaten-Ei-Omelett.")
        ));

        assertTrue(response.isConfigured());
        assertTrue(response.getMessage().contains("Fuer welchen Tag und welche Mahlzeit"));
        verifyNoInteractions(mealPlanTool);
        verifyNoInteractions(groqClient);
    }

    @Test
    void answer_should_ask_for_slot_when_only_date_is_present() {
        AppUser user = user();
        stubEmptyContext(user);

        AiChatResponse response = underTest.answer(user, "morgen", List.of(
                turn("assistant", "Eine moegliche Idee waere Tomaten-Ei-Omelett.")
        ));

        assertTrue(response.isConfigured());
        assertEquals("Fuer morgen: Fruehstueck, Mittag oder Abendessen?", response.getMessage());
        verifyNoInteractions(mealPlanTool);
        verifyNoInteractions(groqClient);
    }

    @Test
    void answer_should_ask_for_date_when_only_slot_is_present() {
        AppUser user = user();
        stubEmptyContext(user);

        AiChatResponse response = underTest.answer(user, "Abendessen", List.of(
                turn("assistant", "Eine moegliche Idee waere Tomaten-Ei-Omelett.")
        ));

        assertTrue(response.isConfigured());
        assertEquals("Fuer welchen Tag soll ich es zum Abendessen eintragen?", response.getMessage());
        verifyNoInteractions(mealPlanTool);
        verifyNoInteractions(groqClient);
    }

    @Test
    void answer_should_ask_before_overwriting_existing_meal_plan_slot() {
        AppUser user = user();
        stubEmptyContext(user);
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        doReturn(new AiMealPlanToolResult(false, true, "Tomaten-Ei-Omelett", "Pasta", tomorrow, MealSlot.DINNER))
                .when(mealPlanTool).addToMealPlan(user, tomorrow, MealSlot.DINNER, null, "Tomaten-Ei-Omelett");

        AiChatResponse response = underTest.answer(user, "morgen Abend", List.of(
                turn("assistant", "Eine moegliche Idee waere Tomaten-Ei-Omelett.")
        ));

        assertTrue(response.isConfigured());
        assertEquals("Fuer morgen Abend ist bereits Pasta eingetragen. Soll ich es ersetzen?", response.getMessage());
        verify(mealPlanTool).addToMealPlan(user, tomorrow, MealSlot.DINNER, null, "Tomaten-Ei-Omelett");
        verifyNoInteractions(groqClient);
    }

    @Test
    void answer_should_ask_for_meal_title_when_no_recipe_context_exists() {
        AppUser user = user();
        stubEmptyContext(user);

        AiChatResponse response = underTest.answer(user, "trag es morgen Abend ein", List.of());

        assertTrue(response.isConfigured());
        assertEquals("Welches Gericht soll ich eintragen?", response.getMessage());
        verifyNoInteractions(mealPlanTool);
        verifyNoInteractions(groqClient);
    }

    @Test
    void answer_should_execute_combined_meal_plan_and_missing_shopping_list_command() {
        AppUser user = user();
        stubEmptyContext(user);
        LocalDate sunday = nextOrSame(java.time.DayOfWeek.SUNDAY);
        List<String> ingredients = List.of("Ei", "Salz", "Pfeffer");
        doReturn(new AiMealPlanToolResult(true, false, "Eiersalat", null, sunday, MealSlot.DINNER))
                .when(mealPlanTool).addToMealPlan(user, sunday, MealSlot.DINNER, null, "Eiersalat");
        doReturn(new AiShoppingListToolResult(List.of("Salz", "Pfeffer"), List.of("Ei"), List.of()))
                .when(shoppingListTool).addMissingIngredients(user, ingredients);

        AiChatResponse response = underTest.answer(user,
                "ja bitte fuege es fuer sonntag abend hinzu und fuege die fehlenden zutaten in meine einkaufsliste",
                List.of(turn("assistant", """
                        Wie waere es mit einem einfachen Eiersalat?
                        Zutaten: Ei, Salz, Pfeffer
                        Wenn Sie diese Idee interessant finden, kann ich Ihnen helfen, die fehlenden Zutaten auf Ihre Einkaufsliste zu setzen.
                        """)));

        assertTrue(response.isConfigured());
        assertEquals("Erledigt. Ich habe Eiersalat fuer Sonntag Abend in deinen Wochenplan eingetragen und Salz und Pfeffer zur Einkaufsliste hinzugefuegt.", response.getMessage());
        verify(mealPlanTool).addToMealPlan(user, sunday, MealSlot.DINNER, null, "Eiersalat");
        verify(shoppingListTool).addMissingIngredients(user, ingredients);
        verifyNoInteractions(groqClient);
    }

    @Test
    void answer_should_execute_combined_command_and_skip_existing_shopping_item() {
        AppUser user = user();
        stubEmptyContext(user);
        LocalDate sunday = nextOrSame(java.time.DayOfWeek.SUNDAY);
        List<String> ingredients = List.of("Ei", "Salz", "Pfeffer");
        doReturn(new AiMealPlanToolResult(true, false, "Eiersalat", null, sunday, MealSlot.DINNER))
                .when(mealPlanTool).addToMealPlan(user, sunday, MealSlot.DINNER, null, "Eiersalat");
        doReturn(new AiShoppingListToolResult(List.of("Pfeffer"), List.of("Ei"), List.of("Salz")))
                .when(shoppingListTool).addMissingIngredients(user, ingredients);

        AiChatResponse response = underTest.answer(user,
                "ja bitte fuer sonntag abend und die fehlenden zutaten einkaufsliste",
                List.of(turn("assistant", """
                        Wie waere es mit einem einfachen Eiersalat?
                        Zutaten: Ei, Salz, Pfeffer
                        """)));

        assertTrue(response.isConfigured());
        assertEquals("Erledigt. Ich habe Eiersalat fuer Sonntag Abend in deinen Wochenplan eingetragen und Pfeffer zur Einkaufsliste hinzugefuegt.", response.getMessage());
        verify(mealPlanTool).addToMealPlan(user, sunday, MealSlot.DINNER, null, "Eiersalat");
        verify(shoppingListTool).addMissingIngredients(user, ingredients);
        verifyNoInteractions(groqClient);
    }

    @Test
    void answer_should_not_run_shopping_list_for_plain_meal_plan_command() {
        AppUser user = user();
        stubEmptyContext(user);
        LocalDate sunday = nextOrSame(java.time.DayOfWeek.SUNDAY);
        doReturn(new AiMealPlanToolResult(true, false, "Eiersalat", null, sunday, MealSlot.DINNER))
                .when(mealPlanTool).addToMealPlan(user, sunday, MealSlot.DINNER, null, "Eiersalat");

        AiChatResponse response = underTest.answer(user, "fuege es fuer sonntag abend hinzu", List.of(
                turn("assistant", "Wie waere es mit einem einfachen Eiersalat? Zutaten: Ei, Salz, Pfeffer")
        ));

        assertTrue(response.isConfigured());
        assertTrue(response.getMessage().contains("Eiersalat"));
        verify(mealPlanTool).addToMealPlan(user, sunday, MealSlot.DINNER, null, "Eiersalat");
        verifyNoInteractions(shoppingListTool);
        verifyNoInteractions(groqClient);
    }

    @Test
    void answer_should_stop_combined_command_when_meal_plan_slot_is_occupied() {
        AppUser user = user();
        stubEmptyContext(user);
        LocalDate sunday = nextOrSame(java.time.DayOfWeek.SUNDAY);
        doReturn(new AiMealPlanToolResult(false, true, "Eiersalat", "Pasta", sunday, MealSlot.DINNER))
                .when(mealPlanTool).addToMealPlan(user, sunday, MealSlot.DINNER, null, "Eiersalat");

        AiChatResponse response = underTest.answer(user,
                "ja bitte fuege es fuer sonntag abend hinzu und fuege die fehlenden zutaten in meine einkaufsliste",
                List.of(turn("assistant", """
                        Wie waere es mit einem einfachen Eiersalat?
                        Zutaten: Ei, Salz, Pfeffer
                        """)));

        assertTrue(response.isConfigured());
        assertEquals("Fuer Sonntag Abend ist bereits Pasta eingetragen. Soll ich es ersetzen? Die fehlenden Zutaten habe ich nicht hinzugefuegt, bis wir das geklaert haben.", response.getMessage());
        verify(mealPlanTool).addToMealPlan(user, sunday, MealSlot.DINNER, null, "Eiersalat");
        verifyNoInteractions(shoppingListTool);
        verifyNoInteractions(groqClient);
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
        return recipe(title, category, calories, protein, ingredients, "en");
    }

    private Recipe recipe(String title, String category, Integer calories, Double protein, String ingredients, String language) {
        Recipe recipe = new Recipe();
        recipe.setTitle(title);
        recipe.setCategory(category);
        recipe.setCalories(calories);
        recipe.setProtein(protein);
        recipe.setIngredients(ingredients);
        recipe.setPublished(true);
        recipe.setLanguage(language);
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

    private LocalDate nextOrSame(java.time.DayOfWeek dayOfWeek) {
        return LocalDate.now().with(java.time.temporal.TemporalAdjusters.nextOrSame(dayOfWeek));
    }
}
