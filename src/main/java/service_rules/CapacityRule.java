package service_rules;



import domain.Appointment;

public class CapacityRule implements BookingRuleStrategy {

    @Override
    public boolean isValid(Appointment appointment) {
        return appointment.getParticipantsCount()
                <= appointment.getMaxParticipants();
    }

    @Override
    public String errorMessage() {
        return "Participant limit exceeded.";
    }
}

