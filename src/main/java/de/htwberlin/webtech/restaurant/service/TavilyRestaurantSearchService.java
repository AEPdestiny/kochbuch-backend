package de.htwberlin.webtech.restaurant.service;

import de.htwberlin.webtech.restaurant.client.TavilyRestaurantSearchClient;
import de.htwberlin.webtech.restaurant.client.TavilyRestaurantSearchClient.TavilyRestaurantResult;
import de.htwberlin.webtech.restaurant.dto.RestaurantResponse;
import de.htwberlin.webtech.restaurant.dto.TavilyRestaurantSearchResponse;
import jakarta.enterprise.context.ApplicationScoped;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@ApplicationScoped
public class TavilyRestaurantSearchService {

    // Single dish-related words that alone are too generic to confirm a dish-specific match
    private static final Set<String> GENERIC_DISH_WORDS = Set.of(
            "pasta", "pizza", "sushi", "rice", "chicken", "soup", "salad",
            "bowl", "curry", "burger", "sandwich", "bread", "cake", "fish",
            "meat", "beef", "pork", "steak", "egg", "eggs", "taco", "wrap",
            "roll", "rolls", "ramen", "pho", "noodle", "noodles", "dip",
            "sauce", "broth", "stew", "toast", "pancake", "wok",
            "fried", "grilled", "baked", "steamed", "roasted",
            "asian", "chinese", "japanese", "korean", "italian", "french",
            "german", "indian", "mexican", "thai", "greek", "turkish", "spanish",
            "asiatisch", "chinesisch", "japanisch", "koreanisch",
            "koreanische", "koreanisches", "koreanischen",
            "italienisch", "indisch", "mexikanisch", "griechisch", "spanisch",
            "food", "dish", "meal", "plate", "snack", "starter", "dessert",
            "vegetarian", "vegan", "veggie", "tofu", "bean", "beans", "lentil",
            "spicy", "sweet", "sour", "creamy", "crispy"
    );

    // At least one of these must appear to confirm this is a restaurant/dining page
    private static final Set<String> RESTAURANT_CONTEXT = Set.of(
            "restaurant", "ristorante", "trattoria", "bistro", "cafe", "café",
            "menu", "speisekarte", "speiseangebot",
            "bestellen", "lieferung", "delivery", "takeaway", "takeout",
            "reservation", "reservierung", "buchen", "reservieren",
            "dine", "dining", "eatery", "diner",
            "grill", "lokal", "gastro", "gasthaus", "wirtshaus", "gasthof",
            "bewertung", "review", "rating", "stars",
            "öffnungszeiten"
    );

    private static final int MIN_DISTINCTIVE_LENGTH = 4;

    private final TavilyRestaurantSearchClient client;

    public TavilyRestaurantSearchService(TavilyRestaurantSearchClient client) {
        this.client = client;
    }

    public TavilyRestaurantSearchResponse search(String recipeTitle, String location) {
        if (!client.isConfigured()) {
            return new TavilyRestaurantSearchResponse("unavailable", List.of());
        }
        List<TavilyRestaurantResult> raw = client.search(recipeTitle, location);
        String normalizedTitle = normalizeTitle(recipeTitle);
        List<RestaurantResponse> matched = raw.stream()
                .filter(result -> isPlausible(result, normalizedTitle))
                .map(result -> toResponse(result, location))
                .toList();
        String status = matched.isEmpty() ? "no_results" : "ok";
        return new TavilyRestaurantSearchResponse(status, matched);
    }

    private boolean isPlausible(TavilyRestaurantResult result, String normalizedTitle) {
        String haystack = normalizeTitle(result.title() + " " + result.content());

        // Must look like a restaurant or dining page, not a recipe blog
        boolean hasContext = RESTAURANT_CONTEXT.stream().anyMatch(haystack::contains);
        if (!hasContext) {
            return false;
        }

        // Rule 1: Exact full (normalized) title appears in result
        if (haystack.contains(normalizedTitle)) {
            return true;
        }

        String[] titleWords = normalizedTitle.split("\\s+");

        // Rule 2: Consecutive word pair where at least one word is distinctive (not generic)
        for (int i = 0; i < titleWords.length - 1; i++) {
            String w1 = titleWords[i];
            String w2 = titleWords[i + 1];
            if (!GENERIC_DISH_WORDS.contains(w1) || !GENERIC_DISH_WORDS.contains(w2)) {
                if (haystack.contains(w1 + " " + w2)) {
                    return true;
                }
            }
        }

        // Rule 3: Single distinctive word of sufficient length
        for (String word : titleWords) {
            if (word.length() >= MIN_DISTINCTIVE_LENGTH
                    && !GENERIC_DISH_WORDS.contains(word)
                    && haystack.contains(word)) {
                return true;
            }
        }

        return false;
    }

    private String normalizeTitle(String value) {
        if (value == null) return "";
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9äöüß\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private RestaurantResponse toResponse(TavilyRestaurantResult result, String location) {
        RestaurantResponse response = new RestaurantResponse();
        String name = extractName(result.title());
        response.setName(name);
        response.setAddress(null);
        response.setDistanceMeters(null);
        response.setLatitude(null);
        response.setLongitude(null);
        response.setGoogleMapsUrl(mapsUrl(name, location));
        return response;
    }

    private String extractName(String title) {
        int dashIdx = title.indexOf(" - ");
        if (dashIdx > 0) {
            return title.substring(0, dashIdx).trim();
        }
        int emDashIdx = title.indexOf(" – ");
        if (emDashIdx > 0) {
            return title.substring(0, emDashIdx).trim();
        }
        return title;
    }

    private String mapsUrl(String restaurantName, String location) {
        String query = URLEncoder.encode(restaurantName + " " + location, StandardCharsets.UTF_8);
        return "https://www.google.com/maps/search/?api=1&query=" + query;
    }
}
