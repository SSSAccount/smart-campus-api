package com.smartcampus.config;

import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * JAX-RS application configuration.
 * Sets the base API path to /api/v1 and registers all
 * resource classes, exception mappers, and filters.
 */
@ApplicationPath("/api/v1")
public class AppConfig extends ResourceConfig {

    public AppConfig() {
        packages("com.smartcampus.resource",
                 "com.smartcampus.exception",
                 "com.smartcampus.filter");
    }
}