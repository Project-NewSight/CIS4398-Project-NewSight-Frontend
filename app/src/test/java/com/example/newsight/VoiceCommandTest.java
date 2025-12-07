package com.example.newsight;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class VoiceCommandTest {

    @Mock
    private VoiceCommandHelper.VoiceCommandCallback mockCallback;

    private VoiceCommandHelper voiceCommandHelper;
    private Context context;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        context = RuntimeEnvironment.getApplication();
        voiceCommandHelper = new VoiceCommandHelper(context);
        voiceCommandHelper.setCallback(mockCallback);
    }

    @Test
    public void testInitializationAndCleanup() {
        // Just verify it doesn't crash on init
        voiceCommandHelper.setSessionId("test-session");
        
        // Cleanup should stop everything
        voiceCommandHelper.cleanup();
        
        // Since we can't easily verify internal private state without reflection,
        // and most methods run on background threads, we rely on the fact that
        // no exception was thrown during these calls in a Robolectric environment.
    }

    @Test
    public void testStopListening() {
        voiceCommandHelper.stopListening();
        // Verify no crash
    }
}
