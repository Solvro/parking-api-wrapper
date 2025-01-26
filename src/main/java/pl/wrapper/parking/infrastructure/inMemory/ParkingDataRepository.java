package pl.wrapper.parking.infrastructure.inMemory;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.wrapper.parking.infrastructure.inMemory.dto.AvailabilityData;
import pl.wrapper.parking.infrastructure.inMemory.dto.ParkingData;
import pl.wrapper.parking.infrastructure.util.DateTimeUtils;
import pl.wrapper.parking.pwrResponseHandler.PwrApiServerCaller;
import pl.wrapper.parking.pwrResponseHandler.dto.ParkingResponse;

@Component("parkingDataRepository")
@Slf4j
public class ParkingDataRepository extends InMemoryRepositoryImpl<Integer, ParkingData> {

    @Value("${pwr-api.data-fetch.minutes}")
    private Integer minuteInterval;

    private final PwrApiServerCaller pwrApiServerCaller;

    public ParkingDataRepository(
            @Value("${serialization.location.parkingData}") String saveToLocationPath,
            PwrApiServerCaller pwrApiServerCaller) {
        super(saveToLocationPath, new HashMap<>(), null);
        this.pwrApiServerCaller = pwrApiServerCaller;
    }

    public Collection<ParkingData> values() {
        return dataMap.values();
    }

    @Scheduled(fixedRateString = "${pwr-api.data-fetch.minutes}", timeUnit = TimeUnit.MINUTES)
    private void handleData() {
        LocalDateTime currentDateTime = DateTimeUtils.roundToNearestInterval(LocalDateTime.now(), minuteInterval);
        LocalTime currentTime = currentDateTime.toLocalTime();
        DayOfWeek currentDay = currentDateTime.getDayOfWeek();

        log.info("Saving parking data with rounded time: {}, day: {}", currentTime, currentDay);

        List<ParkingResponse> parkings = pwrApiServerCaller.fetchData();
        for (ParkingResponse parking : parkings) {
            int parkingId = parking.parkingId();
            double availability = (double) parking.freeSpots() / parking.totalSpots();

            ParkingData parkingData = get(parkingId);
            if (parkingData == null) {
                parkingData = ParkingData.builder()
                        .parkingId(parkingId)
                        .totalSpots(parking.totalSpots())
                        .freeSpotsHistory(new HashMap<>())
                        .build();
            }

            Map<LocalTime, AvailabilityData> dailyHistory =
                    parkingData.freeSpotsHistory().computeIfAbsent(currentDay, k -> new HashMap<>());
            AvailabilityData availabilityData =
                    dailyHistory.computeIfAbsent(currentTime, k -> new AvailabilityData(0, 0.0));

            int newSampleCount = availabilityData.sampleCount() + 1;
            double newAvgAvailability =
                    (availabilityData.averageAvailability() * availabilityData.sampleCount() + availability)
                            / newSampleCount;
            AvailabilityData newAvailabilityData = new AvailabilityData(newSampleCount, newAvgAvailability);

            dailyHistory.put(currentTime, newAvailabilityData);
            add(parkingId, parkingData);
        }

        log.info("Parking data saved successfully. Storage updated.");
    }
}
