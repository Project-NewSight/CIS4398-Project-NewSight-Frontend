package com.example.newsight.models;

/**
 * NavigationStep - Represents a single step in the navigation directions
 * Matches the backend schema from navigation_service.py
 */
public class NavigationStep {
    private String instruction;
    private String distance;
    private String duration;
    private int distance_meters;
    private int duration_seconds;
    private LocationCoordinates start_location;
    private LocationCoordinates end_location;

    public NavigationStep() {}

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public int getDistanceMeters() {
        return distance_meters;
    }

    public void setDistanceMeters(int distance_meters) {
        this.distance_meters = distance_meters;
    }

    public int getDurationSeconds() {
        return duration_seconds;
    }

    public void setDurationSeconds(int duration_seconds) {
        this.duration_seconds = duration_seconds;
    }

    public LocationCoordinates getStartLocation() {
        return start_location;
    }

    public void setStartLocation(LocationCoordinates start_location) {
        this.start_location = start_location;
    }

    public LocationCoordinates getEndLocation() {
        return end_location;
    }

    public void setEndLocation(LocationCoordinates end_location) {
        this.end_location = end_location;
    }

    @Override
    public String toString() {
        return instruction + " (" + distance + ", " + duration + ")";
    }
}

