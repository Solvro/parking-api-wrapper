package pl.wrapper.parking.facade.dto.stats.weekly;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Map;
import org.springframework.http.MediaType;
import pl.wrapper.parking.facade.dto.stats.basis.ParkingInfo;
import pl.wrapper.parking.facade.dto.stats.basis.ParkingStats;

public record CollectiveWeeklyParkingStats(
        @Schema(implementation = ParkingInfo.class) ParkingInfo parkingInfo,
        @Schema(
                        contentMediaType = MediaType.APPLICATION_JSON_VALUE,
                        example = "{\"MONDAY\": "
                                + "{\"08:00:00\": {\"averageAvailability\": 0.723, \"averageFreeSpots\": 37},"
                                + "\"08:10:00\": {\"averageAvailability\": 0.69, \"averageFreeSpots\": 33}},"
                                + "\"TUESDAY\": "
                                + "{\"09:30:00\": {\"averageAvailability\": 0.431, \"averageFreeSpots\": 23},"
                                + "\"09:40:00\": {\"averageAvailability\": 0.411, \"averageFreeSpots\": 21}}}")
                Map<DayOfWeek, Map<LocalTime, ParkingStats>> statsMap) {}
