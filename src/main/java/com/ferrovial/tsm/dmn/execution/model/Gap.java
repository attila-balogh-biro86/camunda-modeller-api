package com.ferrovial.tsm.dmn.execution.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Slf4j
@Getter
@Setter
@Builder
@ToString
public class Gap {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private GapReason reason;
    private Boolean active;

    public Integer getGapLength() {
        return (int) java.time.Duration.between(startTime, LocalDateTime.now()).toMinutes();
    }
}
