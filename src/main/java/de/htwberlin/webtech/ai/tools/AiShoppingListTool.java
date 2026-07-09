package de.htwberlin.webtech.ai.tools;

import de.htwberlin.webtech.pantry.entity.PantryItem;
import de.htwberlin.webtech.pantry.repository.PantryItemRepository;
import de.htwberlin.webtech.shopping.dto.ShoppingListItemRequest;
import de.htwberlin.webtech.shopping.entity.ShoppingListItem;
import de.htwberlin.webtech.shopping.service.ShoppingListService;
import de.htwberlin.webtech.user.entity.AppUser;
import jakarta.enterprise.context.ApplicationScoped;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@ApplicationScoped
public class AiShoppingListTool {

    private final ShoppingListService shoppingListService;
    private final PantryItemRepository pantryItemRepository;

    public AiShoppingListTool(ShoppingListService shoppingListService, PantryItemRepository pantryItemRepository) {
        this.shoppingListService = shoppingListService;
        this.pantryItemRepository = pantryItemRepository;
    }

    public AiShoppingListToolResult addMissingIngredients(AppUser currentUser, List<String> ingredients) {
        List<ShoppingListItem> existingShoppingItems = shoppingListService.listMine(currentUser);
        List<PantryItem> pantryItems = pantryItemRepository.findByOwner(currentUser);
        Set<String> shoppingNames = normalizedNames(existingShoppingItems.stream()
                .map(ShoppingListItem::getName)
                .toList());
        Set<String> pantryNames = normalizedNames(pantryItems.stream()
                .map(PantryItem::getName)
                .toList());

        List<String> added = new ArrayList<>();
        List<String> skippedPantry = new ArrayList<>();
        List<String> skippedShoppingList = new ArrayList<>();
        Set<String> handled = new LinkedHashSet<>();

        for (String ingredient : ingredients) {
            String cleaned = cleanIngredientName(ingredient);
            String normalized = normalizeName(cleaned);
            if (cleaned.isBlank() || normalized.isBlank() || isRejectedFragment(cleaned) || handled.contains(normalized)) {
                continue;
            }
            handled.add(normalized);
            if (pantryNames.contains(normalized)) {
                skippedPantry.add(cleaned);
                continue;
            }
            if (shoppingNames.contains(normalized)) {
                skippedShoppingList.add(cleaned);
                continue;
            }

            ShoppingListItemRequest request = new ShoppingListItemRequest();
            request.setName(cleaned);
            request.setChecked(false);
            shoppingListService.create(request, currentUser);
            shoppingNames.add(normalized);
            added.add(cleaned);
        }

        return new AiShoppingListToolResult(List.copyOf(added), List.copyOf(skippedPantry), List.copyOf(skippedShoppingList));
    }

    private Set<String> normalizedNames(List<String> names) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String name : names) {
            String value = normalizeName(name);
            if (!value.isBlank()) {
                normalized.add(value);
            }
        }
        return normalized;
    }

    private String cleanIngredientName(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replaceAll("(?iu)^(?:fehlende\\s+zutaten|zutaten|ingredients|malzemeler)\\s*[:\\-]\\s*", "")
                .replaceAll("(?iu)^(?:f.r\\s+das\\s+rezept\\s+brauchst\\s+du|fuer\\s+das\\s+rezept\\s+brauchst\\s+du|du\\s+ben.tigst|du\\s+benoetigst|folgende\\s+zutaten\\s+hinzuf.gen|folgende\\s+zutaten\\s+hinzufuegen)\\s*[:\\-]?\\s*", "")
                .replaceAll("^[\\-_*\\d.)\\s]+", "")
                .replaceAll("(?iu)^(g|kg|gr|gramm|ml|l|liter|stueck|stück|stk|tl|el|essloeffel|esslöffel|teeloeffel|teelöffel|prise|prisen|cup|cups|tasse|tassen|packung|dose)\\s+", "")
                .replaceAll("(?iu)\\b(?:zum\\s+braten|nach\\s+geschmack|gehackt|frisch|frische|frischer|optional)\\b", "")
                .replaceAll("[.;:]+$", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean isRejectedFragment(String value) {
        String normalized = normalizeForReject(value);
        return normalized.contains("in die einkaufsliste")
                || normalized.contains("in meine einkaufsliste")
                || normalized.contains("bitte")
                || normalized.contains("koennen sie")
                || normalized.contains("konnen sie")
                || normalized.contains("kannst du")
                || normalized.contains("moechten sie")
                || normalized.contains("mochten sie")
                || normalized.contains("um den")
                || normalized.contains("du hast bereits")
                || normalized.contains("im vorrat")
                || normalized.contains("aber noch")
                || normalized.contains("zubereiten")
                || normalized.contains("koch")
                || normalized.contains("brat")
                || normalized.contains("rezept")
                || normalized.contains("gericht")
                || normalized.startsWith("es fur")
                || normalized.startsWith("das fur")
                || normalized.startsWith("fur ")
                || normalized.contains("wochenplan")
                || normalized.contains("hinzu")
                || normalized.contains("eintragen")
                || normalized.contains("trag")
                || normalized.contains("fuge es")
                || normalized.contains("fuege es")
                || normalized.startsWith("es morgen")
                || normalized.startsWith("das morgen")
                || normalized.contains("sonntag")
                || normalized.contains("montag")
                || normalized.contains("dienstag")
                || normalized.contains("mittwoch")
                || normalized.contains("donnerstag")
                || normalized.contains("freitag")
                || normalized.contains("samstag")
                || normalized.contains("abend")
                || normalized.contains("mittag")
                || normalized.contains("fruhstuck");
    }

    private String normalizeForReject(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private String normalizeName(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
        normalized = normalized
                .replaceAll("^\\d+\\s*", "")
                .replaceAll("^(g|kg|gr|gramm|ml|l|liter|stueck|stuck|stk|tl|el|essloeffel|essloffel|teeloeffel|teeloffel|prise|prisen|cup|cups|tasse|tassen|packung|dose)\\s+", "")
                .replaceAll("\\b(zum braten|nach geschmack|gehackt|frisch|frische|frischer|optional)\\b", "")
                .replaceAll("\\s+", " ")
                .trim();
        normalized = singularize(normalized);
        return normalized;
    }

    private String singularize(String normalized) {
        if (normalized.equals("eier")) {
            return "ei";
        }
        if (normalized.endsWith("nen") && normalized.length() > 5) {
            return normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith("en") && normalized.length() > 4) {
            return normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith("e") && normalized.length() > 4) {
            return normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith("s") && normalized.length() > 3) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
