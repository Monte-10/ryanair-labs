package com.monte.interconnecting_flights;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "Interconnecting Flights API", version = "1.0.0"))
public class InterconnectingFlightsApplication {
    public static void main(String[] args) {
        SpringApplication.run(InterconnectingFlightsApplication.class, args);
    }
}
