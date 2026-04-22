package com.smartcampus.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Webhook {

    public enum EventType {
        READING_CREATED,        // fires when any new reading is added
        THRESHOLD_EXCEEDED,     // fires when a reading exceeds a threshold
        SENSOR_STATUS_CHANGED,  // fires when sensor goes OFFLINE/MAINTENANCE
        ROOM_CREATED,           // fires when a new room is created
        ROOM_DELETED            // fires when a room is deleted
    }

    private String id;
    private String callbackUrl;         // URL to send notifications to
    private List<EventType> events;     // which events to listen for
    private String description;
    private String createdAt;
    private Boolean active;
    private Double threshold;           // optional: for THRESHOLD_EXCEEDED
    private String sensorId;            // optional: listen to specific sensor
    private Integer failureCount;       // track consecutive failures
    private static final Integer MAX_FAILURES = 5;

    public Webhook() {
        this.events = new ArrayList<>();
        this.createdAt = Instant.now().toString();
        this.active = true;
        this.failureCount = 0;
    }

    public Webhook(String id, String callbackUrl,
                   List<EventType> events, String description) {
        this.id = id;
        this.callbackUrl = callbackUrl;
        this.events = events;
        this.description = description;
        this.createdAt = Instant.now().toString();
        this.active = true;
        this.failureCount = 0;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public List<EventType> getEvents() {
        return events;
    }

    public void setEvents(List<EventType> events) {
        this.events = events;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    public String getSensorId() {
        return sensorId;
    }

    public void setSensorId(String sensorId) {
        this.sensorId = sensorId;
    }

    public Integer getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(Integer failureCount) {
        this.failureCount = failureCount;
    }

    public void incrementFailureCount() {
        this.failureCount++;
        // Auto-disable after too many failures
        if (this.failureCount >= MAX_FAILURES) {
            this.active = false;
        }
    }

    public void resetFailureCount() {
        this.failureCount = 0;
    }

    public Boolean isActive() {
        return this.active;
    }
}