package pl.wrapper.parking.infrastructure.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
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

    @Override
    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        LocalDateTime timestamp = LocalDateTime.now();
        RequestStatus requestStatus = RequestStatus.FAILED;
        if (HttpStatus.Series.valueOf(response.getStatus()) == HttpStatus.Series.SUCCESSFUL) {
            requestStatus = RequestStatus.SUCCESS;
        }

        parkingRequestRepository.add(timestamp, new ParkingRequest(timestamp, requestStatus, request.getRequestURI()));
    }
}
