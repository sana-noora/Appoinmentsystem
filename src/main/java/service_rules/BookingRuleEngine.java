package service_rules;


import domain.Appointment;
import java.util.List;

public class BookingRuleEngine {

    private final List<BookingRuleStrategy> rules;

    public BookingRuleEngine(List<BookingRuleStrategy> rules) {
        this.rules = rules;
    }

    public void validate(Appointment appointment) {
        for (BookingRuleStrategy rule : rules) {
            if (!rule.isValid(appointment)) {
                throw new IllegalArgumentException(rule.errorMessage());
            }
        }
    }
}
