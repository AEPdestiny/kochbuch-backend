package de.htwberlin.webtech.profile.resource;

import de.htwberlin.webtech.profile.dto.UserPreferencesRequest;
import de.htwberlin.webtech.profile.dto.UserPreferencesResponse;
import de.htwberlin.webtech.profile.mapper.UserPreferencesMapper;
import de.htwberlin.webtech.profile.service.UserPreferencesService;
import de.htwberlin.webtech.security.UserContext;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/profile/preferences")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserPreferencesResource {

    private final UserPreferencesService service;
    private final UserPreferencesMapper mapper;
    private final UserContext userContext;

    public UserPreferencesResource(UserPreferencesService service, UserPreferencesMapper mapper, UserContext userContext) {
        this.service = service;
        this.mapper = mapper;
        this.userContext = userContext;
    }

    @GET
    public UserPreferencesResponse getPreferences(@HeaderParam("Authorization") String authorizationHeader) {
        return mapper.toResponse(service.getOrCreate(userContext.requireUser(authorizationHeader)));
    }

    @PUT
    public UserPreferencesResponse updatePreferences(
            @HeaderParam("Authorization") String authorizationHeader,
            @Valid UserPreferencesRequest request
    ) {
        return mapper.toResponse(service.update(request, userContext.requireUser(authorizationHeader)));
    }
}
