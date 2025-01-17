package pl.wrapper.parking.infrastructure.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import pl.wrapper.parking.infrastructure.inMemory.ParkingRequestRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParkingRequestInterceptor implements HandlerInterceptor {
    private final ParkingRequestRepository parkingRequestRepository;
    private LocalTime requestTime;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        requestTime = LocalTime.now();
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        boolean isSuccessful = HttpStatus.Series.valueOf(response.getStatus()) == HttpStatus.Series.SUCCESSFUL;
        parkingRequestRepository.updateRequestEndpointData(request.getRequestURI(), isSuccessful, requestTime);
    }
}
