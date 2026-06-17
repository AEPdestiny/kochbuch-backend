package de.htwberlin.webtech.ai;

import de.htwberlin.webtech.ai.client.GroqClient;
import de.htwberlin.webtech.ai.client.GroqClientException;
import de.htwberlin.webtech.ai.dto.AiChatResponse;
import de.htwberlin.webtech.ai.service.AiChatService;
import de.htwberlin.webtech.favorite.repository.ExternalRecipeFavoriteRepository;
import de.htwberlin.webtech.mealplan.repository.MealPlanRepository;
import de.htwberlin.webtech.pantry.repository.PantryItemRepository;
import de.htwberlin.webtech.profile.repository.UserPreferencesRepository;
import de.htwberlin.webtech.user.entity.AppUser;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AiChatServiceTest {

    private final GroqClient groqClient = mock(GroqClient.class);
    private final UserPreferencesRepository preferencesRepository = mock(UserPreferencesRepository.class);
    private final PantryItemRepository pantryItemRepository = mock(PantryItemRepository.class);
    private final MealPlanRepository mealPlanRepository = mock(MealPlanRepository.class);
    private final ExternalRecipeFavoriteRepository favoriteRepository = mock(ExternalRecipeFavoriteRepository.class);
    private final AiChatService underTest = new AiChatService(
            groqClient,
            preferencesRepository,
            pantryItemRepository,
            mealPlanRepository,
            favoriteRepository
    );

    @Test
    void answer_should_call_groq_with_real_context() {
        AppUser user = user();
        doReturn(Optional.empty()).when(preferencesRepository).findByOwner(user);
        doReturn(List.of()).when(pantryItemRepository).findByOwner(user);
        doReturn(List.of()).when(favoriteRepository).findByOwner(user);
        doReturn(List.of()).when(mealPlanRepository).findByOwnerAndPlannedDateBetween(any(), any(LocalDate.class), any(LocalDate.class));
        doReturn("Nutze Pasta aus deinem Vorrat.").when(groqClient).complete(any(), contains("Nutzerfrage: Was soll ich kochen?"));

        AiChatResponse response = underTest.answer(user, "Was soll ich kochen?");

        assertTrue(response.isConfigured());
        assertEquals("Nutze Pasta aus deinem Vorrat.", response.getMessage());
        verify(groqClient).complete(any(), contains("Wochenplan aktuelle Woche"));
    }

    @Test
    void answer_should_return_honest_fallback_when_groq_is_missing() {
        AppUser user = user();
        doReturn(Optional.empty()).when(preferencesRepository).findByOwner(user);
        doReturn(List.of()).when(pantryItemRepository).findByOwner(user);
        doReturn(List.of()).when(favoriteRepository).findByOwner(user);
        doReturn(List.of()).when(mealPlanRepository).findByOwnerAndPlannedDateBetween(any(), any(LocalDate.class), any(LocalDate.class));
        doThrow(new GroqClientException("GROQ_API_KEY is not configured.")).when(groqClient).complete(any(), any());

        AiChatResponse response = underTest.answer(user, "Hallo");

        assertFalse(response.isConfigured());
        assertTrue(response.getMessage().contains("GROQ_API_KEY"));
    }

    @Test
    void answer_should_return_controlled_groq_error_message() {
        AppUser user = user();
        doReturn(Optional.empty()).when(preferencesRepository).findByOwner(user);
        doReturn(List.of()).when(pantryItemRepository).findByOwner(user);
        doReturn(List.of()).when(favoriteRepository).findByOwner(user);
        doReturn(List.of()).when(mealPlanRepository).findByOwnerAndPlannedDateBetween(any(), any(LocalDate.class), any(LocalDate.class));
        doThrow(new GroqClientException("Groq request failed with status 401.")).when(groqClient).complete(any(), any());

        AiChatResponse response = underTest.answer(user, "Hallo");

        assertFalse(response.isConfigured());
        assertTrue(response.getMessage().contains("Groq request failed with status 401."));
    }

    private AppUser user() {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setUsername("produuser");
        user.setEmail("user@example.com");
        user.setPasswordHash("hash");
        return user;
    }
}
