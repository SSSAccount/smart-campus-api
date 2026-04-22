package com.smartcampus.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Discovery endpoint providing API metadata, contact info,
 * a structured resource map, and HATEOAS navigation links.
 * This is the entry point for API consumers to discover
 * all available resources and operations.
 */
@Path("/")
public class DiscoveryResource {

    /**
     * Returns a complete JSON discovery document including
     * versioning, contact information, resource maps with
     * all available operations, and HATEOAS navigation links.
     *
     * @return JSON discovery document
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApiDiscovery() {

        Map<String, Object> discovery = new LinkedHashMap<>();
        discovery.put("apiName", "Smart Campus API");
        discovery.put("version", "v1");
        discovery.put("description",
                "REST API for managing rooms and sensors on a smart campus");

        Map<String, String> contact = new LinkedHashMap<>();
        contact.put("name", "Smart Campus Team");
        contact.put("email", "admin@smartcampus.ac.uk");
        contact.put("url", "https://smartcampus.ac.uk");
        contact.put("department", "Computer Science");
        discovery.put("contact", contact);

        // Resource map
        List<Map<String, Object>> resourceMap = new ArrayList<>();

        // Rooms
        Map<String, Object> roomsRes = new LinkedHashMap<>();
        roomsRes.put("name", "Rooms");
        roomsRes.put("description",
                "Manage campus rooms including creation, retrieval, update, and deletion");
        roomsRes.put("basePath", "/api/v1/rooms");
        List<Map<String, String>> roomOps = new ArrayList<>();
        roomOps.add(makeOp("GET", "/api/v1/rooms",
                "List all rooms (supports pagination with ?page=&size=)"));
        roomOps.add(makeOp("POST", "/api/v1/rooms",
                "Create a new room"));
        roomOps.add(makeOp("GET", "/api/v1/rooms/{roomId}",
                "Get a specific room by ID"));
        roomOps.add(makeOp("PUT", "/api/v1/rooms/{roomId}",
                "Update a room's name or capacity"));
        roomOps.add(makeOp("DELETE", "/api/v1/rooms/{roomId}",
                "Delete a room (blocked if sensors assigned)"));
        roomsRes.put("operations", roomOps);
        resourceMap.add(roomsRes);

        // Sensors
        Map<String, Object> sensorsRes = new LinkedHashMap<>();
        sensorsRes.put("name", "Sensors");
        sensorsRes.put("description",
                "Manage sensors including registration, listing, filtering, and status updates");
        sensorsRes.put("basePath", "/api/v1/sensors");
        List<Map<String, String>> sensorOps = new ArrayList<>();
        sensorOps.add(makeOp("GET", "/api/v1/sensors",
                "List all sensors (supports ?type= filter)"));
        sensorOps.add(makeOp("POST", "/api/v1/sensors",
                "Register a new sensor (validates roomId exists)"));
        sensorOps.add(makeOp("GET", "/api/v1/sensors/{sensorId}",
                "Get a specific sensor by ID"));
        sensorOps.add(makeOp("PUT", "/api/v1/sensors/{sensorId}",
                "Update sensor type or status"));
        sensorOps.add(makeOp("DELETE", "/api/v1/sensors/{sensorId}",
                "Delete a sensor"));
        sensorsRes.put("operations", sensorOps);
        resourceMap.add(sensorsRes);

        // Readings
        Map<String, Object> readingsRes = new LinkedHashMap<>();
        readingsRes.put("name", "Sensor Readings");
        readingsRes.put("description",
                "Sub-resource for managing reading history of a specific sensor");
        readingsRes.put("basePath", "/api/v1/sensors/{sensorId}/readings");
        List<Map<String, String>> readingOps = new ArrayList<>();
        readingOps.add(makeOp("GET", "/api/v1/sensors/{sensorId}/readings",
                "Get reading history"));
        readingOps.add(makeOp("POST", "/api/v1/sensors/{sensorId}/readings",
                "Add a reading (updates currentValue)"));
        readingsRes.put("operations", readingOps);
        resourceMap.add(readingsRes);

        // Webhooks
        Map<String, Object> webhooksRes = new LinkedHashMap<>();
        webhooksRes.put("name", "Webhooks");
        webhooksRes.put("description",
                "Register callback URLs for real-time event notifications");
        webhooksRes.put("basePath", "/api/v1/webhooks");
        List<Map<String, String>> webhookOps = new ArrayList<>();
        webhookOps.add(makeOp("GET", "/api/v1/webhooks",
                "List all webhooks"));
        webhookOps.add(makeOp("POST", "/api/v1/webhooks",
                "Register a new webhook"));
        webhookOps.add(makeOp("GET", "/api/v1/webhooks/{webhookId}",
                "Get a specific webhook"));
        webhookOps.add(makeOp("DELETE", "/api/v1/webhooks/{webhookId}",
                "Unregister a webhook"));
        webhooksRes.put("operations", webhookOps);
        resourceMap.add(webhooksRes);

        discovery.put("resources", resourceMap);

        // Navigation links
        List<Map<String, String>> links = new ArrayList<>();
        links.add(makeLink("self", "/api/v1", "GET"));
        links.add(makeLink("rooms", "/api/v1/rooms", "GET"));
        links.add(makeLink("create-room", "/api/v1/rooms", "POST"));
        links.add(makeLink("sensors", "/api/v1/sensors", "GET"));
        links.add(makeLink("create-sensor", "/api/v1/sensors", "POST"));
        links.add(makeLink("webhooks", "/api/v1/webhooks", "GET"));
        links.add(makeLink("create-webhook", "/api/v1/webhooks", "POST"));
        discovery.put("links", links);

        return Response.ok(discovery).build();
    }

    /**
     * Hidden test endpoint to trigger a 500 error for demonstration.
     * Proves the CatchAllExceptionMapper returns clean JSON
     * with no stack trace exposed to the client.
     */
    @GET
    @Path("/test-error")
    @Produces(MediaType.APPLICATION_JSON)
    public Response testError() {
        String nullString = null;
        nullString.length();
        return Response.ok().build();
    }

    /**
     * Helper method to build an operation map for the resource map.
     */
    private Map<String, String> makeOp(String method, String path,
                                        String description) {
        Map<String, String> op = new LinkedHashMap<>();
        op.put("method", method);
        op.put("path", path);
        op.put("description", description);
        return op;
    }

    /**
     * Helper method to build a HATEOAS link map.
     */
    private Map<String, String> makeLink(String rel, String href,
                                          String method) {
        Map<String, String> link = new LinkedHashMap<>();
        link.put("rel", rel);
        link.put("href", href);
        link.put("method", method);
        return link;
    }
}