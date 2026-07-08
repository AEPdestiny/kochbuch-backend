package de.htwberlin.webtech.ai.model;

import java.util.List;

public record AiClarificationRequest(
        String question,
        List<String> missingFields,
        List<AiActionPlan> pendingPlans
) {
}
