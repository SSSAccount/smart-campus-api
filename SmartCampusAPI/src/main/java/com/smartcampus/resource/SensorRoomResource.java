package com.smartcampus.resource;

import com.smartcampus.exception.ResourceNotFoundException;
import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.Webhook;
import com.smartcampus.repository.RoomRepository;
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
 * Resource class for managing campus rooms.
 * Supports full CRUD with pagination, input validation,
 * deletion protection, and webhook notifications.
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorRoomResource {

    private final RoomRepository roomRepo = RoomRepository.getInstance();
    private final SensorRepository sensorRepo = SensorRepository.getInstance();

    @Context
    private UriInfo uriInfo;

    /**
     * Lists all rooms with optional pagination.
     * Supports ?page= and ?size= query parameters.
     * Default page size is 20 if not specified.
     * Response includes pagination metadata and HATEOAS page links
     * (self, previous, next, first, last).
     *
     * @param page the page number (1-based, default 1)
     * @param size the number of items per page (default 20)
     * @return paginated list of rooms with metadata and links
     */
    @GET
    public Response getAllRooms(@QueryParam("page") Integer page,
                                @QueryParam("size") Integer size) {
        List<Room> allRooms = roomRepo.findAll();

        Integer currentPage = (page != null && page > 0) ? page : 1;
        Integer pageSize = (size != null && size > 0) ? size : 20;
        Integer totalItems = allRooms.size();
        Integer totalPages = (Integer) Math.max(1,
                (int) Math.ceil((double) totalItems / pageSize));
        Integer startIndex = (currentPage - 1) * pageSize;
        Integer endIndex = Math.min(startIndex + pageSize, totalItems);

        List<Room> pagedRooms;
        if (startIndex >= totalItems) {
            pagedRooms = new ArrayList<>();
        } else {
            pagedRooms = allRooms.subList(startIndex, endIndex);
        }

        List<Map<String, Object>> roomList = pagedRooms.stream()
                .map(this::buildRoomResponse)
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("count", pagedRooms.size());
        response.put("totalItems", totalItems);
        response.put("currentPage", currentPage);
        response.put("pageSize", pageSize);
        response.put("totalPages", totalPages);
        response.put("rooms", roomList);

        // Pagination HATEOAS links
        List<Map<String, String>> pageLinks = new ArrayList<>();
        pageLinks.add(makeLink("self",
                "/api/v1/rooms?page=" + currentPage
                + "&size=" + pageSize, "GET"));
        if (currentPage > 1) {
            pageLinks.add(makeLink("previous",
                    "/api/v1/rooms?page=" + (currentPage - 1)
                    + "&size=" + pageSize, "GET"));
        }
        if (currentPage < totalPages) {
            pageLinks.add(makeLink("next",
                    "/api/v1/rooms?page=" + (currentPage + 1)
                    + "&size=" + pageSize, "GET"));
        }
        pageLinks.add(makeLink("first",
                "/api/v1/rooms?page=1&size=" + pageSize, "GET"));
        pageLinks.add(makeLink("last",
                "/api/v1/rooms?page=" + totalPages
                + "&size=" + pageSize, "GET"));
        response.put("pageLinks", pageLinks);

        return Response.ok(response).build();
    }

    /**
     * Creates a new room. Validates that name and capacity are provided.
     * Returns 201 Created with a Location header pointing to the
     * newly created resource.
     *
     * @param room the room data from the request body
     * @return 201 Created with room details and Location header
     */
    @POST
    public Response createRoom(Room room) {
        // Input validation
        if (room == null) {
            throw new IllegalArgumentException(
                    "Request body cannot be empty.");
        }
        if (room.getName() == null || room.getName().trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Room name is required.");
        }
        if (room.getCapacity() == null || room.getCapacity() <= 0) {
            throw new IllegalArgumentException(
                    "Room capacity must be a positive number.");
        }

        String id = roomRepo.generateId();
        room.setId(id);
        if (room.getSensorIds() == null) {
            room.setSensorIds(new ArrayList<>());
        }

        roomRepo.add(room);

        // Webhook notification
        try {
            WebhookService.getInstance()
                    .notifyRoomEvent(room.getId(), room.getName(),
                            Webhook.EventType.ROOM_CREATED);
        } catch (Exception e) {
            System.err.println("[WEBHOOK] Error: " + e.getMessage());
        }

        URI locationUri = uriInfo.getAbsolutePathBuilder()
                .path(room.getId()).build();

        return Response.created(locationUri)
                .entity(buildRoomResponse(room)).build();
    }

    /**
     * Returns a specific room by ID.
     * Throws 404 if the room does not exist.
     *
     * @param roomId the ID of the room to retrieve
     * @return 200 OK with room details
     */
    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = roomRepo.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Room not found with id: " + roomId));

        return Response.ok(buildRoomResponse(room)).build();
    }

    /**
     * Updates an existing room's name and/or capacity.
     * Only updates fields that are provided and valid.
     * Throws 404 if the room does not exist.
     *
     * @param roomId the ID of the room to update
     * @param updatedRoom the updated room data
     * @return 200 OK with updated room details
     */
    @PUT
    @Path("/{roomId}")
    public Response updateRoom(@PathParam("roomId") String roomId,
                                Room updatedRoom) {
        Room existing = roomRepo.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Room not found with id: " + roomId));

        if (updatedRoom == null) {
            throw new IllegalArgumentException(
                    "Request body cannot be empty.");
        }

        if (updatedRoom.getName() != null
                && !updatedRoom.getName().trim().isEmpty()) {
            existing.setName(updatedRoom.getName());
        }
        if (updatedRoom.getCapacity() != null
                && updatedRoom.getCapacity() > 0) {
            existing.setCapacity(updatedRoom.getCapacity());
        }

        return Response.ok(buildRoomResponse(existing)).build();
    }

    /**
     * Deletes a room by ID. Blocks deletion if the room still has
     * sensors assigned, returning 409 Conflict.
     * Returns 204 No Content on successful deletion.
     * Triggers ROOM_DELETED webhook notification.
     *
     * @param roomId the ID of the room to delete
     * @return 204 No Content on success, 409 if room has sensors
     */
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = roomRepo.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Room not found with id: " + roomId));

        List<Sensor> sensorsInRoom = sensorRepo.findByRoomId(roomId);
        if (!sensorsInRoom.isEmpty() || !room.getSensorIds().isEmpty()) {
            Integer sensorCount = Math.max(sensorsInRoom.size(),
                    room.getSensorIds().size());
            throw new RoomNotEmptyException(
                    "Cannot delete room '" + roomId
                    + "' because it still has " + sensorCount
                    + " sensor(s) assigned. Remove all sensors first.");
        }

        String roomName = room.getName();
        roomRepo.remove(roomId);

        // Webhook notification
        try {
            WebhookService.getInstance()
                    .notifyRoomEvent(roomId, roomName,
                            Webhook.EventType.ROOM_DELETED);
        } catch (Exception e) {
            System.err.println("[WEBHOOK] Error: " + e.getMessage());
        }

        return Response.noContent().build();
    }

    /**
     * Builds a standard room response map with HATEOAS links.
     *
     * @param room the room to build a response for
     * @return map containing room fields and links
     */
    private Map<String, Object> buildRoomResponse(Room room) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", room.getId());
        response.put("name", room.getName());
        response.put("capacity", room.getCapacity());
        response.put("sensorIds", room.getSensorIds());

        List<Map<String, String>> links = new ArrayList<>();
        links.add(makeLink("self",
                "/api/v1/rooms/" + room.getId(), "GET"));
        links.add(makeLink("update",
                "/api/v1/rooms/" + room.getId(), "PUT"));
        links.add(makeLink("delete",
                "/api/v1/rooms/" + room.getId(), "DELETE"));
        links.add(makeLink("sensors",
                "/api/v1/sensors?roomId=" + room.getId(), "GET"));
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