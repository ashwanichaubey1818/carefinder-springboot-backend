package com.carefinder.backend;

import com.carefinder.backend.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class CareFinderBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CareFinderBackendApplication.class, args);
    }
}
