package de.htwberlin.webtech.restaurant.resource;

import de.htwberlin.webtech.restaurant.dto.RestaurantSearchRequest;
import de.htwberlin.webtech.restaurant.dto.RestaurantResponse;
import de.htwberlin.webtech.restaurant.service.RestaurantService;
import de.htwberlin.webtech.security.UserContext;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/restaurants")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Restaurants", description = "Nearby restaurant discovery")
public class RestaurantResource {

    private final RestaurantService restaurantService;
    private final UserContext userContext;

    public RestaurantResource(RestaurantService restaurantService, UserContext userContext) {
        this.restaurantService = restaurantService;
        this.userContext = userContext;
    }

    @POST
    @Path("/search")
    @Operation(summary = "Search nearby restaurants", description = "Searches nearby restaurants with Geoapify based on a recipe title and browser location.")
    @APIResponse(responseCode = "200", description = "Restaurants returned")
    @APIResponse(responseCode = "400", description = "Invalid search request")
    @APIResponse(responseCode = "401", description = "Missing or invalid Bearer token")
    public List<RestaurantResponse> search(@HeaderParam("Authorization") String authorizationHeader,
                                           @Valid RestaurantSearchRequest request) {
        userContext.requireUser(authorizationHeader);
        return restaurantService.search(request);
    }
}
