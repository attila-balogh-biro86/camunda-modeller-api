package com.ferrovial.tsm.dmn.common;

import com.ferrovial.tsm.dmn.generation.provider.CustomFunctionProvider;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.dmn.engine.DmnEngineConfiguration;
import org.camunda.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class CamundaConfig {

    @Bean
    public DmnEngine getDmnEngine(){
        DefaultDmnEngineConfiguration cfg =
                (DefaultDmnEngineConfiguration) DmnEngineConfiguration.createDefaultDmnEngineConfiguration();
        cfg.setFeelCustomFunctionProviders(List.of(new CustomFunctionProvider()));
        return cfg.buildEngine();
    }
}
