package de.htwberlin.webtech.shopping.resource;

import de.htwberlin.webtech.security.UserContext;
import de.htwberlin.webtech.shopping.dto.ShoppingListItemRequest;
import de.htwberlin.webtech.shopping.dto.ShoppingListItemResponse;
import de.htwberlin.webtech.shopping.mapper.ShoppingListItemMapper;
import de.htwberlin.webtech.shopping.service.ShoppingListService;
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

@Path("/shopping-list/items")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Shopping List", description = "Personal shopping list item management")
public class ShoppingListResource {

    private final ShoppingListService service;
    private final ShoppingListItemMapper mapper;
    private final UserContext userContext;

    public ShoppingListResource(ShoppingListService service, ShoppingListItemMapper mapper, UserContext userContext) {
        this.service = service;
        this.mapper = mapper;
        this.userContext = userContext;
    }

    @GET
    @Operation(summary = "List own shopping list items", description = "Returns shopping list items owned by the authenticated user.")
    @APIResponse(
            responseCode = "200",
            description = "Shopping list items returned",
            content = @Content(schema = @Schema(implementation = ShoppingListItemResponse.class, type = SchemaType.ARRAY))
    )
    @APIResponse(responseCode = "401", description = "Missing or invalid Bearer token")
    public List<ShoppingListItemResponse> getMine(@HeaderParam("Authorization") String authorizationHeader) {
        return mapper.toResponseList(service.listMine(userContext.requireUser(authorizationHeader)));
    }

    @POST
    @Operation(summary = "Create shopping list item", description = "Creates a shopping list item for the authenticated user.")
    @APIResponse(responseCode = "201", description = "Shopping list item created")
    @APIResponse(responseCode = "400", description = "Invalid shopping list item data")
    @APIResponse(responseCode = "401", description = "Missing or invalid Bearer token")
    public Response create(@HeaderParam("Authorization") String authorizationHeader, @Valid ShoppingListItemRequest request) {
        ShoppingListItemResponse created = mapper.toResponse(service.create(
                request,
                userContext.requireUser(authorizationHeader)
        ));
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update shopping list item", description = "Updates an owned shopping list item.")
    @APIResponse(responseCode = "200", description = "Shopping list item updated")
    @APIResponse(responseCode = "400", description = "Invalid shopping list item data")
    @APIResponse(responseCode = "401", description = "Missing or invalid Bearer token")
    @APIResponse(responseCode = "403", description = "Only the owner may update the shopping list item")
    @APIResponse(responseCode = "404", description = "Shopping list item not found")
    public ShoppingListItemResponse update(@PathParam("id") Long id, @HeaderParam("Authorization") String authorizationHeader, @Valid ShoppingListItemRequest request) {
        return mapper.toResponse(service.update(
                id,
                request,
                userContext.requireUser(authorizationHeader)
        ));
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete shopping list item", description = "Deletes an owned shopping list item.")
    @APIResponse(responseCode = "204", description = "Shopping list item deleted")
    @APIResponse(responseCode = "401", description = "Missing or invalid Bearer token")
    @APIResponse(responseCode = "403", description = "Only the owner may delete the shopping list item")
    @APIResponse(responseCode = "404", description = "Shopping list item not found")
    public Response delete(@PathParam("id") Long id, @HeaderParam("Authorization") String authorizationHeader) {
        service.delete(id, userContext.requireUser(authorizationHeader));
        return Response.noContent().build();
    }
}
