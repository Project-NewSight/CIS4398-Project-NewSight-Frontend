package com.example.newsight;

import android.content.Intent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk = 33)
public class ObjectDetectionTest {

    @Test
    public void testCloudDetectionModels() {
        CloudDetectionModels.DetectResponse response = new CloudDetectionModels.DetectResponse();
        response.frame_id = 123;
        
        CloudDetectionModels.Summary summary = new CloudDetectionModels.Summary();
        summary.message = "Test Warning";
        summary.high_priority_warning = true;
        
        response.summary = summary;
        
        assertEquals(Integer.valueOf(123), response.frame_id);
        assertEquals("Test Warning", response.summary.message);
        
        CloudDetectionModels.BackendDetection detection = new CloudDetectionModels.BackendDetection();
        detection.cls = "person";
        detection.confidence = 0.95f;
        
        assertEquals("person", detection.cls);
        assertEquals(0.95f, detection.confidence, 0.001);
    }

    @Test
    public void testObstacleActivityCreation() {
        // Just verify it starts up without crashing on Robolectric
        // This implicitly tests simple lifecycle
        Intent intent = new Intent(Intent.ACTION_MAIN);
        ObstacleActivity activity = Robolectric.buildActivity(ObstacleActivity.class, intent)
                .create().resume().get();
                
        assertNotNull(activity);
    }
}
