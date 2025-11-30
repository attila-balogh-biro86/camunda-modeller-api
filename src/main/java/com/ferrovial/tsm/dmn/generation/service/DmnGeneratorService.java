package com.ferrovial.tsm.dmn.generation.service;

import com.ferrovial.tsm.dmn.generation.dto.DmnGenerationRequest;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.model.dmn.Dmn;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.camunda.bpm.model.dmn.HitPolicy;
import org.camunda.bpm.model.dmn.instance.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
public class DmnGeneratorService {

        public static String generateDmn(DmnGenerationRequest req) {

            DmnModelInstance modelInstance = Dmn.createEmptyModel();

            // Definitions root
            Definitions definitions = modelInstance.newInstance(Definitions.class);
            definitions.setNamespace("https://www.camunda.org/schema/1.0/dmn");
            definitions.setId("definitions");
            definitions.setName("definitions");
            modelInstance.setDocumentElement(definitions);

            // Decision
            Decision decision = modelInstance.newInstance(Decision.class);
            decision.setId(req.getDecisionName());
            decision.setName(req.getDecisionName());
            definitions.addChildElement(decision);

            // Decision Table
            DecisionTable table = modelInstance.newInstance(DecisionTable.class);
            table.setHitPolicy(HitPolicy.FIRST);
            table.setId(req.getDecisionName() + "Table");
            decision.addChildElement(table);

            // Inputs
            for (int i = 0; i < req.getInputs().size(); i++) {
                var dto = req.getInputs().get(i);
                Input input = modelInstance.newInstance(Input.class);
                input.setId("input_" + i);
                input.setLabel(dto.getLabel());

                InputExpression inputExpression = modelInstance.newInstance(InputExpression.class);
                inputExpression.setId("inputExpr_" + i);

                Text tex = modelInstance.newInstance(Text.class);
                tex.setTextContent(dto.getExpression());
                inputExpression.addChildElement(tex);
                inputExpression.setTypeRef(dto.getType());
                input.addChildElement(inputExpression);

                table.addChildElement(input);
            }

            // Outputs
            for (int i = 0; i < req.getOutputs().size(); i++) {
                var dto = req.getOutputs().get(i);
                Output output = modelInstance.newInstance(Output.class);
                output.setId("output_" + i);
                output.setLabel(dto.getLabel());
                output.setName(dto.getName());
                output.setTypeRef(dto.getType());
                table.addChildElement(output);
            }

            // Rules
            for (int r = 0; r < req.getRules().size(); r++) {
                var dto = req.getRules().get(r);

                Rule rule = modelInstance.newInstance(Rule.class);
                rule.setId("rule_" + r);

                // Input entries
                for (int i = 0; i < dto.getInputEntries().size(); i++) {
                    InputEntry entry = modelInstance.newInstance(InputEntry.class);
                    entry.setId("inputEntry_" + r + "_" + i);
                    Text text = modelInstance.newInstance(Text.class);
                    text.setTextContent((dto.getInputEntries().get(i)));
                    entry.addChildElement(text);
                    rule.addChildElement(entry);
                }

                // Output entries
                for (int i = 0; i < dto.getOutputEntries().size(); i++) {
                    OutputEntry entry = modelInstance.newInstance(OutputEntry.class);
                    entry.setId("outputEntry_" + r + "_" + i);
                    Text text = modelInstance.newInstance(Text.class);
                    text.setTextContent((dto.getOutputEntries().get(i)));
                    entry.addChildElement(text);
                    rule.addChildElement(entry);
                }

                table.addChildElement(rule);
            }

            // Convert to XML
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            Dmn.writeModelToStream(stream, modelInstance);
            return stream.toString(StandardCharsets.UTF_8);
        }
}