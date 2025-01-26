package pl.wrapper.parking.infrastructure.util;

import java.time.LocalDateTime;

public class DateTimeUtils {

    public static LocalDateTime roundToNearestInterval(LocalDateTime dateTime, int minuteInterval) {
        if (minuteInterval <= 0) {
            throw new IllegalArgumentException("Minute interval must be positive");
        }

        int minutes = dateTime.getHour() * 60 + dateTime.getMinute();
        int roundedMinutes = (minutes / minuteInterval) * minuteInterval;

        return dateTime.toLocalDate().atStartOfDay().plusMinutes(roundedMinutes);
    }
}
