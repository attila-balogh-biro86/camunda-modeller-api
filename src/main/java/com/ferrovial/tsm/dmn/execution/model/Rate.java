package com.ferrovial.tsm.dmn.execution.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@AllArgsConstructor
@ToString
public class Rate {
    private Double rate;
}
