package com.sngmin.cropyieldapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CropYieldApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CropYieldApiApplication.class, args);
    }

}
