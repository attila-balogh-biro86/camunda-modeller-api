package com.ferrovial.tsm.dmn;

import com.ferrovial.tsm.dmn.execution.service.BusinessRuleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class CamundaDemoApplicationTests {

    @Autowired
    private BusinessRuleService businessRuleService;


    @Test
    public void contextLoads() throws FileNotFoundException {

        final Map<String, Object> VARS = new HashMap<>();
        VARS.put("mode", "Mandatory");               // or "Mandatory"
        VARS.put("currentRatePerMile", 1.10d);
        VARS.put("requestedIncreasePerMile", 9.0d);
        VARS.put("minutesSinceLastIncrease", 3);
        VARS.put("maxIncreasePerStep", 0.20d);    // X $ per step
        VARS.put("minIntervalMinutes", 5);        // Y minutes
        VARS.put("softCapPerMile", 1.40d);        // Normal only
        VARS.put("segmentCap", 24.0d);            // per-segment absolute cap
        VARS.put("segmentLengthMiles", 3.2d);     // segment miles

        Double newRatePerMile = businessRuleService.calculateNewRatePerMile(VARS);

        System.out.println(newRatePerMile);

        assertEquals(7.5d, newRatePerMile);
    }

}
