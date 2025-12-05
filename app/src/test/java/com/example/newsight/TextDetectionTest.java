package com.example.newsight;

import android.graphics.Bitmap;

import androidx.camera.core.ImageInfo;
import androidx.camera.core.ImageProxy;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class TextDetectionTest {

    @Mock
    private WebSocketManager mockWsManager;
    
    @Mock
    private ReadTextFrameAnalyzer.FeatureProvider mockFeatureProvider;
    
    @Mock
    private ImageProxy mockImageProxy;
    
    @Mock
    private ImageInfo mockImageInfo;
    
    @Mock
    private Bitmap mockBitmap;

    private ReadTextFrameAnalyzer analyzer;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        analyzer = new ReadTextFrameAnalyzer(mockWsManager, mockFeatureProvider);
        
        // Setup default mock behaviors
        when(mockImageProxy.getImageInfo()).thenReturn(mockImageInfo);
        when(mockImageProxy.toBitmap()).thenReturn(mockBitmap);
        when(mockBitmap.getWidth()).thenReturn(100);
        when(mockBitmap.getHeight()).thenReturn(100);
        when(mockImageInfo.getRotationDegrees()).thenReturn(0);
        
        // Ensure compress doesn't crash mock
        when(mockBitmap.compress(any(Bitmap.CompressFormat.class), any(Integer.class), any())).thenReturn(true);
    }

    @Test
    public void testAnalyze_NoConnection_ShouldNotSend() {
        when(mockWsManager.isConnected()).thenReturn(false);
        when(mockFeatureProvider.getActiveFeature()).thenReturn("text_detection");
        
        analyzer.analyze(mockImageProxy);
        
        verify(mockWsManager, never()).sendFrame(any(), anyString());
        verify(mockImageProxy).close();
    }

    @Test
    public void testAnalyze_NoFeature_ShouldNotSend() {
        when(mockWsManager.isConnected()).thenReturn(true);
        when(mockFeatureProvider.getActiveFeature()).thenReturn(null);
        
        analyzer.analyze(mockImageProxy);
        
        verify(mockWsManager, never()).sendFrame(any(), anyString());
       verify(mockImageProxy).close();
    }

    @Test
    public void testAnalyze_Success_ShouldSend() {
        when(mockWsManager.isConnected()).thenReturn(true);
        when(mockFeatureProvider.getActiveFeature()).thenReturn("text_detection");
        
        analyzer.analyze(mockImageProxy);
        
        verify(mockWsManager).sendFrame(any(), anyString());
        verify(mockImageProxy).close();
    }
}
