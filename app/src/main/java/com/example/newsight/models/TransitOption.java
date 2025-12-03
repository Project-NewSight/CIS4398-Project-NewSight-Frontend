package com.example.newsight.models;

import java.util.List;

/**
 * TransitOption - Represents one possible transit route
 */
public class TransitOption {
    private Integer duration_min;
    private Long start_time;
    private Long end_time;
    private List<TransitLeg> legs;

    public Integer getDurationMin() {
        return duration_min;
    }

    public void setDurationMin(Integer duration_min) {
        this.duration_min = duration_min;
    }

    public Long getStartTime() {
        return start_time;
    }

    public void setStartTime(Long start_time) {
        this.start_time = start_time;
    }

    public Long getEndTime() {
        return end_time;
    }

    public void setEndTime(Long end_time) {
        this.end_time = end_time;
    }

    public List<TransitLeg> getLegs() {
        return legs;
    }

    public void setLegs(List<TransitLeg> legs) {
        this.legs = legs;
    }

    /**
     * Get a human-readable summary of this route
     */
    public String getSummary() {
        if (legs == null || legs.isEmpty()) {
            return "No route information";
        }

        StringBuilder summary = new StringBuilder();
        for (TransitLeg leg : legs) {
            if (leg.isTransit()) {
                if (summary.length() > 0) {
                    summary.append(" → ");
                }
                summary.append(leg.getRouteShortName());
                
                // Add status if available
                if (leg.getDepartureStatus() != null) {
                    String status = leg.getDepartureStatus().getStatus();
                    if ("delayed".equals(status)) {
                        summary.append(" (DELAYED)");
                    } else if ("cancelled".equals(status)) {
                        summary.append(" (CANCELLED)");
                    }
                }
            }
        }

        if (duration_min != null) {
            summary.append(String.format(" • %d min", duration_min));
        }

        return summary.toString();
    }
}

