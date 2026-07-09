package de.htwberlin.webtech.ai;

import de.htwberlin.webtech.ai.dto.AiChatRequest;
import de.htwberlin.webtech.ai.model.AiActionType;
import de.htwberlin.webtech.ai.model.AiDetectedLanguage;
import de.htwberlin.webtech.ai.model.AiIntent;
import de.htwberlin.webtech.ai.model.AiIntentDetectionResult;
import de.htwberlin.webtech.ai.orchestrator.AiIntentDetector;
import de.htwberlin.webtech.mealplan.entity.MealSlot;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiIntentDetectorTest {

    private final AiIntentDetector underTest = new AiIntentDetector();

    @Test
    void detect_should_map_numeric_reply_to_follow_up_selection() {
        AiIntentDetectionResult result = underTest.detect("1", numberedHistory());

        assertEquals(AiIntent.FOLLOW_UP_SELECTION, result.primaryIntent());
        assertEquals(AiActionType.ADD_INGREDIENTS_TO_SHOPPING_LIST, result.plannedActions().getFirst().type());
        assertFalse(result.needsClarification());
    }

    @Test
    void detect_should_map_multiple_numeric_replies_to_multiple_actions() {
        AiIntentDetectionResult result = underTest.detect("1 und 3", numberedHistory());

        assertEquals(AiIntent.FOLLOW_UP_SELECTION, result.primaryIntent());
        assertEquals(2, result.plannedActions().size());
        assertEquals(AiActionType.ADD_INGREDIENTS_TO_SHOPPING_LIST, result.plannedActions().get(0).type());
        assertEquals(AiActionType.ADD_RECIPE_TO_MEAL_PLAN, result.plannedActions().get(1).type());
        assertTrue(result.needsClarification());
    }

    @Test
    void detect_should_resolve_numeric_reply_to_previous_recipe_option_without_restaurant_action() {
        AiIntentDetectionResult result = underTest.detect("2", List.of(
                turn("assistant", """
                        (1) ein einfaches Ei-Rezept
                        (2) ein Rezept fuer Milchreis-Pfannkuchen
                        (3) ein anderes Gericht
                        """)
        ));

        assertEquals(AiIntent.FOLLOW_UP_SELECTION, result.primaryIntent());
        assertTrue(result.normalizedUserRequest().contains("Option 2"));
        assertTrue(result.normalizedUserRequest().contains("Milchreis-Pfannkuchen"));
        assertTrue(result.plannedActions().isEmpty());
        assertFalse(result.needsClarification());
    }

    @Test
    void detect_should_map_german_shopping_list_command() {
        AiIntentDetectionResult result = underTest.detect("fuege es zur einkaufsliste hinzu", List.of());

        assertEquals(AiIntent.ADD_TO_SHOPPING_LIST, result.primaryIntent());
        assertEquals(AiActionType.ADD_INGREDIENTS_TO_SHOPPING_LIST, result.plannedActions().getFirst().type());
        assertFalse(result.needsClarification());
    }

    @Test
    void detect_should_map_meal_plan_command_with_tomorrow_dinner() {
        AiIntentDetectionResult result = underTest.detect("mach es morgen abend rein", List.of());

        assertEquals(AiIntent.ADD_TO_MEAL_PLAN, result.primaryIntent());
        assertEquals(AiActionType.ADD_RECIPE_TO_MEAL_PLAN, result.plannedActions().getFirst().type());
        assertEquals(MealSlot.DINNER, result.plannedActions().getFirst().mealSlot());
        assertEquals(LocalDate.now().plusDays(1), result.plannedActions().getFirst().targetDate());
        assertFalse(result.needsClarification());
    }

    @Test
    void detect_should_map_recipe_detail_request() {
        AiIntentDetectionResult result = underTest.detect("details davon", List.of());

        assertEquals(AiIntent.OPEN_RECIPE_DETAILS, result.primaryIntent());
        assertEquals(AiActionType.OPEN_RECIPE, result.plannedActions().getFirst().type());
        assertTrue(result.needsClarification());
    }

    @Test
    void detect_should_map_restaurant_search_request() {
        AiIntentDetectionResult result = underTest.detect("restaurant suchen", List.of());

        assertEquals(AiIntent.FIND_RESTAURANT, result.primaryIntent());
        assertEquals(AiActionType.FIND_RESTAURANT, result.plannedActions().getFirst().type());
        assertTrue(result.needsClarification());
    }

    @Test
    void detect_should_map_turkish_shopping_list_command() {
        AiIntentDetectionResult result = underTest.detect("bunu alışveriş listesine ekle", List.of());

        assertEquals(AiDetectedLanguage.TR, result.detectedLanguage());
        assertEquals(AiIntent.ADD_TO_SHOPPING_LIST, result.primaryIntent());
        assertEquals(AiActionType.ADD_INGREDIENTS_TO_SHOPPING_LIST, result.plannedActions().getFirst().type());
    }

    @Test
    void detect_should_handle_typo_heavy_german_meal_plan_command() {
        AiIntentDetectionResult result = underTest.detect("morge abnd wochenplan", List.of());

        assertEquals(AiIntent.ADD_TO_MEAL_PLAN, result.primaryIntent());
        assertEquals(AiActionType.ADD_RECIPE_TO_MEAL_PLAN, result.plannedActions().getFirst().type());
        assertEquals(MealSlot.DINNER, result.plannedActions().getFirst().mealSlot());
        assertEquals(LocalDate.now().plusDays(1), result.plannedActions().getFirst().targetDate());
        assertFalse(result.needsClarification());
    }

    private List<AiChatRequest.AiChatTurn> numberedHistory() {
        return List.of(turn("assistant", "Moechtest du (1) die Zutaten zur Einkaufsliste hinzufuegen, (2) ein Restaurant finden, (3) es zum Wochenplan hinzufuegen?"));
    }

    private AiChatRequest.AiChatTurn turn(String role, String text) {
        AiChatRequest.AiChatTurn turn = new AiChatRequest.AiChatTurn();
        turn.setRole(role);
        turn.setText(text);
        return turn;
    }
}
