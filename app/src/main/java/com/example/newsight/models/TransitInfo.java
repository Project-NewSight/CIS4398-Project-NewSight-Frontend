package com.example.newsight.models;

import java.util.List;
import java.util.Map;

/**
 * TransitInfo - Contains the best transit route and alerts
 */
public class TransitInfo {
    private TransitOption best_option;  // Single best route
    private List<TransitAlert> alerts;
    private Map<String, Object> destination;

    public static class TransitAlert {
        private String type;  // "DELAY", "CANCELLED", etc.
        private String route;
        private String message;
        private Integer delay_minutes;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getRoute() {
            return route;
        }

        public void setRoute(String route) {
            this.route = route;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Integer getDelayMinutes() {
            return delay_minutes;
        }

        public void setDelayMinutes(Integer delay_minutes) {
            this.delay_minutes = delay_minutes;
        }
    }

    public TransitOption getBestOption() {
        return best_option;
    }

    public void setBestOption(TransitOption best_option) {
        this.best_option = best_option;
    }

    public List<TransitAlert> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<TransitAlert> alerts) {
        this.alerts = alerts;
    }

    public Map<String, Object> getDestination() {
        return destination;
    }

    public void setDestination(Map<String, Object> destination) {
        this.destination = destination;
    }

    public boolean hasBestOption() {
        return best_option != null;
    }

    public boolean hasAlerts() {
        return alerts != null && !alerts.isEmpty();
    }
}

