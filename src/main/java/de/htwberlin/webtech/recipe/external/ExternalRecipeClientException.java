package de.htwberlin.webtech.recipe.external;

public class ExternalRecipeClientException extends RuntimeException {

    public ExternalRecipeClientException(String message) {
        super(message);
    }

    public ExternalRecipeClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
