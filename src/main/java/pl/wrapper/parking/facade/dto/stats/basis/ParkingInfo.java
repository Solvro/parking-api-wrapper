package pl.wrapper.parking.facade.dto.stats.basis;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record ParkingInfo(@Schema(example = "3") int parkingId, @Schema(example = "54") int totalSpots) {}
