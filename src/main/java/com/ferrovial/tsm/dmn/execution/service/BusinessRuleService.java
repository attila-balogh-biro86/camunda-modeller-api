package com.ferrovial.tsm.dmn.execution.service;

import com.ferrovial.tsm.dmn.execution.model.Rate;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.dmn.engine.*;
import org.camunda.bpm.model.dmn.Dmn;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;

@Service
@Slf4j
public class BusinessRuleService {

    @Autowired
    private DmnEngine dmnEngine;
    private DmnModelInstance modelInstance;


    @PostConstruct
    public void init(){
        try {
            File modelFile = ResourceUtils.getFile(
                    "classpath:rules/toll-pricing-effective-dated.dmn");
            modelInstance = Dmn.readModelFromFile(modelFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    // ONLY FROM FILE - FOR TESTING PURPOSES
    public Double calculateNewRatePerMile(Map<String, Object> vars) throws FileNotFoundException {
        DmnDecision decision = dmnEngine.parseDecision("NewRatePerMile", modelInstance);
        DmnDecisionResult result = dmnEngine.evaluateDecision(decision, vars);
        Double newRatePerMile = ((Number) result.getSingleResult().getSingleEntry()).doubleValue();
        log.debug("NewRatePerMile: {}", newRatePerMile);
        return newRatePerMile;
    }

    // FROM INPUT STREAM - FROM CONTROLLER
    public GapCloseEvaluationResult evaluateGapClosingBusinessRule(InputStream modelInputStream, Map<String, Object> vars) throws Exception {
        DmnDecision decision = dmnEngine.parseDecision("decision_automatic_gap_closing", modelInputStream);
        DmnDecisionResult result = dmnEngine.evaluateDecision(decision, vars);

        String action = result.getSingleResult().getEntry("action");
        Long rate = result.getSingleResult().getEntry("rate");
        String description = result.getSingleResult().getEntry("description");

        log.debug("Gantry operation: {}", GantryOperation.valueOf(action));
        log.debug("New rate: {}", rate);
        log.debug("Description: {}", description);

        return GapCloseEvaluationResult.builder()
                .gantryOperation(GantryOperation.valueOf(action))
                .description(description)
                .newRate(rate != null ? new Rate(rate.doubleValue()) : new Rate(0.0))
                .build();
    }

}
