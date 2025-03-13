package com.monte.interconnecting_flights.domain.port.outbound;

import java.util.List;
import java.util.Map;

public interface RoutesPort {
    List<Map<String, String>> getRoutes();
}
