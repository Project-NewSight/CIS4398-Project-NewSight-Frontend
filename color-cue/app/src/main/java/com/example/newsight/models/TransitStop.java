package com.example.newsight.models;

/**
 * TransitStop - Represents a bus/train stop
 */
public class TransitStop {
    private String name;
    private double lat;
    private double lng;
    private int distance_m;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public int getDistanceM() {
        return distance_m;
    }

    public void setDistanceM(int distance_m) {
        this.distance_m = distance_m;
    }

    @Override
    public String toString() {
        return String.format("TransitStop[%s at (%.6f, %.6f), %dm away]", 
                name, lat, lng, distance_m);
    }
}

