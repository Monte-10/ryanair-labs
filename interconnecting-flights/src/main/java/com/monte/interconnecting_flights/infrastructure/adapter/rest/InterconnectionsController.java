package com.monte.interconnecting_flights.infrastructure.adapter.rest;

import com.monte.interconnecting_flights.application.service.FlightService;
import com.monte.interconnecting_flights.domain.model.FlightResponse;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/interconnections")
public class InterconnectionsController {

    private final FlightService flightService;

    public InterconnectionsController(FlightService flightService) {
        this.flightService = flightService;
    }

    @GetMapping
    public List<FlightResponse> getInterconnections(
            @RequestParam String departure,
            @RequestParam String arrival,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime departureDateTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime arrivalDateTime
    ) {
        return flightService.findFlights(departure, arrival, departureDateTime, arrivalDateTime);
    }
}
