package de.htwberlin.webtech.recipe.resource;

import de.htwberlin.webtech.recipe.dto.RecipeRequest;
import de.htwberlin.webtech.recipe.dto.RecipeResponse;
import de.htwberlin.webtech.recipe.dto.ExternalRecipeMatchResponse;
import de.htwberlin.webtech.recipe.exception.RecipeNotFoundException;
import de.htwberlin.webtech.recipe.external.ExternalRecipeService;
import de.htwberlin.webtech.recipe.mapper.RecipeMapper;
import de.htwberlin.webtech.recipe.service.RecipeService;
import de.htwberlin.webtech.security.UserContext;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.validation.Valid;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.Arrays;

@Path("/recipes")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Recipes", description = "Recipe management and external recipe discovery")
public class RecipeResource {

    private final RecipeService service;
    private final ExternalRecipeService externalService;
    private final RecipeMapper mapper;
    private final UserContext userContext;

    public RecipeResource(RecipeService service, ExternalRecipeService externalService, RecipeMapper mapper, UserContext userContext) {
        this.service = service;
        this.externalService = externalService;
        this.mapper = mapper;
        this.userContext = userContext;
    }

    @POST
    @Operation(summary = "Create recipe", description = "Creates a new local recipe.")
    @APIResponse(responseCode = "201", description = "Recipe created")
    @APIResponse(responseCode = "401", description = "Missing or invalid Bearer token")
    @APIResponse(responseCode = "400", description = "Invalid recipe data")
    public Response create(@HeaderParam("Authorization") String authorizationHeader, @Valid RecipeRequest request) {
        RecipeResponse created = mapper.toResponse(service.create(
                mapper.toEntity(request),
                userContext.requireUser(authorizationHeader)
        ));
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @GET
    @Operation(summary = "List public recipes", description = "Returns recipes marked as published.")
    @APIResponse(
            responseCode = "200",
            description = "Recipes returned",
            content = @Content(schema = @Schema(implementation = RecipeResponse.class, type = SchemaType.ARRAY))
    )
    public List<RecipeResponse> getAll() {
        return mapper.toResponseList(service.findAll());
    }

    @GET
    @Path("/published")
    @Operation(summary = "List published recipes", description = "Returns recipes marked as published.")
    @APIResponse(responseCode = "200", description = "Published recipes returned")
    public List<RecipeResponse> getPublished() {
        return mapper.toResponseList(service.findAllPublished());
    }

    @GET
    @Path("/mine")
    @Operation(summary = "List own recipes", description = "Returns recipes owned by the authenticated user.")
    @APIResponse(responseCode = "200", description = "Own recipes returned")
    @APIResponse(responseCode = "401", description = "Missing or invalid Bearer token")
    public List<RecipeResponse> getMine(@HeaderParam("Authorization") String authorizationHeader) {
        return mapper.toResponseList(service.findMine(userContext.requireUser(authorizationHeader)));
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get recipe by id", description = "Returns one recipe by its technical id.")
    @APIResponse(responseCode = "200", description = "Recipe returned")
    @APIResponse(responseCode = "404", description = "Recipe not found")
    public RecipeResponse getById(@PathParam("id") Long id, @HeaderParam("Authorization") String authorizationHeader) {
        return mapper.toResponse(service.findVisibleById(
                id,
                userContext.currentUserOrNull(authorizationHeader)
        ));
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update recipe", description = "Updates an existing local recipe.")
    @APIResponse(responseCode = "200", description = "Recipe updated")
    @APIResponse(responseCode = "400", description = "Invalid recipe data")
    @APIResponse(responseCode = "401", description = "Missing or invalid Bearer token")
    @APIResponse(responseCode = "403", description = "Only the owner may update the recipe")
    @APIResponse(responseCode = "404", description = "Recipe not found")
    public RecipeResponse update(@PathParam("id") Long id, @HeaderParam("Authorization") String authorizationHeader, @Valid RecipeRequest request) {
        return mapper.toResponse(service.update(
                id,
                mapper.toEntity(request),
                userContext.requireUser(authorizationHeader)
        ));
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete recipe", description = "Deletes an existing local recipe.")
    @APIResponse(responseCode = "204", description = "Recipe deleted")
    @APIResponse(responseCode = "401", description = "Missing or invalid Bearer token")
    @APIResponse(responseCode = "403", description = "Only the owner may delete the recipe")
    @APIResponse(responseCode = "404", description = "Recipe not found")
    public Response delete(@PathParam("id") Long id, @HeaderParam("Authorization") String authorizationHeader) {
        service.delete(id, userContext.requireUser(authorizationHeader));
        return Response.noContent().build();
    }

    @GET
    @Path("/external")
    @Operation(summary = "List external recipes", description = "Returns recipes fetched from Spoonacular. Optional search query filters the external source.")
    @APIResponse(responseCode = "200", description = "External recipes returned")
    public List<RecipeResponse> getExternal(@QueryParam("search") String search) {
        return externalService.fetchExternalRecipes(search);
    }

    @GET
    @Path("/external/{id}")
    @Operation(summary = "Get external recipe detail", description = "Returns one external Spoonacular recipe with detail fields.")
    @APIResponse(responseCode = "200", description = "External recipe returned")
    @APIResponse(responseCode = "404", description = "External recipe not found")
    public Response getExternalById(@PathParam("id") Long id) {
        return externalService.fetchExternalRecipeDetail(id)
                .map(detail -> Response.ok(detail).build())
                .orElseThrow(() -> new RecipeNotFoundException(id));
    }

    @GET
    @Path("/external/by-ingredients")
    @Operation(summary = "Find external recipes by ingredients", description = "Returns Spoonacular recipe matches for a comma separated ingredient list.")
    @APIResponse(responseCode = "200", description = "External recipe matches returned")
    public List<ExternalRecipeMatchResponse> findExternalByIngredients(@QueryParam("ingredients") String ingredients) {
        List<String> ingredientList = ingredients == null ? List.of() : Arrays.stream(ingredients.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
        return externalService.findRecipesByIngredients(ingredientList);
    }
}
