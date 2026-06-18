package de.htwberlin.webtech.recipe.instructions;

public class InstructionSearchClientException extends RuntimeException {

    public InstructionSearchClientException(String message) {
        super(message);
    }

    public InstructionSearchClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
