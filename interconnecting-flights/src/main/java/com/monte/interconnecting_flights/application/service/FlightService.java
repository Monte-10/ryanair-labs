package com.monte.interconnecting_flights.application.service;

import com.monte.interconnecting_flights.domain.model.FlightLeg;
import com.monte.interconnecting_flights.domain.model.FlightResponse;
import com.monte.interconnecting_flights.domain.port.outbound.RoutesPort;
import com.monte.interconnecting_flights.domain.port.outbound.SchedulesPort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FlightService {

    private final RoutesPort routesPort;
    private final SchedulesPort schedulesPort;

    public FlightService(RoutesPort routesPort, SchedulesPort schedulesPort) {
        this.routesPort = routesPort;
        this.schedulesPort = schedulesPort;
    }

    public List<FlightResponse> findFlights(
            String departure,
            String arrival,
            LocalDateTime departureDateTime,
            LocalDateTime arrivalDateTime
    ) {
        // Validation
        if (departure == null || departure.isBlank()) {
            throw new IllegalArgumentException("The 'departure' parameter cannot be null or empty");
        }
        if (arrival == null || arrival.isBlank()) {
            throw new IllegalArgumentException("The 'arrival' parameter cannot be null or empty");
        }
        if (departureDateTime == null || arrivalDateTime == null) {
            throw new IllegalArgumentException("Departure/arrival dates cannot be null");
        }
        if (!departureDateTime.isBefore(arrivalDateTime)) {
            throw new IllegalArgumentException("departureDateTime must be earlier than arrivalDateTime");
        }
        if (departure.equals(arrival)) {
            throw new IllegalArgumentException("departure and arrival cannot be the same");
        }

        // Calling the Routes API via the abstraction
        List<Map<String, String>> routes;
        try {
            routes = routesPort.getRoutes();
        } catch (Exception ex) {
            throw ex;
        }

        // Filter routes that have no connecting airport
        List<Map<String, String>> filteredRoutes = routes.stream()
            .filter(route -> "RYANAIR".equals(route.get("operator")) &&
                             route.get("connectingAirport") == null)
            .collect(Collectors.toList());

        // Flight search
        Set<FlightResponse> allFlights = new HashSet<>();

        // Direct flights
        boolean hasDirect = filteredRoutes.stream()
            .anyMatch(route ->
                departure.equals(route.get("airportFrom")) &&
                arrival.equals(route.get("airportTo"))
            );

        if (hasDirect) {
            List<FlightResponse> directFlights = findDirectFlights(departure, arrival,
                    departureDateTime, arrivalDateTime);
            allFlights.addAll(directFlights);
        }

        // Flights with a stopover
        List<Map<String, String>> firstLegRoutes = filteredRoutes.stream()
            .filter(route ->
                departure.equals(route.get("airportFrom")) &&
                !arrival.equals(route.get("airportTo"))
            )
            .collect(Collectors.toList());

        for (Map<String, String> firstLegRoute : firstLegRoutes) {
            String stopover = firstLegRoute.get("airportTo");

            boolean hasSecondLeg = filteredRoutes.stream()
                .anyMatch(route ->
                    stopover.equals(route.get("airportFrom")) &&
                    arrival.equals(route.get("airportTo"))
                );

            if (hasSecondLeg) {
                List<FlightResponse> connecting = findConnectingFlights(
                        departure, stopover, arrival,
                        departureDateTime, arrivalDateTime
                );

                if (!connecting.isEmpty()) {
                    System.out.println("Stopover route detected: " +
                            departure + " -> " + stopover + " -> " + arrival);
                    allFlights.addAll(connecting);
                }
            }
        }

        return new ArrayList<>(allFlights);
    }

    private List<FlightResponse> findDirectFlights(
            String departure,
            String arrival,
            LocalDateTime departureDateTime,
            LocalDateTime arrivalDateTime
    ) {
        // Call the Schedules API via the abstraction
        Map<String, Object> schedule;
        try {
            schedule = schedulesPort.getSchedule(
                departure, arrival,
                departureDateTime.getYear(),
                departureDateTime.getMonthValue()
            );
        } catch (Exception ex) {
            throw ex;
        }

        List<FlightResponse> flights = new ArrayList<>();

        // If there are no "days" in the schedule, return an empty list
        if (!schedule.containsKey("days")) {
            return flights;
        }

        List<Map<String, Object>> days = (List<Map<String, Object>>) schedule.get("days");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        for (Map<String, Object> dayEntry : days) {
            List<Map<String, Object>> flightsInfo =
                    (List<Map<String, Object>>) dayEntry.get("flights");
            if (flightsInfo == null) continue;

            int dayNumber = (int) dayEntry.get("day");

            for (Map<String, Object> flight : flightsInfo) {
                String depTimeStr = (String) flight.get("departureTime");
                String arrTimeStr = (String) flight.get("arrivalTime");

                if (depTimeStr != null && arrTimeStr != null) {
                    try {
                        LocalTime depTime = LocalTime.parse(depTimeStr, timeFormatter);
                        LocalTime arrTime = LocalTime.parse(arrTimeStr, timeFormatter);

                        LocalDateTime flightDeparture = LocalDateTime.of(
                                departureDateTime.getYear(),
                                departureDateTime.getMonthValue(),
                                dayNumber,
                                depTime.getHour(),
                                depTime.getMinute()
                        );

                        // If arrivalTime is before departureTime, it means the flight crosses midnight
                        int arrivalDayNumber = dayNumber;
                        if (arrTime.isBefore(depTime)) {
                            arrivalDayNumber++;
                        }

                        LocalDateTime flightArrival = LocalDateTime.of(
                                departureDateTime.getYear(),
                                departureDateTime.getMonthValue(),
                                arrivalDayNumber,
                                arrTime.getHour(),
                                arrTime.getMinute()
                        );

                        if (!flightDeparture.isBefore(departureDateTime) &&
                            !flightArrival.isAfter(arrivalDateTime)) {

                            FlightLeg leg = new FlightLeg(departure, arrival,
                                    flightDeparture, flightArrival);
                            flights.add(new FlightResponse(0, List.of(leg)));
                        }

                    } catch (java.time.DateTimeException e) {
                        // Example: Invalid date 'JUNE 31'
                        System.out.println("Invalid date detected: " +
                                dayNumber + " " + departureDateTime.getMonth() +
                                " => " + e.getMessage());
                        continue; // Discard this flight
                    }
                }
            }
        }
        return flights;
    }

    private List<FlightResponse> findConnectingFlights(
            String departure,
            String stopover,
            String arrival,
            LocalDateTime departureDateTime,
            LocalDateTime arrivalDateTime
    ) {
        // First leg
        List<FlightResponse> firstLegFlights = findDirectFlights(
                departure, stopover, departureDateTime, arrivalDateTime
        );
        // Second leg
        List<FlightResponse> secondLegFlights = findDirectFlights(
                stopover, arrival, departureDateTime, arrivalDateTime
        );

        List<FlightResponse> connectedFlights = new ArrayList<>();

        for (FlightResponse firstLeg : firstLegFlights) {
            FlightLeg leg1 = firstLeg.getLegs().get(0);

            for (FlightResponse secondLeg : secondLegFlights) {
                FlightLeg leg2 = secondLeg.getLegs().get(0);

                // Minimum of 2 hours between the arrival of the first leg and the departure of the second leg
                if (leg1.getArrivalDateTime().plusHours(2).isBefore(leg2.getDepartureDateTime())) {
                    FlightResponse connecting = new FlightResponse(1, List.of(leg1, leg2));
                    connectedFlights.add(connecting);
                }
            }
        }
        return connectedFlights;
    }
}
