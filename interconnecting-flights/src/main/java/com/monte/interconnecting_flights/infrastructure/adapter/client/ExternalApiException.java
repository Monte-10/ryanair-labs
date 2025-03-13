package com.monte.interconnecting_flights.infrastructure.adapter.client;

public class ExternalApiException extends RuntimeException {
    public ExternalApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
