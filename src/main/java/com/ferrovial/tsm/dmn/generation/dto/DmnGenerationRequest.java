package com.ferrovial.tsm.dmn.generation.dto;

import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class DmnGenerationRequest {
    private String decisionName;
    private List<DmnInputDto> inputs;
    private List<DmnOutputDto> outputs;
    private List<DmnRuleDto> rules;
}
