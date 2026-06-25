package de.htwberlin.webtech.recipe.image;

public class SupabaseStorageException extends RuntimeException {

    public SupabaseStorageException(String message) {
        super(message);
    }

    public SupabaseStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
