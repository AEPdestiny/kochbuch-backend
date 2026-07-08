package de.htwberlin.webtech.ai.model;

import java.util.List;

public record AiIntentDetectionResult(
        AiDetectedLanguage detectedLanguage,
        String normalizedUserRequest,
        AiIntent primaryIntent,
        List<AiActionPlan> plannedActions,
        double confidence,
        boolean needsClarification,
        String clarificationQuestion
) {
    public static AiIntentDetectionResult question(AiDetectedLanguage language, String normalizedUserRequest) {
        return new AiIntentDetectionResult(
                language,
                normalizedUserRequest,
                AiIntent.ASK_QUESTION,
                List.of(),
                0.4,
                false,
                null
        );
    }
}
