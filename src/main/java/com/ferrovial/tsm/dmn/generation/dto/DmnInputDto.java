package com.ferrovial.tsm.dmn.generation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class DmnInputDto {
    private String label;
    private String expression;
    private String type;
}