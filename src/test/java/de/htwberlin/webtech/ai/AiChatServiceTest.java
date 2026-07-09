package de.htwberlin.webtech.ai;

import de.htwberlin.webtech.ai.dto.AiChatRequest;
import de.htwberlin.webtech.ai.dto.AiChatResponse;
import de.htwberlin.webtech.ai.orchestrator.DishlyAiOrchestrator;
import de.htwberlin.webtech.ai.service.AiChatService;
import de.htwberlin.webtech.user.entity.AppUser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AiChatServiceTest {

    private final DishlyAiOrchestrator orchestrator = mock(DishlyAiOrchestrator.class);
    private final AiChatService underTest = new AiChatService(orchestrator);

    @Test
    void answer_should_delegate_to_orchestrator_with_history() {
        AppUser user = user();
        List<AiChatRequest.AiChatTurn> history = List.of(turn("assistant", "Moechtest du (1) Details oder (2) Restaurant?"));
        AiChatResponse expected = new AiChatResponse("Antwort", true);
        doReturn(expected).when(orchestrator).answer(user, "2", history, null);

        AiChatResponse response = underTest.answer(user, "2", history);

        assertEquals(expected, response);
        verify(orchestrator).answer(user, "2", history, null);
    }

    @Test
    void answer_should_delegate_to_orchestrator_with_locale() {
        AppUser user = user();
        List<AiChatRequest.AiChatTurn> history = List.of(turn("assistant", "Zutaten: Reis"));
        AiChatResponse expected = new AiChatResponse("Antwort", true);
        doReturn(expected).when(orchestrator).answer(user, "Hallo", history, "de");

        AiChatResponse response = underTest.answer(user, "Hallo", history, "de");

        assertEquals(expected, response);
        verify(orchestrator).answer(user, "Hallo", history, "de");
    }

    @Test
    void answer_without_history_should_delegate_with_empty_history() {
        AppUser user = user();
        AiChatResponse expected = new AiChatResponse("Antwort", true);
        doReturn(expected).when(orchestrator).answer(user, "Hallo", List.of(), null);

        AiChatResponse response = underTest.answer(user, "Hallo");

        assertEquals(expected, response);
        verify(orchestrator).answer(user, "Hallo", List.of(), null);
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
