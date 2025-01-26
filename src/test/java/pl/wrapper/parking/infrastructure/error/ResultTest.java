package pl.wrapper.parking.infrastructure.error;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pl.wrapper.parking.facade.domain.ParkingController;
import pl.wrapper.parking.facade.domain.ParkingServiceImpl;
import pl.wrapper.parking.pwrResponseHandler.PwrApiServerCaller;
import pl.wrapper.parking.pwrResponseHandler.domain.PwrApiCaller;
import pl.wrapper.parking.pwrResponseHandler.domain.PwrApiServerCallerImpl;
import pl.wrapper.parking.pwrResponseHandler.dto.ParkingResponse;
import reactor.core.publisher.Mono;

@WebMvcTest(ParkingController.class)
class ResultTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ParkingServiceImpl service;

    private ParkingResponse parkingResponse;

    @BeforeEach
    void setUp() {
        parkingResponse = new ParkingResponse(4, 10, 10, "test", "T", LocalTime.now(), LocalTime.now(), null);
    }

    @Test
    void shouldReturnDummyBody() throws Exception {

        Mockito.when(service.getById(parkingResponse.parkingId(), null)).thenReturn(Result.success(parkingResponse));

        MvcResult mvcResult = mockMvc.perform(get("/parkings/id")
                        .param("id", String.valueOf(parkingResponse.parkingId()))) // add url and variables
                .andReturn();

        Integer status = mvcResult.getResponse().getStatus(); // get response status

        Integer okStatus = 200;
        assertEquals(okStatus, status); // check status
    }

    @Test
    void shouldReturnException() {
        Exception provided = new ClassCastException("simulated");

        PwrApiCaller apiCaller = Mockito.mock(PwrApiCaller.class);
        Mockito.when(apiCaller.fetchParkingPlaces()).thenReturn(Mono.error(provided)); // simulate response

        PwrApiServerCaller pwrApiServerCaller = new PwrApiServerCallerImpl(apiCaller);
        Exception e = assertThrows(provided.getClass(), pwrApiServerCaller::fetchData); // check error class

        assertEquals(provided.getMessage(), e.getMessage()); // check error message
    }
}
