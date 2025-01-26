package pl.wrapper.parking.facade.domain;

import static org.hamcrest.CoreMatchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.*;
import org.springframework.test.web.servlet.MockMvc;
import pl.wrapper.parking.facade.dto.NominatimLocation;
import pl.wrapper.parking.infrastructure.inMemory.ParkingDataRepository;
import pl.wrapper.parking.infrastructure.nominatim.client.NominatimClient;
import pl.wrapper.parking.pwrResponseHandler.PwrApiServerCaller;
import pl.wrapper.parking.pwrResponseHandler.dto.Address;
import pl.wrapper.parking.pwrResponseHandler.dto.ParkingResponse;
import reactor.core.publisher.Flux;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class ParkingControllerIT {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PwrApiServerCaller pwrApiServerCaller;

    @MockBean
    private NominatimClient nominatimClient;

    @MockBean
    private ParkingDataRepository dataRepository;

    private List<ParkingResponse> parkings;

    @BeforeEach
    void setUp() {
        parkings = List.of(
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
    }

    @Test
    void getClosestParking_returnClosestParking() throws Exception {
        String address = "test place";
        NominatimLocation location = new NominatimLocation(37.0, -158.0);

        when(nominatimClient.search(eq(address), anyString())).thenReturn(Flux.just(location));
        when(pwrApiServerCaller.fetchData()).thenReturn(parkings);

        mockMvc.perform(get("/parkings/address").queryParam("address", address).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parkingId", is(1)))
                .andExpect(jsonPath("$.name", is("Parking 1")))
                .andExpect(jsonPath("$.address.geoLatitude").value(37.1f))
                .andExpect(jsonPath("$.address.geoLongitude").value(-158.8f));
    }

    @Test
    void getClosestParking_returnNotFound_whenNoResultsFromApi() throws Exception {
        String address = "non-existent address";
        when(nominatimClient.search(eq(address), anyString())).thenReturn(Flux.empty());

        mockMvc.perform(get("/parkings/address").queryParam("address", address).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage", anything()));

        verify(pwrApiServerCaller, never()).fetchData();
    }

    @Test
    void getClosestParking_returnNotFound_whenNoParkingsAvailable() throws Exception {
        String address = "test place";
        NominatimLocation location = new NominatimLocation(37.0, -158.0);
        when(nominatimClient.search(eq(address), anyString())).thenReturn(Flux.just(location));
        when(pwrApiServerCaller.fetchData()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/parkings/address").queryParam("address", address).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage", anything()));
    }

    @Test
    public void getParkingByParams_returnAllParkings_whenNoParamsGiven() throws Exception {
        when(pwrApiServerCaller.fetchData()).thenReturn(parkings);

        mockMvc.perform(get("/parkings").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(parkings)));
    }

    @Test
    public void getParkingBySymbol_returnFoundParking() throws Exception {
        when(pwrApiServerCaller.fetchData()).thenReturn(parkings);

        mockMvc.perform(get("/parkings/symbol")
                        .accept(MediaType.APPLICATION_JSON)
                        .queryParam("symbol", "P1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parkingId", is(1)))
                .andExpect(jsonPath("$.name", is("Parking 1")))
                .andExpect(jsonPath("$.address.geoLatitude").value(37.1f))
                .andExpect(jsonPath("$.address.geoLongitude").value(-158.8f));
    }

    @Test
    public void getParkingByName_returnNoParkings() throws Exception {
        when(pwrApiServerCaller.fetchData()).thenReturn(parkings);

        mockMvc.perform(get("/parkings/name")
                        .accept(MediaType.APPLICATION_JSON)
                        .queryParam("name", "Non-existent name"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage", anything()));
    }
}
