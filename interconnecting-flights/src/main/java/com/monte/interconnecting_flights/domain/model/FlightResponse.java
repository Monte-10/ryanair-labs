package com.monte.interconnecting_flights.domain.model;

import java.util.List;

public class FlightResponse {
    private int stops;
    private List<FlightLeg> legs;

    public FlightResponse(int stops, List<FlightLeg> legs) {
        this.stops = stops;
        this.legs = legs;
    }

    public int getStops() {
        return stops;
    }

    public List<FlightLeg> getLegs() {
        return legs;
    }
}
