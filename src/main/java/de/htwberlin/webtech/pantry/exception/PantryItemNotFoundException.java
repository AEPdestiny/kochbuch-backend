package de.htwberlin.webtech.pantry.exception;

public class PantryItemNotFoundException extends RuntimeException {

    public PantryItemNotFoundException(Long id) {
        super("Pantry item with ID " + id + " not found.");
    }
}
