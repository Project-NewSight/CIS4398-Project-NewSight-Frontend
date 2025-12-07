package com.example.newsight;

import com.example.newsight.models.TransitInfo;
import com.example.newsight.models.TransitLeg;
import com.example.newsight.models.TransitOption;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TransitTest {

    @Test
    public void testTransitLegLogic() {
        TransitLeg leg = new TransitLeg();
        
        leg.setType("transit");
        assertTrue(leg.isTransit());
        assertFalse(leg.isWalk());
        
        leg.setType("walk");
        assertFalse(leg.isTransit());
        assertTrue(leg.isWalk());
    }

    @Test
    public void testTransitInfoLogic() {
        TransitInfo info = new TransitInfo();
        
        // Initially no option
        assertFalse(info.hasBestOption());
        
        // Set option
        TransitOption option = new TransitOption();
        info.setBestOption(option);
        assertTrue(info.hasBestOption());
        
        // Alerts
        assertFalse(info.hasAlerts());
        info.setAlerts(Collections.singletonList(new TransitInfo.TransitAlert()));
        assertTrue(info.hasAlerts());
    }
}
