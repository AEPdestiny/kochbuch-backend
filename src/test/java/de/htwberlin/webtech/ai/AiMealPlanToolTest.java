package de.htwberlin.webtech.ai;

import de.htwberlin.webtech.ai.tools.AiMealPlanTool;
import de.htwberlin.webtech.ai.tools.AiMealPlanToolResult;
import de.htwberlin.webtech.mealplan.dto.MealPlanEntryRequest;
import de.htwberlin.webtech.mealplan.entity.MealPlan;
import de.htwberlin.webtech.mealplan.entity.MealSlot;
import de.htwberlin.webtech.mealplan.repository.MealPlanRepository;
import de.htwberlin.webtech.mealplan.service.MealPlanService;
import de.htwberlin.webtech.user.entity.AppUser;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class AiMealPlanToolTest {

    private final MealPlanService mealPlanService = mock(MealPlanService.class);
    private final MealPlanRepository mealPlanRepository = mock(MealPlanRepository.class);
    private final AiMealPlanTool underTest = new AiMealPlanTool(mealPlanService, mealPlanRepository);

    @Test
    void addToMealPlan_should_create_custom_title_entry_when_slot_is_empty() {
        AppUser user = user();
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        MealPlan created = mealPlan("Tomaten-Ei-Omelett", tomorrow, MealSlot.DINNER);
        doReturn(Optional.empty()).when(mealPlanRepository).findByOwnerAndPlannedDateAndMealSlot(user, tomorrow, MealSlot.DINNER);
        doReturn(created).when(mealPlanService).setRecipeForSlot(eq(user), eq(tomorrow), eq(MealSlot.DINNER), org.mockito.ArgumentMatchers.any(MealPlanEntryRequest.class));

        AiMealPlanToolResult result = underTest.addToMealPlan(user, tomorrow, MealSlot.DINNER, null, "Tomaten-Ei-Omelett");

        assertTrue(result.success());
        assertFalse(result.conflict());
        assertEquals("Tomaten-Ei-Omelett", result.plannedTitle());
        ArgumentCaptor<MealPlanEntryRequest> requestCaptor = ArgumentCaptor.forClass(MealPlanEntryRequest.class);
        verify(mealPlanService).setRecipeForSlot(eq(user), eq(tomorrow), eq(MealSlot.DINNER), requestCaptor.capture());
        assertEquals("Tomaten-Ei-Omelett", requestCaptor.getValue().getCustomTitle());
    }

    @Test
    void addToMealPlan_should_not_overwrite_existing_slot() {
        AppUser user = user();
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        doReturn(Optional.of(mealPlan("Pasta", tomorrow, MealSlot.DINNER)))
                .when(mealPlanRepository).findByOwnerAndPlannedDateAndMealSlot(user, tomorrow, MealSlot.DINNER);

        AiMealPlanToolResult result = underTest.addToMealPlan(user, tomorrow, MealSlot.DINNER, null, "Tomaten-Ei-Omelett");

        assertFalse(result.success());
        assertTrue(result.conflict());
        assertEquals("Pasta", result.existingTitle());
        verifyNoInteractions(mealPlanService);
    }

    private MealPlan mealPlan(String title, LocalDate date, MealSlot slot) {
        MealPlan mealPlan = new MealPlan();
        mealPlan.setCustomTitle(title);
        mealPlan.setPlannedDate(date);
        mealPlan.setMealSlot(slot);
        return mealPlan;
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
