package pl.wrapper.parking.infrastructure.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import pl.wrapper.parking.infrastructure.inMemory.ParkingRequestRepository;
import pl.wrapper.parking.infrastructure.inMemory.dto.ParkingRequest;
import pl.wrapper.parking.infrastructure.inMemory.dto.RequestStatus;

@Component
@RequiredArgsConstructor
public class ParkingRequestInterceptor implements HandlerInterceptor {
    private final ParkingRequestRepository parkingRequestRepository;
    private LocalDateTime requestArrivalTime;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        requestArrivalTime = LocalDateTime.now();
        ParkingRequest parkingRequest = new ParkingRequest(requestArrivalTime, RequestStatus.PENDING, null);
        parkingRequestRepository.add(parkingRequest.timestamp(), parkingRequest);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        Long responseTime = ChronoUnit.MILLIS.between(requestArrivalTime, LocalDateTime.now());
        RequestStatus newRequestStatus = RequestStatus.FAILED;
        if (HttpStatus.Series.valueOf(response.getStatus()) == HttpStatus.Series.SUCCESSFUL) {
            newRequestStatus = RequestStatus.SUCCESS;
        }

        parkingRequestRepository.add(
                requestArrivalTime, new ParkingRequest(requestArrivalTime, newRequestStatus, responseTime));
    }
}
