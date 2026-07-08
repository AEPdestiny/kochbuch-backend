package de.htwberlin.webtech.ai.model;

import de.htwberlin.webtech.ai.dto.AiChatRequest;
import de.htwberlin.webtech.user.entity.AppUser;

import java.util.List;

public record AiConversationContext(
        AppUser user,
        String message,
        List<AiChatRequest.AiChatTurn> history,
        String appContextSummary
) {
}
