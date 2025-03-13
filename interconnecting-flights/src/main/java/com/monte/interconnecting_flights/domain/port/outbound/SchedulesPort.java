package com.monte.interconnecting_flights.domain.port.outbound;

import java.util.Map;

public interface SchedulesPort {
    Map<String, Object> getSchedule(String departure, String arrival, int year, int month);
}
