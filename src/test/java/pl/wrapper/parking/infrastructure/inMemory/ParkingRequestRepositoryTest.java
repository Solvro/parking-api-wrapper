package pl.wrapper.parking.infrastructure.inMemory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.wrapper.parking.infrastructure.inMemory.dto.ParkingRequest;
import pl.wrapper.parking.infrastructure.inMemory.dto.RequestStatus;

public class ParkingRequestRepositoryTest {
    private ParkingRequestRepository parkingRequestRepository;
    private static final String path = "data/statistics/tests";

    @BeforeEach
    void setUp() {
        parkingRequestRepository = new ParkingRequestRepository(path);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @AfterEach
    void tearDown() {
        File file = new File(path);
        file.delete();
    }

    @Test
    void createUpdateGetEntry() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 12, 30, 12, 11);
        ParkingRequest parkingRequest = new ParkingRequest(dateTime, RequestStatus.SUCCESS, "/api/v1");
        parkingRequestRepository.add(dateTime, parkingRequest);
        assertTrue(parkingRequestRepository.fetchAllKeys().contains(dateTime));
        assertEquals(parkingRequestRepository.get(dateTime), parkingRequest);

        parkingRequest = new ParkingRequest(null, RequestStatus.FAILED, "");
        parkingRequestRepository.add(null, parkingRequest);
        assertTrue(parkingRequestRepository.fetchAllKeys().contains(null));
        assertEquals(parkingRequestRepository.get(null), parkingRequest);

        parkingRequest = new ParkingRequest(dateTime, RequestStatus.SUCCESS, null);
        parkingRequestRepository.add(dateTime, parkingRequest);
        assertTrue(parkingRequestRepository.fetchAllKeys().contains(dateTime));
        assertEquals(parkingRequestRepository.get(dateTime), parkingRequest);
    }
}
