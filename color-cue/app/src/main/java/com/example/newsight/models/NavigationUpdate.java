package com.example.newsight.models;

/**
 * NavigationUpdate - Real-time navigation update from /navigation/ws WebSocket
 * Matches the backend NavigationUpdate schema from schemas.py
 */
public class NavigationUpdate {
    private String status; // "navigating" | "step_completed" | "arrived"
    private int current_step;
    private int total_steps;
    private String instruction;
    private double distance_to_next;
    private boolean should_announce;
    private String announcement;

    public NavigationUpdate() {}

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCurrentStep() {
        return current_step;
    }

    public void setCurrentStep(int current_step) {
        this.current_step = current_step;
    }

    public int getTotalSteps() {
        return total_steps;
    }

    public void setTotalSteps(int total_steps) {
        this.total_steps = total_steps;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public double getDistanceToNext() {
        return distance_to_next;
    }

    public void setDistanceToNext(double distance_to_next) {
        this.distance_to_next = distance_to_next;
    }

    public boolean isShouldAnnounce() {
        return should_announce;
    }

    public void setShouldAnnounce(boolean should_announce) {
        this.should_announce = should_announce;
    }

    public String getAnnouncement() {
        return announcement;
    }

    public void setAnnouncement(String announcement) {
        this.announcement = announcement;
    }

    /**
     * Format distance for display (meters -> feet/miles)
     */
    public String getFormattedDistance() {
        double feet = distance_to_next * 3.28084;
        
        if (feet < 528) { // Less than 0.1 miles
            return String.format("%.0f ft", feet);
        } else {
            double miles = feet / 5280;
            return String.format("%.1f mi", miles);
        }
    }

    @Override
    public String toString() {
        return String.format("Step %d/%d: %s (%.0fm)", 
                current_step, total_steps, instruction, distance_to_next);
    }
}

