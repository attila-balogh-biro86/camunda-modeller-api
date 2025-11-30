package com.ferrovial.tsm.dmn.execution.service;


import com.ferrovial.tsm.dmn.execution.model.Rate;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@ToString
@Getter
@Setter
public class GapCloseEvaluationResult {

    private GantryOperation gantryOperation;
    private Rate newRate;
    private String description;
}
