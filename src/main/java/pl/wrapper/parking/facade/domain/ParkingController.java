package pl.wrapper.parking.facade.domain;

import static pl.wrapper.parking.infrastructure.error.HandleResult.handleResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.wrapper.parking.facade.ParkingService;
import pl.wrapper.parking.facade.dto.stats.ParkingStatsResponse;
import pl.wrapper.parking.facade.dto.stats.daily.CollectiveDailyParkingStats;
import pl.wrapper.parking.facade.dto.stats.daily.DailyParkingStatsResponse;
import pl.wrapper.parking.facade.dto.stats.weekly.CollectiveWeeklyParkingStats;
import pl.wrapper.parking.facade.dto.stats.weekly.WeeklyParkingStatsResponse;
import pl.wrapper.parking.infrastructure.error.ErrorWrapper;
import pl.wrapper.parking.infrastructure.error.Result;
import pl.wrapper.parking.pwrResponseHandler.dto.ParkingResponse;

@RequestMapping("/parkings")
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Parking API", description = "API for managing parking-related operations")
public class ParkingController {
    private final ParkingService parkingService;

    @Operation(
            summary = "get parking statistics",
            description = "Fetch statistics for specified parking IDs, day of week, and time",
            parameters = {
                @Parameter(name = "ids", description = "List of parking IDs to filter stats for", example = "1,3,5"),
                @Parameter(
                        name = "day_of_week",
                        description = "Day of week to filter stats for",
                        example = "WEDNESDAY"),
                @Parameter(
                        name = "time",
                        description = "Time to filter stats for in ISO time format",
                        required = true,
                        example = "12:34:00")
            },
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Parking stats retrieved successfully",
                        content =
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        array =
                                                @ArraySchema(
                                                        schema =
                                                                @Schema(implementation = ParkingStatsResponse.class)))),
                @ApiResponse(
                        responseCode = "400",
                        description = "Incorrect input query parameters",
                        content =
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        schema = @Schema(implementation = ErrorWrapper.class)))
            })
    @GetMapping(path = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ParkingStatsResponse>> getParkingStats(
            @RequestParam(name = "ids", required = false) List<Integer> parkingIds,
            @RequestParam(name = "day_of_week", required = false) DayOfWeek dayOfWeek,
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) @RequestParam(name = "time") LocalTime time) {
        log.info(
                "Fetching parking stats with parameters: ids = {}, day_of_week = {}, time = {}",
                parkingIds,
                dayOfWeek,
                time);
        List<ParkingStatsResponse> result = parkingService.getParkingStats(parkingIds, dayOfWeek, time);
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "get daily parking statistics",
            description = "Fetch daily statistics for specified parking IDs and day of week",
            parameters = {
                @Parameter(name = "ids", description = "List of parking IDs to filter stats for", example = "1,3,5"),
                @Parameter(
                        name = "day_of_week",
                        description = "Day of week to filter stats for",
                        required = true,
                        example = "WEDNESDAY"),
            },
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Daily parking stats retrieved successfully",
                        content =
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        array =
                                                @ArraySchema(
                                                        schema =
                                                                @Schema(
                                                                        implementation =
                                                                                DailyParkingStatsResponse.class)))),
                @ApiResponse(
                        responseCode = "400",
                        description = "Incorrect input query parameters",
                        content =
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        schema = @Schema(implementation = ErrorWrapper.class)))
            })
    @GetMapping(path = "/stats/daily", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DailyParkingStatsResponse>> getDailyParkingStats(
            @RequestParam(name = "ids", required = false) List<Integer> parkingIds,
            @RequestParam(name = "day_of_week") DayOfWeek dayOfWeek) {
        log.info("Fetching daily parking stats with parameters: ids = {}, day_of_week = {}", parkingIds, dayOfWeek);
        List<DailyParkingStatsResponse> result = parkingService.getDailyParkingStats(parkingIds, dayOfWeek);
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "get weekly parking statistics",
            description = "Fetch weekly statistics for specified parking IDs",
            parameters = {
                @Parameter(name = "ids", description = "List of parking IDs to filter stats for", example = "1,3,5")
            },
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Weekly parking stats retrieved successfully",
                        content =
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        array =
                                                @ArraySchema(
                                                        schema =
                                                                @Schema(
                                                                        implementation =
                                                                                WeeklyParkingStatsResponse.class)))),
                @ApiResponse(
                        responseCode = "400",
                        description = "Incorrect input query parameters",
                        content =
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        schema = @Schema(implementation = ErrorWrapper.class)))
            })
    @GetMapping(path = "/stats/weekly", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<WeeklyParkingStatsResponse>> getWeeklyParkingStats(
            @RequestParam(name = "ids", required = false) List<Integer> parkingIds) {
        log.info("Fetching weekly parking stats with parameters: ids = {}", parkingIds);
        List<WeeklyParkingStatsResponse> result = parkingService.getWeeklyParkingStats(parkingIds);
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "get collective daily parking statistics",
            description = "Fetch daily statistics for specified parking IDs and day of week for each time interval",
            parameters = {
                @Parameter(name = "ids", description = "List of parking IDs to filter stats for", example = "1,3,5"),
                @Parameter(
                        name = "day_of_week",
                        description = "Day of week to filter stats for",
                        required = true,
                        example = "WEDNESDAY"),
            },
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Collective daily parking stats retrieved successfully",
                        content =
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        array =
                                                @ArraySchema(
                                                        schema =
                                                                @Schema(
                                                                        implementation =
                                                                                CollectiveDailyParkingStats.class)),
                                        examples =
                                                @ExampleObject(
                                                        "[{\"parkingInfo\": {\"parkingId\": 3, \"totalSpots\": 54},"
                                                                + "\"statsMap\": {\"08:00:00\": {\"averageAvailability\": 0.723, \"averageFreeSpots\": 37},"
                                                                + "\"08:10:00\": {\"averageAvailability\": 0.69, \"averageFreeSpots\": 33}}}]"))),
                @ApiResponse(
                        responseCode = "400",
                        description = "Incorrect input query parameters",
                        content =
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        schema = @Schema(implementation = ErrorWrapper.class)))
            })
    @GetMapping(path = "/stats/daily/collective", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CollectiveDailyParkingStats>> getCollectiveDailyParkingStats(
            @RequestParam(name = "ids", required = false) List<Integer> parkingIds,
            @RequestParam(name = "day_of_week") DayOfWeek dayOfWeek) {
        log.info(
                "Fetching collective daily parking stats with parameters: ids = {}, day_of_week = {}",
                parkingIds,
                dayOfWeek);
        List<CollectiveDailyParkingStats> result = parkingService.getCollectiveDailyParkingStats(parkingIds, dayOfWeek);
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "get collective weekly parking statistics",
            description = "Fetch weekly statistics for specified parking IDs for each time interval of each day",
            parameters = {
                @Parameter(name = "ids", description = "List of parking IDs to filter stats for", example = "1,3,5")
            },
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Collective weekly parking stats retrieved successfully",
                        content =
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        array =
                                                @ArraySchema(
                                                        schema =
                                                                @Schema(
                                                                        implementation =
                                                                                CollectiveWeeklyParkingStats.class)))),
                @ApiResponse(
                        responseCode = "400",
                        description = "Incorrect input query parameters",
                        content =
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        schema = @Schema(implementation = ErrorWrapper.class)))
            })
    @GetMapping(path = "/stats/weekly/collective", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CollectiveWeeklyParkingStats>> getCollectiveWeeklyParkingStats(
            @RequestParam(name = "ids", required = false) List<Integer> parkingIds) {
        log.info("Fetching collective weekly parking stats with parameters: ids = {}", parkingIds);
        List<CollectiveWeeklyParkingStats> result = parkingService.getCollectiveWeeklyParkingStats(parkingIds);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "get list of parking lots with free spots from all/opened/closed parking lots")
    @ApiResponse(
            responseCode = "200",
            description = "list of parking lots",
            content =
                    @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ParkingResponse.class))))
    @GetMapping(path = "/free", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ParkingResponse>> getAllParkingWithFreeSpots(
            @Parameter(description = "search in opened parking lots") @RequestParam(required = false) Boolean opened) {
        log.info("Finding all parking with free spots");
        List<ParkingResponse> result = parkingService.getAllWithFreeSpots(opened);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @Operation(summary = "get parking with the most free spots from all/opened/closed parking lots")
    @ApiResponse(
            responseCode = "200",
            description = "parking found",
            content =
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ParkingResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "parking not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorWrapper.class)))
    @GetMapping(path = "/free/top", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getParkingWithTheMostFreeSpots(
            @Parameter(description = "search in opened parking lots") @RequestParam(required = false) Boolean opened,
            HttpServletRequest request) {
        log.info("Finding parking with the most free spots");
        Result<ParkingResponse> result = parkingService.getWithTheMostFreeSpots(opened);
        return handleResult(result, HttpStatus.OK, request.getRequestURI());
    }

    @Operation(
            summary = "find the closest parking by address",
            parameters =
                    @Parameter(
                            name = "address",
                            description = "The address to find the closest parking for",
                            required = true,
                            example = "Flower 20, 50-337 Wroclaw"),
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Closest parking found",
                        content =
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        schema = @Schema(implementation = ParkingResponse.class))),
                @ApiResponse(
                        responseCode = "404",
                        description = "No parking found for the given address",
                        content =
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        schema = @Schema(implementation = ErrorWrapper.class)))
            })
    @GetMapping(path = "/address", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getClosestParking(
            @RequestParam("address") String address, HttpServletRequest request) {
        log.info("Finding closest parking for address: {}", address);
        Result<ParkingResponse> result = parkingService.getClosestParking(address);
        return handleResult(result, HttpStatus.OK, request.getRequestURI());
    }

    @Operation(summary = "get parking by name if opened")
    @ApiResponse(
            responseCode = "200",
            description = "parking found",
            content =
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ParkingResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "parking not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorWrapper.class)))
    @GetMapping(path = "/name", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getParkingByName(
            @Parameter(description = "parking name") @RequestParam String name,
            @Parameter(description = "is parking opened") @RequestParam(required = false) Boolean opened,
            HttpServletRequest request) {
        log.info("Received request: get parking by name: {}", name);
        Result<ParkingResponse> result = parkingService.getByName(name, opened);
        return handleResult(result, HttpStatus.OK, request.getRequestURI());
    }

    @Operation(summary = "get parking by id if opened")
    @ApiResponse(
            responseCode = "200",
            description = "parking found",
            content =
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ParkingResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "parking not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorWrapper.class)))
    @GetMapping(path = "/id", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getParkingById(
            @Parameter(description = "parking id") @RequestParam Integer id,
            @Parameter(description = "is parking opened") @RequestParam(required = false) Boolean opened,
            HttpServletRequest request) {
        log.info("Received request: get parking by id: {}", id);
        Result<ParkingResponse> result = parkingService.getById(id, opened);
        return handleResult(result, HttpStatus.OK, request.getRequestURI());
    }

    @Operation(summary = "get parking by symbol if opened")
    @ApiResponse(
            responseCode = "200",
            description = "parking found",
            content =
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ParkingResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "parking not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorWrapper.class)))
    @GetMapping(path = "/symbol", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getParkingBySymbol(
            @Parameter(description = "parking symbol") @RequestParam String symbol,
            @Parameter(description = "is parking opened") @RequestParam(required = false) Boolean opened,
            HttpServletRequest request) {
        log.info("Received request: get parking by symbol: {}", symbol);
        Result<ParkingResponse> result = parkingService.getBySymbol(symbol, opened);
        return handleResult(result, HttpStatus.OK, request.getRequestURI());
    }

    @Operation(summary = "get list of parking lots by name, id, symbol, if opened and has free spots")
    @ApiResponse(
            responseCode = "200",
            description = "list with parking lots",
            content =
                    @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ParkingResponse.class))))
    @GetMapping(path = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ParkingResponse>> getParkingByParams(
            @Parameter(description = "parking symbol") @RequestParam(required = false) String symbol,
            @Parameter(description = "parking id") @RequestParam(required = false) Integer id,
            @Parameter(description = "parking name") @RequestParam(required = false) String name,
            @Parameter(description = "is parking opened") @RequestParam(required = false) Boolean opened,
            @Parameter(description = "if parking has free spots") @RequestParam(required = false) Boolean freeSpots) {
        log.info(
                "Received request: get parking by symbol: {}, id: {}, name: {} and hasFreeSpots: {}",
                symbol,
                id,
                name,
                freeSpots);
        List<ParkingResponse> result = parkingService.getByParams(symbol, id, name, opened, freeSpots);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
