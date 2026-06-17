package de.htwberlin.webtech.ai.dto;

public class AiChatResponse {

    private String message;
    private boolean configured;

    public AiChatResponse() {
    }

    public AiChatResponse(String message, boolean configured) {
        this.message = message;
        this.configured = configured;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isConfigured() {
        return configured;
    }

    public void setConfigured(boolean configured) {
        this.configured = configured;
    }
}
