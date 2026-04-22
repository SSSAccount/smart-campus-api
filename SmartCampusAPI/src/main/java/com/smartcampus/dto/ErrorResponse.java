package com.smartcampus.dto;

import java.time.Instant;

/**
 * Structured error response returned by all exception mappers.
 * Provides consistent error format across the entire API.
 */
public class ErrorResponse {

    private Integer statusCode;
    private String error;
    private String message;
    private String timestamp;
    private String path;

    public ErrorResponse() {
        this.timestamp = Instant.now().toString();
    }

    public ErrorResponse(Integer statusCode, String error,
                          String message, String path) {
        this.statusCode = statusCode;
        this.error = error;
        this.message = message;
        this.timestamp = Instant.now().toString();
        this.path = path;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}