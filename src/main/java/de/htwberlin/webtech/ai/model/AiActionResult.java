package de.htwberlin.webtech.ai.model;

public record AiActionResult(
        AiActionType type,
        boolean success,
        String userMessage,
        Object payload
) {
}
