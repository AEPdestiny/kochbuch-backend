package de.htwberlin.webtech.shared.exception;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable exception) {
        Response.StatusType status = status(exception);
        ErrorResponse error = new ErrorResponse(
                status.getStatusCode(),
                status.getReasonPhrase(),
                message(exception),
                path()
        );
        return Response.status(status).entity(error).build();
    }

    private Response.StatusType status(Throwable exception) {
        if (exception instanceof WebApplicationException webApplicationException) {
            return webApplicationException.getResponse().getStatusInfo();
        }
        return Response.Status.INTERNAL_SERVER_ERROR;
    }

    private String message(Throwable exception) {
        if (exception instanceof WebApplicationException) {
            return "Request could not be processed.";
        }
        return "An unexpected error occurred.";
    }

    private String path() {
        return uriInfo == null ? "" : uriInfo.getPath();
    }
}
