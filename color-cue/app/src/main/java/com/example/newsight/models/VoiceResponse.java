package com.example.newsight.models;

import com.google.gson.annotations.SerializedName;

/**
 * VoiceResponse - Response from /voice/transcribe endpoint
 * Contains feature identification and extracted params (including navigation directions)
 */
public class VoiceResponse {
    private double confidence;
    private ExtractedParams extracted_params;
    private TtsOutput TTS_Output;

    public static class ExtractedParams {
        private String feature;
        private String query;
        private String destination;
        private DirectionsResponse directions;
        private String navigation_type;  // "walking" or "transit"
        private TransitInfo transit_info;
        private TransitStop nearest_stop;

        public String getFeature() {
            return feature;
        }

        public void setFeature(String feature) {
            this.feature = feature;
        }

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public String getDestination() {
            return destination;
        }

        public void setDestination(String destination) {
            this.destination = destination;
        }

        public DirectionsResponse getDirections() {
            return directions;
        }

        public void setDirections(DirectionsResponse directions) {
            this.directions = directions;
        }

        public String getNavigationType() {
            return navigation_type;
        }

        public void setNavigationType(String navigation_type) {
            this.navigation_type = navigation_type;
        }

        public TransitInfo getTransitInfo() {
            return transit_info;
        }

        public void setTransitInfo(TransitInfo transit_info) {
            this.transit_info = transit_info;
        }

        public TransitStop getNearestStop() {
            return nearest_stop;
        }

        public void setNearestStop(TransitStop nearest_stop) {
            this.nearest_stop = nearest_stop;
        }

        public boolean isTransitNavigation() {
            return "transit".equals(navigation_type);
        }
    }

    public static class TtsOutput {
        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public ExtractedParams getExtractedParams() {
        return extracted_params;
    }

    public void setExtractedParams(ExtractedParams extracted_params) {
        this.extracted_params = extracted_params;
    }

    public TtsOutput getTtsOutput() {
        return TTS_Output;
    }

    public void setTtsOutput(TtsOutput TTS_Output) {
        this.TTS_Output = TTS_Output;
    }

    @Override
    public String toString() {
        return String.format("VoiceResponse[feature=%s, confidence=%.2f]",
                extracted_params != null ? extracted_params.getFeature() : "null",
                confidence);
    }
}

