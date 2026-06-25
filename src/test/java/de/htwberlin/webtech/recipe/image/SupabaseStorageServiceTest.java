package de.htwberlin.webtech.recipe.image;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SupabaseStorageServiceTest {

    @Test
    void upload_should_fail_cleanly_when_supabase_url_is_missing() {
        SupabaseStorageService service = new SupabaseStorageService(
                Optional.empty(),
                Optional.of("service-key"),
                Optional.of("recipe-images")
        );

        SupabaseStorageException exception = assertThrows(SupabaseStorageException.class,
                () -> service.upload(Path.of("missing.png"), "recipes/1/image.png", "image/png"));

        assertEquals("Supabase URL is not configured.", exception.getMessage());
    }

    @Test
    void upload_should_fail_cleanly_when_service_key_is_missing() {
        SupabaseStorageService service = new SupabaseStorageService(
                Optional.of("https://example.supabase.co"),
                Optional.empty(),
                Optional.of("recipe-images")
        );

        SupabaseStorageException exception = assertThrows(SupabaseStorageException.class,
                () -> service.upload(Path.of("missing.png"), "recipes/1/image.png", "image/png"));

        assertEquals("Supabase service role key is not configured.", exception.getMessage());
    }

    @Test
    void upload_should_fail_cleanly_when_bucket_is_missing() {
        SupabaseStorageService service = new SupabaseStorageService(
                Optional.of("https://example.supabase.co"),
                Optional.of("service-key"),
                Optional.empty()
        );

        SupabaseStorageException exception = assertThrows(SupabaseStorageException.class,
                () -> service.upload(Path.of("missing.png"), "recipes/1/image.png", "image/png"));

        assertEquals("Supabase bucket is not configured.", exception.getMessage());
    }
}
