package com.ferrovial.tsm.dmn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CamundaDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(CamundaDemoApplication.class, args);
    }

}
