package com.smartcampus.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.time.Instant;

/**
 * JAX-RS filter that logs every incoming request and outgoing response.
 * Demonstrates use of container filters for cross-cutting concerns
 * like logging, without modifying resource class code.
 *
 * Logs: HTTP method, URL path, response status code, and
 * response time in milliseconds.
 */
@Provider
public class RequestLoggingFilter
        implements ContainerRequestFilter, ContainerResponseFilter {

    /**
     * Called before the resource method is invoked.
     * Logs the incoming request and stores the start time
     * for response time calculation.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) {
        String method = requestContext.getMethod();
        String path = requestContext.getUriInfo()
                .getAbsolutePath().toString();
        String timestamp = Instant.now().toString();

        System.out.println("[REQUEST]  " + timestamp
                + " | " + method + " " + path);

        // Store start time for response time calculation
        requestContext.setProperty("startTime",
                System.currentTimeMillis());
    }

    /**
     * Called after the resource method has produced a response.
     * Logs the response status code and how long the request took.
     */
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) {
        Long startTime = (Long) requestContext
                .getProperty("startTime");
        Long duration = startTime != null
                ? System.currentTimeMillis() - startTime : 0L;

        Integer status = responseContext.getStatus();
        String method = requestContext.getMethod();
        String path = requestContext.getUriInfo()
                .getAbsolutePath().toString();

        System.out.println("[RESPONSE] " + Instant.now().toString()
                + " | " + method + " " + path
                + " | Status: " + status
                + " | Time: " + duration + "ms");
    }
}