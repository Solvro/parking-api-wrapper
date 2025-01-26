package pl.wrapper.parking.infrastructure.inMemory.dto;

import java.io.Serializable;

public record AvailabilityData(int sampleCount, double averageAvailability) implements Serializable {}
