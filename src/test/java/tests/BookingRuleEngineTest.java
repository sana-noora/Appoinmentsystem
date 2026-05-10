package tests;


import domain.Appointment;
import service_rules.BookingRuleEngine;
import service_rules.CapacityRule;
import service_rules.DurationRule;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;

class BookingRuleEngineTest {

    @Test
    void validate_passes_whenAllRulesAreValid() {
        Appointment appt = mock(Appointment.class);

        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime end = start.plusMinutes(30);

        when(appt.getStartTime()).thenReturn(start);
        when(appt.getEndTime()).thenReturn(end);
        when(appt.getParticipantsCount()).thenReturn(2);
        when(appt.getMaxParticipants()).thenReturn(5);

        BookingRuleEngine engine = new BookingRuleEngine(
        		Arrays.asList(new DurationRule(), new CapacityRule())
        );

        assertDoesNotThrow(() -> engine.validate(appt));
    }

    @Test
    void validate_throwsException_whenAnyRuleFails() {
        Appointment appt = mock(Appointment.class);

        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime end = start.plusMinutes(120);

        when(appt.getStartTime()).thenReturn(start);
        when(appt.getEndTime()).thenReturn(end);
        when(appt.getParticipantsCount()).thenReturn(10);
        when(appt.getMaxParticipants()).thenReturn(5);

        BookingRuleEngine engine = new BookingRuleEngine(
        		Arrays.asList(new DurationRule(), new CapacityRule())
        );

        assertThrows(IllegalArgumentException.class,
                () -> engine.validate(appt));
    }
}

