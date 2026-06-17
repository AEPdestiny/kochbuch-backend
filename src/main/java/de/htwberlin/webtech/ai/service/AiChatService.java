package de.htwberlin.webtech.ai.service;

import de.htwberlin.webtech.ai.client.GroqClient;
import de.htwberlin.webtech.ai.client.GroqClientException;
import de.htwberlin.webtech.ai.dto.AiChatResponse;
import de.htwberlin.webtech.favorite.repository.ExternalRecipeFavoriteRepository;
import de.htwberlin.webtech.mealplan.entity.MealPlan;
import de.htwberlin.webtech.mealplan.repository.MealPlanRepository;
import de.htwberlin.webtech.pantry.repository.PantryItemRepository;
import de.htwberlin.webtech.profile.entity.UserPreferences;
import de.htwberlin.webtech.profile.repository.UserPreferencesRepository;
import de.htwberlin.webtech.user.entity.AppUser;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.util.stream.Collectors;

@ApplicationScoped
public class AiChatService {

    private final GroqClient groqClient;
    private final UserPreferencesRepository preferencesRepository;
    private final PantryItemRepository pantryItemRepository;
    private final MealPlanRepository mealPlanRepository;
    private final ExternalRecipeFavoriteRepository favoriteRepository;

    public AiChatService(GroqClient groqClient,
                         UserPreferencesRepository preferencesRepository,
                         PantryItemRepository pantryItemRepository,
                         MealPlanRepository mealPlanRepository,
                         ExternalRecipeFavoriteRepository favoriteRepository) {
        this.groqClient = groqClient;
        this.preferencesRepository = preferencesRepository;
        this.pantryItemRepository = pantryItemRepository;
        this.mealPlanRepository = mealPlanRepository;
        this.favoriteRepository = favoriteRepository;
    }

    public AiChatResponse answer(AppUser currentUser, String message) {
        String systemPrompt = """
                Du bist Dishly AI, ein ehrlicher Koch- und Planungsassistent.
                Nutze nur die bereitgestellten Nutzerdaten. Erfinde keine gespeicherten Aktionen.
                Wenn Daten fehlen, sage klar, welche Daten fehlen.
                Gib keine medizinische Beratung. Bei Gesundheitsfragen nur allgemeine Hinweise geben.
                Antworte kurz, konkret und auf Deutsch.
                """;
        String prompt = buildContext(currentUser) + "\n\nNutzerfrage: " + message;
        try {
            return new AiChatResponse(groqClient.complete(systemPrompt, prompt), true);
        } catch (GroqClientException exception) {
            if (exception.getMessage() != null && exception.getMessage().contains("GROQ_API_KEY")) {
                return new AiChatResponse("Dishly AI ist noch nicht konfiguriert. Setze GROQ_API_KEY im Backend, damit echte Antworten erzeugt werden.", false);
            }
            return new AiChatResponse("Dishly AI konnte Groq gerade nicht erreichen: " + exception.getMessage(), false);
        }
    }

    private String buildContext(AppUser currentUser) {
        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        UserPreferences preferences = preferencesRepository.findByOwner(currentUser).orElse(null);
        String pantry = pantryItemRepository.findByOwner(currentUser).stream()
                .map(item -> item.getName() + quantity(item.getQuantity(), item.getUnit()))
                .limit(30)
                .collect(Collectors.joining(", "));
        String mealPlan = mealPlanRepository.findByOwnerAndPlannedDateBetween(currentUser, weekStart, weekEnd).stream()
                .map(this::mealPlanLine)
                .collect(Collectors.joining("; "));
        String favorites = favoriteRepository.findByOwner(currentUser).stream()
                .map(favorite -> favorite.getExternalTitle())
                .limit(15)
                .collect(Collectors.joining(", "));

        return """
                Nutzer: %s
                Profil: %s
                Vorrat: %s
                Wochenplan aktuelle Woche: %s
                Favoriten: %s
                """.formatted(
                currentUser.getUsername(),
                preferences == null ? "nicht ausgefüllt" : preferencesText(preferences),
                pantry.isBlank() ? "keine Vorratsdaten" : pantry,
                mealPlan.isBlank() ? "keine geplanten Mahlzeiten" : mealPlan,
                favorites.isBlank() ? "keine externen Favoriten" : favorites
        );
    }

    private String preferencesText(UserPreferences preferences) {
        return "Ziel=" + preferences.getGoal()
                + ", Kalorienziel=" + preferences.getDailyCalorieTarget()
                + ", vegan=" + preferences.isVegan()
                + ", vegetarisch=" + preferences.isVegetarian()
                + ", glutenfrei=" + preferences.isGlutenFree()
                + ", laktosefrei=" + preferences.isLactoseFree()
                + ", proteinreich=" + preferences.isHighProtein()
                + ", kalorienbewusst=" + preferences.isCalorieConscious()
                + ", günstig=" + preferences.isBudgetFriendly()
                + ", Allergien=" + preferences.getAllergies()
                + ", Vorlieben=" + preferences.getLikes()
                + ", Abneigungen=" + preferences.getDislikes();
    }

    private String mealPlanLine(MealPlan entry) {
        String title = entry.getRecipe() != null ? entry.getRecipe().getTitle() : entry.getCustomTitle();
        return entry.getPlannedDate() + " " + entry.getMealSlot().name().toLowerCase() + ": " + title;
    }

    private String quantity(Object quantity, String unit) {
        if (quantity == null) {
            return "";
        }
        return " (" + quantity + (unit == null || unit.isBlank() ? "" : " " + unit) + ")";
    }
}
