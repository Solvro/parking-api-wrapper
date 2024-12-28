package pl.wrapper.parking.infrastructure.inMemory;

import java.time.LocalDateTime;
import java.util.HashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pl.wrapper.parking.infrastructure.inMemory.dto.ParkingRequest;

@Component("parkingRequestRepository")
public class ParkingRequestRepository extends InMemoryRepositoryImpl<LocalDateTime, ParkingRequest> {

    public ParkingRequestRepository(@Value("${serialization.location.ParkingRequests}") String saveToLocationPath) {
        super(
                saveToLocationPath, // to modify location path, change above @Value's value
                new HashMap<>(), // put here whatever map type you want
                null); // Add default value here (empty object probably)
    }
}
