package tests;


import domain.Appointment;
import service_rules.DurationRule;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DurationRuleTest {

    @Test
    void isValid_returnsTrue_whenDurationIsWithinLimit() {
        Appointment appt = mock(Appointment.class);

        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime end = start.plusMinutes(45);

        when(appt.getStartTime()).thenReturn(start);
        when(appt.getEndTime()).thenReturn(end);

        DurationRule rule = new DurationRule();

        assertTrue(rule.isValid(appt));
    }

    @Test
    void isValid_returnsFalse_whenDurationExceedsLimit() {
        Appointment appt = mock(Appointment.class);

        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime end = start.plusMinutes(120);

        when(appt.getStartTime()).thenReturn(start);
        when(appt.getEndTime()).thenReturn(end);

        DurationRule rule = new DurationRule();

        assertFalse(rule.isValid(appt));
    }

@Test
void errorMessage_returnsExpectedMessage() {
    DurationRule rule = new DurationRule();
    assertEquals("Duration cannot exceed 60 minutes.", rule.errorMessage());
}

}
