package de.htwberlin.webtech.mealplan.resource;

import de.htwberlin.webtech.mealplan.dto.MealPlanEntryRequest;
import de.htwberlin.webtech.mealplan.dto.MealPlanEntryResponse;
import de.htwberlin.webtech.mealplan.dto.MealPlanMoveRequest;
import de.htwberlin.webtech.mealplan.dto.MealPlanShoppingListResponse;
import de.htwberlin.webtech.mealplan.dto.MealPlanWeekResponse;
import de.htwberlin.webtech.mealplan.entity.MealSlot;
import de.htwberlin.webtech.mealplan.mapper.MealPlanMapper;
import de.htwberlin.webtech.mealplan.service.MealPlanShoppingListService;
import de.htwberlin.webtech.mealplan.service.MealPlanService;
import de.htwberlin.webtech.security.UserContext;
import de.htwberlin.webtech.user.entity.AppUser;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Path("/meal-plan")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Meal Plan", description = "Personal weekly meal planning")
public class MealPlanResource {

    private final MealPlanService service;
    private final MealPlanShoppingListService shoppingListService;
    private final MealPlanMapper mapper;
    private final UserContext userContext;

    public MealPlanResource(MealPlanService service,
                            MealPlanShoppingListService shoppingListService,
                            MealPlanMapper mapper,
                            UserContext userContext) {
        this.service = service;
        this.shoppingListService = shoppingListService;
        this.mapper = mapper;
        this.userContext = userContext;
    }

    @GET
    @Path("/week")
    @Operation(summary = "Get weekly meal plan", description = "Returns meal plan entries for the authenticated user's week.")
    @APIResponse(responseCode = "200", description = "Meal plan week returned")
    @APIResponse(responseCode = "401", description = "Missing or invalid Bearer token")
    public MealPlanWeekResponse getWeek(@HeaderParam("Authorization") String authorizationHeader,
                                        @QueryParam("startDate") String startDate) {
        AppUser currentUser = userContext.requireUser(authorizationHeader);
        LocalDate weekStart = service.normalizeWeekStart(parseOptionalDate(startDate));
        LocalDate weekEnd = weekStart.plusDays(6);
        return mapper.toWeekResponse(weekStart, weekEnd, service.getWeek(currentUser, weekStart));
    }

    @PUT
    @Path("/days/{date}")
    @Operation(summary = "Set recipe for day", description = "Sets or replaces one owned recipe for the authenticated user's day.")
    @APIResponse(responseCode = "200", description = "Meal plan entry saved")
    @APIResponse(responseCode = "400", description = "Invalid date or request body")
    @APIResponse(responseCode = "401", description = "Missing or invalid Bearer token")
    @APIResponse(responseCode = "403", description = "Only own recipes can be planned")
    @APIResponse(responseCode = "404", description = "Recipe not found")
    public MealPlanEntryResponse setDay(@PathParam("date") String date,
                                        @HeaderParam("Authorization") String authorizationHeader,
                                        @Valid MealPlanEntryRequest request) {
        return mapper.toResponse(service.setRecipeForDay(
                userContext.requireUser(authorizationHeader),
                parseDate(date),
                request
        ));
    }

    @DELETE
    @Path("/days/{date}")
    @Operation(summary = "Delete recipe for day", description = "Deletes the authenticated user's meal plan entry for a day.")
    @APIResponse(responseCode = "204", description = "Meal plan entry deleted")
    @APIResponse(responseCode = "401", description = "Missing or invalid Bearer token")
    @APIResponse(responseCode = "404", description = "Meal plan entry not found")
    public Response deleteDay(@PathParam("date") String date,
                              @HeaderParam("Authorization") String authorizationHeader) {
        service.deleteForDay(userContext.requireUser(authorizationHeader), parseDate(date));
        return Response.noContent().build();
    }

