package pl.wrapper.parking.facade.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pl.wrapper.parking.facade.ParkingService;
import pl.wrapper.parking.facade.dto.NominatimLocation;
import pl.wrapper.parking.facade.dto.RequestStatsResponse;
import pl.wrapper.parking.infrastructure.error.ParkingError;
import pl.wrapper.parking.infrastructure.error.Result;
import pl.wrapper.parking.infrastructure.inMemory.ParkingRequestRepository;
import pl.wrapper.parking.infrastructure.inMemory.dto.ParkingRequest;
import pl.wrapper.parking.infrastructure.inMemory.dto.RequestStatus;
import pl.wrapper.parking.infrastructure.nominatim.client.NominatimClient;
import pl.wrapper.parking.pwrResponseHandler.PwrApiServerCaller;
import pl.wrapper.parking.pwrResponseHandler.dto.ParkingResponse;

@Service
@Slf4j
public record ParkingServiceImpl(
        PwrApiServerCaller pwrApiServerCaller,
        NominatimClient nominatimClient,
        ParkingRequestRepository requestRepository,
        @Value("${default.timeframe.length.inMinutes}") int defaultTimeframeLength)
        implements ParkingService {

    @Override
    public RequestStatsResponse getBasicRequestStats(LocalDateTime start, LocalDateTime end) {
        List<ParkingRequest> requests = requestRepository.values().stream()
                .filter(generatePredicateToParkingRequestForParams(start, end))
                .toList();
        return calculateBasicRequestStats(requests);
    }

    @Override
    public List<Map.Entry<String, Double>> getRequestPeakTimes(
            LocalDateTime start, LocalDateTime end, Integer timeframeLengthInMinutes) {
        int timeframeLength = Optional.ofNullable(timeframeLengthInMinutes).orElse(defaultTimeframeLength);
        List<ParkingRequest> requests = requestRepository.values().stream()
                .filter(generatePredicateToParkingRequestForParams(start, end))
                .toList();
        return calculatePeakRequestTimes(requests, timeframeLength, start, end);
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

    private Predicate<ParkingRequest> generatePredicateToParkingRequestForParams(
            LocalDateTime start, LocalDateTime end) {
        Predicate<ParkingRequest> predicate = parkingRequest -> true;
        if (start != null)
            predicate =
                    predicate.and(parkingRequest -> !parkingRequest.timestamp().isBefore(start));
        if (end != null)
            predicate =
                    predicate.and(parkingRequest -> !parkingRequest.timestamp().isAfter(end));

        return predicate;
    }

    private RequestStatsResponse calculateBasicRequestStats(List<ParkingRequest> requests) {
        log.info("Calculating request stats for matching requests: {}", requests);

        long totalRequests = requests.size();
        double requestSuccessRate = calculateSuccessRate(requests, totalRequests);

        Map<String, Long> requestForEndpoint = requests.stream()
                .collect(Collectors.groupingBy(
                        req -> Optional.ofNullable(req.requestURI()).orElse("UNKNOWN"), Collectors.counting()));

        return new RequestStatsResponse(totalRequests, requestSuccessRate, requestForEndpoint);
    }

    private double calculateSuccessRate(List<ParkingRequest> requests, long totalRequests) {
        if (totalRequests == 0) return 0.0;

        long successfulRequests = requests.stream()
                .filter(request -> request.requestStatus() == RequestStatus.SUCCESS)
                .count();

        return BigDecimal.valueOf((double) successfulRequests / totalRequests * 100)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private List<Map.Entry<String, Double>> calculatePeakRequestTimes(
            List<ParkingRequest> requests, int timeframeLengthInMinutes, LocalDateTime start, LocalDateTime end) {
        log.info(
                "Calculating peak request times for matching requests: {} and timeframeLength = {}min",
                requests,
                timeframeLengthInMinutes);

        int timeframesCount = calculateTimeframesCount(timeframeLengthInMinutes);
        int[] requestsCount = new int[timeframesCount];

        Function<LocalDateTime, Integer> mapToTimeframeIndex = createTimeframeToIndexMapper(timeframeLengthInMinutes);

        start = Optional.ofNullable(start).orElseGet(() -> findMinTimestamp(requests));
        end = Optional.ofNullable(end).orElseGet(() -> findMaxTimestamp(requests));

        long daysValue = ChronoUnit.DAYS.between(start, end);

        requests.forEach(request -> requestsCount[mapToTimeframeIndex.apply(request.timestamp())]++);

        Map<String, Double> averageRequestsPerTimeframe = calculateAverageRequestsPerTimeframe(
                timeframesCount, requestsCount, timeframeLengthInMinutes, start, end, daysValue, mapToTimeframeIndex);

        return averageRequestsPerTimeframe.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(3)
                .toList();
    }

    private int calculateTimeframesCount(int timeframeLengthInMinutes) {
        return (int) Math.ceil((double) 60 * 24 / timeframeLengthInMinutes);
    }

    private Function<LocalDateTime, Integer> createTimeframeToIndexMapper(int timeframeLengthInMinutes) {
        return timestamp -> {
            long minutesFromMidnight = ChronoUnit.MINUTES.between(LocalTime.MIDNIGHT, timestamp);
            return (int) minutesFromMidnight / timeframeLengthInMinutes;
        };
    }

    private LocalDateTime findMinTimestamp(List<ParkingRequest> requests) {
        return requests.stream()
                .min(Comparator.comparing(ParkingRequest::timestamp))
                .map(ParkingRequest::timestamp)
                .orElse(LocalDateTime.now());
    }

    private LocalDateTime findMaxTimestamp(List<ParkingRequest> requests) {
        return requests.stream()
                .max(Comparator.comparing(ParkingRequest::timestamp))
                .map(ParkingRequest::timestamp)
                .orElse(LocalDateTime.now());
    }

    private Map<String, Double> calculateAverageRequestsPerTimeframe(
            int timeframesCount,
            int[] requestsCount,
            int timeframeLengthInMinutes,
            LocalDateTime start,
            LocalDateTime end,
            long daysValue,
            Function<LocalDateTime, Integer> mapToTimeframeIndex) {
        Map<String, Double> averageRequestsPerTimeframe = new LinkedHashMap<>();

        for (int i = 0; i < timeframesCount; i++) {
            LocalTime startTime = LocalTime.MIDNIGHT.plusMinutes((long) i * timeframeLengthInMinutes);
            LocalTime endTime = calculateEndTime(startTime, timeframeLengthInMinutes, i, timeframesCount);

            String timeframeKey = formatTimeframeKey(startTime, endTime);
            long effectiveDays =
                    isIndexBetweenCircular(i, mapToTimeframeIndex.apply(start), mapToTimeframeIndex.apply(end))
                            ? daysValue
                            : daysValue - 1;

            double average = effectiveDays > 0 ? requestsCount[i] / (double) effectiveDays : 0.0;
            averageRequestsPerTimeframe.put(
                    timeframeKey,
                    BigDecimal.valueOf(average)
                            .setScale(3, RoundingMode.HALF_UP)
                            .doubleValue());
        }

        return averageRequestsPerTimeframe;
    }

    private LocalTime calculateEndTime(
            LocalTime startTime, int timeframeLengthInMinutes, int index, int timeframesCount) {
        LocalTime endTime = startTime.plusMinutes(timeframeLengthInMinutes - 1);
        if (index == timeframesCount - 1) {
            endTime = LocalTime.of(23, 59);
        }
        return endTime;
    }

    private String formatTimeframeKey(LocalTime start, LocalTime end) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        return dateTimeFormatter.format(start) + " - " + dateTimeFormatter.format(end);
    }

    private boolean isIndexBetweenCircular(int index, int fromIndex, int toIndex) {
        if (fromIndex <= toIndex) {
            return index >= fromIndex && index <= toIndex;
        } else {
            return index >= fromIndex || index <= toIndex;
        }
    }
}
