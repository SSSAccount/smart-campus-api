package com.smartcampus.repository;

import com.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SensorReadingRepository {

    private static SensorReadingRepository instance;

    private final Map<String, List<SensorReading>> readings
            = new ConcurrentHashMap<>();

    private SensorReadingRepository() {
    }

    public static synchronized SensorReadingRepository getInstance() {
        if (instance == null) {
            instance = new SensorReadingRepository();
        }
        return instance;
    }

    public SensorReading add(String sensorId, SensorReading reading) {
        readings.computeIfAbsent(sensorId,
                k -> Collections.synchronizedList(new ArrayList<>()));
        readings.get(sensorId).add(reading);
        return reading;
    }

    public List<SensorReading> findBySensorId(String sensorId) {
        return readings.getOrDefault(sensorId, new ArrayList<>());
    }
}