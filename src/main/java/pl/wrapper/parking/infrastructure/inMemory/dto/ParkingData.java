package pl.wrapper.parking.infrastructure.inMemory.dto;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Map;
import lombok.Builder;

@Builder
public record ParkingData(
        int parkingId, int totalSpots, Map<DayOfWeek, Map<LocalTime, AvailabilityData>> freeSpotsHistory)
        implements Serializable {}
