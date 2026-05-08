package tests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.*;
import java.lang.reflect.Method;
import java.time.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import domain.Appointment;

class MainTest {

    private Object call(String method, Class<?>[] types, Object... args) throws Exception {
        Method m = main.Main.class.getDeclaredMethod(method, types);
        m.setAccessible(true);
        return m.invoke(null, args);
    }

    // ====================================================
    // friendlyType ✅
    // ====================================================
    @ParameterizedTest
    @CsvSource({
        "FIRST_VISIT, Individual – First Visit",
        "FOLLOW_UP, Individual – Follow-up",
        "VIRTUAL, Individual – Virtual",
        "GROUP_FIRST_VISIT, Group – First Visit",
        "XYZ, XYZ"
    })
    void testFriendlyType(String input, String expected) throws Exception {
        assertEquals(expected,
                call("friendlyType", new Class[]{String.class}, input));
    }

    // ====================================================
    // isWithin24h ✅
    // ====================================================
    @Test
    void testIsWithin24h() throws Exception {
        OffsetDateTime near = OffsetDateTime.now(ZoneOffset.UTC).plusHours(2);
        OffsetDateTime far  = OffsetDateTime.now(ZoneOffset.UTC).plusDays(3);

        assertTrue((boolean) call("isWithin24h",
                new Class[]{OffsetDateTime.class}, near));
        assertFalse((boolean) call("isWithin24h",
                new Class[]{OffsetDateTime.class}, far));
    }

    // ====================================================
    // sortGroup ✅
    // ====================================================
    @Test
    void testSortGroup() throws Exception {
        Appointment done = mock(Appointment.class);
        when(done.getStatus()).thenReturn(Appointment.STATUS_DONE);

        Appointment canceled = mock(Appointment.class);
        when(canceled.getStatus()).thenReturn(Appointment.STATUS_CANCELED);

        Appointment confirmed = mock(Appointment.class);
        when(confirmed.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);

        assertEquals(0, call("sortGroup",
                new Class[]{Appointment.class}, done));
        assertEquals(1, call("sortGroup",
                new Class[]{Appointment.class}, canceled));
        assertEquals(2, call("sortGroup",
                new Class[]{Appointment.class}, confirmed));
    }

    // ====================================================
    // fmtTime ✅
    // ====================================================
    @Test
    void testFmtTime() throws Exception {
        String s = (String) call("fmtTime",
                new Class[]{OffsetDateTime.class},
                OffsetDateTime.now(ZoneOffset.UTC));
        assertNotNull(s);
        assertFalse(s.isEmpty());
    }
}
