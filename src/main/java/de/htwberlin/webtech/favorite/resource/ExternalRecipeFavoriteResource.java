package de.htwberlin.webtech.favorite.resource;

import de.htwberlin.webtech.favorite.dto.ExternalRecipeFavoriteRequest;
import de.htwberlin.webtech.favorite.dto.ExternalRecipeFavoriteResponse;
import de.htwberlin.webtech.favorite.mapper.ExternalRecipeFavoriteMapper;
import de.htwberlin.webtech.favorite.service.ExternalRecipeFavoriteService;
import de.htwberlin.webtech.security.UserContext;
import de.htwberlin.webtech.user.entity.AppUser;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/favorites/external")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ExternalRecipeFavoriteResource {

    private final ExternalRecipeFavoriteService service;
    private final ExternalRecipeFavoriteMapper mapper;
    private final UserContext userContext;

    public ExternalRecipeFavoriteResource(ExternalRecipeFavoriteService service, ExternalRecipeFavoriteMapper mapper, UserContext userContext) {
        this.service = service;
        this.mapper = mapper;
        this.userContext = userContext;
    }

    @GET
    public List<ExternalRecipeFavoriteResponse> list(@HeaderParam("Authorization") String authorizationHeader) {
        AppUser currentUser = userContext.requireUser(authorizationHeader);
        return mapper.toResponseList(service.listMine(currentUser));
    }

    @POST
    public ExternalRecipeFavoriteResponse add(@HeaderParam("Authorization") String authorizationHeader,
                                              @Valid ExternalRecipeFavoriteRequest request) {
        AppUser currentUser = userContext.requireUser(authorizationHeader);
        return mapper.toResponse(service.add(currentUser, request));
    }

    @DELETE
    @Path("/{source}/{externalRecipeId}")
    public Response remove(@HeaderParam("Authorization") String authorizationHeader,
                           @PathParam("source") String source,
                           @PathParam("externalRecipeId") String externalRecipeId) {
        AppUser currentUser = userContext.requireUser(authorizationHeader);
        service.remove(currentUser, source, externalRecipeId);
        return Response.noContent().build();
    }
}
