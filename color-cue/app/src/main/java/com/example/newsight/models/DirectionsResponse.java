package com.example.newsight.models;

import java.util.List;

/**
 * DirectionsResponse - Full response from /voice/transcribe with navigation directions
 * Matches the backend response from voice_routes.py
 */
public class DirectionsResponse {
    private String status;
    private String destination;
    private LocationCoordinates origin;
    private String total_distance;
    private String total_duration;
    private int total_distance_meters;
    private int total_duration_seconds;
    private List<NavigationStep> steps;
    private String message;

    public DirectionsResponse() {}

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public LocationCoordinates getOrigin() {
        return origin;
    }

    public void setOrigin(LocationCoordinates origin) {
        this.origin = origin;
    }

    public String getTotalDistance() {
        return total_distance;
    }

    public void setTotalDistance(String total_distance) {
        this.total_distance = total_distance;
    }

    public String getTotalDuration() {
        return total_duration;
    }

    public void setTotalDuration(String total_duration) {
        this.total_duration = total_duration;
    }

    public int getTotalDistanceMeters() {
        return total_distance_meters;
    }

    public void setTotalDistanceMeters(int total_distance_meters) {
        this.total_distance_meters = total_distance_meters;
    }

    public int getTotalDurationSeconds() {
        return total_duration_seconds;
    }

    public void setTotalDurationSeconds(int total_duration_seconds) {
        this.total_duration_seconds = total_duration_seconds;
    }

    public List<NavigationStep> getSteps() {
        return steps;
    }

    public void setSteps(List<NavigationStep> steps) {
        this.steps = steps;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return String.format("Directions to %s: %s (%d steps)",
                destination, total_distance, steps != null ? steps.size() : 0);
    }
}

