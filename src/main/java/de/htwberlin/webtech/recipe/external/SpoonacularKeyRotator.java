package de.htwberlin.webtech.recipe.external;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

class SpoonacularKeyRotator {

    private final List<String> keys;
    private final AtomicInteger index = new AtomicInteger(0);

    SpoonacularKeyRotator(Optional<String> multiKeys, Optional<String> singleKey) {
        this.keys = resolveKeys(multiKeys, singleKey);
    }

    SpoonacularKeyRotator(List<String> keys) {
        this.keys = List.copyOf(keys);
    }

    private static List<String> resolveKeys(Optional<String> multi, Optional<String> single) {
        if (multi.isPresent() && !multi.get().isBlank()) {
            List<String> parsed = Arrays.stream(multi.get().split(","))
                    .map(String::trim)
                    .filter(k -> !k.isBlank())
                    .toList();
            if (!parsed.isEmpty()) return parsed;
        }
        return single.filter(k -> !k.isBlank()).map(List::of).orElse(List.of());
    }

    boolean isEmpty() {
        return keys.isEmpty();
    }

    int size() {
        return keys.size();
    }

    int currentIndex() {
        return index.get();
    }

    String keyAt(int absoluteIndex) {
        return keys.get(absoluteIndex % keys.size());
    }

    void advance() {
        index.incrementAndGet();
    }

    static boolean isKeyError(int statusCode) {
        return statusCode == 401 || statusCode == 402 || statusCode == 403 || statusCode == 429;
    }
}
