package de.htwberlin.webtech.restaurant.service;

import de.htwberlin.webtech.restaurant.client.TavilyRestaurantSearchClient;
import de.htwberlin.webtech.restaurant.client.TavilyRestaurantSearchClient.TavilyRestaurantResult;
import de.htwberlin.webtech.restaurant.dto.RestaurantResponse;
import de.htwberlin.webtech.restaurant.dto.TavilyRestaurantSearchResponse;
import jakarta.enterprise.context.ApplicationScoped;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

    // Separators used in web page titles to divide the restaurant name from a page-type suffix
    private static final List<String> TITLE_SEPARATORS = List.of(" - ", " – ", " — ", " | ", " : ");

    // First words that indicate a snippet or sentence, not a restaurant name
    private static final Set<String> SENTENCE_STARTERS = Set.of(
            "von", "vom", "zu", "zum", "zur", "aus", "mit", "bei", "am", "im", "an",
            "unsere", "unserer", "unserem", "unseren", "unser", "wir", "ich",
            "neue", "neuen", "neuer", "neues",
            "from", "our", "here", "try", "taste", "visit", "discover", "find", "get",
            "check", "book", "order", "enjoy",
            "best", "top", "good", "great", "excellent",
            "menu", "speisekarte", "karte", "menü"
    );

    // Exact page titles that are generic descriptions, not a specific restaurant name
    private static final Set<String> GENERIC_PAGE_TITLES = Set.of(
            "speisekarte", "menu", "restaurant menu", "menü", "karte",
            "our menu", "food menu", "dinner menu", "online menu", "full menu",
            "unsere speisekarte", "unsere karte", "restaurant"
    );

    // Aggregator and social-media domains: the domain name itself is not a restaurant name
    private static final Set<String> AGGREGATOR_HOSTS = Set.of(
            "instagram.com", "facebook.com", "twitter.com", "x.com", "tiktok.com",
            "google.com", "maps.google.com",
            "tripadvisor.com", "tripadvisor.de", "tripadvisor.at", "tripadvisor.ch", "tripadvisor.co.uk",
            "lieferando.de", "lieferheld.de", "pizza.de",
            "ubereats.com", "doordash.com", "deliveroo.de", "deliveroo.com",
            "yelp.com", "yelp.de", "opentable.com", "thefork.com", "zomato.com",
            "pinterest.com", "youtube.com", "wikipedia.org", "reddit.com",
            "speisekarte.de", "allmenus.com",
            "chefkoch.de", "rezeptwelt.de", "lecker.de", "yummly.com", "allrecipes.com", "food.com"
    );

    // Ordered list of prefixes used to identify a restaurant name in content snippets
    private static final List<String> CONTENT_RESTAURANT_PREFIXES = List.of(
            "Restaurant ", "Café ", "Cafe ", "Trattoria ", "Ristorante ",
            "Bistro ", "Gasthaus ", "Wirtshaus ", "Gasthof ", "Fischrestaurant ",
            "Sushirestaurant ", "Steakhaus "
    );

    // Lowercase particles allowed in the middle of a restaurant name (e.g., "Trattoria di Mario")
    private static final Set<String> NAME_PARTICLES = Set.of(
            "de", "di", "van", "le", "la", "les", "des", "du", "del", "von"
    );

    private static final int MAX_RESTAURANT_NAME_WORDS = 8;
    private static final int MAX_RESTAURANT_NAME_LENGTH = 60;
    private static final int MIN_DISTINCTIVE_LENGTH = 4;
    private static final int MAX_CONTENT_NAME_WORDS = 4;

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
                .map(result -> toResponse(result, location, recipeTitle))
                .filter(Objects::nonNull)
                .toList();
        String status = matched.isEmpty() ? "no_results" : "ok";
        return new TavilyRestaurantSearchResponse(status, matched);
    }

    private boolean isPlausible(TavilyRestaurantResult result, String normalizedTitle) {
        String haystack = normalizeTitle(result.title() + " " + result.content());

        boolean hasContext = RESTAURANT_CONTEXT.stream().anyMatch(haystack::contains);
        if (!hasContext) return false;

        if (haystack.contains(normalizedTitle)) return true;

        String[] titleWords = normalizedTitle.split("\\s+");

        for (int i = 0; i < titleWords.length - 1; i++) {
            String w1 = titleWords[i];
            String w2 = titleWords[i + 1];
            if (!GENERIC_DISH_WORDS.contains(w1) || !GENERIC_DISH_WORDS.contains(w2)) {
                if (haystack.contains(w1 + " " + w2)) return true;
            }
        }

        for (String word : titleWords) {
            if (word.length() >= MIN_DISTINCTIVE_LENGTH
                    && !GENERIC_DISH_WORDS.contains(word)
                    && haystack.contains(word)) {
                return true;
            }
        }

        return false;
    }

    private RestaurantResponse toResponse(TavilyRestaurantResult result, String location, String recipeTitle) {
        String name = extractRestaurantName(result, recipeTitle);
        if (name == null) return null;

        RestaurantResponse response = new RestaurantResponse();
        response.setName(name);
        response.setAddress(null);
        response.setDistanceMeters(null);
        response.setLatitude(null);
        response.setLongitude(null);
        response.setGoogleMapsUrl(mapsUrl(name, location));
        return response;
    }

    // Visible for tests
    String extractRestaurantName(TavilyRestaurantResult result, String recipeTitle) {
        String title = result.title();
        if (title != null && !title.isBlank()) {
            for (String segment : splitAtSeparators(title)) {
                String trimmed = segment.trim();
                if (!trimmed.isEmpty() && isPlausibleRestaurantName(trimmed, recipeTitle)) {
                    return trimmed;
                }
            }
        }
        // URL domain fallback (skips aggregator domains)
        String urlName = fallbackNameFromUrl(result.url(), recipeTitle);
        if (urlName != null) return urlName;
        // Content-pattern extraction — works even for aggregator URLs
        return extractNameFromContent(result.content(), recipeTitle);
    }

    // Visible for tests
    String extractNameFromContent(String content, String recipeTitle) {
        if (content == null || content.isBlank()) return null;
        String contentLower = content.toLowerCase(Locale.ROOT);

        for (String prefix : CONTENT_RESTAURANT_PREFIXES) {
            String prefixLower = prefix.toLowerCase(Locale.ROOT);
            int idx = contentLower.indexOf(prefixLower);
            if (idx < 0) continue;

            String afterPrefix = content.substring(idx + prefix.length());
            String namePart = extractWordsForRestaurantName(afterPrefix);
            if (namePart == null) continue;

            String candidate = prefix.trim() + " " + namePart;
            if (isPlausibleRestaurantName(candidate, recipeTitle)) {
                return candidate;
            }
        }
        return null;
    }

    private String extractWordsForRestaurantName(String text) {
        if (text == null || text.isBlank()) return null;
        String[] words = text.trim().split("\\s+");
        List<String> nameWords = new ArrayList<>();

        for (String raw : words) {
            if (nameWords.size() >= MAX_CONTENT_NAME_WORDS) break;

            String word = raw.replaceAll("[.,;:!?()\"]$", "");
            if (word.isEmpty()) break;

            boolean startsUpper = Character.isUpperCase(word.charAt(0));
            boolean isParticle = NAME_PARTICLES.contains(word.toLowerCase(Locale.ROOT));

            if (nameWords.isEmpty()) {
                if (!startsUpper) return null; // first word must be a proper noun
            } else {
                if (!startsUpper && !isParticle) break;
            }
            nameWords.add(word);
        }

        // Drop trailing particles (e.g., "Trattoria de" alone → discard "de")
        while (!nameWords.isEmpty()
                && NAME_PARTICLES.contains(nameWords.getLast().toLowerCase(Locale.ROOT))) {
            nameWords.removeLast();
        }

        return nameWords.isEmpty() ? null : String.join(" ", nameWords);
    }

    // Split title at the first separator found, then recursively split the remainder
    private List<String> splitAtSeparators(String title) {
        int firstIdx = Integer.MAX_VALUE;
        String firstSep = null;
        for (String sep : TITLE_SEPARATORS) {
            int idx = title.indexOf(sep);
            if (idx > 0 && idx < firstIdx) {
                firstIdx = idx;
                firstSep = sep;
            }
        }
        if (firstSep == null) {
            return List.of(title);
        }
        List<String> parts = new ArrayList<>();
        parts.add(title.substring(0, firstIdx));
        parts.addAll(splitAtSeparators(title.substring(firstIdx + firstSep.length())));
        return parts;
    }

    // Visible for tests
    boolean isPlausibleRestaurantName(String candidate, String recipeTitle) {
        if (candidate == null || candidate.isBlank()) return false;
        String trimmed = candidate.trim();

        if (trimmed.length() > MAX_RESTAURANT_NAME_LENGTH) return false;

        String[] words = trimmed.split("\\s+");
        if (words.length > MAX_RESTAURANT_NAME_WORDS) return false;

        // Social media / snippet indicators
        if (trimmed.contains("#") || trimmed.contains("…") || trimmed.contains("...")) return false;

        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (GENERIC_PAGE_TITLES.contains(lower)) return false;
        if (isDishTitle(trimmed, recipeTitle)) return false;

        // First word indicates a sentence/snippet, not a proper noun
        String firstWord = words[0].toLowerCase(Locale.ROOT).replaceAll("[^a-zäöüß]", "");
        if (SENTENCE_STARTERS.contains(firstWord)) return false;

        return true;
    }

    // Returns true when candidate is (or contains) the dish name — not a valid restaurant name
    boolean isDishTitle(String candidate, String recipeTitle) {
        String normalizedCandidate = normalizeTitle(candidate);
        String normalizedRecipe = normalizeTitle(recipeTitle);
        if (normalizedCandidate.isEmpty() || normalizedRecipe.isEmpty()) return false;
        if (normalizedCandidate.equals(normalizedRecipe)) return true;
        // Candidate wraps the full dish name (e.g., "Pasta Carbonara Rezept" for recipe "Pasta Carbonara")
        if (normalizedRecipe.length() >= 10 && normalizedCandidate.contains(normalizedRecipe)) return true;
        return false;
    }

    // Derives a restaurant name from the URL domain when the title yields nothing
    String fallbackNameFromUrl(String url, String recipeTitle) {
        if (url == null || url.isBlank()) return null;
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null) return null;

            String domain = host.startsWith("www.") ? host.substring(4) : host;
            if (AGGREGATOR_HOSTS.contains(domain)) return null;

            String[] parts = domain.split("\\.");
            if (parts.length < 2) return null;

            // Registered domain label is second-to-last (handle .co.uk etc.)
            String domainName = parts[parts.length - 2];
            if (Set.of("co", "com", "org", "net", "gov", "edu").contains(domainName) && parts.length >= 3) {
                domainName = parts[parts.length - 3];
            }

            // Convert hyphens/underscores → title-cased words
            String name = Arrays.stream(domainName.split("[-_]"))
                    .filter(s -> !s.isEmpty())
                    .map(s -> Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase(Locale.ROOT))
                    .collect(Collectors.joining(" "));

            return isPlausibleRestaurantName(name, recipeTitle) ? name : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeTitle(String value) {
        if (value == null) return "";
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9äöüß\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String mapsUrl(String restaurantName, String location) {
        String query = URLEncoder.encode(restaurantName + " " + location, StandardCharsets.UTF_8);
        return "https://www.google.com/maps/search/?api=1&query=" + query;
    }
}
