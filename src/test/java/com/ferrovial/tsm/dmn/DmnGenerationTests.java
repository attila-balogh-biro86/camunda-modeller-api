package com.ferrovial.tsm.dmn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ferrovial.tsm.dmn.execution.model.GantryStatus;
import com.ferrovial.tsm.dmn.generation.dto.DmnGenerationRequest;
import com.ferrovial.tsm.dmn.generation.service.DmnGeneratorService;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.dmn.engine.DmnDecision;
import org.camunda.bpm.dmn.engine.DmnDecisionResult;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Slf4j
@SpringBootTest
public class DmnGenerationTests {


    @Autowired
    private DmnEngine dmnEngine;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testAutomaticClosingGapRuleGenerationWorksOk() throws IOException {

        InputStream is = getClass().getResourceAsStream("/test_gap_auto_closing.json");
        assertNotNull(is);
        String generatedDmn = generateAndValidateDmnRequest(is);

        Map<String, Object> vars = new HashMap<>();
        vars.put("maxGapLength", 2); // in minutes
        vars.put("gantryStatus", GantryStatus.OPEN.name());
        vars.put("gapLength", 0);

        DmnDecision decision = dmnEngine.parseDecision("automatic_gap_closing", new ByteArrayInputStream(generatedDmn.getBytes(StandardCharsets.UTF_8)));
        DmnDecisionResult result = dmnEngine.evaluateDecision(decision, vars);

        String gantryStatus = result.getSingleResult().getEntry("action");
        Long rate = result.getSingleResult().getEntry("rate");
        String description = result.getSingleResult().getEntry("description");

        log.debug("Gantry operation: {}", GantryStatus.valueOf(gantryStatus));
        log.debug("New rate: {}", rate);
        log.debug("Description: {}", description);

        Assertions.assertEquals("OPEN", gantryStatus);
        assertNull(rate);
        Assertions.assertEquals("Gantry is open, no auto closing",description);
    }

    @Test
    public void testDriverEligibilityWorksOk() throws IOException {

        InputStream is = getClass().getResourceAsStream("/test_driver_eligibility.json");
        assertNotNull(is);
        String generatedDmn = generateAndValidateDmnRequest(is);

        Map<String, Object> vars = new HashMap<>();
        vars.put("age", 61); // in minutes
        vars.put("hasLicense", true);
        vars.put("country", "ES");

        DmnDecision decision = dmnEngine.parseDecision("driver_eligibility", new ByteArrayInputStream(generatedDmn.getBytes(StandardCharsets.UTF_8)));
        DmnDecisionResult result = dmnEngine.evaluateDecision(decision, vars);

        Boolean eligible = result.getSingleResult().getEntry("eligible");
        String explanation = result.getSingleResult().getEntry("explanation");

        log.debug("Does the driver eligible and explanation: {} {}", eligible, explanation);

        Assertions.assertNotNull(eligible);
        Assertions.assertFalse(eligible);
        Assertions.assertNotNull(explanation);
        Assertions.assertEquals("Driver is too old to drive regardless its country!",explanation);
    }


    private String generateAndValidateDmnRequest(InputStream json) throws IOException {
        String jsonRequest = new String(json.readAllBytes(), StandardCharsets.UTF_8);
        DmnGenerationRequest request = objectMapper.readValue(jsonRequest, DmnGenerationRequest.class);
        String generatedDmn = DmnGeneratorService.generateDmn(request);
        assertNotNull(generatedDmn);
        return generatedDmn;
    }
}
