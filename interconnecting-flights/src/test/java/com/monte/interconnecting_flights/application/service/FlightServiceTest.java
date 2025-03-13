package com.monte.interconnecting_flights.application.service;

import com.monte.interconnecting_flights.domain.model.FlightResponse;
import com.monte.interconnecting_flights.infrastructure.adapter.client.ExternalApiException;
import com.monte.interconnecting_flights.infrastructure.adapter.client.RoutesClient;
import com.monte.interconnecting_flights.infrastructure.adapter.client.SchedulesClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

/**
 * Unit test for FlightService.
 */
class FlightServiceTest {

    private FlightService flightService;
    private RoutesClient routesClientMock;
    private SchedulesClient schedulesClientMock;

    @BeforeEach
    void setUp() {
        routesClientMock = Mockito.mock(RoutesClient.class);
        schedulesClientMock = Mockito.mock(SchedulesClient.class);

        flightService = new FlightService(routesClientMock, schedulesClientMock);
    }

    @Test
    void testFindFlights_DirectFlightFound() {
        // GIVEN
        // 1) Preparamos data simulada para routesClientMock

        Map<String, String> routeMap = new HashMap<>();
        routeMap.put("airportFrom", "DUB");
        routeMap.put("airportTo", "WRO");
        routeMap.put("connectingAirport", null);
        routeMap.put("operator", "RYANAIR");

        List<Map<String, String>> routesData = List.of(routeMap);
        given(routesClientMock.getRoutes()).willReturn(routesData);

        // 2) Preparamos data simulada para SchedulesClient
        Map<String, Object> scheduleMock = Map.of(
            "days", List.of(
                Map.of(
                    "day", 10,
                    "flights", List.of(
                        Map.of("departureTime", "09:30", "arrivalTime", "12:55")
                    )
                )
            )
        );
        given(schedulesClientMock.getSchedule("DUB", "WRO", 2025, 3))
                .willReturn(scheduleMock);

        // WHEN
        LocalDateTime departureDateTime = LocalDateTime.of(2025, 3, 10, 7, 0);
        LocalDateTime arrivalDateTime = LocalDateTime.of(2025, 3, 10, 21, 0);

        List<FlightResponse> result = flightService.findFlights("DUB", "WRO", departureDateTime, arrivalDateTime);

        // THEN
        assertEquals(1, result.size());
        FlightResponse flight = result.get(0);

        assertEquals(0, flight.getStops());
        assertEquals("DUB", flight.getLegs().get(0).getDepartureAirport());
        assertEquals("WRO", flight.getLegs().get(0).getArrivalAirport());
    }

