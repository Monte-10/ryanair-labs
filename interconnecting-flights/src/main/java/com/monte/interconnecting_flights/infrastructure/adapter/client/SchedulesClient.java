package com.monte.interconnecting_flights.infrastructure.adapter.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class SchedulesClient {

    private final RestTemplate restTemplate;
    private final String schedulesBaseUrl;

    public SchedulesClient(@Value("${schedules.url}") String schedulesBaseUrl) {
        this.restTemplate = new RestTemplate();
        this.schedulesBaseUrl = schedulesBaseUrl;
    }

    public Map<String, Object> getSchedule(String departure, String arrival, int year, int month) {
        // Build URL with: departure/arrival/year/month
        String url = String.format("%s/%s/%s/years/%d/months/%d",
            schedulesBaseUrl, departure, arrival, year, month);

        try {
            return restTemplate.getForObject(url, Map.class);
        } catch (HttpClientErrorException e) {
            throw new ExternalApiException("Error while querying the Schedules API: " 
                                            + e.getStatusCode(), e);
        }
    }
}
