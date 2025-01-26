package pl.wrapper.parking.facade;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import org.springframework.lang.Nullable;
import pl.wrapper.parking.facade.dto.stats.ParkingStatsResponse;
import pl.wrapper.parking.facade.dto.stats.daily.CollectiveDailyParkingStats;
import pl.wrapper.parking.facade.dto.stats.daily.DailyParkingStatsResponse;
import pl.wrapper.parking.facade.dto.stats.weekly.CollectiveWeeklyParkingStats;
import pl.wrapper.parking.facade.dto.stats.weekly.WeeklyParkingStatsResponse;
import pl.wrapper.parking.infrastructure.error.Result;
import pl.wrapper.parking.pwrResponseHandler.dto.ParkingResponse;

public interface ParkingService {
    List<ParkingStatsResponse> getParkingStats(
            @Nullable List<Integer> parkingIds, @Nullable DayOfWeek dayOfWeek, LocalTime time);

    List<DailyParkingStatsResponse> getDailyParkingStats(@Nullable List<Integer> parkingIds, DayOfWeek dayOfWeek);

    List<WeeklyParkingStatsResponse> getWeeklyParkingStats(@Nullable List<Integer> parkingIds);

    List<CollectiveDailyParkingStats> getCollectiveDailyParkingStats(
            @Nullable List<Integer> parkingIds, DayOfWeek dayOfWeek);

    List<CollectiveWeeklyParkingStats> getCollectiveWeeklyParkingStats(@Nullable List<Integer> parkingIds);

    List<ParkingResponse> getAllWithFreeSpots(@Nullable Boolean opened);

    Result<ParkingResponse> getWithTheMostFreeSpots(@Nullable Boolean opened);

    Result<ParkingResponse> getClosestParking(String address);

    Result<ParkingResponse> getByName(String name, @Nullable Boolean opened);

    Result<ParkingResponse> getById(Integer id, @Nullable Boolean opened);

    Result<ParkingResponse> getBySymbol(String symbol, @Nullable Boolean opened);

    List<ParkingResponse> getByParams(
            @Nullable String symbol,
            @Nullable Integer id,
            @Nullable String name,
            @Nullable Boolean opened,
            @Nullable Boolean hasFreeSpots);
}
