package pl.wrapper.parking.facade.dto.stats.basis;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record ParkingStats(
        @Schema(example = "0.723") double averageAvailability, @Schema(example = "37") int averageFreeSpots) {}
