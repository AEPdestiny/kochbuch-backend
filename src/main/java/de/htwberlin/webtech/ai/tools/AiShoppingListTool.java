package de.htwberlin.webtech.ai.tools;

import de.htwberlin.webtech.pantry.entity.PantryItem;
import de.htwberlin.webtech.pantry.repository.PantryItemRepository;
import de.htwberlin.webtech.shopping.dto.ShoppingListItemRequest;
import de.htwberlin.webtech.shopping.entity.ShoppingListItem;
import de.htwberlin.webtech.shopping.service.ShoppingListService;
import de.htwberlin.webtech.user.entity.AppUser;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.math.MathContext;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class AiShoppingListTool {

    private static final Pattern LEADING_QUANTITY = Pattern.compile(
            "^(?<quantity>\\d+\\s*(?:-|\\u2013)\\s*\\d+|\\d+\\s*/\\s*\\d+|\\d+(?:[\\.,]\\d+)?)(?:\\s+|(?=\\p{L}))(?<rest>.+)$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern LEADING_UNIT = Pattern.compile(
            "^(?<unit>g|kg|gr|gramm|ml|l|liter|tl|el|essloeffel|essloffel|essl\\p{L}ffel|teeloeffel|teeloffel|teel\\p{L}ffel|tasse|tassen|stueck|stuck|st\\p{L}ck|stk|prise|prisen|bund|scheibe|scheiben|zehe|zehen)\\b\\s*(?<rest>.*)$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

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
            IngredientParse parsed = parseIngredient(ingredient);
            String cleaned = parsed.cleanedName();
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
            request.setQuantity(parsed.quantity());
            request.setUnit(parsed.unit());
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
        return parseIngredient(value).cleanedName();
    }

    private IngredientParse parseIngredient(String value) {
        if (value == null) {
            return new IngredientParse("", null, null);
        }
        String cleaned = value
                .replaceAll("(?iu)^(?:fehlende\\s+zutaten|zutaten|ingredients|malzemeler)\\s*[:\\-]\\s*", "")
                .replaceAll("(?iu)^(?:f.r\\s+das\\s+rezept\\s+brauchst\\s+du|fuer\\s+das\\s+rezept\\s+brauchst\\s+du|du\\s+ben.tigst|du\\s+benoetigst|folgende\\s+zutaten\\s+hinzuf.gen|folgende\\s+zutaten\\s+hinzufuegen)\\s*[:\\-]?\\s*", "")
                .replaceAll("^[\\-_*\\s]+", "")
                .trim();

        BigDecimal quantity = null;
        String unit = null;
        Matcher quantityMatcher = LEADING_QUANTITY.matcher(cleaned);
        if (quantityMatcher.matches()) {
            String rawQuantity = quantityMatcher.group("quantity");
            quantity = parseQuantity(rawQuantity);
            cleaned = quantityMatcher.group("rest").trim();
        }

        Matcher unitMatcher = LEADING_UNIT.matcher(cleaned);
        if (unitMatcher.matches()) {
            unit = normalizeUnit(unitMatcher.group("unit"));
            cleaned = unitMatcher.group("rest").trim();
        }

        cleaned = cleaned
                .replaceAll("(?iu)\\b(?:etwas|ca\\.?|circa|ungef.hr|ungefaehr)\\b", "")
                .replaceAll("(?iu)\\b(?:zum\\s+braten|nach\\s+bedarf|nach\\s+geschmack|gehackt|frisch|frische|frischer|optional)\\b", "")
                .replaceAll("(?iu)^\\s*(?:oder|und|&)\\s+", "")
                .replaceAll("^[\\-/_,.;:\\s]+", "")
                .replaceAll("[.;:]+$", "")
                .replaceAll("\\s+", " ")
                .trim();

        return new IngredientParse(cleaned, quantity, unit);
    }

    private BigDecimal parseQuantity(String rawQuantity) {
        if (rawQuantity == null || rawQuantity.isBlank() || rawQuantity.contains("-") || rawQuantity.contains("\u2013")) {
            return null;
        }
        String normalized = rawQuantity.replaceAll("\\s+", "").replace(',', '.');
        if (normalized.contains("/")) {
            String[] parts = normalized.split("/", 2);
            try {
                BigDecimal numerator = new BigDecimal(parts[0]);
                BigDecimal denominator = new BigDecimal(parts[1]);
                if (BigDecimal.ZERO.compareTo(denominator) == 0) {
                    return null;
                }
                return numerator.divide(denominator, MathContext.DECIMAL64).stripTrailingZeros();
            } catch (NumberFormatException e) {
                return null;
            }
        }
        try {
            return new BigDecimal(normalized).stripTrailingZeros();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String normalizeUnit(String unit) {
        if (unit == null || unit.isBlank()) {
            return null;
        }
        String normalized = normalizeForReject(unit);
        return switch (normalized) {
            case "g", "gr", "gramm" -> "g";
            case "kg" -> "kg";
            case "ml" -> "ml";
            case "l", "liter" -> "l";
            case "tl", "teeloeffel", "teeloffel" -> "TL";
            case "el", "essloeffel", "essloffel" -> "EL";
            case "tasse", "tassen" -> "Tasse";
            case "stueck", "stuck", "stk" -> "Stueck";
            case "prise", "prisen" -> "Prise";
            case "bund" -> "Bund";
            case "scheibe", "scheiben" -> "Scheibe";
            case "zehe", "zehen" -> "Zehe";
            default -> unit.trim();
        };
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
        String cleaned = cleanIngredientName(value);
        String normalized = Normalizer.normalize(cleaned, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
        normalized = normalized
                .replaceAll("\\b(zum braten|nach bedarf|nach geschmack|gehackt|frisch|frische|frischer|optional)\\b", "")
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

    private record IngredientParse(String cleanedName, BigDecimal quantity, String unit) {
    }
}
