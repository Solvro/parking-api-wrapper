package pl.wrapper.parking.infrastructure.inMemory.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public record ParkingRequest(LocalDateTime timestamp, RequestStatus requestStatus, Long responseTime)
        implements Serializable {}
