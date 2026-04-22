package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.exception.ResourceNotFoundException;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.repository.RoomRepository;
import com.smartcampus.repository.SensorReadingRepository;
import com.smartcampus.repository.SensorRepository;
import com.smartcampus.service.WebhookService;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
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

/**
 * Resource class for managing sensors.
 * Supports CRUD operations, filtering by type, status updates,
 * input validation, and delegates to SensorReadingResource
 * for the readings sub-resource.
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final SensorRepository sensorRepo
            = SensorRepository.getInstance();
    private final RoomRepository roomRepo
            = RoomRepository.getInstance();
    private final SensorReadingRepository readingRepo
            = SensorReadingRepository.getInstance();

    @Context
    private UriInfo uriInfo;

    /**
     * Lists all sensors with optional type filtering.
     * Supports ?type= query parameter for filtering by sensor type.
     * Query parameters are optional — omitting them returns all sensors.
     *
     * @param type optional sensor type filter (e.g., "CO2", "Temperature")
     * @return list of sensors matching the filter, or all sensors
     */
    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        List<Sensor> sensors;

        if (type != null && !type.trim().isEmpty()) {
            sensors = sensorRepo.findByType(type);
        } else {
            sensors = sensorRepo.findAll();
        }

        List<Map<String, Object>> sensorList = sensors.stream()
                .map(this::buildSensorResponse)
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("count", sensors.size());
        if (type != null && !type.trim().isEmpty()) {
            response.put("filter", "type=" + type);
        }
        response.put("sensors", sensorList);

        return Response.ok(response).build();
    }

    /**
     * Registers a new sensor. Validates that:
     * - type is provided and not empty
     * - status (if provided) is ACTIVE, MAINTENANCE, or OFFLINE
     * - roomId is provided and references an existing room
     *
     * Uses ACID-compliant try-catch with rollback if the room
     * update fails after sensor creation.
     *
     * Returns 201 Created with a Location header.
     *
     * @param sensor the sensor data from the request body
     * @return 201 Created with sensor details and Location header
     */
    @POST
    public Response createSensor(Sensor sensor) {
        // Input validation
        if (sensor == null) {
            throw new IllegalArgumentException(
                    "Request body cannot be empty.");
        }
        if (sensor.getType() == null
                || sensor.getType().trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Sensor type is required.");
        }
        if (sensor.getStatus() != null
                && !sensor.getStatus().equalsIgnoreCase("ACTIVE")
                && !sensor.getStatus().equalsIgnoreCase("MAINTENANCE")
                && !sensor.getStatus().equalsIgnoreCase("OFFLINE")) {
            throw new IllegalArgumentException(
                    "Sensor status must be ACTIVE, MAINTENANCE, or OFFLINE. "
                    + "Received: '" + sensor.getStatus() + "'");
        }
        if (sensor.getRoomId() == null
                || sensor.getRoomId().trim().isEmpty()) {
            throw new LinkedResourceNotFoundException(
                    "roomId is required when creating a sensor.");
        }
        if (!roomRepo.exists(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException(
                    "Room with id '" + sensor.getRoomId()
                    + "' does not exist. Cannot create sensor "
                    + "for a non-existent room.");
        }

        String id = sensorRepo.generateId();
        sensor.setId(id);
        if (sensor.getStatus() == null
                || sensor.getStatus().trim().isEmpty()) {
            sensor.setStatus("ACTIVE");
        } else {
            sensor.setStatus(sensor.getStatus().toUpperCase());
        }

        // ACID: atomic operation with rollback
        try {
            sensorRepo.add(sensor);
            Room room = roomRepo.findById(sensor.getRoomId()).get();
            room.addSensorId(sensor.getId());
        } catch (Exception e) {
            sensorRepo.remove(sensor.getId());
            throw new RuntimeException(
                    "Failed to register sensor. Transaction rolled back.");
        }

        URI locationUri = uriInfo.getAbsolutePathBuilder()
                .path(sensor.getId()).build();

        return Response.created(locationUri)
                .entity(buildSensorResponse(sensor)).build();
    }

    /**
     * Returns a specific sensor by ID.
     * Throws 404 if the sensor does not exist.
     *
     * @param sensorId the ID of the sensor to retrieve
     * @return 200 OK with sensor details
     */
    @GET
    @Path("/{sensorId}")
    public Response getSensorById(
            @PathParam("sensorId") String sensorId) {
        Sensor sensor = sensorRepo.findById(sensorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sensor not found with id: " + sensorId));

        return Response.ok(buildSensorResponse(sensor)).build();
    }

    /**
     * Updates a sensor's type or status.
     * Validates that status (if provided) is a valid value.
     * Triggers a SENSOR_STATUS_CHANGED webhook if status changes.
     *
     * @param sensorId the ID of the sensor to update
     * @param updatedSensor the updated sensor data
     * @return 200 OK with updated sensor details
     */
    @PUT
    @Path("/{sensorId}")
    public Response updateSensor(
            @PathParam("sensorId") String sensorId,
            Sensor updatedSensor) {
        Sensor existing = sensorRepo.findById(sensorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sensor not found with id: " + sensorId));

        if (updatedSensor == null) {
            throw new IllegalArgumentException(
                    "Request body cannot be empty.");
        }

        String oldStatus = existing.getStatus();

        if (updatedSensor.getType() != null
                && !updatedSensor.getType().trim().isEmpty()) {
            existing.setType(updatedSensor.getType());
        }
        if (updatedSensor.getStatus() != null
                && !updatedSensor.getStatus().trim().isEmpty()) {
            if (!updatedSensor.getStatus().equalsIgnoreCase("ACTIVE")
                    && !updatedSensor.getStatus().equalsIgnoreCase("MAINTENANCE")
                    && !updatedSensor.getStatus().equalsIgnoreCase("OFFLINE")) {
                throw new IllegalArgumentException(
                        "Sensor status must be ACTIVE, MAINTENANCE, "
                        + "or OFFLINE.");
            }
            existing.setStatus(
                    updatedSensor.getStatus().toUpperCase());
        }

        // Webhook notification if status changed
        String newStatus = existing.getStatus();
        if (!oldStatus.equals(newStatus)) {
            try {
                WebhookService.getInstance()
                        .notifySensorStatusChanged(
                                existing, oldStatus, newStatus);
            } catch (Exception e) {
                System.err.println("[WEBHOOK] Error: "
                        + e.getMessage());
            }
        }

        return Response.ok(buildSensorResponse(existing)).build();
    }

    /**
     * Deletes a sensor and removes its ID from the parent room's
     * sensorIds list. Returns 204 No Content on success.
     *
     * @param sensorId the ID of the sensor to delete
     * @return 204 No Content
     */
    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(
            @PathParam("sensorId") String sensorId) {
        Sensor sensor = sensorRepo.findById(sensorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sensor not found with id: " + sensorId));

        // Remove sensor ID from parent room's list
        roomRepo.findById(sensor.getRoomId())
                .ifPresent(room -> room.removeSensorId(sensorId));

        sensorRepo.remove(sensorId);

        return Response.noContent().build();
    }

    /**
     * Sub-resource locator method for sensor readings.
     *
     * This method has NO @GET, @POST, or other HTTP method annotation,
     * which tells JAX-RS this is a locator, not a handler.
     *
     * It validates the sensor exists, then delegates all
     * /sensors/{sensorId}/readings requests to the dedicated
     * SensorReadingResource class.
     *
     * This follows the delegation pattern — the parent validates,
     * the child handles the business logic.
     *
     * @param sensorId the ID of the sensor
     * @return SensorReadingResource instance to handle the request
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingsSubResource(
            @PathParam("sensorId") String sensorId) {

        Sensor sensor = sensorRepo.findById(sensorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sensor not found with id: " + sensorId));

        return new SensorReadingResource(
                sensor, sensorRepo, readingRepo);
    }

    /**
     * Builds a standard sensor response map with HATEOAS links.
     *
     * @param sensor the sensor to build a response for
     * @return map containing sensor fields and links
     */
    private Map<String, Object> buildSensorResponse(Sensor sensor) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", sensor.getId());
        response.put("type", sensor.getType());
        response.put("status", sensor.getStatus());
        response.put("currentValue", sensor.getCurrentValue());
        response.put("roomId", sensor.getRoomId());

        List<Map<String, String>> links = new ArrayList<>();
        links.add(makeLink("self",
                "/api/v1/sensors/" + sensor.getId(), "GET"));
        links.add(makeLink("update",
                "/api/v1/sensors/" + sensor.getId(), "PUT"));
        links.add(makeLink("delete",
                "/api/v1/sensors/" + sensor.getId(), "DELETE"));
        links.add(makeLink("readings",
                "/api/v1/sensors/" + sensor.getId()
                + "/readings", "GET"));
        links.add(makeLink("room",
                "/api/v1/rooms/" + sensor.getRoomId(), "GET"));
        response.put("links", links);

        return response;
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