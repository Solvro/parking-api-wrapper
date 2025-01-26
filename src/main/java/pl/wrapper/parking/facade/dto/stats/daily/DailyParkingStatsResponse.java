package pl.wrapper.parking.facade.dto.stats.daily;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalTime;
import pl.wrapper.parking.facade.dto.stats.basis.ParkingInfo;
import pl.wrapper.parking.facade.dto.stats.basis.ParkingStats;

public record DailyParkingStatsResponse(
        @Schema(implementation = ParkingInfo.class) ParkingInfo parkingInfo,
        @Schema(implementation = ParkingStats.class) ParkingStats stats,
        @Schema(type = "string", format = "time", example = "15:30:00") LocalTime maxOccupancyAt,
        @Schema(type = "string", format = "time", example = "02:30:00") LocalTime minOccupancyAt) {}
