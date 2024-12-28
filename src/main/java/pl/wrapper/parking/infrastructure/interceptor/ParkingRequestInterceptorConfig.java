package pl.wrapper.parking.infrastructure.interceptor;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import pl.wrapper.parking.infrastructure.inMemory.ParkingRequestRepository;

@Configuration
@RequiredArgsConstructor
public class ParkingRequestInterceptorConfig implements WebMvcConfigurer {
    private final ParkingRequestRepository parkingRequestRepository;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ParkingRequestInterceptor(parkingRequestRepository))
                .addPathPatterns("/parkings/**");
    }
}
