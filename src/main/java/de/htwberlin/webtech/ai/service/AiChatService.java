package de.htwberlin.webtech.ai.service;

import de.htwberlin.webtech.ai.dto.AiChatRequest;
import de.htwberlin.webtech.ai.dto.AiChatResponse;
import de.htwberlin.webtech.ai.orchestrator.DishlyAiOrchestrator;
import de.htwberlin.webtech.user.entity.AppUser;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * API-facing facade for Dishly AI. The orchestration internals live behind this
 * service so the resource contract can stay stable while agent behavior evolves.
 */
@ApplicationScoped
public class AiChatService {

    private final DishlyAiOrchestrator orchestrator;

    public AiChatService(DishlyAiOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    public AiChatResponse answer(AppUser currentUser, String message) {
        return answer(currentUser, message, List.of());
    }

    public AiChatResponse answer(AppUser currentUser, String message, List<AiChatRequest.AiChatTurn> history) {
        return answer(currentUser, message, history, null);
    }

    public AiChatResponse answer(AppUser currentUser, String message, List<AiChatRequest.AiChatTurn> history, String locale) {
        return orchestrator.answer(currentUser, message, history, locale);
    }
}
