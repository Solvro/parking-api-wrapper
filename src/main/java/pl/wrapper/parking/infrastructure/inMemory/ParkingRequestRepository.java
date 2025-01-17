package pl.wrapper.parking.infrastructure.inMemory;

import java.time.LocalTime;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.wrapper.parking.infrastructure.inMemory.dto.EndpointData;
import pl.wrapper.parking.infrastructure.inMemory.dto.EndpointDataFactory;

@Slf4j
@Component("parkingRequestRepository")
public class ParkingRequestRepository extends InMemoryRepositoryImpl<String, EndpointData> {
    private static final String TOTAL_ENDPOINT_NAME = "total";
    private final EndpointDataFactory endpointDataFactory;

    @Autowired
    public ParkingRequestRepository(
            @Value("${serialization.location.ParkingRequests}") String saveToLocationPath,
            EndpointDataFactory endpointDataFactory) {
        super(saveToLocationPath, new HashMap<>(), null);
        this.endpointDataFactory = endpointDataFactory;
        dataMap.put(TOTAL_ENDPOINT_NAME, endpointDataFactory.create());
    }

    public EndpointData getTotalEndpoint() {
        return dataMap.get(TOTAL_ENDPOINT_NAME);
    }

    public void updateRequestEndpointData(String requestURI, boolean isSuccessful, LocalTime requestTime) {
        dataMap.computeIfAbsent(requestURI, key -> endpointDataFactory.create())
                .registerRequest(isSuccessful, requestTime);
        getTotalEndpoint().registerRequest(isSuccessful, requestTime);
    }

    @Scheduled(cron = "0 */${timeframe.default.length.inMinutes} * * * *")
    public void updateAverages() {
        LocalTime currentTime = LocalTime.now();
        values().forEach(endpointData -> endpointData.recalculateAverageForPreviousTimeframe(currentTime));

        int previousTimeframeIndex = getTotalEndpoint().getPreviousTimeframeIndex(currentTime);
        double totalAverage = dataMap.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(TOTAL_ENDPOINT_NAME))
                .mapToDouble(entry -> entry.getValue()
                        .getTimeframeStatistics()[previousTimeframeIndex]
                        .getAverageNumberOfRequests())
                .sum();

        getTotalEndpoint().getTimeframeStatistics()[previousTimeframeIndex].setAverageNumberOfRequests(totalAverage);
    }
}