    @PUT
    @Path("/days/{date}/slots/{slot}")
    @Operation(summary = "Set recipe for day slot", description = "Sets or replaces one owned recipe for a day and meal slot.")
    @APIResponse(responseCode = "200", description = "Meal plan entry saved")
    @APIResponse(responseCode = "400", description = "Invalid date, slot or request body")
    @APIResponse(responseCode = "401", description = "Missing or invalid Bearer token")
    @APIResponse(responseCode = "403", description = "Only own recipes can be planned")
    @APIResponse(responseCode = "404", description = "Recipe not found")
    public MealPlanEntryResponse setSlot(@PathParam("date") String date,
                                         @PathParam("slot") String slot,
                                         @HeaderParam("Authorization") String authorizationHeader,
                                         @Valid MealPlanEntryRequest request) {
        return mapper.toResponse(service.setRecipeForSlot(
                userContext.requireUser(authorizationHeader),
                parseDate(date),
                MealSlot.fromPath(slot),
                request
        ));
    }

    @DELETE
    @Path("/days/{date}/slots/{slot}")
    @Operation(summary = "Delete recipe for day slot", description = "Deletes the authenticated user's meal plan entry for a day and meal slot.")
    @APIResponse(responseCode = "204", description = "Meal plan entry deleted")
    @APIResponse(responseCode = "401", description = "Missing or invalid Bearer token")
    @APIResponse(responseCode = "404", description = "Meal plan entry not found")
    public Response deleteSlot(@PathParam("date") String date,
                               @PathParam("slot") String slot,
                               @HeaderParam("Authorization") String authorizationHeader) {
        service.deleteForSlot(userContext.requireUser(authorizationHeader), parseDate(date), MealSlot.fromPath(slot));
        return Response.noContent().build();
    }

    @PATCH
    @Path("/entries/{id}/move")
    @Operation(summary = "Move or swap meal plan entry", description = "Moves an owned meal plan entry to another slot. If the target is occupied, entries are swapped when requested.")
    @APIResponse(responseCode = "200", description = "Updated meal plan week returned")
    @APIResponse(responseCode = "400", description = "Invalid target date, slot or request body")
    @APIResponse(responseCode = "401", description = "Missing or invalid Bearer token")
    @APIResponse(responseCode = "403", description = "Only own meal plan entries can be moved")
    @APIResponse(responseCode = "404", description = "Meal plan entry not found")
    public MealPlanWeekResponse moveEntry(@PathParam("id") Long id,
                                          @HeaderParam("Authorization") String authorizationHeader,
                                          @Valid MealPlanMoveRequest request) {
        if (request == null || request.getTargetDate() == null || request.getTargetSlot() == null || request.getTargetSlot().isBlank()) {
            throw new IllegalArgumentException("targetDate and targetSlot must be provided.");
        }
        AppUser currentUser = userContext.requireUser(authorizationHeader);
        LocalDate weekStart = service.normalizeWeekStart(request.getTargetDate());
        LocalDate weekEnd = weekStart.plusDays(6);
        return mapper.toWeekResponse(weekStart, weekEnd, service.moveEntry(
                currentUser,
                id,
                request.getTargetDate(),
                MealSlot.fromPath(request.getTargetSlot()),
                request.isSwapIfOccupied()
        ));
    }

    @POST
    @Path("/shopping-list")
    @Operation(summary = "Create shopping list from meal plan", description = "Creates missing shopping list items from the authenticated user's week and respects pantry items where safe.")
    @APIResponse(responseCode = "200", description = "Shopping list result returned")
    @APIResponse(responseCode = "401", description = "Missing or invalid Bearer token")
    public MealPlanShoppingListResponse createShoppingListFromWeek(@HeaderParam("Authorization") String authorizationHeader,
                                                                   @QueryParam("startDate") String startDate) {
        AppUser currentUser = userContext.requireUser(authorizationHeader);
        LocalDate weekStart = service.normalizeWeekStart(parseOptionalDate(startDate));
        return shoppingListService.createShoppingList(currentUser, weekStart);
    }

    private LocalDate parseOptionalDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parseDate(value);
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Date must use ISO format YYYY-MM-DD.");
        }
    }
}
