package pl.wrapper.parking.facade;

import java.util.List;
import java.util.Map;
import org.springframework.lang.Nullable;
import pl.wrapper.parking.facade.dto.EndpointStats;
import pl.wrapper.parking.infrastructure.error.Result;
import pl.wrapper.parking.pwrResponseHandler.dto.ParkingResponse;

public interface ParkingService {
    Map<String, EndpointStats> getBasicRequestStats();

    Map<String, List<Map.Entry<String, Double>>> getRequestStatsForTimes();

    List<Map.Entry<String, Double>> getRequestPeakTimes();

    Map<String, Double> getDailyRequestStats();

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
