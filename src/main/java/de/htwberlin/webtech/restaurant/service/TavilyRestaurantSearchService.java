package de.htwberlin.webtech.restaurant.service;

import de.htwberlin.webtech.restaurant.client.GeoapifyClient;
import de.htwberlin.webtech.restaurant.client.GeoapifyClientException;
import de.htwberlin.webtech.restaurant.client.TavilyRestaurantSearchClient;
import de.htwberlin.webtech.restaurant.client.TavilyRestaurantSearchClient.TavilyRestaurantResult;
import de.htwberlin.webtech.restaurant.client.dto.GeoapifyFeature;
import de.htwberlin.webtech.restaurant.client.dto.GeoapifyResponse;
import de.htwberlin.webtech.restaurant.dto.RestaurantResponse;
import de.htwberlin.webtech.restaurant.dto.TavilyRestaurantSearchResponse;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import de.htwberlin.webtech.restaurant.client.GeoapifyClient.ReverseGeocodeResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
            "unsere speisekarte", "unsere karte", "restaurant",
            "blog", "food", "cooking", "foodblog", "rezept", "rezepte", "essen", "kochen"
    );

    // Hosts whose entire result must be dropped (community/forum content, never a restaurant page)
    private static final Set<String> HARD_BLOCK_HOSTS = Set.of(
            "reddit.com", "old.reddit.com", "np.reddit.com",
            "quora.com", "stackexchange.com"
    );

    // Article/list/guide title phrases (lowercase substrings) — indicate a listicle, not a restaurant
    private static final List<String> ARTICLE_LIST_INDICATORS = List.of(
            "best restaurants", "top restaurants", "restaurants in",
            "where to eat", "guide to", "ultimate guide",
            "spots in", "places to eat", "best sushi", "best pizza",
            "best places", "top places", "top sushi", "top pizza"
    );

    // Article/list title prefixes (lowercase) — words at the very start that indicate a listicle
    private static final List<String> ARTICLE_LIST_PREFIXES = List.of(
            "the top ", "top ", "best ", "the best ",
            "the ultimate ", "where to ", "how to "
    );

    // Platform/brand names that must never appear as restaurant names
    private static final Set<String> BLOCKED_RESTAURANT_NAMES = Set.of(
            "tiktok", "instagram", "facebook", "youtube", "twitter",
            "x", "pinterest", "linkedin", "google", "tripadvisor",
            "lieferando", "wolt", "ubereats", "deliveroo", "doordash",
            "yelp", "thefork", "zomato", "opentable", "reddit",
            "wikipedia", "chefkoch", "allrecipes", "yummly", "snapchat"
    );

    // Words/phrases that indicate a recipe blog post, not a restaurant page
    private static final Set<String> RECIPE_BLOG_INDICATORS = Set.of(
            "recipe", "recipes", "rezept", "rezepte",
            "how to make", "how to cook", "how to prepare",
            "step by step", "zubereitung", "ingredients", "instructions"
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
    private static final int GEO_ENRICH_RADIUS_METERS = 20000;
    // Upper bound for the Geoapify search circle when GPS accuracy is poor
    private static final int MAX_ENRICH_RADIUS_METERS = 50000;
    // A Geoapify hit farther than this from the user is a wrong-city/country match (e.g. Berlin, NJ) → discard
    private static final int MAX_ENRICH_DISTANCE_METERS = 100000;

    // Known (mostly German) city names → country context, to disambiguate ambiguous names like "Berlin"
    private record KnownCity(String country, String countryCode) {
    }

    private static final KnownCity DE = new KnownCity("Germany", "de");
    private static final Map<String, KnownCity> KNOWN_CITIES = Map.ofEntries(
            Map.entry("berlin", DE),
            Map.entry("münchen", DE), Map.entry("munchen", DE), Map.entry("munich", DE),
            Map.entry("hamburg", DE),
            Map.entry("köln", DE), Map.entry("koeln", DE), Map.entry("cologne", DE),
            Map.entry("frankfurt", DE),
            Map.entry("stuttgart", DE),
            Map.entry("düsseldorf", DE), Map.entry("dusseldorf", DE),
            Map.entry("dortmund", DE), Map.entry("leipzig", DE), Map.entry("dresden", DE),
            Map.entry("bremen", DE), Map.entry("hannover", DE),
            Map.entry("nürnberg", DE), Map.entry("nuernberg", DE), Map.entry("nuremberg", DE)
    );

    // Location context resolved from the typed city and/or the user's GPS coordinates.
    // tavilyCountry is only set from GPS reverse geocoding; mapsCountry additionally uses the known-city table.
    // mismatch = true when the typed city and the GPS location disagree on country → GPS won.
    private record LocationContext(String city, String tavilyCountry, String mapsCountry,
                                   String countryCode, boolean mismatch) {
        String tavilyLabel() {
            return tavilyCountry == null ? city : city + " " + tavilyCountry;
        }

        String mapsQueryLabel() {
            return mapsCountry == null ? city : city + " " + mapsCountry;
        }

        String displayLabel() {
            return mapsCountry == null ? city : city + ", " + mapsCountry;
        }
    }

    private static final Logger LOG = Logger.getLogger(TavilyRestaurantSearchService.class);

    private final TavilyRestaurantSearchClient client;
    private final GeoapifyClient geoapifyClient;

    public TavilyRestaurantSearchService(TavilyRestaurantSearchClient client, GeoapifyClient geoapifyClient) {
        this.client = client;
        this.geoapifyClient = geoapifyClient;
    }

    public TavilyRestaurantSearchResponse search(String recipeTitle, String location, Double userLat, Double userLon) {
        return search(recipeTitle, location, userLat, userLon, null);
    }

    public TavilyRestaurantSearchResponse search(String recipeTitle, String location,
                                                 Double userLat, Double userLon, Double accuracyMeters) {
        if (!client.isConfigured()) {
            return new TavilyRestaurantSearchResponse("unavailable", List.of());
        }

        boolean hasGps = userLat != null && userLon != null;
        boolean typedBlank = location == null || location.isBlank();

        // Reverse geocode the user's coordinates to derive city + country (disambiguates "Berlin")
        ReverseGeocodeResult geo = hasGps ? reverseGeocodeSafe(userLat, userLon) : null;

        // GPS-only search needs a reverse-geocoded city
        if (typedBlank && hasGps && (geo == null || geo.city() == null || geo.city().isBlank())) {
            return new TavilyRestaurantSearchResponse("no_location", List.of());
        }
        // No location text and no usable GPS (defensive — the resource already guards this)
        if (typedBlank && geo == null) {
            return new TavilyRestaurantSearchResponse("no_results", List.of());
        }

        LocationContext ctx = buildLocationContext(location, geo);
        // Show the disambiguated label whenever a country is known or the city came from GPS
        String resolvedLocation = (ctx.mapsCountry() != null || typedBlank) ? ctx.displayLabel() : null;
        int radius = enrichRadius(accuracyMeters);

        // Stage 1 — exact dish search: only real restaurants serving exactly this dish
        List<RestaurantResponse> exact = runExactSearch(recipeTitle, ctx, userLat, userLon, radius);
        if (!exact.isEmpty()) {
            return buildResponse("ok", "exact", exact, resolvedLocation, ctx.mismatch());
        }

        // Stage 2 — general cuisine suggestions when no exact match survives filtering
        String category = deriveRestaurantCategory(recipeTitle);
        List<RestaurantResponse> suggestions = runSuggestionSearch(category, ctx, recipeTitle, userLat, userLon, radius);
        if (!suggestions.isEmpty()) {
            return buildResponse("ok", "suggestions", suggestions, resolvedLocation, ctx.mismatch());
        }

        return buildResponse("no_results", null, List.of(), resolvedLocation, ctx.mismatch());
    }

    // Geoapify search circle: 20 km by default, widened for poor GPS accuracy, capped at 50 km
    private int enrichRadius(Double accuracyMeters) {
        if (accuracyMeters == null || accuracyMeters <= 0) return GEO_ENRICH_RADIUS_METERS;
        int scaled = (int) Math.min(MAX_ENRICH_RADIUS_METERS, Math.max(GEO_ENRICH_RADIUS_METERS, accuracyMeters * 2));
        return scaled;
    }

    // Builds the location context: city + country used for Tavily queries, Maps links and display.
    // GPS coordinates are the primary truth: when the typed city clearly belongs to a different country
    // than the user's GPS position, GPS wins and the typed city is discarded (Fall B).
    private LocationContext buildLocationContext(String typedLocation, ReverseGeocodeResult geo) {
        String typed = typedLocation == null ? "" : typedLocation.trim();
        String gpsCity = geo != null ? blankToNull(geo.city()) : null;
        String gpsCountry = geo != null ? blankToNull(geo.country()) : null;
        String gpsCountryCode = geo != null ? blankToNull(geo.countryCode()) : null;

        if (typed.isBlank()) {
            // GPS-only: city and country come from reverse geocoding
            return new LocationContext(gpsCity, gpsCountry, gpsCountry, gpsCountryCode, false);
        }

        KnownCity known = KNOWN_CITIES.get(typed.toLowerCase(Locale.ROOT));
        String knownCountryCode = known != null ? known.countryCode() : null;

        // Fall B — typed city belongs to a different country than the GPS position: GPS wins.
        boolean crossCountryConflict = gpsCountryCode != null && knownCountryCode != null
                && !gpsCountryCode.equalsIgnoreCase(knownCountryCode);
        if (crossCountryConflict) {
            String city = gpsCity != null ? gpsCity : typed;
            return new LocationContext(city, gpsCountry, gpsCountry, gpsCountryCode, true);
        }

        // Fall A / same country / unknown typed city: keep the typed city, country from GPS or known table
        String knownCountry = known != null ? known.country() : null;
        String countryCode = gpsCountryCode != null ? gpsCountryCode : knownCountryCode;
        // Tavily query only gets a country when GPS confirms it; Maps/display also use the known-city table
        String mapsCountry = gpsCountry != null ? gpsCountry : knownCountry;
        return new LocationContext(typed, gpsCountry, mapsCountry, countryCode, false);
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private TavilyRestaurantSearchResponse buildResponse(String status, String searchMode,
                                                         List<RestaurantResponse> results,
                                                         String resolvedLocation, boolean locationMismatch) {
        TavilyRestaurantSearchResponse response = new TavilyRestaurantSearchResponse(status, results);
        response.setSearchMode(searchMode);
        if (resolvedLocation != null) {
            response.setResolvedLocation(resolvedLocation);
        }
        response.setLocationMismatch(locationMismatch);
        return response;
    }

    private List<RestaurantResponse> runExactSearch(String recipeTitle, LocationContext ctx,
                                                    Double userLat, Double userLon, int radius) {
        List<TavilyRestaurantResult> raw = client.search(recipeTitle, ctx.tavilyLabel());
        String normalizedTitle = normalizeTitle(recipeTitle);
        return raw.stream()
                .filter(result -> isPlausible(result, normalizedTitle))
                .map(result -> toResponse(result, ctx, recipeTitle, userLat, userLon, radius))
                .filter(Objects::nonNull)
                .toList();
    }

    private List<RestaurantResponse> runSuggestionSearch(String category, LocationContext ctx, String recipeTitle,
                                                         Double userLat, Double userLon, int radius) {
        List<TavilyRestaurantResult> raw = client.searchGeneral(suggestionQuery(category, ctx.tavilyLabel()));
        return raw.stream()
                .filter(this::isPlausibleSuggestion)
                .map(result -> toResponse(result, ctx, recipeTitle, userLat, userLon, radius))
                .filter(Objects::nonNull)
                .toList();
    }

    private String suggestionQuery(String category, String location) {
        String loc = location == null ? "" : location.trim();
        if ("restaurant".equals(category)) {
            return ("restaurants " + loc).trim();
        }
        return (category + " restaurant " + loc).trim();
    }

    // Derives a cuisine/category keyword from the recipe title (German + English). Public for tests.
    public String deriveRestaurantCategory(String recipeTitle) {
        String t = recipeTitle == null ? "" : recipeTitle.toLowerCase(Locale.ROOT);
        if (containsAny(t, "sushi", "nigiri", "maki", "sashimi")) return "sushi";
        if (containsAny(t, "ramen", "udon", "teriyaki")) return "japanese";
        if (containsAny(t, "pizza")) return "pizza";
        if (containsAny(t, "pasta", "spaghetti", "lasagna", "lasagne", "risotto",
                "gnocchi", "carbonara", "nudeln", "tagliatelle", "penne")) return "italian";
        if (containsAny(t, "ratatouille", "quiche", "croissant")) return "french";
        if (containsAny(t, "curry", "masala", "tikka", "biryani")) return "indian";
        if (containsAny(t, "taco", "burrito", "quesadilla")) return "mexican";
        if (containsAny(t, "burger")) return "burger";
        if (containsAny(t, "kebab", "döner", "doner", "lahmacun")) return "turkish";
        if (containsAny(t, "falafel", "hummus", "shawarma", "shawerma")) return "middle eastern";
        if (containsAny(t, "salad", "salat", "bowl")) return "healthy";
        if (containsAny(t, "cake", "waffle", "waffel", "pancake", "pfannkuchen", "dessert", "kuchen")) return "cafe";
        return "restaurant";
    }

    private boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) return true;
        }
        return false;
    }

    private ReverseGeocodeResult reverseGeocodeSafe(double lat, double lon) {
        try {
            return geoapifyClient.reverseGeocode(lat, lon);
        } catch (Exception e) {
            LOG.debugf("Reverse geocoding failed for %.4f,%.4f: %s", lat, lon, e.getMessage());
            return null;
        }
    }

    // Discard the whole result when the source URL or title is clearly a listicle,
    // subreddit, blocked platform, or recipe/how-to page — no URL/content fallback allowed.
    private boolean isHardExcluded(TavilyRestaurantResult result) {
        if (isHardBlockedUrl(result.url())) return true;
        String rawTitleLower = result.title() == null ? "" : result.title().toLowerCase(Locale.ROOT).trim();
        if (!rawTitleLower.isEmpty()) {
            if (isArticleListTitle(rawTitleLower)) return true;
            if (isBlockedName(rawTitleLower)) return true;
            if (RECIPE_BLOG_INDICATORS.stream().anyMatch(rawTitleLower::contains)) return true;
        }
        return false;
    }

    // Suggestion mode: real restaurant page, but NOT required to match the exact dish
    private boolean isPlausibleSuggestion(TavilyRestaurantResult result) {
        if (isHardExcluded(result)) return false;
        String haystack = normalizeTitle(result.title() + " " + result.content());
        return RESTAURANT_CONTEXT.stream().anyMatch(haystack::contains);
    }

    private boolean isPlausible(TavilyRestaurantResult result, String normalizedTitle) {
        if (isHardExcluded(result)) return false;
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

    private RestaurantResponse toResponse(TavilyRestaurantResult result, LocationContext ctx, String recipeTitle,
                                          Double userLat, Double userLon, int radius) {
        String name = extractRestaurantName(result, recipeTitle);
        if (name == null) return null;

        RestaurantResponse response = new RestaurantResponse();
        response.setName(name);
        response.setAddress(null);
        response.setDistanceMeters(null);
        response.setLatitude(null);
        response.setLongitude(null);
        enrichWithGeoapify(response, ctx, userLat, userLon, radius);
        response.setGoogleMapsUrl(mapsUrl(name, ctx.mapsQueryLabel(), response.getLatitude(), response.getLongitude(), userLat, userLon));
        return response;
    }

    private void enrichWithGeoapify(RestaurantResponse response, LocationContext ctx,
                                    Double userLat, Double userLon, int radius) {
        if (userLat == null || userLon == null) return;
        try {
            String query = response.getName() + " " + ctx.city();
            GeoapifyResponse geoResponse = geoapifyClient.searchRestaurants(query, userLat, userLon, radius, 1);
            if (geoResponse == null || geoResponse.getFeatures() == null || geoResponse.getFeatures().isEmpty()) return;
            GeoapifyFeature feature = geoResponse.getFeatures().get(0);
            if (feature.getGeometry() == null || feature.getProperties() == null) return;
            // Only enrich with results confirmed as catering/restaurant
            List<String> cats = feature.getProperties().getCategories();
            if (cats == null || cats.stream().noneMatch(c -> c.startsWith("catering"))) return;
            // GPS coordinates are the truth: reject a hit whose country differs from the user's country
            String hitCountryCode = blankToNull(feature.getProperties().getCountryCode());
            if (ctx.countryCode() != null && hitCountryCode != null
                    && !hitCountryCode.equalsIgnoreCase(ctx.countryCode())) {
                LOG.debugf("Discarding Geoapify hit for '%s': country %s != user country %s",
                        response.getName(), hitCountryCode, ctx.countryCode());
                return;
            }
            List<Double> coords = feature.getGeometry().getCoordinates();
            if (coords == null || coords.size() < 2 || coords.get(0) == null || coords.get(1) == null) return;
            double restLon = coords.get(0);
            double restLat = coords.get(1);
            int distance = haversineMeters(userLat, userLon, restLat, restLon);
            // Reject hits that are implausibly far away (wrong city/country, e.g. Berlin, New Jersey)
            if (distance > MAX_ENRICH_DISTANCE_METERS) {
                LOG.debugf("Discarding Geoapify hit for '%s': %d m from user is too far", response.getName(), distance);
                return;
            }
            response.setLatitude(restLat);
            response.setLongitude(restLon);
            String address = feature.getProperties().getFormatted();
            if (address != null && !address.isBlank()) response.setAddress(address);
            response.setDistanceMeters(distance);
        } catch (GeoapifyClientException e) {
            LOG.debugf("Geoapify enrichment skipped for '%s': %s", response.getName(), e.getMessage());
        } catch (Exception e) {
            LOG.debugf("Geoapify enrichment error for '%s': %s", response.getName(), e.getMessage());
        }
    }

    private static int haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return (int) Math.round(R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
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
        if (isBlockedName(lower)) return false;
        if (RECIPE_BLOG_INDICATORS.stream().anyMatch(lower::contains)) return false;
        if (isArticleListTitle(lower)) return false;
        if (isDishTitle(trimmed, recipeTitle)) return false;

        // First word indicates a sentence/snippet, not a proper noun
        String firstWord = words[0].toLowerCase(Locale.ROOT).replaceAll("[^a-zäöüß]", "");
        if (SENTENCE_STARTERS.contains(firstWord)) return false;

        return true;
    }

    private boolean isBlockedName(String lower) {
        for (String word : lower.split("\\s+")) {
            String clean = word.replaceAll("[^a-z0-9äöüß]", "");
            if (BLOCKED_RESTAURANT_NAMES.contains(clean)) return true;
        }
        return false;
    }

    private boolean isHardBlockedUrl(String url) {
        if (url == null || url.isBlank()) return false;
        try {
            String host = URI.create(url).getHost();
            if (host == null) return false;
            String domain = host.startsWith("www.") ? host.substring(4) : host;
            return HARD_BLOCK_HOSTS.contains(domain);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isArticleListTitle(String lower) {
        // Reddit / subreddit prefix
        if (lower.startsWith("r/")) return true;
        // Prefix-based patterns like "the top ", "top ", "best ", "where to ", "how to "
        for (String prefix : ARTICLE_LIST_PREFIXES) {
            if (lower.startsWith(prefix)) return true;
        }
        // Substring patterns like "restaurants in", "where to eat", "guide to"
        for (String phrase : ARTICLE_LIST_INDICATORS) {
            if (lower.contains(phrase)) return true;
        }
        // Number + restaurants/spots/places pattern (e.g., "11 sushi restaurants", "10 places")
        if (lower.matches(".*\\b\\d+\\s+\\w*\\s*(restaurants|spots|places).*")) return true;
        return false;
    }

    // Returns true when candidate is (or contains) the dish name — not a valid restaurant name
    boolean isDishTitle(String candidate, String recipeTitle) {
        String normalizedCandidate = normalizeTitle(candidate);
        String normalizedRecipe = normalizeTitle(recipeTitle);
        if (normalizedCandidate.isEmpty() || normalizedRecipe.isEmpty()) return false;
        if (normalizedCandidate.equals(normalizedRecipe)) return true;
        // Candidate wraps the full dish name (e.g., "Pasta Carbonara Rezept" for "Pasta Carbonara")
        if (normalizedRecipe.length() >= 10 && normalizedCandidate.contains(normalizedRecipe)) return true;
        // Candidate shares >= 2 non-trivial words with recipe title → likely a dish/blog title
        Set<String> recipeWordSet = Arrays.stream(normalizedRecipe.split("\\s+")).collect(Collectors.toSet());
        long sharedWords = Arrays.stream(normalizedCandidate.split("\\s+"))
                .filter(w -> w.length() >= 4)
                .filter(recipeWordSet::contains)
                .filter(w -> !NAME_PARTICLES.contains(w))
                .count();
        if (sharedWords >= 2) return true;
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

    private String mapsUrl(String restaurantName, String mapsQueryLabel, Double restLat, Double restLon, Double userLat, Double userLon) {
        boolean hasRestCoords = restLat != null && restLon != null;
        boolean hasUserCoords = userLat != null && userLon != null;

        // Precise route when both endpoints are known
        if (hasRestCoords && hasUserCoords) {
            String origin = URLEncoder.encode(userLat + "," + userLon, StandardCharsets.UTF_8);
            String dest = URLEncoder.encode(restLat + "," + restLon, StandardCharsets.UTF_8);
            return "https://www.google.com/maps/dir/?api=1&origin=" + origin + "&destination=" + dest;
        }
        if (hasRestCoords) {
            return "https://www.google.com/maps/search/?api=1&query=" + restLat + "," + restLon;
        }

        // No restaurant coordinates: use a country-qualified text query so "Berlin" is never ambiguous
        String query = URLEncoder.encode(restaurantName + " " + mapsQueryLabel, StandardCharsets.UTF_8);
        if (hasUserCoords) {
            String origin = URLEncoder.encode(userLat + "," + userLon, StandardCharsets.UTF_8);
            return "https://www.google.com/maps/dir/?api=1&origin=" + origin + "&destination=" + query;
        }
        return "https://www.google.com/maps/search/?api=1&query=" + query;
    }
}
