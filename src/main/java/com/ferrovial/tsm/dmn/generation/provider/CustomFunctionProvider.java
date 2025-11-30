package com.ferrovial.tsm.dmn.generation.provider;

import com.ferrovial.tsm.dmn.execution.model.GantryStatus;
import com.ferrovial.tsm.dmn.execution.service.GantryOperation;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.dmn.feel.impl.scala.function.CustomFunction;
import org.camunda.bpm.dmn.feel.impl.scala.function.FeelCustomFunctionProvider;

import java.util.*;

@Slf4j
public class CustomFunctionProvider implements FeelCustomFunctionProvider {

    protected Map<String, CustomFunction> functions = new HashMap<>();

    public CustomFunctionProvider() {
        CustomFunction updateGantryStatus = CustomFunction.create()
                .setParams("gantryOperation")
                .setFunction(args -> {
                    GantryOperation gantryOperation = GantryOperation.valueOf(args.get(0).toString());
                    log.debug("Gantry operation to execute: {} ",gantryOperation);

                    if(gantryOperation == GantryOperation.OPEN_GANTRY){
                        log.debug("New Gantry status is: {} ",GantryStatus.OPEN);
                        return GantryStatus.OPEN.name();
                    }
                    else {
                        log.debug("New Gantry status is: {} ",GantryStatus.CLOSED);
                        return GantryStatus.CLOSED.name();
                    }
                })
                .build();

        functions.put("executeGantryOperation", updateGantryStatus);

        functions.put(
                "logDebug",
                CustomFunction.create()
                        .setParams("msg")
                        .setFunction(args -> {
                            String msg = (String) args.get(0);
                            log.debug("[DMN] " + msg);
                            return msg;
                        })
                        .build()
        );

        functions.put(
                "logInfo",
                CustomFunction.create()
                        .setParams("msg")
                        .setFunction(args -> {
                            String msg = (String) args.get(0);
                            log.info("[DMN] " + msg);
                            return msg;
                        })
                        .build()
        );
    }

    @Override
    public Optional<CustomFunction> resolveFunction(String functionName) {
        return Optional.ofNullable(functions.get(functionName));
    }

    @Override
    public Collection<String> getFunctionNames() {
        return functions.keySet();
    }
}