package de.htwberlin.webtech.pantry.resource;

import de.htwberlin.webtech.pantry.dto.PantryItemRequest;
import de.htwberlin.webtech.pantry.dto.PantryItemResponse;
import de.htwberlin.webtech.pantry.mapper.PantryItemMapper;
import de.htwberlin.webtech.pantry.service.PantryService;
import de.htwberlin.webtech.security.UserContext;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/pantry/items")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Pantry", description = "Personal pantry item management")
public class PantryResource {

    private final PantryService service;
    private final PantryItemMapper mapper;
    private final UserContext userContext;

    public PantryResource(PantryService service, PantryItemMapper mapper, UserContext userContext) {
        this.service = service;
        this.mapper = mapper;
        this.userContext = userContext;
    }

    @GET
    @Operation(summary = "List own pantry items", description = "Returns pantry items owned by the authenticated user.")
    @APIResponse(
            responseCode = "200",
            description = "Pantry items returned",
            content = @Content(schema = @Schema(implementation = PantryItemResponse.class, type = SchemaType.ARRAY))
    )
    @APIResponse(responseCode = "401", description = "Missing or invalid Bearer token")
    public List<PantryItemResponse> getMine(@HeaderParam("Authorization") String authorizationHeader) {
        return mapper.toResponseList(service.listMine(userContext.requireUser(authorizationHeader)));
    }

    @POST
    @Operation(summary = "Create pantry item", description = "Creates a pantry item for the authenticated user.")
    @APIResponse(responseCode = "201", description = "Pantry item created")
    @APIResponse(responseCode = "400", description = "Invalid pantry item data")
    @APIResponse(responseCode = "401", description = "Missing or invalid Bearer token")
    public Response create(@HeaderParam("Authorization") String authorizationHeader, @Valid PantryItemRequest request) {
        PantryItemResponse created = mapper.toResponse(service.create(
                request,
                userContext.requireUser(authorizationHeader)
        ));
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update pantry item", description = "Updates an owned pantry item.")
    @APIResponse(responseCode = "200", description = "Pantry item updated")
    @APIResponse(responseCode = "400", description = "Invalid pantry item data")
    @APIResponse(responseCode = "401", description = "Missing or invalid Bearer token")
    @APIResponse(responseCode = "403", description = "Only the owner may update the pantry item")
    @APIResponse(responseCode = "404", description = "Pantry item not found")
    public PantryItemResponse update(@PathParam("id") Long id, @HeaderParam("Authorization") String authorizationHeader, @Valid PantryItemRequest request) {
        return mapper.toResponse(service.update(
                id,
                request,
                userContext.requireUser(authorizationHeader)
        ));
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete pantry item", description = "Deletes an owned pantry item.")
    @APIResponse(responseCode = "204", description = "Pantry item deleted")
    @APIResponse(responseCode = "401", description = "Missing or invalid Bearer token")
    @APIResponse(responseCode = "403", description = "Only the owner may delete the pantry item")
    @APIResponse(responseCode = "404", description = "Pantry item not found")
    public Response delete(@PathParam("id") Long id, @HeaderParam("Authorization") String authorizationHeader) {
        service.delete(id, userContext.requireUser(authorizationHeader));
        return Response.noContent().build();
    }
}
