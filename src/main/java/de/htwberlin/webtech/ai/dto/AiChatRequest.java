package de.htwberlin.webtech.ai.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

public class AiChatRequest {

    @NotBlank(message = "must not be blank")
    private String message;
    private List<AiChatTurn> history = new ArrayList<>();

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
