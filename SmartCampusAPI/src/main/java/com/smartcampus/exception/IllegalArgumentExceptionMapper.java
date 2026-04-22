package com.smartcampus.exception;

import com.smartcampus.dto.ErrorResponse;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps IllegalArgumentException to 400 Bad Request.
 * Catches all input validation failures across the API
 * and returns a structured JSON error response.
 */
@Provider
public class IllegalArgumentExceptionMapper
        implements ExceptionMapper<IllegalArgumentException> {

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(IllegalArgumentException exception) {
        String path = uriInfo != null
                ? uriInfo.getAbsolutePath().toString() : "";

        ErrorResponse error = new ErrorResponse(
                400, "Bad Request",
                exception.getMessage(), path);

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}