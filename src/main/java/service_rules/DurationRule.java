package service_rules;


import domain.Appointment;
import java.time.Duration;

public class DurationRule implements BookingRuleStrategy {

    private static final long MAX_MINUTES = 60;

    @Override
    public boolean isValid(Appointment appointment) {
        long minutes = Duration.between(
                appointment.getStartTime(),
                appointment.getEndTime()
        ).toMinutes();
        return minutes <= MAX_MINUTES;
    }

    @Override
    public String errorMessage() {
        return "Duration cannot exceed 60 minutes.";
    }
}
