package com.smartcampus.exception;

import com.smartcampus.dto.ErrorResponse;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class CatchAllExceptionMapper implements ExceptionMapper<Throwable> {

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable exception) {
        // Log the real error on the server console ONLY
        System.err.println("=== UNEXPECTED ERROR ===");
        System.err.println("Type: " + exception.getClass().getName());
        System.err.println("Message: " + exception.getMessage());
        exception.printStackTrace();
        System.err.println("========================");

        String path = uriInfo != null
                ? uriInfo.getAbsolutePath().toString() : "";

        // Send a safe generic message — NO stack trace, NO class names
        ErrorResponse error = new ErrorResponse(
                500,
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                path
        );

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}