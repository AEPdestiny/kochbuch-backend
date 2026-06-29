package de.htwberlin.webtech.recipe.external;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpoonacularKeyRotatorTest {

    // --- Key resolution ---

    @Test
    void resolves_single_key_from_spoonacular_api_key() {
        SpoonacularKeyRotator rotator = new SpoonacularKeyRotator(Optional.empty(), Optional.of("key-abc"));
        assertFalse(rotator.isEmpty());
        assertEquals(1, rotator.size());
        assertEquals("key-abc", rotator.keyAt(0));
    }

    @Test
    void resolves_multiple_keys_from_spoonacular_api_keys() {
        SpoonacularKeyRotator rotator = new SpoonacularKeyRotator(Optional.of("key1, key2, key3"), Optional.empty());
        assertEquals(3, rotator.size());
        assertEquals("key1", rotator.keyAt(0));
        assertEquals("key2", rotator.keyAt(1));
        assertEquals("key3", rotator.keyAt(2));
    }

    @Test
    void multi_keys_take_precedence_over_single_key() {
        SpoonacularKeyRotator rotator = new SpoonacularKeyRotator(Optional.of("multi1, multi2"), Optional.of("single"));
        assertEquals(2, rotator.size());
        assertEquals("multi1", rotator.keyAt(0));
        assertEquals("multi2", rotator.keyAt(1));
    }

    @Test
    void falls_back_to_single_key_when_multi_keys_is_blank() {
        SpoonacularKeyRotator rotator = new SpoonacularKeyRotator(Optional.of("  "), Optional.of("fallback"));
        assertEquals(1, rotator.size());
        assertEquals("fallback", rotator.keyAt(0));
    }

    @Test
    void falls_back_to_single_key_when_multi_keys_has_only_empty_entries() {
        SpoonacularKeyRotator rotator = new SpoonacularKeyRotator(Optional.of(" , , "), Optional.of("fallback"));
        assertEquals(1, rotator.size());
        assertEquals("fallback", rotator.keyAt(0));
    }

    @Test
    void is_empty_when_no_keys_configured() {
        SpoonacularKeyRotator rotator = new SpoonacularKeyRotator(Optional.empty(), Optional.empty());
        assertTrue(rotator.isEmpty());
    }

    @Test
    void is_empty_when_single_key_is_blank() {
        SpoonacularKeyRotator rotator = new SpoonacularKeyRotator(Optional.empty(), Optional.of("  "));
        assertTrue(rotator.isEmpty());
    }

    // --- advance / currentIndex ---

    @Test
    void advance_moves_current_index_to_next_key() {
        SpoonacularKeyRotator rotator = new SpoonacularKeyRotator(List.of("key1", "key2", "key3"));
        assertEquals("key1", rotator.keyAt(rotator.currentIndex()));
        rotator.advance();
        assertEquals("key2", rotator.keyAt(rotator.currentIndex()));
        rotator.advance();
        assertEquals("key3", rotator.keyAt(rotator.currentIndex()));
    }

    @Test
    void advance_wraps_around_after_last_key() {
        SpoonacularKeyRotator rotator = new SpoonacularKeyRotator(List.of("key1", "key2"));
        rotator.advance();
        rotator.advance();
        assertEquals("key1", rotator.keyAt(rotator.currentIndex()));
    }

    // --- keyAt with absolute index ---

    @Test
    void keyAt_returns_correct_key_for_absolute_index() {
        SpoonacularKeyRotator rotator = new SpoonacularKeyRotator(List.of("key1", "key2", "key3"));
        int base = rotator.currentIndex();
        assertEquals("key1", rotator.keyAt((base + 0) % rotator.size()));
        assertEquals("key2", rotator.keyAt((base + 1) % rotator.size()));
        assertEquals("key3", rotator.keyAt((base + 2) % rotator.size()));
    }

    @Test
    void keyAt_wraps_around_when_absolute_index_exceeds_size() {
        SpoonacularKeyRotator rotator = new SpoonacularKeyRotator(List.of("key1", "key2"));
        assertEquals("key1", rotator.keyAt(0));
        assertEquals("key2", rotator.keyAt(1));
        assertEquals("key1", rotator.keyAt(2));
    }

    // --- isKeyError ---

    @Test
    void isKeyError_returns_true_for_401() {
        assertTrue(SpoonacularKeyRotator.isKeyError(401));
    }

    @Test
    void isKeyError_returns_true_for_402() {
        assertTrue(SpoonacularKeyRotator.isKeyError(402));
    }

    @Test
    void isKeyError_returns_true_for_403() {
        assertTrue(SpoonacularKeyRotator.isKeyError(403));
    }

    @Test
    void isKeyError_returns_true_for_429() {
        assertTrue(SpoonacularKeyRotator.isKeyError(429));
    }

    @Test
    void isKeyError_returns_false_for_200() {
        assertFalse(SpoonacularKeyRotator.isKeyError(200));
    }

    @Test
    void isKeyError_returns_false_for_404() {
        assertFalse(SpoonacularKeyRotator.isKeyError(404));
    }

    @Test
    void isKeyError_returns_false_for_500() {
        assertFalse(SpoonacularKeyRotator.isKeyError(500));
    }

    // --- Rotation correctness (regression for the double-shift bug) ---

    @Test
    void rotation_visits_all_four_keys_exactly_once_in_order_when_all_fail() {
        // Mirrors exactly what sendWithRotation does:
        //   base = currentIndex()
        //   for attempt 0..size-1: idx = (base + attempt) % size, then advance()
        // Previously the bug caused: key(attempt) used (index + attempt) PLUS advance(),
        // shifting by 2 each iteration and skipping keys.
        SpoonacularKeyRotator rotator = new SpoonacularKeyRotator(List.of("k1", "k2", "k3", "k4"));
        int size = rotator.size();
        int base = rotator.currentIndex();
        List<String> tried = new ArrayList<>();

        for (int attempt = 0; attempt < size; attempt++) {
            int idx = (base + attempt) % size;
            tried.add(rotator.keyAt(idx));
            rotator.advance();
        }

        assertEquals(List.of("k1", "k2", "k3", "k4"), tried);
    }

    @Test
    void rotation_does_not_skip_key2_when_key1_fails() {
        SpoonacularKeyRotator rotator = new SpoonacularKeyRotator(List.of("k1", "k2", "k3", "k4"));
        int size = rotator.size();
        int base = rotator.currentIndex();

        // k1 fails
        assertEquals("k1", rotator.keyAt((base + 0) % size));
        rotator.advance();

        // next attempted key must be k2, not k3
        assertEquals("k2", rotator.keyAt((base + 1) % size));
    }

    @Test
    void rotation_starts_subsequent_request_from_last_rotated_position() {
        // After k1 fails (advance) and k2 succeeds (no advance),
        // the next request should start at currentIndex pointing to k2.
        SpoonacularKeyRotator rotator = new SpoonacularKeyRotator(List.of("k1", "k2", "k3"));
        int size = rotator.size();
        int base = rotator.currentIndex();

        // k1 fails → advance
        assertEquals("k1", rotator.keyAt((base + 0) % size));
        rotator.advance();

        // k2 succeeds → no advance; return here in sendWithRotation
        assertEquals("k2", rotator.keyAt((base + 1) % size));

        // Next request snapshots currentIndex = 1 → starts with k2
        int nextBase = rotator.currentIndex();
        assertEquals("k2", rotator.keyAt(nextBase % size));
    }

    @Test
    void rotation_with_single_key_tries_it_once_and_fails() {
        SpoonacularKeyRotator rotator = new SpoonacularKeyRotator(List.of("only-key"));
        int size = rotator.size();
        int base = rotator.currentIndex();
        List<String> tried = new ArrayList<>();

        for (int attempt = 0; attempt < size; attempt++) {
            tried.add(rotator.keyAt((base + attempt) % size));
            rotator.advance();
        }

        assertEquals(List.of("only-key"), tried);
    }
}