    @Test
    void testFindFlights_NoRoutesFound() {
        // GIVEN
        // Retornamos lista vacía de rutas
        given(routesClientMock.getRoutes()).willReturn(Collections.emptyList());

        // WHEN
        LocalDateTime depDate = LocalDateTime.of(2025, 3, 10, 7, 0);
        LocalDateTime arrDate = LocalDateTime.of(2025, 3, 10, 21, 0);

        List<FlightResponse> result = flightService.findFlights("DUB", "WRO", depDate, arrDate);

        // THEN
        // Sin rutas => esperamos una lista vacía
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindFlights_InvalidDates_ThrowsException() {
        // WHEN
        LocalDateTime invalidDep = LocalDateTime.of(2025, 3, 10, 10, 0);
        LocalDateTime invalidArr = LocalDateTime.of(2025, 3, 10, 9, 0);

        // THEN
        // Esperamos que lance IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            flightService.findFlights("DUB", "WRO", invalidDep, invalidArr);
        });
    }

    @Test
    void testFindFlights_SameAirport_ThrowsException() {
        LocalDateTime dep = LocalDateTime.of(2025, 3, 10, 7, 0);
        LocalDateTime arr = LocalDateTime.of(2025, 3, 10, 21, 0);

        // departure == arrival => error
        assertThrows(IllegalArgumentException.class, () -> {
            flightService.findFlights("DUB", "DUB", dep, arr);
        });
    }

    @Test
    void testFindFlights_ExternalApiError() {
        // GIVEN
        // Simulamos un fallo en la API
        given(routesClientMock.getRoutes()).willThrow(
            new ExternalApiException("Error al consultar la Routes API: 404 NOT_FOUND", null)
        );

        // WHEN
        LocalDateTime dep = LocalDateTime.of(2025, 3, 10, 7, 0);
        LocalDateTime arr = LocalDateTime.of(2025, 3, 10, 21, 0);

        // THEN
        // Esperamos ExternalApiException
        assertThrows(ExternalApiException.class, () -> {
            flightService.findFlights("DUB", "WRO", dep, arr);
        });
    }

    @Test
    void testFindFlights_TwoLegsWithMinimum2Hours() {
        // GIVEN
        // Rutas: 
        //   DUB -> STN (operator=RYANAIR, connectingAirport=null)
        //   STN -> WRO
        List<Map<String, String>> routesData = List.of(
            new HashMap<>() {{
                put("airportFrom", "DUB");
                put("airportTo", "STN");
                put("connectingAirport", null);
                put("operator", "RYANAIR");
            }},
            new HashMap<>() {{
                put("airportFrom", "STN");
                put("airportTo", "WRO");
                put("connectingAirport", null);
                put("operator", "RYANAIR");
            }}
        );
        given(routesClientMock.getRoutes()).willReturn(routesData);

        // SchedulesClient mock:
        // DUB->STN => day=10, flight 07:00 -> 08:00 (1h)
        Map<String, Object> scheduleDUB_STN = Map.of(
            "days", List.of(
                Map.of(
                    "day", 10,
                    "flights", List.of(
                        Map.of("departureTime", "07:00", "arrivalTime", "08:00")
                    )
                )
            )
        );
        given(schedulesClientMock.getSchedule("DUB", "STN", 2025, 3))
            .willReturn(scheduleDUB_STN);

        // STN->WRO => day=10, flight 10:05 -> 12:55
        // (sale 2h 5min despues de 08:00 => sí cumple 2h mínimas)
        Map<String, Object> scheduleSTN_WRO = Map.of(
            "days", List.of(
                Map.of(
                    "day", 10,
                    "flights", List.of(
                        Map.of("departureTime", "10:05", "arrivalTime", "12:55")
                    )
                )
            )
        );
        given(schedulesClientMock.getSchedule("STN", "WRO", 2025, 3))
            .willReturn(scheduleSTN_WRO);

        // WHEN
        LocalDateTime depDate = LocalDateTime.of(2025, 3, 10, 6, 0);
        LocalDateTime arrDate = LocalDateTime.of(2025, 3, 10, 21, 0);
        List<FlightResponse> result = flightService.findFlights("DUB", "WRO", depDate, arrDate);

        // THEN
        // Esperamos 1 vuelo con stops=1
        assertEquals(1, result.size());
        FlightResponse flight = result.get(0);
        assertEquals(1, flight.getStops());
        assertEquals("DUB", flight.getLegs().get(0).getDepartureAirport());
        assertEquals("STN", flight.getLegs().get(0).getArrivalAirport());
        assertEquals("STN", flight.getLegs().get(1).getDepartureAirport());
        assertEquals("WRO", flight.getLegs().get(1).getArrivalAirport());
    }

    @Test
    void testFindFlights_CrossingMidnight() {
        // GIVEN
        // Rutas: DUB->WRO directa
        List<Map<String, String>> routesData = List.of(
            new HashMap<>() {{
                put("airportFrom", "DUB");
                put("airportTo", "WRO");
                put("connectingAirport", null);
                put("operator", "RYANAIR");
            }}
        );
        given(routesClientMock.getRoutes()).willReturn(routesData);

        // Schedules: día=10 con flight 22:00 -> 01:00 (arrivalTime < departureTime => cruza medianoche)
        Map<String, Object> scheduleDUB_WRO = Map.of(
            "days", List.of(
                Map.of(
                    "day", 10,
                    "flights", List.of(
                        Map.of("departureTime", "22:00", "arrivalTime", "01:00") 
                        // arrivalTime=01:00 => interpretado como day=10+1=11
                    )
                )
            )
        );
        given(schedulesClientMock.getSchedule("DUB", "WRO", 2025, 3))
            .willReturn(scheduleDUB_WRO);

        // WHEN
        // Permitimos buscar hasta el day=11 a las 02:00 => deberíamos ver el vuelo
        LocalDateTime depDate = LocalDateTime.of(2025, 3, 10, 20, 0); // 10-mar 20:00
        LocalDateTime arrDate = LocalDateTime.of(2025, 3, 11, 2, 0);  // 11-mar 02:00
        List<FlightResponse> result = flightService.findFlights("DUB", "WRO", depDate, arrDate);

        // THEN
        // Debe aparecer 1 vuelo directo (stops=0) con leg 22:00 -> 01:00 (day+1)
        assertEquals(1, result.size());
        FlightResponse flight = result.get(0);
        assertEquals(0, flight.getStops());
        // Chequear que arrival sea 2025-03-11T01:00
        assertEquals("2025-03-10T22:00", flight.getLegs().get(0).getDepartureDateTime().toString());
        assertEquals("2025-03-11T01:00", flight.getLegs().get(0).getArrivalDateTime().toString());
    }

    @Test
    void testFindFlights_InvalidDayNumber_Ignored() {
        // GIVEN
        // Rutas: DUB->WRO
        List<Map<String, String>> routesData = List.of(
            new HashMap<>() {{
                put("airportFrom", "DUB");
                put("airportTo", "WRO");
                put("connectingAirport", null);
                put("operator", "RYANAIR");
            }}
        );
        given(routesClientMock.getRoutes()).willReturn(routesData);

        // Schedules con day=32 (un día inexistente)
        Map<String, Object> scheduleDUB_WRO = Map.of(
            "days", List.of(
                Map.of(
                    "day", 32,
                    "flights", List.of(
                        Map.of("departureTime", "09:30", "arrivalTime", "12:55")
                    )
                )
            )
        );
        // => Al intentar LocalDateTime.of(..., day=32) => salta DateTimeException en findDirectFlights
        given(schedulesClientMock.getSchedule("DUB", "WRO", 2025, 3))
            .willReturn(scheduleDUB_WRO);

        // WHEN
        LocalDateTime depDate = LocalDateTime.of(2025, 3, 10, 7, 0);
        LocalDateTime arrDate = LocalDateTime.of(2025, 3, 10, 21, 0);
        List<FlightResponse> result = flightService.findFlights("DUB", "WRO", depDate, arrDate);

        // THEN
        // Esperamos una lista vacía, porque se ignora el flight con day=32
        // (la excepción se captura y se hace continue)
        assertTrue(result.isEmpty());
    }

}
