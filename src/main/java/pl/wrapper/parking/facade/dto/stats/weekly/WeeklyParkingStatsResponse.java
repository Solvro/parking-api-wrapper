package pl.wrapper.parking.facade.dto.stats.weekly;

import io.swagger.v3.oas.annotations.media.Schema;
import pl.wrapper.parking.facade.dto.stats.basis.OccupancyInfo;
import pl.wrapper.parking.facade.dto.stats.basis.ParkingInfo;
import pl.wrapper.parking.facade.dto.stats.basis.ParkingStats;

public record WeeklyParkingStatsResponse(
        @Schema(implementation = ParkingInfo.class) ParkingInfo parkingInfo,
        @Schema(implementation = ParkingStats.class) ParkingStats stats,
        @Schema(implementation = OccupancyInfo.class) OccupancyInfo maxOccupancyInfo,
        @Schema(implementation = OccupancyInfo.class) OccupancyInfo minOccupancyInfo) {}
