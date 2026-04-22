package com.smartcampus.repository;

import com.smartcampus.model.Room;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RoomRepository {

    // Singleton instance - shared across all requests
    private static RoomRepository instance;

    // Thread-safe storage
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger(1);

    // Private constructor prevents direct creation
    private RoomRepository() {
    }

    // Get the single shared instance
    public static synchronized RoomRepository getInstance() {
        if (instance == null) {
            instance = new RoomRepository();
        }
        return instance;
    }

    public String generateId() {
        return "room-" + idCounter.getAndIncrement();
    }

    public Room add(Room room) {
        rooms.put(room.getId(), room);
        return room;
    }

    public Optional<Room> findById(String id) {
        return Optional.ofNullable(rooms.get(id));
    }

    public List<Room> findAll() {
        return new ArrayList<>(rooms.values());
    }

    public boolean exists(String id) {
        return rooms.containsKey(id);
    }

    public boolean remove(String id) {
        return rooms.remove(id) != null;
    }
}