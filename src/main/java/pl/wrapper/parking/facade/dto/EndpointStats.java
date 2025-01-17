package pl.wrapper.parking.facade.dto;

public record EndpointStats(long totalRequests, long successfulRequests, double successRate) {}
