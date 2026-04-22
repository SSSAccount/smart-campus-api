package com.smartcampus.resource;

import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.repository.SensorReadingRepository;
import com.smartcampus.repository.SensorRepository;
import com.smartcampus.service.WebhookService;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Sub-resource class for managing sensor readings.
 * Accessed via the sub-resource locator in SensorResource.
 * This class is NOT annotated with @Path because it is
 * instantiated and returned by the parent resource.
 *
 * Handles GET (reading history) and POST (new reading).
 * POST automatically updates the parent sensor's currentValue
 * and triggers webhook notifications.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final Sensor sensor;
    private final SensorRepository sensorRepo;
    private final SensorReadingRepository readingRepo;

    /**
     * Constructor called by the sub-resource locator in SensorResource.
     * Receives the validated sensor object and repository references.
     *
     * @param sensor the parent sensor (already validated to exist)
     * @param sensorRepo sensor repository for updating currentValue
     * @param readingRepo reading repository for storing readings
     */
    public SensorReadingResource(Sensor sensor,
                                  SensorRepository sensorRepo,
                                  SensorReadingRepository readingRepo) {
        this.sensor = sensor;
        this.sensorRepo = sensorRepo;
        this.readingRepo = readingRepo;
    }

    /**
     * Returns the full reading history for this sensor.
     * Includes sensor metadata, reading count, and HATEOAS links.
     *
     * @return 200 OK with list of all readings for this sensor
     */
    @GET
    public Response getReadings() {
        List<SensorReading> readings
                = readingRepo.findBySensorId(sensor.getId());

        List<Map<String, Object>> readingList = readings.stream()
                .map(reading -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", reading.getId());
                    map.put("timestamp", reading.getTimestamp());
                    map.put("value", reading.getValue());
                    return map;
                }).collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sensorId", sensor.getId());
        response.put("sensorType", sensor.getType());
        response.put("count", readings.size());
        response.put("readings", readingList);

        List<Map<String, String>> links = new ArrayList<>();
        links.add(makeLink("self",
                "/api/v1/sensors/" + sensor.getId()
                + "/readings", "GET"));
        links.add(makeLink("add-reading",
                "/api/v1/sensors/" + sensor.getId()
                + "/readings", "POST"));
        links.add(makeLink("sensor",
                "/api/v1/sensors/" + sensor.getId(), "GET"));
        response.put("links", links);

        return Response.ok(response).build();
    }

    /**
     * Adds a new reading to this sensor.
     *
     * Validates:
     * - Sensor is not in MAINTENANCE status (returns 403)
     * - Sensor is not in OFFLINE status (returns 403)
     * - Reading body is not null (returns 400)
     * - Reading value is provided (returns 400)
     *
     * On success:
     * - Generates a UUID for the reading ID
     * - Sets timestamp to current epoch milliseconds if not provided
     * - Saves the reading to the repository
     * - Updates the parent sensor's currentValue atomically
     * - Triggers webhook notifications for subscribers
     * - Returns 201 Created
     *
     * @param reading the reading data from the request body
     * @return 201 Created with reading details and updated currentValue
     */
    @POST
    public Response addReading(SensorReading reading) {

        // Block readings if sensor is in MAINTENANCE
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                    "Sensor '" + sensor.getId()
                    + "' is currently in MAINTENANCE mode "
                    + "and cannot accept readings.");
        }

        // Block readings if sensor is OFFLINE
        if ("OFFLINE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                    "Sensor '" + sensor.getId()
                    + "' is currently OFFLINE "
                    + "and cannot accept readings.");
        }

        // Validate reading body
        if (reading == null) {
            throw new IllegalArgumentException(
                    "Reading body cannot be empty.");
        }
        if (reading.getValue() == null) {
            throw new IllegalArgumentException(
                    "Reading value is required.");
        }

        // Generate UUID and set timestamp
        reading.setId(UUID.randomUUID().toString());
        if (reading.getTimestamp() == null) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        // ACID: atomic operation with rollback
        Double previousValue = sensor.getCurrentValue();
        try {
            readingRepo.add(sensor.getId(), reading);
            sensor.setCurrentValue(reading.getValue());
        } catch (Exception e) {
            sensor.setCurrentValue(previousValue);
            throw new RuntimeException(
                    "Failed to save reading. Transaction rolled back.");
        }

        // Webhook notification (non-blocking, runs in background)
        try {
            WebhookService.getInstance()
                    .notifyReadingCreated(sensor, reading);
        } catch (Exception e) {
            System.err.println("[WEBHOOK] Error: "
                    + e.getMessage());
        }

        // Build response
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", reading.getId());
        response.put("sensorId", sensor.getId());
        response.put("timestamp", reading.getTimestamp());
        response.put("value", reading.getValue());
        response.put("sensorCurrentValue",
                sensor.getCurrentValue());

        List<Map<String, String>> links = new ArrayList<>();
        links.add(makeLink("all-readings",
                "/api/v1/sensors/" + sensor.getId()
                + "/readings", "GET"));
        links.add(makeLink("sensor",
                "/api/v1/sensors/" + sensor.getId(), "GET"));
        response.put("links", links);

        return Response.status(Response.Status.CREATED)
                .entity(response).build();
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