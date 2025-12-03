package com.example.newsight;

import java.util.List;

public class CloudDetectionModels {

    public static class BBox {
        public float x_min;
        public float y_min;
        public float x_max;
        public float y_max;
    }

    public static class BackendDetection {
        public String cls;
        public float confidence;
        public BBox bbox;
        public Float distance_m;
        public String direction;
    }

    public static class Summary {
        public boolean high_priority_warning;
        public String message;
        public String device_id;
    }

    public static class DetectResponse {
        public Integer frame_id;
        public List<BackendDetection> detections;
        public Summary summary;
    }
}
