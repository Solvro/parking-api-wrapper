package pl.wrapper.parking.facade.domain;

import static java.time.DayOfWeek.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.wrapper.parking.facade.dto.NominatimLocation;
import pl.wrapper.parking.facade.dto.stats.ParkingStatsResponse;
import pl.wrapper.parking.facade.dto.stats.basis.OccupancyInfo;
import pl.wrapper.parking.facade.dto.stats.basis.ParkingInfo;
import pl.wrapper.parking.facade.dto.stats.basis.ParkingStats;
import pl.wrapper.parking.facade.dto.stats.daily.CollectiveDailyParkingStats;
import pl.wrapper.parking.facade.dto.stats.daily.DailyParkingStatsResponse;
import pl.wrapper.parking.facade.dto.stats.weekly.CollectiveWeeklyParkingStats;
import pl.wrapper.parking.facade.dto.stats.weekly.WeeklyParkingStatsResponse;
import pl.wrapper.parking.infrastructure.error.ParkingError;
import pl.wrapper.parking.infrastructure.error.Result;
import pl.wrapper.parking.infrastructure.inMemory.ParkingDataRepository;
import pl.wrapper.parking.infrastructure.inMemory.dto.AvailabilityData;
import pl.wrapper.parking.infrastructure.inMemory.dto.ParkingData;
import pl.wrapper.parking.infrastructure.nominatim.client.NominatimClient;
import pl.wrapper.parking.pwrResponseHandler.PwrApiServerCaller;
import pl.wrapper.parking.pwrResponseHandler.dto.Address;
import pl.wrapper.parking.pwrResponseHandler.dto.ParkingResponse;
import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceImplTests {
    @Mock
    private PwrApiServerCaller pwrApiServerCaller;

    @Mock
    private NominatimClient nominatimClient;

    @Mock
    private ParkingDataRepository dataRepository;

    private ParkingServiceImpl parkingService;

    private List<ParkingResponse> parkings1;
    private List<ParkingResponse> parkings2;
    private List<ParkingData> parkingData;

    @BeforeEach
    void setUp() {
        parkingService = new ParkingServiceImpl(pwrApiServerCaller, nominatimClient, dataRepository, 10);
        parkings1 = List.of(
                ParkingResponse.builder()
                        .parkingId(1)
                        .name("Parking 1")
                        .symbol("P1")
                        .address(new Address("street 1", 37.1f, -158.8f))
                        .build(),
                ParkingResponse.builder()
                        .parkingId(2)
                        .name("Parking 2")
                        .symbol("P2")
                        .address(new Address("street 2", -44.4f, 123.6f))
                        .build());
        parkings2 = List.of(
                ParkingResponse.builder()
                        .parkingId(1)
                        .name("Parking 1")
                        .symbol("P1")
                        .freeSpots(0)
                        .openingHours(null)
                        .closingHours(null)
                        .build(),
                ParkingResponse.builder()
                        .parkingId(2)
                        .name("Parking 2")
                        .symbol("P2")
                        .freeSpots(325)
                        .openingHours(LocalTime.NOON)
                        .closingHours(LocalTime.NOON)
                        .build(),
                ParkingResponse.builder()
                        .parkingId(3)
                        .name("Parking 3")
                        .symbol("P3")
                        .freeSpots(117)
                        .openingHours(LocalTime.NOON)
                        .closingHours(LocalTime.NOON)
                        .build(),
                ParkingResponse.builder()
                        .parkingId(4)
                        .name("Parking 4")
                        .symbol("P4")
                        .freeSpots(51)
                        .openingHours(null)
                        .closingHours(null)
                        .build());
        parkingData = List.of(
                ParkingData.builder()
                        .parkingId(1)
                        .totalSpots(100)
                        .freeSpotsHistory(Map.of(
                                MONDAY,
                                        Map.of(
                                                LocalTime.of(10, 0), new AvailabilityData(1, 0.8),
                                                LocalTime.of(12, 0), new AvailabilityData(1, 0.5)),
                                TUESDAY, Map.of(LocalTime.of(10, 0), new AvailabilityData(1, 0.7))))
                        .build(),
                ParkingData.builder()
                        .parkingId(2)
                        .totalSpots(200)
                        .freeSpotsHistory(Map.of(
                                MONDAY, Map.of(LocalTime.of(10, 0), new AvailabilityData(1, 0.6)),
                                WEDNESDAY, Map.of(LocalTime.of(14, 0), new AvailabilityData(1, 0.9))))
                        .build());
    }

    @Test
    void getClosestParking_returnSuccessWithClosestParking() {
        String address = "test place";
        NominatimLocation location = new NominatimLocation(37.0, -158.0);

        when(nominatimClient.search(eq(address), anyString())).thenReturn(Flux.just(location));
        when(pwrApiServerCaller.fetchData()).thenReturn(parkings1);

        Result<ParkingResponse> result = parkingService.getClosestParking(address);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).matches(p -> p.name().equals("Parking 1"));

        verify(nominatimClient).search(address, "json");
        verify(pwrApiServerCaller).fetchData();
    }

    @Test
    void getClosestParking_returnFailureOfAddressNotFound_whenNoResultsFromApi() {
        String address = "non-existent address";

        when(nominatimClient.search(eq(address), anyString())).thenReturn(Flux.empty());

        Result<ParkingResponse> result = parkingService.getClosestParking(address);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError()).isInstanceOf(ParkingError.ParkingNotFoundByAddress.class);

        verify(nominatimClient).search(address, "json");
        verify(pwrApiServerCaller, never()).fetchData();
    }

    @Test
    void getClosestParking_returnFailureOfAddressNotFound_whenNoParkingsAvailable() {
        String address = "test place";
        NominatimLocation location = new NominatimLocation(37.0, -158.0);

        when(nominatimClient.search(eq(address), anyString())).thenReturn(Flux.just(location));
        when(pwrApiServerCaller.fetchData()).thenReturn(Collections.emptyList());

        Result<ParkingResponse> result = parkingService.getClosestParking(address);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError()).isInstanceOf(ParkingError.ParkingNotFoundByAddress.class);

        verify(nominatimClient).search(address, "json");
        verify(pwrApiServerCaller).fetchData();
    }

    @Test()
    void getAllParkingsWithFreeSpots_shouldReturnList() {
        when(pwrApiServerCaller.fetchData()).thenReturn(parkings2);
        List<ParkingResponse> result = parkingService.getAllWithFreeSpots(null);

        assertEquals(3, result.size());
        assertTrue(result.stream().allMatch(parking -> parking.freeSpots() > 0));
    }

    @Test
    void getOpenedParkingsWithFreeSpots_shouldReturnList() {
        when(pwrApiServerCaller.fetchData()).thenReturn(parkings2);
        List<ParkingResponse> result = parkingService.getAllWithFreeSpots(true);

        assertEquals(1, result.size());
        assertTrue(result.stream().allMatch(parking -> parking.freeSpots() > 0 && parking.isOpened()));
    }

    @Test
    void getOpenedParkingsWithFreeSpots_shouldReturnEmptyList() {
        List<ParkingResponse> parkingDataLocal = new ArrayList<>(parkings2);
        parkingDataLocal.remove(3);

        when(pwrApiServerCaller.fetchData()).thenReturn(parkingDataLocal);
        List<ParkingResponse> result = parkingService.getAllWithFreeSpots(true);

        assertEquals(0, result.size());
    }

    @Test
    void getClosedParkingsWithFreeSpots_shouldReturnList() {
        when(pwrApiServerCaller.fetchData()).thenReturn(parkings2);
        List<ParkingResponse> result = parkingService.getAllWithFreeSpots(false);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(parking -> parking.freeSpots() > 0 && !parking.isOpened()));
    }

    @Test
    void getParkingWithTheMostFreeSpacesFromAll_shouldReturnSuccessResult() {
        when(pwrApiServerCaller.fetchData()).thenReturn(parkings2);
        Result<ParkingResponse> result = parkingService.getWithTheMostFreeSpots(null);

        assertTrue(result.isSuccess());
        assertEquals(325, result.getData().freeSpots());
        assertEquals("P2", result.getData().symbol());
    }

    @Test
    void getParkingWithTheMostFreeSpacesFromOpened_shouldReturnSuccessResult() {
        when(pwrApiServerCaller.fetchData()).thenReturn(parkings2);
        Result<ParkingResponse> result = parkingService.getWithTheMostFreeSpots(true);

        assertTrue(result.isSuccess());
        assertEquals(51, result.getData().freeSpots());
        assertEquals("P4", result.getData().symbol());
    }

    @Test
    void getParkingWithTheMostFreeSpacesFromClosed_shouldReturnSuccessResult() {
        when(pwrApiServerCaller.fetchData()).thenReturn(parkings2);
        Result<ParkingResponse> result = parkingService.getWithTheMostFreeSpots(false);

        assertTrue(result.isSuccess());
        assertEquals(325, result.getData().freeSpots());
        assertEquals("P2", result.getData().symbol());
    }

    @Test
    void getParkingWithTheMostFreeSpacesFromClosed_shouldReturnNotFoundError() {
        List<ParkingResponse> parkingDataLocal = new ArrayList<>(parkings2);
        parkingDataLocal.remove(2);
        parkingDataLocal.remove(1);
        when(pwrApiServerCaller.fetchData()).thenReturn(parkingDataLocal);
        Result<ParkingResponse> result = parkingService.getWithTheMostFreeSpots(false);

        assertFalse(result.isSuccess());
        assertInstanceOf(ParkingError.NoFreeParkingSpotsAvailable.class, result.getError());
    }

    @Test
    void getParkingStats_withDayOfWeekAndTime_returnCorrectStats() {
        when(dataRepository.values()).thenReturn(parkingData);

        List<ParkingStatsResponse> result = parkingService.getParkingStats(null, MONDAY, LocalTime.of(10, 7));

        assertThat(result).hasSize(2);
        ParkingStatsResponse stats1 = result.get(0);
        ParkingStatsResponse stats2 = result.get(1);
        assertThat(stats1.parkingInfo())
                .extracting(ParkingInfo::parkingId, ParkingInfo::totalSpots)
                .containsExactly(1, 100);
        assertThat(stats1.stats())
                .extracting(ParkingStats::averageAvailability, ParkingStats::averageFreeSpots)
                .containsExactly(0.8, 80);

        assertThat(stats2.parkingInfo())
                .extracting(ParkingInfo::parkingId, ParkingInfo::totalSpots)
                .containsExactly(2, 200);
        assertThat(stats2.stats())
                .extracting(ParkingStats::averageAvailability, ParkingStats::averageFreeSpots)
                .containsExactly(0.6, 120);
    }

    @Test
    void getParkingStats_withWeirdIdListAndTime_returnCorrectStats() {
        when(dataRepository.fetchAllKeys()).thenReturn(Set.of(1, 2));
        when(dataRepository.get(anyInt())).thenReturn(parkingData.get(0), parkingData.get(1));

        List<ParkingStatsResponse> result = parkingService.getParkingStats(List.of(1, 2, 3), null, LocalTime.of(10, 7));

        assertThat(result).hasSize(2);
        ParkingStatsResponse stats1 = result.get(0);
        ParkingStatsResponse stats2 = result.get(1);
        assertThat(stats1.parkingInfo())
                .extracting(ParkingInfo::parkingId, ParkingInfo::totalSpots)
                .containsExactly(1, 100);
        assertThat(stats1.stats())
                .extracting(ParkingStats::averageAvailability, ParkingStats::averageFreeSpots)
                .containsExactly(0.75, 75);

        assertThat(stats2.parkingInfo())
                .extracting(ParkingInfo::parkingId, ParkingInfo::totalSpots)
                .containsExactly(2, 200);
        assertThat(stats2.stats())
                .extracting(ParkingStats::averageAvailability, ParkingStats::averageFreeSpots)
                .containsExactly(0.6, 120);
    }

    @Test
    void getParkingStats_withEmptyDataRepository_returnEmptyList() {
        when(dataRepository.values()).thenReturn(List.of());

        List<ParkingStatsResponse> result = parkingService.getParkingStats(List.of(1, 2), MONDAY, LocalTime.of(10, 7));

        assertThat(result).isEmpty();
    }

    @Test
    void getDailyParkingStats_withIdList_returnCorrectDailyStats() {
        when(dataRepository.fetchAllKeys()).thenReturn(Set.of(1, 2));
        when(dataRepository.get(1)).thenReturn(parkingData.getFirst());

        List<DailyParkingStatsResponse> result = parkingService.getDailyParkingStats(List.of(1), MONDAY);

        assertThat(result).hasSize(1);
        DailyParkingStatsResponse stats = result.getFirst();
        assertThat(stats.parkingInfo())
                .extracting(ParkingInfo::parkingId, ParkingInfo::totalSpots)
                .containsExactly(1, 100);
        assertThat(stats.stats())
                .extracting(ParkingStats::averageAvailability, ParkingStats::averageFreeSpots)
                .containsExactly(0.65, 65);
        assertThat(stats)
                .extracting(DailyParkingStatsResponse::maxOccupancyAt, DailyParkingStatsResponse::minOccupancyAt)
                .containsExactly(LocalTime.of(12, 0), LocalTime.of(10, 0));
    }

    @Test
    void getWeeklyParkingStats_withEmptyIdList_returnCorrectWeeklyStats() {
        when(dataRepository.values()).thenReturn(parkingData);

        List<WeeklyParkingStatsResponse> result = parkingService.getWeeklyParkingStats(List.of());

        assertThat(result).hasSize(2);
        WeeklyParkingStatsResponse stats1 = result.get(0);
        WeeklyParkingStatsResponse stats2 = result.get(1);
        assertThat(stats1.parkingInfo())
                .extracting(ParkingInfo::parkingId, ParkingInfo::totalSpots)
                .containsExactly(1, 100);
        assertThat(stats1.stats().averageAvailability()).isCloseTo(0.666, within(0.001));
        assertThat(stats1.stats().averageFreeSpots()).isCloseTo(66, within(1));
        assertThat(stats1.maxOccupancyInfo())
                .extracting(OccupancyInfo::dayOfWeek, OccupancyInfo::time)
                .containsExactly(MONDAY, LocalTime.of(12, 0));
        assertThat(stats1.minOccupancyInfo())
                .extracting(OccupancyInfo::dayOfWeek, OccupancyInfo::time)
                .containsExactly(MONDAY, LocalTime.of(10, 0));

        assertThat(stats2.parkingInfo())
                .extracting(ParkingInfo::parkingId, ParkingInfo::totalSpots)
                .containsExactly(2, 200);
        assertThat(stats2.stats())
                .extracting(ParkingStats::averageAvailability, ParkingStats::averageFreeSpots)
                .containsExactly(0.75, 150);
        assertThat(stats2.maxOccupancyInfo())
                .extracting(OccupancyInfo::dayOfWeek, OccupancyInfo::time)
                .containsExactly(MONDAY, LocalTime.of(10, 0));
        assertThat(stats2.minOccupancyInfo())
                .extracting(OccupancyInfo::dayOfWeek, OccupancyInfo::time)
                .containsExactly(WEDNESDAY, LocalTime.of(14, 0));
    }

    @Test
    void getCollectiveDailyParkingStats_withWeirdIdList_returnCorrectCollectiveDailyStats() {
        when(dataRepository.fetchAllKeys()).thenReturn(Set.of(1, 2));
        when(dataRepository.get(1)).thenReturn(parkingData.getFirst());

        List<CollectiveDailyParkingStats> result =
                parkingService.getCollectiveDailyParkingStats(List.of(-7, 1, 100), MONDAY);

        assertThat(result).hasSize(1);
        CollectiveDailyParkingStats stats = result.getFirst();
        assertThat(stats.parkingInfo())
                .extracting(ParkingInfo::parkingId, ParkingInfo::totalSpots)
                .containsExactly(1, 100);
        assertThat(stats.statsMap())
                .containsOnly(
                        entry(LocalTime.of(10, 0), new ParkingStats(0.8, 80)),
                        entry(LocalTime.of(12, 0), new ParkingStats(0.5, 50)));
    }

    @Test
    void getCollectiveWeeklyParkingStats_withoutIdList_returnCorrectCollectiveDailyStats() {
        when(dataRepository.values()).thenReturn(parkingData);

        List<CollectiveWeeklyParkingStats> result = parkingService.getCollectiveWeeklyParkingStats(null);

        assertThat(result).hasSize(2);
        CollectiveWeeklyParkingStats stats1 = result.get(0);
        CollectiveWeeklyParkingStats stats2 = result.get(1);
        assertThat(stats1.parkingInfo())
                .extracting(ParkingInfo::parkingId, ParkingInfo::totalSpots)
                .containsExactly(1, 100);
        assertThat(stats1.statsMap())
                .containsOnly(
                        entry(
                                MONDAY,
                                Map.of(
                                        LocalTime.of(10, 0), new ParkingStats(0.8, 80),
                                        LocalTime.of(12, 0), new ParkingStats(0.5, 50))),
                        entry(TUESDAY, Map.of(LocalTime.of(10, 0), new ParkingStats(0.7, 70))));

        assertThat(stats2.parkingInfo())
                .extracting(ParkingInfo::parkingId, ParkingInfo::totalSpots)
                .containsExactly(2, 200);
        assertThat(stats2.statsMap())
                .containsOnly(
                        entry(MONDAY, Map.of(LocalTime.of(10, 0), new ParkingStats(0.6, 120))),
                        entry(WEDNESDAY, Map.of(LocalTime.of(14, 0), new ParkingStats(0.9, 180))));
    }
}
