package com.smartcampus.repository;

import com.smartcampus.model.Sensor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class SensorRepository {

    private static SensorRepository instance;

    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger(1);

    private SensorRepository() {
    }

    public static synchronized SensorRepository getInstance() {
        if (instance == null) {
            instance = new SensorRepository();
        }
        return instance;
    }

    public String generateId() {
        return "sensor-" + idCounter.getAndIncrement();
    }

    public Sensor add(Sensor sensor) {
        sensors.put(sensor.getId(), sensor);
        return sensor;
    }

    public Optional<Sensor> findById(String id) {
        return Optional.ofNullable(sensors.get(id));
    }

    public List<Sensor> findAll() {
        return new ArrayList<>(sensors.values());
    }

    public List<Sensor> findByType(String type) {
        return sensors.values().stream()
                .filter(s -> s.getType().equalsIgnoreCase(type))
                .collect(Collectors.toList());
    }

    public List<Sensor> findByRoomId(String roomId) {
        return sensors.values().stream()
                .filter(s -> s.getRoomId().equals(roomId))
                .collect(Collectors.toList());
    }

    public boolean exists(String id) {
        return sensors.containsKey(id);
    }

    public boolean remove(String id) {
        return sensors.remove(id) != null;
    }
}