package pl.wrapper.parking.infrastructure.inMemory;

import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;

//@Component("parkingEndpointRepository")
public class ParkingEndpointRepository extends InMemoryRepositoryImpl<Integer, Integer> { // <Key, value>

    public ParkingEndpointRepository(@Value("${serialization.location.ParkingRequests}") String saveToLocationPath) {
        super(
                saveToLocationPath, // to modify location path, change above @Value's value
                new HashMap<>(), // put here whatever map type you want
                null); // Add default value here (empty object probably)
    }
}
