package com.smartcampus.resource;

import com.smartcampus.exception.ResourceNotFoundException;
import com.smartcampus.model.Webhook;
import com.smartcampus.repository.WebhookRepository;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/webhooks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WebhookResource {

    private final WebhookRepository webhookRepo
            = WebhookRepository.getInstance();

    @Context
    private UriInfo uriInfo;

    // GET /api/v1/webhooks — List all registered webhooks
    @GET
    public Response getAllWebhooks() {
        List<Webhook> webhooks = webhookRepo.findAll();

        List<Map<String, Object>> webhookList = webhooks.stream()
                .map(webhook -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", webhook.getId());
                    map.put("callbackUrl", webhook.getCallbackUrl());
                    map.put("events", webhook.getEvents());
                    map.put("description", webhook.getDescription());
                    map.put("active", webhook.getActive());
                    map.put("threshold", webhook.getThreshold());
                    map.put("sensorId", webhook.getSensorId());
                    map.put("failureCount", webhook.getFailureCount());
                    map.put("createdAt", webhook.getCreatedAt());

                    List<Map<String, String>> links = new ArrayList<>();

                    Map<String, String> selfLink = new LinkedHashMap<>();
                    selfLink.put("rel", "self");
                    selfLink.put("href",
                            "/api/v1/webhooks/" + webhook.getId());
                    selfLink.put("method", "GET");
                    links.add(selfLink);

                    Map<String, String> deleteLink = new LinkedHashMap<>();
                    deleteLink.put("rel", "delete");
                    deleteLink.put("href",
                            "/api/v1/webhooks/" + webhook.getId());
                    deleteLink.put("method", "DELETE");
                    links.add(deleteLink);

                    map.put("links", links);
                    return map;
                }).collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("count", webhooks.size());
        response.put("webhooks", webhookList);

        return Response.ok(response).build();
    }

    // POST /api/v1/webhooks — Register a new webhook
    @POST
    public Response createWebhook(Webhook webhook) {
        // Validate callback URL
        if (webhook.getCallbackUrl() == null
                || webhook.getCallbackUrl().trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "callbackUrl is required.");
        }

        // Validate at least one event type
        if (webhook.getEvents() == null
                || webhook.getEvents().isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one event type is required.");
        }

        String id = webhookRepo.generateId();
        webhook.setId(id);
        if (webhook.getActive() == null) {
            webhook.setActive(true);
        }

        webhookRepo.add(webhook);

        URI locationUri = uriInfo.getAbsolutePathBuilder()
                .path(webhook.getId())
                .build();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", webhook.getId());
        response.put("callbackUrl", webhook.getCallbackUrl());
        response.put("events", webhook.getEvents());
        response.put("description", webhook.getDescription());
        response.put("active", webhook.getActive());
        response.put("threshold", webhook.getThreshold());
        response.put("sensorId", webhook.getSensorId());
        response.put("createdAt", webhook.getCreatedAt());

        List<Map<String, String>> links = new ArrayList<>();

        Map<String, String> selfLink = new LinkedHashMap<>();
        selfLink.put("rel", "self");
        selfLink.put("href", "/api/v1/webhooks/" + webhook.getId());
        selfLink.put("method", "GET");
        links.add(selfLink);

        Map<String, String> deleteLink = new LinkedHashMap<>();
        deleteLink.put("rel", "delete");
        deleteLink.put("href", "/api/v1/webhooks/" + webhook.getId());
        deleteLink.put("method", "DELETE");
        links.add(deleteLink);

        response.put("links", links);

        return Response.created(locationUri)
                .entity(response)
                .build();
    }

    // GET /api/v1/webhooks/{webhookId} — Get a specific webhook
    @GET
    @Path("/{webhookId}")
    public Response getWebhookById(
            @PathParam("webhookId") String webhookId) {
        Webhook webhook = webhookRepo.findById(webhookId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Webhook not found with id: " + webhookId));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", webhook.getId());
        response.put("callbackUrl", webhook.getCallbackUrl());
        response.put("events", webhook.getEvents());
        response.put("description", webhook.getDescription());
        response.put("active", webhook.getActive());
        response.put("threshold", webhook.getThreshold());
        response.put("sensorId", webhook.getSensorId());
        response.put("failureCount", webhook.getFailureCount());
        response.put("createdAt", webhook.getCreatedAt());

        List<Map<String, String>> links = new ArrayList<>();

        Map<String, String> selfLink = new LinkedHashMap<>();
        selfLink.put("rel", "self");
        selfLink.put("href", "/api/v1/webhooks/" + webhook.getId());
        selfLink.put("method", "GET");
        links.add(selfLink);

        Map<String, String> deleteLink = new LinkedHashMap<>();
        deleteLink.put("rel", "delete");
        deleteLink.put("href", "/api/v1/webhooks/" + webhook.getId());
        deleteLink.put("method", "DELETE");
        links.add(deleteLink);

        response.put("links", links);

        return Response.ok(response).build();
    }

    // DELETE /api/v1/webhooks/{webhookId} — Unregister a webhook
    @DELETE
    @Path("/{webhookId}")
    public Response deleteWebhook(
            @PathParam("webhookId") String webhookId) {
        if (!webhookRepo.exists(webhookId)) {
            throw new ResourceNotFoundException(
                    "Webhook not found with id: " + webhookId);
        }

        webhookRepo.remove(webhookId);
        return Response.noContent().build();
    }
}