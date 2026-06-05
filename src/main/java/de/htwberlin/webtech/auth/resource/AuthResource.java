package de.htwberlin.webtech.auth.resource;

import de.htwberlin.webtech.auth.dto.AuthResponse;
import de.htwberlin.webtech.auth.dto.LoginRequest;
import de.htwberlin.webtech.auth.dto.RegisterRequest;
import de.htwberlin.webtech.auth.service.AuthService;
import de.htwberlin.webtech.auth.service.TokenService;
import de.htwberlin.webtech.user.dto.UserResponse;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Authentication", description = "Registration, login, and current user endpoints")
public class AuthResource {

    private final AuthService authService;
    private final TokenService tokenService;

    public AuthResource(AuthService authService, TokenService tokenService) {
        this.authService = authService;
        this.tokenService = tokenService;
    }

    @POST
    @Path("/register")
    @Operation(summary = "Register user", description = "Creates a user account and returns a JWT access token.")
    @APIResponse(responseCode = "201", description = "User registered")
    @APIResponse(responseCode = "400", description = "Invalid registration data")
    @APIResponse(responseCode = "409", description = "Email or username already exists")
    public Response register(@Valid RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @POST
    @Path("/login")
    @Operation(summary = "Login", description = "Authenticates with email and password and returns a JWT access token.")
    @APIResponse(responseCode = "200", description = "Login successful")
    @APIResponse(responseCode = "400", description = "Invalid login data")
    @APIResponse(responseCode = "401", description = "Invalid credentials")
    public AuthResponse login(@Valid LoginRequest request) {
        return authService.login(request);
    }

    @GET
    @Path("/me")
    @Operation(summary = "Current user", description = "Returns the authenticated user for a valid Bearer token.")
    @APIResponse(responseCode = "200", description = "Current user returned")
    @APIResponse(responseCode = "401", description = "Missing or invalid Bearer token")
    public UserResponse me(@HeaderParam("Authorization") String authorizationHeader) {
        return authService.currentUser(tokenService.emailFromBearerToken(authorizationHeader));
    }
}
