package com.smartcampus.repository;

import com.smartcampus.model.Webhook;
import com.smartcampus.model.Webhook.EventType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class WebhookRepository {

    private static WebhookRepository instance;

    private final Map<String, Webhook> webhooks = new ConcurrentHashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger(1);

    private WebhookRepository() {
    }

    public static synchronized WebhookRepository getInstance() {
        if (instance == null) {
            instance = new WebhookRepository();
        }
        return instance;
    }

    public String generateId() {
        return "webhook-" + idCounter.getAndIncrement();
    }

    public Webhook add(Webhook webhook) {
        webhooks.put(webhook.getId(), webhook);
        return webhook;
    }

    public Optional<Webhook> findById(String id) {
        return Optional.ofNullable(webhooks.get(id));
    }

    public List<Webhook> findAll() {
        return new ArrayList<>(webhooks.values());
    }

    // Find all active webhooks subscribed to a specific event type
    public List<Webhook> findByEventType(EventType eventType) {
        return webhooks.values().stream()
                .filter(w -> w.isActive())
                .filter(w -> w.getEvents().contains(eventType))
                .collect(Collectors.toList());
    }

    // Find webhooks for a specific sensor and event type
    public List<Webhook> findBySensorAndEvent(String sensorId,
                                               EventType eventType) {
        return webhooks.values().stream()
                .filter(w -> w.isActive())
                .filter(w -> w.getEvents().contains(eventType))
                .filter(w -> w.getSensorId() == null
                        || w.getSensorId().equals(sensorId))
                .collect(Collectors.toList());
    }

    public boolean exists(String id) {
        return webhooks.containsKey(id);
    }

    public boolean remove(String id) {
        return webhooks.remove(id) != null;
    }
}