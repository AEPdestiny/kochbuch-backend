package de.htwberlin.webtech.ai.dto;

import jakarta.validation.constraints.NotBlank;

public class AiChatRequest {

    @NotBlank(message = "must not be blank")
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
