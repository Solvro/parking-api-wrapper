package pl.wrapper.parking.facade.domain;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.wrapper.parking.facade.ParkingService;
import pl.wrapper.parking.facade.dto.EndpointStats;
import pl.wrapper.parking.facade.dto.NominatimLocation;
import pl.wrapper.parking.infrastructure.error.ParkingError;
import pl.wrapper.parking.infrastructure.error.Result;
import pl.wrapper.parking.infrastructure.inMemory.ParkingRequestRepository;
import pl.wrapper.parking.infrastructure.inMemory.dto.EndpointData;
import pl.wrapper.parking.infrastructure.inMemory.dto.TimeframeStatistic;
import pl.wrapper.parking.infrastructure.nominatim.client.NominatimClient;
import pl.wrapper.parking.pwrResponseHandler.PwrApiServerCaller;
import pl.wrapper.parking.pwrResponseHandler.dto.ParkingResponse;

@Service
@Slf4j
public record ParkingServiceImpl(
        PwrApiServerCaller pwrApiServerCaller,
        NominatimClient nominatimClient,
        ParkingRequestRepository requestRepository)
        implements ParkingService {

    @Override
    public Map<String, EndpointStats> getBasicRequestStats() {
        return requestRepository.fetchAllEntries().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> {
            EndpointData endpointData = entry.getValue();
            return new EndpointStats(
                    endpointData.getRequestCount(), endpointData.getSuccessCount(), endpointData.getSuccessRate());
        }));
    }

    @Override
    public Map<String, List<Map.Entry<String, Double>>> getRequestStatsForTimes() {
        return requestRepository.fetchAllEntries().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> getTimeframesWithAverage(entry.getValue())));
    }

    @Override
    public List<Map.Entry<String, Double>> getRequestPeakTimes() {
        EndpointData totalEndpoint = requestRepository.getTotalEndpoint();
        List<Map.Entry<String, Double>> timeframesWithAverage = getTimeframesWithAverage(totalEndpoint);

        return timeframesWithAverage.stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(3)
                .toList();
    }

    @Override
    public Map<String, Double> getDailyRequestStats() {
        return requestRepository.fetchAllEntries().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> {
            EndpointData endpointData = entry.getValue();
            double sumOfAverages = 0.0;
            for (TimeframeStatistic timeframeStatistic : endpointData.getTimeframeStatistics()) {
                sumOfAverages += timeframeStatistic.getAverageNumberOfRequests();
            }
            return sumOfAverages;
        }));
    }

    @Override
    public List<ParkingResponse> getAllWithFreeSpots(Boolean opened) {
        Predicate<ParkingResponse> predicate = generatePredicateForParams(null, null, null, opened, true);
        return getStreamOfFilteredFetchedParkingLots(predicate).toList();
    }

    @Override
    public Result<ParkingResponse> getWithTheMostFreeSpots(Boolean opened) {
        Predicate<ParkingResponse> predicate = generatePredicateForParams(null, null, null, opened, null);
        return getStreamOfFilteredFetchedParkingLots(predicate)
                .max(Comparator.comparingInt(ParkingResponse::freeSpots))
                .map(this::handleFoundParking)
                .orElse(Result.failure(new ParkingError.NoFreeParkingSpotsAvailable()));
    }

    @Override
    public Result<ParkingResponse> getClosestParking(String address) {
        Optional<NominatimLocation> geoLocation =
                nominatimClient.search(address, "json").next().blockOptional();
        return geoLocation
                .map(location -> {
                    log.info("Geocoded address for coordinates: {} {}", location.latitude(), location.longitude());
                    return findClosestParking(location, pwrApiServerCaller.fetchData())
                            .map(Result::success)
                            .orElse(Result.failure(new ParkingError.ParkingNotFoundByAddress(address)));
                })
                .orElseGet(() -> {
                    log.warn("No geocoding results for address: {}", address);
                    return Result.failure(new ParkingError.ParkingNotFoundByAddress(address));
                });
    }

    @Override
    public Result<ParkingResponse> getByName(String name, Boolean opened) {
        Predicate<ParkingResponse> predicate = generatePredicateForParams(null, null, name, opened, null);
        return findParking(predicate)
                .map(this::handleFoundParking)
                .orElse(Result.failure(new ParkingError.ParkingNotFoundByName(name)));
    }

    @Override
    public Result<ParkingResponse> getById(Integer id, Boolean opened) {
        Predicate<ParkingResponse> predicate = generatePredicateForParams(null, id, null, opened, null);
        return findParking(predicate)
                .map(this::handleFoundParking)
                .orElse(Result.failure(new ParkingError.ParkingNotFoundById(id)));
    }

    @Override
    public Result<ParkingResponse> getBySymbol(String symbol, Boolean opened) {
        Predicate<ParkingResponse> predicate = generatePredicateForParams(symbol, null, null, opened, null);
        return findParking(predicate)
                .map(this::handleFoundParking)
                .orElse(Result.failure(new ParkingError.ParkingNotFoundBySymbol(symbol)));
    }

    @Override
    public List<ParkingResponse> getByParams(
            String symbol, Integer id, String name, Boolean opened, Boolean hasFreeSpots) {
        Predicate<ParkingResponse> predicate = generatePredicateForParams(symbol, id, name, opened, hasFreeSpots);
        return getStreamOfFilteredFetchedParkingLots(predicate).toList();
    }

    private Stream<ParkingResponse> getStreamOfFilteredFetchedParkingLots(
            Predicate<ParkingResponse> filteringPredicate) {
        return pwrApiServerCaller.fetchData().stream().filter(filteringPredicate);
    }

    private Optional<ParkingResponse> findParking(Predicate<ParkingResponse> predicate) {
        return getStreamOfFilteredFetchedParkingLots(predicate).findFirst();
    }

    private Optional<ParkingResponse> findClosestParking(
            NominatimLocation location, List<ParkingResponse> parkingLots) {
        double lat = location.latitude();
        double lon = location.longitude();

        return parkingLots.stream()
                .min(Comparator.comparingDouble(parking -> haversineDistance(
                        lat,
                        lon,
                        parking.address().geoLatitude(),
                        parking.address().geoLongitude())));
    }

    private static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS = 6371;

        double havLat = (1 - Math.cos(Math.toRadians(lat2 - lat1))) / 2;
        double havLon = (1 - Math.cos(Math.toRadians(lon2 - lon1))) / 2;
        double haversine = havLat + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * havLon;

        return 2 * EARTH_RADIUS * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
    }

    private Result<ParkingResponse> handleFoundParking(ParkingResponse found) {
        log.info("Parking found");
        return Result.success(found);
    }

    private Predicate<ParkingResponse> generatePredicateForParams(
            String symbol, Integer id, String name, Boolean isOpened, Boolean hasFreeSpots) {
        Predicate<ParkingResponse> predicate = parking -> true;
        if (symbol != null)
            predicate = predicate.and(
                    parking -> symbol.toLowerCase().contains(parking.symbol().toLowerCase()));
        if (id != null) predicate = predicate.and(parking -> Objects.equals(id, parking.parkingId()));
        if (name != null)
            predicate = predicate.and(
                    parking -> name.toLowerCase().contains(parking.name().toLowerCase()));
        if (isOpened != null) predicate = predicate.and(parking -> Objects.equals(isOpened, parking.isOpened()));
        if (hasFreeSpots != null) predicate = predicate.and(parking -> hasFreeSpots == (parking.freeSpots() > 0));

        return predicate;
    }

    private String formatTimeframe(int index, int timeframeLength) {
        LocalTime start = LocalTime.MIDNIGHT.plusMinutes((long) index * timeframeLength);
        LocalTime end = start.plusMinutes(timeframeLength);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return formatter.format(start) + " - " + formatter.format(end);
    }

    private List<Map.Entry<String, Double>> getTimeframesWithAverage(EndpointData endpointData) {
        List<Map.Entry<String, Double>> averages = new ArrayList<>();
        for (int i = 0; i < endpointData.getTimeframeStatistics().length; ++i) {
            String timeframe = formatTimeframe(i, endpointData.getTimeframeLength());
            Double average = endpointData.getTimeframeStatistics()[i].getAverageNumberOfRequests();
            averages.add(Map.entry(timeframe, average));
        }
        return averages;
    }
}
