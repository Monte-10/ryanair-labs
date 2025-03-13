package com.monte.interconnecting_flights.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.monte.interconnecting_flights.InterconnectingFlightsApplication;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
  classes = InterconnectingFlightsApplication.class,
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test") 
@TestPropertySource(locations = "classpath:application-test.properties")
public class FlightServiceIntegrationTest {

    private static WireMockServer wireMockServer;

    @LocalServerPort
    private int port;

    @BeforeAll
    static void initWireMock() {
        wireMockServer = new WireMockServer(9999);
        wireMockServer.start();
        configureFor("localhost", 9999);
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @Test
    void testDirectFlightIntegration() {
        // 1) Stub Routes
        wireMockServer.stubFor(get(urlEqualTo("/views/locate/3/routes"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[{\n" +
                          "  \"airportFrom\":\"DUB\",\n" +
                          "  \"airportTo\":\"WRO\",\n" +
                          "  \"connectingAirport\":null,\n" +
                          "  \"operator\":\"RYANAIR\"\n" +
                          "}]"))
        );

        // 2) Stub Schedules
        wireMockServer.stubFor(get(urlPathEqualTo("/timtbl/3/schedules/DUB/WRO/years/2025/months/3"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"days\":[" +
                    "{\"day\":10,\"flights\":[{\"departureTime\":\"09:30\",\"arrivalTime\":\"12:55\"}]}" +
                "]}")
            )
        );

        // 3) Calling real app
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://localhost:" + port + "/interconnections" +
                "?departure=DUB&arrival=WRO&departureDateTime=2025-03-10T07:00&arrivalDateTime=2025-03-10T21:00";

        List<?> response = restTemplate.getForObject(url, List.class);

        // 4) Verify
        assertNotNull(response);
        assertEquals(1, response.size());
    }
}
