package pl.wrapper.parking.infrastructure.inMemory.dto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("endpointDataFactory")
public class EndpointDataFactory {
    @Value("${timeframe.default.length.inMinutes}")
    private int timeframeLength;

    public EndpointData create() {
        return new EndpointData(timeframeLength);
    }
}
