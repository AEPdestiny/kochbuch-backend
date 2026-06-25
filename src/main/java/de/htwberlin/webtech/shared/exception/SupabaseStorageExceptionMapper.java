package de.htwberlin.webtech.shared.exception;

import de.htwberlin.webtech.recipe.image.SupabaseStorageException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class SupabaseStorageExceptionMapper implements ExceptionMapper<SupabaseStorageException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(SupabaseStorageException exception) {
        Response.Status status = Response.Status.BAD_GATEWAY;
        ErrorResponse error = new ErrorResponse(
                status.getStatusCode(),
                status.getReasonPhrase(),
                exception.getMessage(),
                path()
        );
        return Response.status(status).entity(error).build();
    }

    private String path() {
        return uriInfo == null ? "" : uriInfo.getPath();
    }
}
