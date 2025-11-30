package com.ferrovial.tsm.dmn.execution.service;

import com.ferrovial.tsm.dmn.execution.model.Gantry;
import com.ferrovial.tsm.dmn.execution.model.GantryStatus;
import com.ferrovial.tsm.dmn.execution.model.Rate;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Getter
@Slf4j
@Component("gapService")
public class GapService {

    private Gantry gantry;

    @PostConstruct
    public void init() {
        log.info("GapService initialized");
        this.gantry = Gantry.builder()
                .status(GantryStatus.OPEN)
                .id("gantry-1")
                .rate(new Rate(5.0))
                .build();
    }

    public void resetGantry() {
        init();
    }

    public void execute(GantryOperation action, Rate rate) {

        if(gantry.getGap() == null) {
            log.warn("No gap present in gantry {}", gantry.getId());
            return;
        }
        switch (action) {
            case GantryOperation.OPEN_GANTRY:
                openGantry(gantry, rate);
                break;
            case GantryOperation.CLOSE_GANTRY:
                closeGantry(gantry);
                break;
            default:
                throw new IllegalArgumentException("Unknown action: " + action);
        }
    }

    private void openGantry(Gantry gantry, Rate newRate) {
        log.debug("Opening gantry {} with new rate {}", gantry, newRate);
        gantry.setRate(newRate);
        gantry.getGap().setEndTime(LocalDateTime.now());
        gantry.getGap().setActive(Boolean.FALSE);
        gantry.setStatus(GantryStatus.OPEN);
    }

    private void closeGantry(Gantry gantry) {
        log.debug("Closing gantry {}", gantry);
        gantry.setStatus(GantryStatus.CLOSED);
    }
}
