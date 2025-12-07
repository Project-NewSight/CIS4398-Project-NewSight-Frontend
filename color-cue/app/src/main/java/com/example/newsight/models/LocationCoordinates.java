package com.example.newsight.models;

/**
 * LocationCoordinates - Represents a GPS location
 */
public class LocationCoordinates {
    private double lat;
    private double lng;

    public LocationCoordinates() {}

    public LocationCoordinates(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    @Override
    public String toString() {
        return String.format("(%.6f, %.6f)", lat, lng);
    }
}

