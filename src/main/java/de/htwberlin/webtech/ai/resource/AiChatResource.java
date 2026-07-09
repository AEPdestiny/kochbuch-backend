package de.htwberlin.webtech.ai.resource;

import de.htwberlin.webtech.ai.dto.AiChatRequest;
import de.htwberlin.webtech.ai.dto.AiChatResponse;
import de.htwberlin.webtech.ai.service.AiChatService;
import de.htwberlin.webtech.security.UserContext;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/ai/chat")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AiChatResource {

    private final AiChatService service;
    private final UserContext userContext;

    public AiChatResource(AiChatService service, UserContext userContext) {
        this.service = service;
        this.userContext = userContext;
    }

    @POST
    public AiChatResponse chat(@HeaderParam("Authorization") String authorizationHeader,
                               @Valid AiChatRequest request) {
        return service.answer(userContext.requireUser(authorizationHeader), request.getMessage(), request.getHistory(), request.getLocale());
    }
}
