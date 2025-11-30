package com.ferrovial.tsm.dmn.execution.api;

import com.ferrovial.tsm.dmn.execution.model.Gantry;
import com.ferrovial.tsm.dmn.execution.service.BusinessRuleService;
import com.ferrovial.tsm.dmn.execution.service.GapCloseEvaluationResult;
import com.ferrovial.tsm.dmn.execution.service.GapService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
public class DmnController {

    private final BusinessRuleService businessRuleService;

    private final GapService gapService;

    @Autowired
    public DmnController(BusinessRuleService businessRuleService, GapService gapService) {
        this.businessRuleService = businessRuleService;
        this.gapService = gapService;
    }

    @PostMapping(value = "/evaluate-model", consumes = "application/xml")
    public GapCloseEvaluationResult handleXml(InputStream inputStream) throws Exception {
        Map<String, Object> vars = new HashMap<>();
        Gantry gantry = gapService.getGantry();
        vars.put("maxGapLength", 2); // in minutes
        vars.put("gantryStatus", gapService.getGantry().getStatus().name());
        vars.put("gapLength", gantry.getGap() != null ? gantry.getGap().getGapLength() : 0);
        GapCloseEvaluationResult evaluationResult = businessRuleService.evaluateGapClosingBusinessRule(inputStream,vars);
        gapService.execute(evaluationResult.getGantryOperation(), evaluationResult.getNewRate());
        return evaluationResult;
    }
}
