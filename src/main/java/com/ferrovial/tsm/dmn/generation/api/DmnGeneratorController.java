package com.ferrovial.tsm.dmn.generation.api;

import com.ferrovial.tsm.dmn.generation.dto.DmnGenerationRequest;
import com.ferrovial.tsm.dmn.generation.service.DmnGeneratorService;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.model.dmn.Dmn;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/api/v1.0/dmn")
public class DmnGeneratorController {

    @PostMapping(consumes = "application/json", produces = "application/xml")
    public ResponseEntity<String> generate(@RequestBody DmnGenerationRequest request) {

        log.debug("Generating DMN table based on the following request {}",request);

        try {
            String xml = DmnGeneratorService.generateDmn(request);
            DmnModelInstance modelInstance =
                    Dmn.readModelFromStream(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            Dmn.validateModel(modelInstance);
            log.debug("DMN is syntactically valid!");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=decision.dmn")
                    .body(xml);
        } catch (Exception e) {
                log.error("Error while generating DMN table from user request {}",e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}