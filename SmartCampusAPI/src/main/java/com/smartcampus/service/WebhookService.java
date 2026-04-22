package com.smartcampus.service;

import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.model.Webhook;
import com.smartcampus.model.Webhook.EventType;
import com.smartcampus.repository.WebhookRepository;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WebhookService {

    private static WebhookService instance;
    private final WebhookRepository webhookRepo
            = WebhookRepository.getInstance();

    private WebhookService() {
    }

    public static synchronized WebhookService getInstance() {
        if (instance == null) {
            instance = new WebhookService();
        }
        return instance;
    }

    // Called when a new reading is created
    public void notifyReadingCreated(Sensor sensor,
                                      SensorReading reading) {
        // Notify READING_CREATED subscribers
        List<Webhook> subscribers = webhookRepo
                .findBySensorAndEvent(sensor.getId(),
                        EventType.READING_CREATED);

        for (Webhook webhook : subscribers) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("event", "READING_CREATED");
            payload.put("timestamp", Instant.now().toString());
            payload.put("sensorId", sensor.getId());
            payload.put("sensorType", sensor.getType());
            payload.put("roomId", sensor.getRoomId());
            payload.put("readingId", reading.getId());
            payload.put("value", reading.getValue());

            sendNotification(webhook, payload);
        }

        // Check THRESHOLD_EXCEEDED subscribers
        List<Webhook> thresholdSubscribers = webhookRepo
                .findBySensorAndEvent(sensor.getId(),
                        EventType.THRESHOLD_EXCEEDED);

        for (Webhook webhook : thresholdSubscribers) {
            if (webhook.getThreshold() != null
                    && reading.getValue() > webhook.getThreshold()) {

                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("event", "THRESHOLD_EXCEEDED");
                payload.put("timestamp", Instant.now().toString());
                payload.put("sensorId", sensor.getId());
                payload.put("sensorType", sensor.getType());
                payload.put("roomId", sensor.getRoomId());
                payload.put("readingValue", reading.getValue());
                payload.put("threshold", webhook.getThreshold());
                payload.put("message",
                        "ALERT: " + sensor.getType() + " sensor '"
                        + sensor.getId() + "' reading of "
                        + reading.getValue()
                        + " exceeded threshold of "
                        + webhook.getThreshold());

                sendNotification(webhook, payload);
            }
        }
    }

    // Called when sensor status changes
    public void notifySensorStatusChanged(Sensor sensor,
                                           String oldStatus,
                                           String newStatus) {
        List<Webhook> subscribers = webhookRepo
                .findBySensorAndEvent(sensor.getId(),
                        EventType.SENSOR_STATUS_CHANGED);

        for (Webhook webhook : subscribers) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("event", "SENSOR_STATUS_CHANGED");
            payload.put("timestamp", Instant.now().toString());
            payload.put("sensorId", sensor.getId());
            payload.put("sensorType", sensor.getType());
            payload.put("previousStatus", oldStatus);
            payload.put("newStatus", newStatus);

            sendNotification(webhook, payload);
        }
    }

    // Called when a room is created or deleted
    public void notifyRoomEvent(String roomId, String roomName,
                                 EventType eventType) {
        List<Webhook> subscribers = webhookRepo
                .findByEventType(eventType);

        for (Webhook webhook : subscribers) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("event", eventType.toString());
            payload.put("timestamp", Instant.now().toString());
            payload.put("roomId", roomId);
            payload.put("roomName", roomName);

            sendNotification(webhook, payload);
        }
    }

    // Actually send the HTTP POST to the callback URL
    private void sendNotification(Webhook webhook,
                                   Map<String, Object> payload) {
        // Run in a separate thread so it doesn't block the API response
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(webhook.getCallbackUrl());
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type",
                        "application/json");
                connection.setRequestProperty("X-Webhook-Id",
                        webhook.getId());
                connection.setRequestProperty("X-Webhook-Event",
                        payload.get("event").toString());
                connection.setDoOutput(true);
                connection.setConnectTimeout(5000);  // 5 second timeout
                connection.setReadTimeout(5000);

                // Convert payload to JSON string manually
                String jsonPayload = mapToJson(payload);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonPayload
                            .getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                Integer responseCode = connection.getResponseCode();

                if (responseCode >= 200 && responseCode < 300) {
                    // Success — reset failure count
                    webhook.resetFailureCount();
                    System.out.println("[WEBHOOK] Successfully notified "
                            + webhook.getCallbackUrl()
                            + " (event: " + payload.get("event") + ")");
                } else {
                    // Server responded with error
                    webhook.incrementFailureCount();
                    System.err.println("[WEBHOOK] Failed to notify "
                            + webhook.getCallbackUrl()
                            + " (HTTP " + responseCode + "). "
                            + "Failures: " + webhook.getFailureCount());
                }

            } catch (Exception e) {
                // Connection failed entirely
                webhook.incrementFailureCount();
                System.err.println("[WEBHOOK] Connection failed to "
                        + webhook.getCallbackUrl()
                        + ": " + e.getMessage()
                        + ". Failures: " + webhook.getFailureCount());

                if (!webhook.isActive()) {
                    System.err.println("[WEBHOOK] Webhook "
                            + webhook.getId()
                            + " auto-disabled after "
                            + webhook.getFailureCount() + " failures.");
                }
            } finally {
                // Always close the connection
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    // Simple map to JSON converter (avoids needing extra libraries)
    private String mapToJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        Boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else {
                sb.append(value);
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}