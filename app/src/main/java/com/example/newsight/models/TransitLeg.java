package com.example.newsight.models;

/**
 * TransitLeg - Represents one segment of a transit route (walk or transit)
 */
public class TransitLeg {
    private String type;  // "walk" or "transit"
    private Integer duration_min;
    private Integer distance_m;
    private String mode_name;  // "Bus", "Train", etc.
    private String route_short_name;  // "23", "R5", etc.
    private String route_long_name;  // Full route name
    private DepartureStatus departure_status;
    private Long departure_time;  // Unix timestamp - when bus arrives
    private Long scheduled_time;  // Scheduled departure time

    public static class DepartureStatus {
        private String status;  // "on_time", "delayed", "cancelled", "live"
        private Integer delay_min;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Integer getDelayMin() {
            return delay_min;
        }

        public void setDelayMin(Integer delay_min) {
            this.delay_min = delay_min;
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getDurationMin() {
        return duration_min;
    }

    public void setDurationMin(Integer duration_min) {
        this.duration_min = duration_min;
    }

    public Integer getDistanceM() {
        return distance_m;
    }

    public void setDistanceM(Integer distance_m) {
        this.distance_m = distance_m;
    }

    public String getModeName() {
        return mode_name;
    }

    public void setModeName(String mode_name) {
        this.mode_name = mode_name;
    }

    public String getRouteShortName() {
        return route_short_name;
    }

    public void setRouteShortName(String route_short_name) {
        this.route_short_name = route_short_name;
    }

    public String getRouteLongName() {
        return route_long_name;
    }

    public void setRouteLongName(String route_long_name) {
        this.route_long_name = route_long_name;
    }

    public DepartureStatus getDepartureStatus() {
        return departure_status;
    }

    public void setDepartureStatus(DepartureStatus departure_status) {
        this.departure_status = departure_status;
    }

    public boolean isTransit() {
        return "transit".equals(type);
    }

    public boolean isWalk() {
        return "walk".equals(type);
    }

    public Long getDepartureTime() {
        return departure_time;
    }

    public void setDepartureTime(Long departure_time) {
        this.departure_time = departure_time;
    }

    public Long getScheduledTime() {
        return scheduled_time;
    }

    public void setScheduledTime(Long scheduled_time) {
        this.scheduled_time = scheduled_time;
    }
}

