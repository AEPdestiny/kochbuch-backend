package de.htwberlin.webtech.shared.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Comparator;
import java.util.stream.Collectors;

@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        Response.Status status = Response.Status.BAD_REQUEST;
        ErrorResponse error = new ErrorResponse(
                status.getStatusCode(),
                status.getReasonPhrase(),
                message(exception),
                path()
        );
        return Response.status(status).entity(error).build();
    }

    private String message(ConstraintViolationException exception) {
        String details = exception.getConstraintViolations().stream()
                .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
                .map(this::format)
                .collect(Collectors.joining(", "));
        return details.isBlank() ? "Validation failed." : "Validation failed: " + details;
    }

    private String format(ConstraintViolation<?> violation) {
        String property = violation.getPropertyPath().toString();
        int lastDot = property.lastIndexOf('.');
        String field = lastDot >= 0 ? property.substring(lastDot + 1) : property;
        return field + " " + violation.getMessage();
    }

    private String path() {
        return uriInfo == null ? "" : uriInfo.getPath();
    }
}
