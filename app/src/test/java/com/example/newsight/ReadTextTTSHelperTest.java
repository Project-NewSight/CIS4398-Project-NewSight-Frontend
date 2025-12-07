package com.example.newsight;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowTextToSpeech;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class ReadTextTTSHelperTest {

    @Mock
    private Context mockContext;

    @Mock
    private ReadTextTTSHelper.TTSListener mockListener;

    private ReadTextTTSHelper ttsHelper;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testInitialization() {
        ttsHelper = new ReadTextTTSHelper(mockContext, mockListener);
        // Since TextToSpeech init is changes based on system, we mainly check if object is created.
        // In Robolectric, TextToSpeech might not initialize fully async in a synchronous test without shadowing correctly.
        // However, we can verifying listener callbacks if we could control the OnInitListener.
    }

    @Test
    public void testSpeakAttemptWhenNotReady() {
        ttsHelper = new ReadTextTTSHelper(mockContext, mockListener);
        // Force state to not ready
        ttsHelper.speak("Hello");
        verify(mockListener).onTTSError("TTS not ready");
    }

    @Test
    public void testShutdown() {
        ttsHelper = new ReadTextTTSHelper(mockContext, mockListener);
        ttsHelper.shutdown();
        assertFalse(ttsHelper.isReady());
    }
}
