package pl.wrapper.parking.infrastructure.inMemory.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TimeframeStatistic implements Serializable {
    private double averageNumberOfRequests;
    private int totalNumberOfRequests;
    private int numberOfAverageCalculations;

    public void registerRequest() {
        totalNumberOfRequests++;
    }

    public void recalculateAverage() {
        averageNumberOfRequests = (averageNumberOfRequests * numberOfAverageCalculations + totalNumberOfRequests)
                / (numberOfAverageCalculations + 1);
        numberOfAverageCalculations++;
        totalNumberOfRequests = 0;
    }
}
