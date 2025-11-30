package com.ferrovial.tsm.dmn.generation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class DmnRuleDto {
    List<String> inputEntries;
    List<String> outputEntries;
}