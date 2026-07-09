package de.htwberlin.webtech.ai.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

public class AiChatRequest {

    @NotBlank(message = "must not be blank")
    private String message;
    private List<AiChatTurn> history = new ArrayList<>();
    private String locale;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<AiChatTurn> getHistory() {
        return history == null ? List.of() : history;
    }

    public void setHistory(List<AiChatTurn> history) {
        this.history = history == null ? new ArrayList<>() : history;
    }

    public String getLocale() {
        return normalizeLocale(locale);
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    private String normalizeLocale(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toLowerCase().split("[-_]")[0];
        return normalized.matches("[a-z]{2}") ? normalized : null;
    }

    public static class AiChatTurn {
        private String role;
        private String text;

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
