package pl.wrapper.parking.infrastructure.nominatim.configuration;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import pl.wrapper.parking.facade.dto.NominatimLocation;
import pl.wrapper.parking.infrastructure.exception.NominatimClientException;
import pl.wrapper.parking.infrastructure.nominatim.client.NominatimClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@SpringBootTest
@ContextConfiguration(classes = NominatimClientConfig.class)
public class NominatimClientTests {
    private static MockWebServer mockWebServer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private NominatimClient nominatimClient;

    @BeforeAll
    static void beforeAll() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        System.setProperty("maps.api.url", mockWebServer.url("/").toString());
    }

    @AfterAll
    static void afterAll() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void returnLocations_whenSearchSuccessful() throws IOException {
        double lat1 = 53.8927406;
        double lon1 = 25.3019590;
        double lat2 = 56.9339537;
        double lon2 = 13.7331028;

        List<NominatimLocation> locations =
                List.of(new NominatimLocation(lat1, lon1), new NominatimLocation(lat2, lon2));

        mockWebServer.enqueue(new MockResponse()
                .newBuilder()
                .body(objectMapper.writeValueAsString(locations))
                .addHeader("Content-Type", "application/json")
                .build());

        Flux<NominatimLocation> locationFlux = nominatimClient.search("Lida", "json");
        StepVerifier.create(locationFlux)
                .expectNextMatches(loc -> loc.latitude() == lat1 && loc.longitude() == lon1)
                .expectNextMatches(loc -> loc.latitude() == lat2 && loc.longitude() == lon2)
                .verifyComplete();
    }

    @Test
    void returnNothing_whenLocationNotFound() throws IOException {
        List<NominatimLocation> locations = List.of();

        mockWebServer.enqueue(new MockResponse()
                .newBuilder()
                .body(objectMapper.writeValueAsString(locations))
                .addHeader("Content-Type", "application/json")
                .build());

        Flux<NominatimLocation> locationFlux = nominatimClient.search("Non-existent", "json");
        StepVerifier.create(locationFlux).expectNextCount(0).verifyComplete();
    }

    @Test
    void throwException_whenSearchFailed() {
        String message = "Internal Server Error";

        mockWebServer.enqueue(
                new MockResponse().newBuilder().code(500).body(message).build());

        Flux<NominatimLocation> locationFlux = nominatimClient.search("Lida", "json");
        StepVerifier.create(locationFlux)
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(NominatimClientException.class)
                        .hasMessageContaining(message))
                .verify();
    }
}
