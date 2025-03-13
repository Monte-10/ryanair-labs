package com.monte.interconnecting_flights.infrastructure.adapter.client;

import com.monte.interconnecting_flights.domain.port.outbound.RoutesPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Component
public class RoutesClient implements RoutesPort {

    private final RestTemplate restTemplate;
    private final String routesUrl;

    public RoutesClient(@Value("${routes.url}") String routesUrl) {
        this.restTemplate = new RestTemplate();
        this.routesUrl = routesUrl; 
    }

    @Override
    public List<Map<String, String>> getRoutes() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        headers.set("Accept", "application/json, text/javascript, */*; q=0.01");
        headers.set("Referer", "https://www.ryanair.com");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map[]> response = restTemplate.exchange(
                routesUrl, HttpMethod.GET, entity, Map[].class
            );

            // Log response
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                String jsonResponse = objectMapper
                                      .writerWithDefaultPrettyPrinter()
                                      .writeValueAsString(response.getBody());
                System.out.println("Ryanair API response (Routes):");
                System.out.println(jsonResponse);
            } catch (Exception e) {
                System.out.println("Error printing API response");
            }

            return List.of(response.getBody());

        } catch (HttpClientErrorException e) {
            throw new ExternalApiException("Error while querying the Routes API: " 
                                            + e.getStatusCode(), e);
        }
    }
}
