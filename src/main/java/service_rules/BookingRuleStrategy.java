package service_rules;




import domain.Appointment;

public interface BookingRuleStrategy {
    boolean isValid(Appointment appointment);
    String errorMessage();
}
