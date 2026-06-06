package de.htwberlin.webtech.shared.exception;

import de.htwberlin.webtech.shopping.exception.ShoppingListItemNotFoundException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ShoppingListItemNotFoundExceptionMapper implements ExceptionMapper<ShoppingListItemNotFoundException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(ShoppingListItemNotFoundException exception) {
        Response.Status status = Response.Status.NOT_FOUND;
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
