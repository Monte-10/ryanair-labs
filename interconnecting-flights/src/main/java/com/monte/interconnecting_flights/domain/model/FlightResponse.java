package com.monte.interconnecting_flights.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class FlightResponse {
    private int stops;
    private List<FlightLeg> legs;
}
