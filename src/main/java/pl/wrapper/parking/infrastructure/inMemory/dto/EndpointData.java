package pl.wrapper.parking.infrastructure.inMemory.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import lombok.Getter;

@Getter
public class EndpointData implements Serializable {
    private long successCount;
    private long requestCount;
    private final TimeframeStatistic[] timeframeStatistics;
    private final int timeframeLength;

    public EndpointData(int timeframeLength) {
        this.successCount = 0;
        this.requestCount = 0;
        this.timeframeLength = timeframeLength;
        this.timeframeStatistics = new TimeframeStatistic[calculateTimeframesCount(timeframeLength)];
        for (int i = 0; i < timeframeStatistics.length; i++) {
            timeframeStatistics[i] = new TimeframeStatistic();
        }
    }

    public double getSuccessRate() {
        if (requestCount == 0) return 0.0;
        return BigDecimal.valueOf((double) successCount / requestCount * 100)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public void registerRequest(boolean isSuccessful, LocalTime requestTime) {
        requestCount++;
        if (isSuccessful) {
            successCount++;
        }
        int timeframe = mapToTimeframeIndex(requestTime);
        timeframeStatistics[timeframe].registerRequest();
    }

    public void recalculateAverageForPreviousTimeframe(LocalTime currentTimeframeTime) {
        timeframeStatistics[getPreviousTimeframeIndex(currentTimeframeTime)].recalculateAverage();
    }

    public int getPreviousTimeframeIndex(LocalTime currentTimeframeTime) {
        return (mapToTimeframeIndex(currentTimeframeTime) - 1 + timeframeStatistics.length)
                % timeframeStatistics.length;
    }

    private int calculateTimeframesCount(int timeframeLengthInMinutes) {
        return (int) Math.ceil((double) 24 * 60 / timeframeLengthInMinutes);
    }

    private int mapToTimeframeIndex(LocalTime time) {
        return (int) ChronoUnit.MINUTES.between(LocalTime.MIDNIGHT, time) / timeframeLength;
    }
}
