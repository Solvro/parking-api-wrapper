package pl.wrapper.parking.facade.dto;

import java.util.Map;

public record RequestStatsResponse(
        long totalNumberOfRequest, double requestSuccessRate, Map<String, Long> numberOfRequestForEndpoint) {}
