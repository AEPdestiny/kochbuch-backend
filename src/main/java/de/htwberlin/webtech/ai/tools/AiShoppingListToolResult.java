package de.htwberlin.webtech.ai.tools;

import java.util.List;

public record AiShoppingListToolResult(
        List<String> addedItems,
        List<String> skippedPantryItems,
        List<String> skippedShoppingListItems
) {
    public boolean changedAnything() {
        return !addedItems.isEmpty();
    }
}
