package de.htwberlin.webtech.shopping.exception;

public class ShoppingListItemNotFoundException extends RuntimeException {

    public ShoppingListItemNotFoundException(Long id) {
        super("Shopping list item with ID " + id + " not found.");
    }
}
