package tests;


import domain.Appointment;
import service_rules.CapacityRule;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CapacityRuleTest {

    @Test
    void isValid_returnsTrue_whenParticipantsWithinLimit() {
        Appointment appt = mock(Appointment.class);

        when(appt.getParticipantsCount()).thenReturn(3);
        when(appt.getMaxParticipants()).thenReturn(5);

        CapacityRule rule = new CapacityRule();

        assertTrue(rule.isValid(appt));
    }

    @Test
    void isValid_returnsFalse_whenParticipantsExceedLimit() {
        Appointment appt = mock(Appointment.class);

        when(appt.getParticipantsCount()).thenReturn(6);
        when(appt.getMaxParticipants()).thenReturn(5);

        CapacityRule rule = new CapacityRule();

        assertFalse(rule.isValid(appt));
    }

@Test
void errorMessage_returnsExpectedMessage() {
    CapacityRule rule = new CapacityRule();
    assertEquals("Participant limit exceeded.", rule.errorMessage());
}

}
