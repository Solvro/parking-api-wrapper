package pl.wrapper.parking.facade;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.lang.Nullable;
import pl.wrapper.parking.facade.dto.RequestStatsResponse;
import pl.wrapper.parking.infrastructure.error.Result;
import pl.wrapper.parking.pwrResponseHandler.dto.ParkingResponse;

public interface ParkingService {
    RequestStatsResponse getBasicRequestStats(LocalDateTime start, LocalDateTime end);

    List<Map.Entry<String, Double>> getRequestPeakTimes(
            LocalDateTime start, LocalDateTime end, Integer timeframeLengthInMinutes);

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
