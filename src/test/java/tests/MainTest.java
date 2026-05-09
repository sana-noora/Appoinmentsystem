package tests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.*;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.time.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import domain.*;
import persistence.*;

/**
 * Comprehensive unit tests for Main class.
 * Tests cover:
 * - Utility methods (friendlyType, isWithin24h, fmtTime, readInt, readLong)
 * - Admin operations (view, cancel, edit appointments; add work days, slots, users)
 * - Visitor operations (book, view, edit, cancel appointments)
 * - Edge cases and error handling
 * - SonarCloud compliance (logging, encapsulation, null handling)
 */
class MainTest {

    // ================================================================
    //  Constants
    // ================================================================
    private static final String M_FRIENDLY       = "friendlyType";
    private static final String M_WITHIN24H      = "isWithin24h";
    private static final String M_SORT           = "sortGroup";
    private static final String M_FMT            = "fmtTime";
    private static final String M_READ_INT       = "readInt";
    private static final String M_READ_LONG      = "readLong";
    private static final String M_BANNER         = "printBanner";
    private static final String M_ADMIN_VIEW     = "adminViewAppointments";
    private static final String M_ADMIN_CANCEL   = "adminCancelAppointment";
    private static final String M_ADMIN_EDIT     = "adminEditAppointment";
    private static final String M_ADMIN_WORKDAY  = "adminAddWorkDay";
    private static final String M_ADMIN_SLOTS    = "adminViewDaySlots";
    private static final String M_ADMIN_ADD_SLOT = "adminAddSlot";
    private static final String M_ADMIN_ADD_USER = "adminAddUser";
    private static final String M_ADMIN_USERS    = "adminViewAllUsers";
    private static final String M_VIS_BOOK       = "visitorBook";
    private static final String M_VIS_MINE       = "visitorMyAppointments";
    private static final String M_VIS_EDIT       = "visitorEdit";
    private static final String M_VIS_CANCEL     = "visitorCancel";

    // Appointment type constants
    private static final String T_FIRST     = "FIRST_VISIT";
    private static final String T_FOLLOW    = "FOLLOW_UP";
    private static final String T_VIRTUAL   = "VIRTUAL";
    private static final String T_GRP_FIRST = "GROUP_FIRST_VISIT";
    private static final String T_GRP_FOLL  = "GROUP_FOLLOW_UP";
    private static final String T_GRP_VIRT  = "GROUP_VIRTUAL";
    private static final String UID         = "1";
    private static final long   LONG_UID    = 1L;

    // Logger for test diagnostics
    private static final Logger logger = Logger.getLogger(MainTest.class.getName());

    // ================================================================
    //  Test Fixtures
    // ================================================================
    private InputStream           originalIn;
    private PrintStream           originalOut;
    private ByteArrayOutputStream out;

    private AppointmentDAO apptDAO;
    private TimeSlotDAO    slotDAO;
    private ScheduleDAO    schedDAO;
    private UserDAO        userDAO;

    @BeforeEach
    void setUp() throws Exception {
        originalIn  = System.in;
        originalOut = System.out;
        out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        apptDAO  = mock(AppointmentDAO.class);
        slotDAO  = mock(TimeSlotDAO.class);
        schedDAO = mock(ScheduleDAO.class);
        userDAO  = mock(UserDAO.class);
    }

    @AfterEach
    void tearDown() throws Exception {
        System.setIn(originalIn);
        System.setOut(originalOut);
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error closing ByteArrayOutputStream", e);
            }
        }
    }

    // ================================================================
    //  Reflection Utilities
    // ================================================================

    /**
     * Invokes a private method on Main class using reflection.
     */
    private Object call(String method, Class<?>[] types, Object... args) throws Exception {
        Method m = main.Main.class.getDeclaredMethod(method, types);
        m.setAccessible(true);
        return m.invoke(null, args);
    }

    /**
     * Feeds input to Scanner for interactive tests.
     */
    private void feedInput(String input) throws Exception {
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        Scanner newScanner = new java.util.Scanner(System.in);
        // Replace scanner via reflection
        java.lang.reflect.Field field = main.Main.class.getDeclaredField("sc");
        field.setAccessible(true);
        Scanner oldScanner = (Scanner) field.get(null);
        if (oldScanner != null) {
            oldScanner.close();
        }
        field.set(null, newScanner);
    }

    /**
     * Gets captured console output.
     */
    private String output() {
        return out != null ? out.toString() : "";
    }

    // ================================================================
    //  Mock Helpers
    // ================================================================

    /**
     * Creates a mocked Schedule for a given number of days in the future.
     */
    private LocalDate setupSchedule(int plusDays) {
        LocalDate d = LocalDate.now().plusDays(plusDays);
        Schedule s = mock(Schedule.class);
        when(s.getWorkDate()).thenReturn(d);
        when(s.getId()).thenReturn(1L);
        try {
            when(schedDAO.getFutureSchedules()).thenReturn(Collections.singletonList(s));
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error setting up schedule mock", e);
            fail("Failed to setup schedule mock");
        }
        return d;
    }

    /**
     * Creates a mocked Appointment with standard properties.
     */
    private Appointment mockAppt(long id, String type, OffsetDateTime start) {
        Appointment a = mock(Appointment.class);
        when(a.getId()).thenReturn(id);
        when(a.getStartTime()).thenReturn(start);
        when(a.getEndTime()).thenReturn(start.plusMinutes(30));
        when(a.getType()).thenReturn(type);
        when(a.getCreatedBy()).thenReturn(LONG_UID);
        return a;
    }

    /**
     * Creates a confirmed Appointment.
     */
    private Appointment mockConfirmedAppt(long id, String type, OffsetDateTime start) {
        Appointment a = mockAppt(id, type, start);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);
        return a;
    }

    /**
     * Creates a mocked Visitor User.
     */
    private User mockVisitor() {
        User v = mock(User.class);
        when(v.getId()).thenReturn(UID);
        when(v.isLoggedIn()).thenReturn(false);
        return v;
    }

    /**
     * Creates a mocked TimeSlot.
     */
    private TimeSlot mockSlot(long id, OffsetDateTime start) {
        TimeSlot s = mock(TimeSlot.class);
        when(s.getId()).thenReturn(id);
        when(s.getStartTime()).thenReturn(start);
        return s;
    }

    // ================================================================
    //  UTILITY TESTS: friendlyType
    // ================================================================

    @ParameterizedTest
    @CsvSource({
        "FIRST_VISIT,       Individual – First Visit",
        "FOLLOW_UP,         Individual – Follow-up",
        "VIRTUAL,           Individual – Virtual",
        "GROUP_FIRST_VISIT, Group – First Visit",
        "GROUP_FOLLOW_UP,   Group – Follow-up",
        "GROUP_VIRTUAL,     Group – Virtual",
        "XYZ,               XYZ"
    })
    @DisplayName("friendlyType should handle all appointment types")
    void testFriendlyType_allTypes(String input, String expected) throws Exception {
        Object result = call(M_FRIENDLY, new Class[]{String.class}, input.trim());
        assertEquals(expected.trim(), result, "Expected friendly type for: " + input);
    }

    @Test
    @DisplayName("friendlyType should return 'Unknown' for null input")
    void testFriendlyType_null() throws Exception {
        Object result = call(M_FRIENDLY, new Class[]{String.class}, (Object) null);
        assertEquals("Unknown", result);
    }

    @Test
    @DisplayName("friendlyType should handle lowercase input")
    void testFriendlyType_lowercase() throws Exception {
        Object result = call(M_FRIENDLY, new Class[]{String.class}, "first_visit");
        assertEquals("Individual – First Visit", result);
    }

    @Test
    @DisplayName("friendlyType should handle empty string")
    void testFriendlyType_empty() throws Exception {
        Object result = call(M_FRIENDLY, new Class[]{String.class}, "");
        assertEquals("", result);
    }

    // ================================================================
    //  UTILITY TESTS: isWithin24h
    // ================================================================

    @Test
    @DisplayName("isWithin24h should return true for appointment in 1 hour")
    void testWithin24h_true_1h() throws Exception {
        boolean result = (boolean) call(M_WITHIN24H, new Class[]{OffsetDateTime.class},
            OffsetDateTime.now(ZoneOffset.UTC).plusHours(1));
        assertTrue(result);
    }

    @Test
    @DisplayName("isWithin24h should return true for appointment in 23 hours")
    void testWithin24h_true_23h() throws Exception {
        boolean result = (boolean) call(M_WITHIN24H, new Class[]{OffsetDateTime.class},
            OffsetDateTime.now(ZoneOffset.UTC).plusHours(23));
        assertTrue(result);
    }

    @Test
    @DisplayName("isWithin24h should return false for appointment in 25 hours")
    void testWithin24h_false_25h() throws Exception {
        boolean result = (boolean) call(M_WITHIN24H, new Class[]{OffsetDateTime.class},
            OffsetDateTime.now(ZoneOffset.UTC).plusHours(25));
        assertFalse(result);
    }

    @Test
    @DisplayName("isWithin24h should return true for past appointments")
    void testWithin24h_past() throws Exception {
        boolean result = (boolean) call(M_WITHIN24H, new Class[]{OffsetDateTime.class},
            OffsetDateTime.now(ZoneOffset.UTC).minusHours(5));
        assertTrue(result);
    }

    @Test
    @DisplayName("isWithin24h should return false for far future appointments")
    void testWithin24h_farFuture() throws Exception {
        boolean result = (boolean) call(M_WITHIN24H, new Class[]{OffsetDateTime.class},
            OffsetDateTime.now(ZoneOffset.UTC).plusDays(10));
        assertFalse(result);
    }

    @Test
    @DisplayName("isWithin24h should return true for edge case just inside")
    void testWithin24h_justInside() throws Exception {
        boolean result = (boolean) call(M_WITHIN24H, new Class[]{OffsetDateTime.class},
            OffsetDateTime.now(ZoneOffset.UTC).plusHours(23).plusMinutes(59));
        assertTrue(result);
    }

    @Test
    @DisplayName("isWithin24h should return false for edge case just outside")
    void testWithin24h_justOutside() throws Exception {
        boolean result = (boolean) call(M_WITHIN24H, new Class[]{OffsetDateTime.class},
            OffsetDateTime.now(ZoneOffset.UTC).plusHours(24).plusMinutes(1));
        assertFalse(result);
    }

    // ================================================================
    //  UTILITY TESTS: sortGroup
    // ================================================================

    @Test
    @DisplayName("sortGroup should return 0 for DONE appointments")
    void testSort_done() throws Exception {
        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn(Appointment.STATUS_DONE);
        int result = (int) call(M_SORT, new Class[]{Appointment.class}, a);
        assertEquals(0, result);
    }

    @Test
    @DisplayName("sortGroup should return 1 for CANCELED appointments")
    void testSort_canceled() throws Exception {
        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CANCELED);
        int result = (int) call(M_SORT, new Class[]{Appointment.class}, a);
        assertEquals(1, result);
    }

    @Test
    @DisplayName("sortGroup should return 2 for CONFIRMED appointments")
    void testSort_confirmed() throws Exception {
        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);
        int result = (int) call(M_SORT, new Class[]{Appointment.class}, a);
        assertEquals(2, result);
    }

    @Test
    @DisplayName("sortGroup should return 2 for null status")
    void testSort_nullStatus() throws Exception {
        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn(null);
        int result = (int) call(M_SORT, new Class[]{Appointment.class}, a);
        assertEquals(2, result);
    }

    @Test
    @DisplayName("sortGroup should maintain correct order: DONE < CANCELED < CONFIRMED")
    void testSort_order() throws Exception {
        Appointment done = mock(Appointment.class);
        when(done.getStatus()).thenReturn(Appointment.STATUS_DONE);

        Appointment canceled = mock(Appointment.class);
        when(canceled.getStatus()).thenReturn(Appointment.STATUS_CANCELED);

        Appointment confirmed = mock(Appointment.class);
        when(confirmed.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);

        int gd = (int) call(M_SORT, new Class[]{Appointment.class}, done);
        int gc = (int) call(M_SORT, new Class[]{Appointment.class}, canceled);
        int gf = (int) call(M_SORT, new Class[]{Appointment.class}, confirmed);

        assertTrue(gd < gc && gc < gf, "Sort order must be: DONE < CANCELED < CONFIRMED");
    }

    // ================================================================
    //  UTILITY TESTS: fmtTime
    // ================================================================

    @Test
    @DisplayName("fmtTime should return 'N/A' for null input")
    void testFmt_null() throws Exception {
        Object result = call(M_FMT, new Class[]{OffsetDateTime.class}, new Object[]{null});
        assertEquals("N/A", result);
    }

    @Test
    @DisplayName("fmtTime should return non-empty string for valid time")
    void testFmt_nonNull() throws Exception {
        String result = (String) call(M_FMT, new Class[]{OffsetDateTime.class},
            OffsetDateTime.now(ZoneOffset.UTC));
        assertNotNull(result);
        assertFalse(result.trim().isEmpty());
    }

    @Test
    @DisplayName("fmtTime should contain day name")
    void testFmt_containsDayName() throws Exception {
        String result = (String) call(M_FMT, new Class[]{OffsetDateTime.class},
            OffsetDateTime.now(ZoneOffset.UTC));
        assertTrue(result.matches("(?i).*(Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday).*"),
            "Time format should include day name");
    }

    @Test
    @DisplayName("fmtTime should format known date correctly")
    void testFmt_knownDate() throws Exception {
        String result = (String) call(M_FMT, new Class[]{OffsetDateTime.class},
            OffsetDateTime.of(2026, 4, 6, 0, 0, 0, 0, ZoneOffset.UTC));
        assertTrue(result.contains("2026") && result.contains("Apr"),
            "Expected year 2026 and month Apr in formatted time");
    }

    @Test
    @DisplayName("fmtTime should include time in HH:mm format")
    void testFmt_containsTime() throws Exception {
        String result = (String) call(M_FMT, new Class[]{OffsetDateTime.class},
            OffsetDateTime.of(2026, 1, 1, 10, 30, 0, 0, ZoneOffset.UTC));
        assertTrue(result.matches(".*\\d{2}:\\d{2}.*"),
            "Expected time in HH:mm format");
    }

    // ================================================================
    //  UTILITY TESTS: readInt
    // ================================================================

    @Test
    @DisplayName("readInt should accept valid input within range")
    void testReadInt_valid() throws Exception {
        feedInput("3\n");
        int result = (int) call(M_READ_INT, new Class[]{int.class, int.class}, 1, 5);
        assertEquals(3, result);
    }

    @Test
    @DisplayName("readInt should accept minimum value")
    void testReadInt_min() throws Exception {
        feedInput("1\n");
        int result = (int) call(M_READ_INT, new Class[]{int.class, int.class}, 1, 5);
        assertEquals(1, result);
    }

    @Test
    @DisplayName("readInt should accept maximum value")
    void testReadInt_max() throws Exception {
        feedInput("5\n");
        int result = (int) call(M_READ_INT, new Class[]{int.class, int.class}, 1, 5);
        assertEquals(5, result);
    }

    @Test
    @DisplayName("readInt should reject value below minimum")
    void testReadInt_below() throws Exception {
        feedInput("0\n");
        int result = (int) call(M_READ_INT, new Class[]{int.class, int.class}, 1, 5);
        assertEquals(-1, result);
        assertTrue(output().contains("Invalid input"));
    }

    @Test
    @DisplayName("readInt should reject value above maximum")
    void testReadInt_above() throws Exception {
        feedInput("99\n");
        int result = (int) call(M_READ_INT, new Class[]{int.class, int.class}, 1, 5);
        assertEquals(-1, result);
        assertTrue(output().contains("Invalid input"));
    }

    @Test
    @DisplayName("readInt should reject non-numeric input")
    void testReadInt_text() throws Exception {
        feedInput("abc\n");
        int result = (int) call(M_READ_INT, new Class[]{int.class, int.class}, 1, 5);
        assertEquals(-1, result);
        assertTrue(output().contains("Invalid input"));
    }

    @Test
    @DisplayName("readInt should reject empty input")
    void testReadInt_empty() throws Exception {
        feedInput("\n");
        int result = (int) call(M_READ_INT, new Class[]{int.class, int.class}, 1, 5);
        assertEquals(-1, result);
    }

    @Test
    @DisplayName("readInt should reject negative values outside range")
    void testReadInt_negative() throws Exception {
        feedInput("-1\n");
        int result = (int) call(M_READ_INT, new Class[]{int.class, int.class}, 1, 5);
        assertEquals(-1, result);
    }

    @Test
    @DisplayName("readInt should reject decimal input")
    void testReadInt_float() throws Exception {
        feedInput("2.5\n");
        int result = (int) call(M_READ_INT, new Class[]{int.class, int.class}, 1, 5);
        assertEquals(-1, result);
    }

    @Test
    @DisplayName("readInt should trim whitespace from input")
    void testReadInt_spaces() throws Exception {
        feedInput("  3  \n");
        int result = (int) call(M_READ_INT, new Class[]{int.class, int.class}, 1, 5);
        assertEquals(3, result);
    }

    // ================================================================
    //  UTILITY TESTS: readLong
    // ================================================================

    @Test
    @DisplayName("readLong should accept valid input")
    void testReadLong_valid() throws Exception {
        feedInput("42\n");
        long result = (long) call(M_READ_LONG, new Class[0]);
        assertEquals(42L, result);
    }

    @Test
    @DisplayName("readLong should reject non-numeric input")
    void testReadLong_text() throws Exception {
        feedInput("xyz\n");
        long result = (long) call(M_READ_LONG, new Class[0]);
        assertEquals(-1L, result);
        assertTrue(output().contains("Invalid ID"));
    }

    @Test
    @DisplayName("readLong should accept negative values")
    void testReadLong_negative() throws Exception {
        feedInput("-5\n");
        long result = (long) call(M_READ_LONG, new Class[0]);
        assertEquals(-5L, result);
    }

    @Test
    @DisplayName("readLong should handle large numbers")
    void testReadLong_large() throws Exception {
        feedInput("9999999999\n");
        long result = (long) call(M_READ_LONG, new Class[0]);
        assertEquals(9999999999L, result);
    }

    @Test
    @DisplayName("readLong should reject empty input")
    void testReadLong_empty() throws Exception {
        feedInput("\n");
        long result = (long) call(M_READ_LONG, new Class[0]);
        assertEquals(-1L, result);
    }

    @Test
    @DisplayName("readLong should accept zero")
    void testReadLong_zero() throws Exception {
        feedInput("0\n");
        long result = (long) call(M_READ_LONG, new Class[0]);
        assertEquals(0L, result);
    }

    @Test
    @DisplayName("readLong should trim whitespace")
    void testReadLong_spaces() throws Exception {
        feedInput("  7  \n");
        long result = (long) call(M_READ_LONG, new Class[0]);
        assertEquals(7L, result);
    }

    // ================================================================
    //  UTILITY TESTS: printBanner
    // ================================================================

    @Test
    @DisplayName("printBanner should display system title")
    void testPrintBanner() throws Exception {
        call(M_BANNER, new Class[0]);
        assertTrue(output().contains("Appointment Scheduling System"));
    }

    // ================================================================
    //  ADMIN TESTS: View Appointments
    // ================================================================

    private static final Class<?>[] VIEW_APPT_TYPES =
        {AppointmentDAO.class, ScheduleDAO.class, UserDAO.class};

    @Test
    @DisplayName("adminViewAppointments should show message when no work days exist")
    void testAdminView_noWorkDays() throws Exception {
        when(schedDAO.getFutureSchedules()).thenReturn(Collections.emptyList());
        call(M_ADMIN_VIEW, VIEW_APPT_TYPES, apptDAO, schedDAO, userDAO);
        assertTrue(output().contains("No work days in system"));
    }

    @Test
    @DisplayName("adminViewAppointments should handle invalid day selection")
    void testAdminView_invalidChoice() throws Exception {
        setupSchedule(1);
        feedInput("99\n");
        call(M_ADMIN_VIEW, VIEW_APPT_TYPES, apptDAO, schedDAO, userDAO);
        assertTrue(output().contains("Invalid input"));
    }

    @Test
    @DisplayName("adminViewAppointments should show message when no appointments exist")
    void testAdminView_noAppointments() throws Exception {
        LocalDate d = setupSchedule(1);
        when(apptDAO.getActiveAppointmentsByDate(d)).thenReturn(Collections.emptyList());
        feedInput("1\n");
        call(M_ADMIN_VIEW, VIEW_APPT_TYPES, apptDAO, schedDAO, userDAO);
        assertTrue(output().contains("No active appointments"));
    }

    @Test
    @DisplayName("adminViewAppointments should display appointment details")
    void testAdminView_withAppointments() throws Exception {
        LocalDate d = setupSchedule(1);
        Appointment a = mockAppt(1L, T_FIRST, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(apptDAO.getActiveAppointmentsByDate(d)).thenReturn(Collections.singletonList(a));
        when(userDAO.getUserById(UID)).thenReturn(Optional.empty());
        feedInput("1\n");
        call(M_ADMIN_VIEW, VIEW_APPT_TYPES, apptDAO, schedDAO, userDAO);
        assertTrue(output().contains("Individual – First Visit"));
    }

    @Test
    @DisplayName("adminViewAppointments should display creator username")
    void testAdminView_showsUsername() throws Exception {
        LocalDate d = setupSchedule(1);
        Appointment a = mockAppt(1L, T_FOLLOW, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        User u = mock(User.class);
        when(u.getUsername()).thenReturn("testuser");
        when(apptDAO.getActiveAppointmentsByDate(d)).thenReturn(Collections.singletonList(a));
        when(userDAO.getUserById(UID)).thenReturn(Optional.of(u));
        feedInput("1\n");
        call(M_ADMIN_VIEW, VIEW_APPT_TYPES, apptDAO, schedDAO, userDAO);
        assertTrue(output().contains("testuser"));
    }

    // ================================================================
    //  ADMIN TESTS: Cancel Appointment
    // ================================================================

    private static final Class<?>[] CANCEL_TYPES =
        {AppointmentDAO.class, TimeSlotDAO.class, ScheduleDAO.class, UserDAO.class};

    @Test
    @DisplayName("adminCancelAppointment should show message when no future work days exist")
    void testAdminCancel_noWorkDays() throws Exception {
        when(schedDAO.getFutureSchedules()).thenReturn(Collections.emptyList());
        call(M_ADMIN_CANCEL, CANCEL_TYPES, apptDAO, slotDAO, schedDAO, userDAO);
        assertTrue(output().contains("No future work days"));
    }

    @Test
    @DisplayName("adminCancelAppointment should handle invalid day input")
    void testAdminCancel_invalidDay() throws Exception {
        setupSchedule(1);
        feedInput("abc\n");
        call(M_ADMIN_CANCEL, CANCEL_TYPES, apptDAO, slotDAO, schedDAO, userDAO);
        assertTrue(output().contains("Invalid input"));
    }

    @Test
    @DisplayName("adminCancelAppointment should show message when no future appointments exist")
    void testAdminCancel_noAppointments() throws Exception {
        LocalDate d = setupSchedule(1);
        when(apptDAO.getFutureAppointmentsByDate(d)).thenReturn(Collections.emptyList());
        feedInput("1\n");
        call(M_ADMIN_CANCEL, CANCEL_TYPES, apptDAO, slotDAO, schedDAO, userDAO);
        assertTrue(output().contains("No future appointments on that day"));
    }

    @Test
    @DisplayName("adminCancelAppointment should handle appointment not found")
    void testAdminCancel_notFound() throws Exception {
        LocalDate d = setupSchedule(1);
        Appointment a = mockAppt(10L, T_FOLLOW, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(apptDAO.getFutureAppointmentsByDate(d)).thenReturn(Collections.singletonList(a));
        when(userDAO.getUserById(anyString())).thenReturn(Optional.empty());
        when(apptDAO.getAppointmentById(99L)).thenReturn(null);
        feedInput("1\n99\n");
        call(M_ADMIN_CANCEL, CANCEL_TYPES, apptDAO, slotDAO, schedDAO, userDAO);
        assertTrue(output().contains("Appointment not found or already past"));
    }

    @Test
    @DisplayName("adminCancelAppointment should cancel appointment without slot")
    void testAdminCancel_success_noSlot() throws Exception {
        LocalDate d = setupSchedule(1);
        OffsetDateTime future = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2);
        Appointment a = mockAppt(5L, T_FIRST, future);
        when(a.getSlotId()).thenReturn(null);
        when(apptDAO.getFutureAppointmentsByDate(d)).thenReturn(Collections.singletonList(a));
        when(apptDAO.getAppointmentById(5L)).thenReturn(a);
        when(userDAO.getUserById(anyString())).thenReturn(Optional.empty());
        feedInput("1\n5\nTest reason\n");
        call(M_ADMIN_CANCEL, CANCEL_TYPES, apptDAO, slotDAO, schedDAO, userDAO);
        assertTrue(output().contains("Appointment canceled"));
        verify(apptDAO).cancelByAdmin(5L, "Test reason");
    }

    @Test
    @DisplayName("adminCancelAppointment should release slot when canceling")
    void testAdminCancel_success_withSlot() throws Exception {
        LocalDate d = setupSchedule(1);
        OffsetDateTime future = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2);
        Appointment a = mockAppt(5L, T_FIRST, future);
        when(a.getSlotId()).thenReturn(10L);
        when(apptDAO.getFutureAppointmentsByDate(d)).thenReturn(Collections.singletonList(a));
        when(apptDAO.getAppointmentById(5L)).thenReturn(a);
        when(userDAO.getUserById(anyString())).thenReturn(Optional.empty());
        feedInput("1\n5\nReason\n");
        call(M_ADMIN_CANCEL, CANCEL_TYPES, apptDAO, slotDAO, schedDAO, userDAO);
        verify(slotDAO).updateAvailability(10L, true);
    }

    @Test
    @DisplayName("adminCancelAppointment should handle empty note as null")
    void testAdminCancel_emptyNote() throws Exception {
        LocalDate d = setupSchedule(1);
        OffsetDateTime future = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2);
        Appointment a = mockAppt(5L, T_FIRST, future);
        when(a.getSlotId()).thenReturn(null);
        when(apptDAO.getFutureAppointmentsByDate(d)).thenReturn(Collections.singletonList(a));
        when(apptDAO.getAppointmentById(5L)).thenReturn(a);
        when(userDAO.getUserById(anyString())).thenReturn(Optional.empty());
        feedInput("1\n5\n\n");
        call(M_ADMIN_CANCEL, CANCEL_TYPES, apptDAO, slotDAO, schedDAO, userDAO);
        verify(apptDAO).cancelByAdmin(5L, null);
    }

    // ================================================================
    //  ADMIN TESTS: Edit Appointment
    // ================================================================

    private static final Class<?>[] EDIT_TYPES =
        {AppointmentDAO.class, ScheduleDAO.class, UserDAO.class};

    @Test
    @DisplayName("adminEditAppointment should show message when no future work days exist")
    void testAdminEdit_noWorkDays() throws Exception {
        when(schedDAO.getFutureSchedules()).thenReturn(Collections.emptyList());
        call(M_ADMIN_EDIT, EDIT_TYPES, apptDAO, schedDAO, userDAO);
        assertTrue(output().contains("No future work days"));
    }

    @Test
    @DisplayName("adminEditAppointment should show message when no future appointments exist")
    void testAdminEdit_noAppointments() throws Exception {
        LocalDate d = setupSchedule(1);
        when(apptDAO.getFutureAppointmentsByDate(d)).thenReturn(Collections.emptyList());
        feedInput("1\n");
        call(M_ADMIN_EDIT, EDIT_TYPES, apptDAO, schedDAO, userDAO);
        assertTrue(output().contains("No future appointments on that day"));
    }

    @Test
    @DisplayName("adminEditAppointment should handle invalid edit choice")
    void testAdminEdit_invalidChoice() throws Exception {
        LocalDate d = setupSchedule(1);
        OffsetDateTime future = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2);
        Appointment a = mockAppt(1L, T_FIRST, future);
        when(apptDAO.getFutureAppointmentsByDate(d)).thenReturn(Collections.singletonList(a));
        when(apptDAO.getAppointmentById(1L)).thenReturn(a);
        when(userDAO.getUserById(anyString())).thenReturn(Optional.empty());
        feedInput("1\n1\n9\n");
        call(M_ADMIN_EDIT, EDIT_TYPES, apptDAO, schedDAO, userDAO);
        assertTrue(output().contains("Invalid choice"));
    }

    @Test
    @DisplayName("adminEditAppointment should change type to individual first visit")
    void testAdminEdit_changeType_individual_firstVisit() throws Exception {
        LocalDate d = setupSchedule(1);
        OffsetDateTime future = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2);
        Appointment a = mockAppt(1L, T_FOLLOW, future);
        when(apptDAO.getFutureAppointmentsByDate(d)).thenReturn(Collections.singletonList(a));
        when(apptDAO.getAppointmentById(1L)).thenReturn(a);
        when(userDAO.getUserById(anyString())).thenReturn(Optional.empty());
        feedInput("1\n1\n1\n1\n1\n\n");
        call(M_ADMIN_EDIT, EDIT_TYPES, apptDAO, schedDAO, userDAO);
        assertTrue(output().contains("Appointment updated"));
        verify(apptDAO).updateTypeAndNote(eq(1L), eq(Appointment.TYPE_FIRST_VISIT), isNull());
    }

    @Test
    @DisplayName("adminEditAppointment should change type to group virtual")
    void testAdminEdit_changeType_group_virtual() throws Exception {
        LocalDate d = setupSchedule(1);
        OffsetDateTime future = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2);
        Appointment a = mockAppt(1L, T_FIRST, future);
        when(apptDAO.getFutureAppointmentsByDate(d)).thenReturn(Collections.singletonList(a));
        when(apptDAO.getAppointmentById(1L)).thenReturn(a);
        when(userDAO.getUserById(anyString())).thenReturn(Optional.empty());
        feedInput("1\n1\n1\n2\n3\n\n");
        call(M_ADMIN_EDIT, EDIT_TYPES, apptDAO, schedDAO, userDAO);
        assertTrue(output().contains("Appointment updated"));
        verify(apptDAO).updateTypeAndNote(eq(1L), eq(Appointment.TYPE_GROUP_VIRTUAL), isNull());
    }

    @Test
    @DisplayName("adminEditAppointment should change type to individual follow-up")
    void testAdminEdit_changeType_individual_followUp() throws Exception {
        LocalDate d = setupSchedule(1);
        OffsetDateTime future = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2);
        Appointment a = mockAppt(1L, T_FIRST, future);
        when(apptDAO.getFutureAppointmentsByDate(d)).thenReturn(Collections.singletonList(a));
        when(apptDAO.getAppointmentById(1L)).thenReturn(a);
        when(userDAO.getUserById(anyString())).thenReturn(Optional.empty());
        feedInput("1\n1\n1\n1\n2\n\n");
        call(M_ADMIN_EDIT, EDIT_TYPES, apptDAO, schedDAO, userDAO);
        verify(apptDAO).updateTypeAndNote(eq(1L), eq(Appointment.TYPE_FOLLOW_UP), isNull());
    }

    @Test
    @DisplayName("adminEditAppointment should change type to individual virtual")
    void testAdminEdit_changeType_individual_virtual() throws Exception {
        LocalDate d = setupSchedule(1);
        OffsetDateTime future = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2);
        Appointment a = mockAppt(1L, T_FIRST, future);
        when(apptDAO.getFutureAppointmentsByDate(d)).thenReturn(Collections.singletonList(a));
        when(apptDAO.getAppointmentById(1L)).thenReturn(a);
        when(userDAO.getUserById(anyString())).thenReturn(Optional.empty());
        feedInput("1\n1\n1\n1\n3\nnote\n");
        call(M_ADMIN_EDIT, EDIT_TYPES, apptDAO, schedDAO, userDAO);
        verify(apptDAO).updateTypeAndNote(eq(1L), eq(Appointment.TYPE_VIRTUAL), eq("note"));
    }

    @Test
    @DisplayName("adminEditAppointment should change type to group first visit")
    void testAdminEdit_changeType_group_firstVisit() throws Exception {
        LocalDate d = setupSchedule(1);
        OffsetDateTime future = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2);
        Appointment a = mockAppt(1L, T_FIRST, future);
        when(apptDAO.getFutureAppointmentsByDate(d)).thenReturn(Collections.singletonList(a));
        when(apptDAO.getAppointmentById(1L)).thenReturn(a);
        when(userDAO.getUserById(anyString())).thenReturn(Optional.empty());
        feedInput("1\n1\n1\n2\n1\n\n");
        call(M_ADMIN_EDIT, EDIT_TYPES, apptDAO, schedDAO, userDAO);
        verify(apptDAO).updateTypeAndNote(eq(1L), eq(Appointment.TYPE_GROUP_FIRST_VISIT), isNull());
    }

    @Test
    @DisplayName("adminEditAppointment should change type to group follow-up")
    void testAdminEdit_changeType_group_followUp() throws Exception {
        LocalDate d = setupSchedule(1);
        OffsetDateTime future = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2);
        Appointment a = mockAppt(1L, T_FIRST, future);
        when(apptDAO.getFutureAppointmentsByDate(d)).thenReturn(Collections.singletonList(a));
        when(apptDAO.getAppointmentById(1L)).thenReturn(a);
        when(userDAO.getUserById(anyString())).thenReturn(Optional.empty());
        feedInput("1\n1\n1\n2\n2\n\n");
        call(M_ADMIN_EDIT, EDIT_TYPES, apptDAO, schedDAO, userDAO);
        verify(apptDAO).updateTypeAndNote(eq(1L), eq(Appointment.TYPE_GROUP_FOLLOW_UP), isNull());
    }

    @Test
    @DisplayName("adminEditAppointment should reject participant count change for non-group appointments")
    void testAdminEdit_changeCount_notGroup() throws Exception {
        LocalDate d = setupSchedule(1);
        OffsetDateTime future = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2);
        Appointment a = mockAppt(1L, T_FIRST, future);
        when(a.isGroup()).thenReturn(false);
        when(apptDAO.getFutureAppointmentsByDate(d)).thenReturn(Collections.singletonList(a));
        when(apptDAO.getAppointmentById(1L)).thenReturn(a);
        when(userDAO.getUserById(anyString())).thenReturn(Optional.empty());
        feedInput("1\n1\n2\n");
        call(M_ADMIN_EDIT, EDIT_TYPES, apptDAO, schedDAO, userDAO);
        assertTrue(output().contains("Participant count is only for Group"));
    }

    @Test
    @DisplayName("adminEditAppointment should update participant count for group appointments")
    void testAdminEdit_changeCount_success() throws Exception {
        LocalDate d = setupSchedule(1);
        OffsetDateTime future = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2);
        Appointment a = mockAppt(1L, T_GRP_FIRST, future);
        when(a.isGroup()).thenReturn(true);
        when(apptDAO.getFutureAppointmentsByDate(d)).thenReturn(Collections.singletonList(a));
        when(apptDAO.getAppointmentById(1L)).thenReturn(a);
        when(userDAO.getUserById(anyString())).thenReturn(Optional.empty());
        feedInput("1\n1\n2\n3\n\n");
        call(M_ADMIN_EDIT, EDIT_TYPES, apptDAO, schedDAO, userDAO);
        assertTrue(output().contains("Appointment updated"));
        verify(apptDAO).updateParticipantsAndNote(eq(1L), eq(3), isNull());
    }

    // ================================================================
    //  ADMIN TESTS: Add Work Day
    // ================================================================

    private static final Class<?>[] WORKDAY_TYPES = {ScheduleDAO.class};

    @Test
    @DisplayName("adminAddWorkDay should reject invalid date format")
    void testWorkday_invalidFormat() throws Exception {
        when(schedDAO.getFutureSchedules()).thenReturn(Collections.emptyList());
        feedInput("not-a-date\n");
        call(M_ADMIN_WORKDAY, WORKDAY_TYPES, schedDAO);
        assertTrue(output().contains("Invalid format"));
    }

    @Test
    @DisplayName("adminAddWorkDay should reject past dates")
    void testWorkday_pastDate() throws Exception {
        when(schedDAO.getFutureSchedules()).thenReturn(Collections.emptyList());
        feedInput("2020-01-01\n");
        call(M_ADMIN_WORKDAY, WORKDAY_TYPES, schedDAO);
        assertTrue(output().contains("Date must be today or in the future"));
    }

    @Test
    @DisplayName("adminAddWorkDay should reject duplicate dates")
    void testWorkday_alreadyExists() throws Exception {
        when(schedDAO.getFutureSchedules()).thenReturn(Collections.emptyList());
        LocalDate future = LocalDate.now().plusDays(5);
        when(schedDAO.existsByDate(future)).thenReturn(true);
        feedInput(future + "\n");
        call(M_ADMIN_WORKDAY, WORKDAY_TYPES, schedDAO);
        assertTrue(output().contains("already exists"));
    }

    @Test
    @DisplayName("adminAddWorkDay should add valid work day")
    void testWorkday_success() throws Exception {
        when(schedDAO.getFutureSchedules()).thenReturn(Collections.emptyList());
        LocalDate future = LocalDate.now().plusDays(3);
        when(schedDAO.existsByDate(future)).thenReturn(false);
        feedInput(future + "\n");
        call(M_ADMIN_WORKDAY, WORKDAY_TYPES, schedDAO);
        assertTrue(output().contains("Work day added successfully"));
        verify(schedDAO).addSchedule(any(Schedule.class));
    }

    @Test
    @DisplayName("adminAddWorkDay should display existing work days")
    void testWorkday_showsExistingDays() throws Exception {
        Schedule s = mock(Schedule.class);
        when(s.getWorkDate()).thenReturn(LocalDate.now().plusDays(1));
        when(schedDAO.getFutureSchedules()).thenReturn(Collections.singletonList(s));
        LocalDate future = LocalDate.now().plusDays(3);
        when(schedDAO.existsByDate(future)).thenReturn(false);
        feedInput(future + "\n");
        call(M_ADMIN_WORKDAY, WORKDAY_TYPES, schedDAO);
        assertTrue(output().contains("Work day added successfully"));
    }

    // ================================================================
    //  ADMIN TESTS: View Day Slots
    // ================================================================

    private static final Class<?>[] SLOTS_TYPES = {TimeSlotDAO.class, ScheduleDAO.class};

    @Test
    @DisplayName("adminViewDaySlots should show message when no work days exist")
    void testSlots_noWorkDays() throws Exception {
        when(schedDAO.getFutureSchedules()).thenReturn(Collections.emptyList());
        call(M_ADMIN_SLOTS, SLOTS_TYPES, slotDAO, schedDAO);
        assertTrue(output().contains("No work days"));
    }

    @Test
    @DisplayName("adminViewDaySlots should show message when no slots exist for day")
    void testSlots_noSlots() throws Exception {
        setupSchedule(1);
        when(slotDAO.getAllSlotsBySchedule(1L)).thenReturn(Collections.emptyList());
        feedInput("1\n");
        call(M_ADMIN_SLOTS, SLOTS_TYPES, slotDAO, schedDAO);
        assertTrue(output().contains("No slots for that day"));
    }

    @Test
    @DisplayName("adminViewDaySlots should display available slots")
    void testSlots_available() throws Exception {
        setupSchedule(1);
        TimeSlot slot = mockSlot(5L, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(slot.isAvailable()).thenReturn(true);
        when(slotDAO.getAllSlotsBySchedule(1L)).thenReturn(Collections.singletonList(slot));
        feedInput("1\n");
        call(M_ADMIN_SLOTS, SLOTS_TYPES, slotDAO, schedDAO);
        assertTrue(output().contains("Available"));
    }

    @Test
    @DisplayName("adminViewDaySlots should display booked slots with username")
    void testSlots_booked_withUsername() throws Exception {
        setupSchedule(1);
        TimeSlot slot = mockSlot(5L, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(slot.isAvailable()).thenReturn(false);
        when(slotDAO.getAllSlotsBySchedule(1L)).thenReturn(Collections.singletonList(slot));
        when(slotDAO.getBookedByUsername(5L)).thenReturn("testuser");
        feedInput("1\n");
        call(M_ADMIN_SLOTS, SLOTS_TYPES, slotDAO, schedDAO);
        assertTrue(output().contains("Booked"));
        assertTrue(output().contains("testuser"));
    }

    @Test
    @DisplayName("adminViewDaySlots should display booked slots without username")
    void testSlots_booked_noUsername() throws Exception {
        setupSchedule(1);
        TimeSlot slot = mockSlot(5L, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(slot.isAvailable()).thenReturn(false);
        when(slotDAO.getAllSlotsBySchedule(1L)).thenReturn(Collections.singletonList(slot));
        when(slotDAO.getBookedByUsername(5L)).thenReturn(null);
        feedInput("1\n");
        call(M_ADMIN_SLOTS, SLOTS_TYPES, slotDAO, schedDAO);
        assertTrue(output().contains("Booked"));
    }

    // ================================================================
    //  ADMIN TESTS: Add Slot
    // ================================================================

    private static final Class<?>[] ADD_SLOT_TYPES = {TimeSlotDAO.class, ScheduleDAO.class};

    @Test
    @DisplayName("adminAddSlot should show message when no work days exist")
    void testAddSlot_noWorkDays() throws Exception {
        when(schedDAO.getFutureSchedules()).thenReturn(Collections.emptyList());
        call(M_ADMIN_ADD_SLOT, ADD_SLOT_TYPES, slotDAO, schedDAO);
        assertTrue(output().contains("No work days"));
    }

    @Test
    @DisplayName("adminAddSlot should reject invalid hour")
    void testAddSlot_invalidHour() throws Exception {
        setupSchedule(1);
        when(slotDAO.getAllSlotsBySchedule(1L)).thenReturn(Collections.emptyList());
        feedInput("1\n99\n");
        call(M_ADMIN_ADD_SLOT, ADD_SLOT_TYPES, slotDAO, schedDAO);
        assertTrue(output().contains("Invalid input"));
    }

    @Test
    @DisplayName("adminAddSlot should reject duplicate slot")
    void testAddSlot_alreadyExists() throws Exception {
        setupSchedule(1);
        when(slotDAO.getAllSlotsBySchedule(1L)).thenReturn(Collections.emptyList());
        when(slotDAO.existsByScheduleAndStart(anyLong(), any())).thenReturn(true);
        feedInput("1\n9\n");
        call(M_ADMIN_ADD_SLOT, ADD_SLOT_TYPES, slotDAO, schedDAO);
        assertTrue(output().contains("already exists"));
    }

    @Test
    @DisplayName("adminAddSlot should add valid slot")
    void testAddSlot_success() throws Exception {
        setupSchedule(1);
        when(slotDAO.getAllSlotsBySchedule(1L)).thenReturn(Collections.emptyList());
        when(slotDAO.existsByScheduleAndStart(anyLong(), any())).thenReturn(false);
        feedInput("1\n10\n");
        call(M_ADMIN_ADD_SLOT, ADD_SLOT_TYPES, slotDAO, schedDAO);
        assertTrue(output().contains("Slot added"));
        verify(slotDAO).addTimeSlot(any(TimeSlot.class));
    }

    @Test
    @DisplayName("adminAddSlot should display existing slots")
    void testAddSlot_showsExistingSlots() throws Exception {
        setupSchedule(1);
        TimeSlot existing = mockSlot(1L, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(slotDAO.getAllSlotsBySchedule(1L)).thenReturn(Collections.singletonList(existing));
        when(slotDAO.existsByScheduleAndStart(anyLong(), any())).thenReturn(false);
        feedInput("1\n14\n");
        call(M_ADMIN_ADD_SLOT, ADD_SLOT_TYPES, slotDAO, schedDAO);
        assertTrue(output().contains("Slot added"));
    }

    // ================================================================
    //  ADMIN TESTS: Add User (NEW)
    // ================================================================

    private static final Class<?>[] ADD_USER_TYPES = {UserDAO.class};

    @Test
    @DisplayName("adminAddUser should add new visitor user")
    void testAdminAddUser_visitor() throws Exception {
        feedInput("John Doe\njohn@test.com\n0501234567\njohnuser\npassword123\n2\n");
        call("adminAddUser", ADD_USER_TYPES, userDAO);
        assertTrue(output().contains("User registered successfully"));
        verify(userDAO).addUser(any(User.class), eq("password123"));
    }

    @Test
    @DisplayName("adminAddUser should add new admin user")
    void testAdminAddUser_admin() throws Exception {
        feedInput("Admin User\nadmin@test.com\n0509999999\nadminuser\nadminpass\n1\n");
        call("adminAddUser", ADD_USER_TYPES, userDAO);
        assertTrue(output().contains("User registered successfully"));
        verify(userDAO).addUser(any(User.class), eq("adminpass"));
    }

    // ================================================================
    //  ADMIN TESTS: View All Users (IMPROVED)
    // ================================================================

    private static final Class<?>[] VIEW_USERS_TYPES = {UserDAO.class};

    @Test
    @DisplayName("adminViewAllUsers should display empty list message")
    void testViewUsers_empty() throws Exception {
        when(userDAO.getAllUsers()).thenReturn(Collections.emptyList());
        call("adminViewAllUsers", VIEW_USERS_TYPES, userDAO);
        assertTrue(output().contains("All Users"));
    }

    @Test
    @DisplayName("adminViewAllUsers should display all user data")
    void testViewUsers_withData() throws Exception {
        User u = mock(User.class);
        when(u.getId()).thenReturn(UID);
        when(u.getName()).thenReturn("Test User");
        when(u.getEmail()).thenReturn("test@test.com");
        when(u.getPhoneNumber()).thenReturn("0501234567");
        when(u.getRole()).thenReturn(User.Role.VISITOR);
        when(userDAO.getAllUsers()).thenReturn(Collections.singletonList(u));
        call("adminViewAllUsers", VIEW_USERS_TYPES, userDAO);
        assertTrue(output().contains("Test User"));
        assertTrue(output().contains("test@test.com"));
        assertTrue(output().contains("VISITOR"));
    }

    @Test
    @DisplayName("adminViewAllUsers should display admin role correctly")
    void testViewUsers_adminRole() throws Exception {
        User u = mock(User.class);
        when(u.getId()).thenReturn("2");
        when(u.getName()).thenReturn("Admin Name");
        when(u.getEmail()).thenReturn("admin@test.com");
        when(u.getPhoneNumber()).thenReturn("0509999999");
        when(u.getRole()).thenReturn(User.Role.ADMIN);
        when(userDAO.getAllUsers()).thenReturn(Collections.singletonList(u));
        call("adminViewAllUsers", VIEW_USERS_TYPES, userDAO);
        assertTrue(output().contains("Admin Name"));
        assertTrue(output().contains("ADMIN"));
    }

    // ================================================================
    //  VISITOR TESTS: Book Appointment
    // ================================================================

    private static final Class<?>[] BOOK_TYPES =
        {User.class, AppointmentDAO.class, TimeSlotDAO.class, ScheduleDAO.class};

    @Test
    @DisplayName("visitorBook should show message when no work days exist")
    void testBook_noWorkDays() throws Exception {
        User v = mockVisitor();
        when(schedDAO.getFutureSchedules()).thenReturn(Collections.emptyList());
        call(M_VIS_BOOK, BOOK_TYPES, v, apptDAO, slotDAO, schedDAO);
        assertTrue(output().contains("No available work days"));
    }

    @Test
    @DisplayName("visitorBook should show message when no slots available")
    void testBook_noSlots() throws Exception {
        User v = mockVisitor();
        setupSchedule(1);
        when(slotDAO.getAvailableSlotsByScheduleId(1L)).thenReturn(Collections.emptyList());
        feedInput("1\n");
        call(M_VIS_BOOK, BOOK_TYPES, v, apptDAO, slotDAO, schedDAO);
        assertTrue(output().contains("No available slots"));
    }

    @Test
    @DisplayName("visitorBook should reject invalid category")
    void testBook_invalidCategory() throws Exception {
        User v = mockVisitor();
        setupSchedule(1);
        TimeSlot slot = mockSlot(5L, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(slotDAO.getAvailableSlotsByScheduleId(1L)).thenReturn(Collections.singletonList(slot));
        feedInput("1\n1\n30\n9\n");
        call(M_VIS_BOOK, BOOK_TYPES, v, apptDAO, slotDAO, schedDAO);
        assertTrue(output().contains("Invalid category"));
    }

    @Test
    @DisplayName("visitorBook should book individual first visit appointment")
    void testBook_individual_firstVisit() throws Exception {
        User v = mockVisitor();
        setupSchedule(1);
        TimeSlot slot = mockSlot(5L, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(slotDAO.getAvailableSlotsByScheduleId(1L)).thenReturn(Collections.singletonList(slot));
        feedInput("1\n1\n30\n1\n1\n");
        call(M_VIS_BOOK, BOOK_TYPES, v, apptDAO, slotDAO, schedDAO);
        assertTrue(output().contains("booked successfully"));
        verify(apptDAO).addAppointment(any());
        verify(slotDAO).updateAvailability(5L, false);
    }

    @Test
    @DisplayName("visitorBook should book individual follow-up appointment")
    void testBook_individual_followUp() throws Exception {
        User v = mockVisitor();
        setupSchedule(1);
        TimeSlot slot = mockSlot(5L, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(slotDAO.getAvailableSlotsByScheduleId(1L)).thenReturn(Collections.singletonList(slot));
        feedInput("1\n1\n30\n1\n2\n");
        call(M_VIS_BOOK, BOOK_TYPES, v, apptDAO, slotDAO, schedDAO);
        assertTrue(output().contains("booked successfully"));
    }

    @Test
    @DisplayName("visitorBook should book individual virtual appointment")
    void testBook_individual_virtual() throws Exception {
        User v = mockVisitor();
        setupSchedule(1);
        TimeSlot slot = mockSlot(5L, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(slotDAO.getAvailableSlotsByScheduleId(1L)).thenReturn(Collections.singletonList(slot));
        feedInput("1\n1\n30\n1\n3\n");
        call(M_VIS_BOOK, BOOK_TYPES, v, apptDAO, slotDAO, schedDAO);
        assertTrue(output().contains("booked successfully"));
    }

    @Test
    @DisplayName("visitorBook should book group first visit appointment with 3 participants")
    void testBook_group_firstVisit() throws Exception {
        User v = mockVisitor();
        setupSchedule(1);
        TimeSlot slot = mockSlot(5L, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(slotDAO.getAvailableSlotsByScheduleId(1L)).thenReturn(Collections.singletonList(slot));
        feedInput("1\n1\n45\n2\n3\n1\n");
        call(M_VIS_BOOK, BOOK_TYPES, v, apptDAO, slotDAO, schedDAO);
        assertTrue(output().contains("booked successfully"));
        assertTrue(output().contains("Visitors : 3"));
    }

    @Test
    @DisplayName("visitorBook should book group follow-up appointment")
    void testBook_group_followUp() throws Exception {
        User v = mockVisitor();
        setupSchedule(1);
        TimeSlot slot = mockSlot(5L, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(slotDAO.getAvailableSlotsByScheduleId(1L)).thenReturn(Collections.singletonList(slot));
        feedInput("1\n1\n45\n2\n2\n2\n");
        call(M_VIS_BOOK, BOOK_TYPES, v, apptDAO, slotDAO, schedDAO);
        assertTrue(output().contains("booked successfully"));
    }

    @Test
    @DisplayName("visitorBook should book group virtual appointment")
    void testBook_group_virtual() throws Exception {
        User v = mockVisitor();
        setupSchedule(1);
        TimeSlot slot = mockSlot(5L, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(slotDAO.getAvailableSlotsByScheduleId(1L)).thenReturn(Collections.singletonList(slot));
        feedInput("1\n1\n45\n2\n1\n3\n");
        call(M_VIS_BOOK, BOOK_TYPES, v, apptDAO, slotDAO, schedDAO);
        assertTrue(output().contains("booked successfully"));
    }

    @Test
    @DisplayName("visitorBook should reject duration exceeding 60 minutes")
    void testBook_invalidDuration() throws Exception {
        User v = mockVisitor();
        setupSchedule(1);
        TimeSlot slot = mockSlot(5L, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(slotDAO.getAvailableSlotsByScheduleId(1L)).thenReturn(Collections.singletonList(slot));
        feedInput("1\n1\n99\n");
        call(M_VIS_BOOK, BOOK_TYPES, v, apptDAO, slotDAO, schedDAO);
        assertTrue(output().contains("Duration cannot exceed 60"));
    }

    @Test
    @DisplayName("visitorBook should handle invalid day choice")
    void testBook_invalidDayChoice() throws Exception {
        User v = mockVisitor();
        setupSchedule(1);
        feedInput("99\n");
        call(M_VIS_BOOK, BOOK_TYPES, v, apptDAO, slotDAO, schedDAO);
        assertTrue(output().contains("Invalid input"));
    }

    @Test
    @DisplayName("visitorBook should handle invalid slot choice")
    void testBook_invalidSlotChoice() throws Exception {
        User v = mockVisitor();
        setupSchedule(1);
        TimeSlot slot = mockSlot(5L, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(slotDAO.getAvailableSlotsByScheduleId(1L)).thenReturn(Collections.singletonList(slot));
        feedInput("1\n99\n");
        call(M_VIS_BOOK, BOOK_TYPES, v, apptDAO, slotDAO, schedDAO);
        assertTrue(output().contains("Invalid input"));
    }

    @Test
    @DisplayName("visitorBook should display booking confirmation with all details")
    void testBook_confirmationDetails() throws Exception {
        User v = mockVisitor();
        setupSchedule(1);
        TimeSlot slot = mockSlot(5L, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(slotDAO.getAvailableSlotsByScheduleId(1L)).thenReturn(Collections.singletonList(slot));
        feedInput("1\n1\n45\n1\n1\n");
        call(M_VIS_BOOK, BOOK_TYPES, v, apptDAO, slotDAO, schedDAO);
        String result = output();
        assertTrue(result.contains("booked successfully"));
        assertTrue(result.contains("Duration : 45 minutes"));
        assertTrue(result.contains("Modification fee"));
        assertTrue(result.contains("Cancellation fee"));
    }

    // ================================================================
    //  VISITOR TESTS: My Appointments
    // ================================================================

    private static final Class<?>[] MY_APPT_TYPES = {User.class, AppointmentDAO.class};

    @Test
    @DisplayName("visitorMyAppointments should show message when no appointments exist")
    void testMyAppts_empty() throws Exception {
        User v = mockVisitor();
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.emptyList());
        call(M_VIS_MINE, MY_APPT_TYPES, v, apptDAO);
        assertTrue(output().contains("You have no appointments"));
    }

    @Test
    @DisplayName("visitorMyAppointments should hide self-canceled appointments")
    void testMyAppts_onlySelfCanceled() throws Exception {
        User v = mockVisitor();
        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CANCELED);
        when(a.isCanceledByAdmin()).thenReturn(false);
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.singletonList(a));
        call(M_VIS_MINE, MY_APPT_TYPES, v, apptDAO);
        assertTrue(output().contains("You have no appointments"));
    }

    @Test
    @DisplayName("visitorMyAppointments should display DONE appointment")
    void testMyAppts_done() throws Exception {
        User v = mockVisitor();
        OffsetDateTime t = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn(Appointment.STATUS_DONE);
        when(a.isCanceledByAdmin()).thenReturn(false);
        when(a.getStartTime()).thenReturn(t);
        when(a.getEndTime()).thenReturn(t.plusMinutes(30));
        when(a.getType()).thenReturn(T_FOLLOW);
        when(a.getId()).thenReturn(1L);
        when(a.isGroup()).thenReturn(false);
        when(a.getAdminNote()).thenReturn(null);
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.singletonList(a));
        call(M_VIS_MINE, MY_APPT_TYPES, v, apptDAO);
        assertTrue(output().contains("[DONE]"));
    }

    @Test
    @DisplayName("visitorMyAppointments should display admin-canceled appointment with note")
    void testMyAppts_canceledByAdmin_withNote() throws Exception {
        User v = mockVisitor();
        OffsetDateTime t = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CANCELED);
        when(a.isCanceledByAdmin()).thenReturn(true);
        when(a.getStartTime()).thenReturn(t);
        when(a.getEndTime()).thenReturn(t.plusMinutes(30));
        when(a.getType()).thenReturn(T_VIRTUAL);
        when(a.getId()).thenReturn(2L);
        when(a.isGroup()).thenReturn(false);
        when(a.getAdminNote()).thenReturn("Admin note here");
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.singletonList(a));
        call(M_VIS_MINE, MY_APPT_TYPES, v, apptDAO);
        assertTrue(output().contains("CANCELED by Admin"));
        assertTrue(output().contains("Admin note here"));
    }

    @Test
    @DisplayName("visitorMyAppointments should flag appointments within 24 hours")
    void testMyAppts_within24h() throws Exception {
        User v = mockVisitor();
        OffsetDateTime t = OffsetDateTime.now(ZoneOffset.UTC).plusHours(2);
        Appointment a = mockConfirmedAppt(1L, T_FIRST, t);
        when(a.isCanceledByAdmin()).thenReturn(false);
        when(a.isGroup()).thenReturn(false);
        when(a.getAdminNote()).thenReturn(null);
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.singletonList(a));
        call(M_VIS_MINE, MY_APPT_TYPES, v, apptDAO);
        assertTrue(output().contains("Less than 24h remaining"));
    }

    @Test
    @DisplayName("visitorMyAppointments should label upcoming appointments")
    void testMyAppts_upcoming() throws Exception {
        User v = mockVisitor();
        OffsetDateTime t = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5);
        Appointment a = mockConfirmedAppt(1L, T_FIRST, t);
        when(a.isCanceledByAdmin()).thenReturn(false);
        when(a.isGroup()).thenReturn(false);
        when(a.getAdminNote()).thenReturn(null);
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.singletonList(a));
        call(M_VIS_MINE, MY_APPT_TYPES, v, apptDAO);
        assertTrue(output().contains("[Upcoming]"));
    }

    @Test
    @DisplayName("visitorMyAppointments should display group appointment with participant count")
    void testMyAppts_group() throws Exception {
        User v = mockVisitor();
        OffsetDateTime t = OffsetDateTime.now(ZoneOffset.UTC).plusDays(3);
        Appointment a = mockConfirmedAppt(1L, T_GRP_FIRST, t);
        when(a.isCanceledByAdmin()).thenReturn(false);
        when(a.isGroup()).thenReturn(true);
        when(a.getParticipantsCount()).thenReturn(3);
        when(a.getAdminNote()).thenReturn(null);
        when(a.getEndTime()).thenReturn(t.plusMinutes(45));
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.singletonList(a));
        call(M_VIS_MINE, MY_APPT_TYPES, v, apptDAO);
        assertTrue(output().contains("visitors: 3"));
    }

    // ================================================================
    //  VISITOR TESTS: Edit Appointment
    // ================================================================

    private static final Class<?>[] EDIT_VIS_TYPES = {User.class, AppointmentDAO.class};

    @Test
    @DisplayName("visitorEdit should show message when no future appointments exist")
    void testVisEdit_noFuture() throws Exception {
        User v = mockVisitor();
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.emptyList());
        call(M_VIS_EDIT, EDIT_VIS_TYPES, v, apptDAO);
        assertTrue(output().contains("No future appointments to edit"));
    }

    @Test
    @DisplayName("visitorEdit should block editing within 24 hours")
    void testVisEdit_within24h_blocked() throws Exception {
        User v = mockVisitor();
        OffsetDateTime soon = OffsetDateTime.now(ZoneOffset.UTC).plusHours(2);
        Appointment a = mockConfirmedAppt(1L, T_FIRST, soon);
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.singletonList(a));
        feedInput("1\n");
        call(M_VIS_EDIT, EDIT_VIS_TYPES, v, apptDAO);
        assertTrue(output().contains("cannot edit"));
        assertTrue(output().contains("100 NIS"));
    }

    @Test
    @DisplayName("visitorEdit should handle appointment not found")
    void testVisEdit_notFound() throws Exception {
        User v = mockVisitor();
        OffsetDateTime far = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5);
        Appointment a = mockConfirmedAppt(1L, T_FIRST, far);
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.singletonList(a));
        feedInput("999\n");
        call(M_VIS_EDIT, EDIT_VIS_TYPES, v, apptDAO);
        assertTrue(output().contains("not found or not yours"));
    }

    @Test
    @DisplayName("visitorEdit should change type to first visit")
    void testVisEdit_changeType_firstVisit() throws Exception {
        User v = mockVisitor();
        OffsetDateTime far = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5);
        Appointment a = mockConfirmedAppt(1L, T_FOLLOW, far);
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.singletonList(a));
        feedInput("1\n1\n1\n1\n");
        call(M_VIS_EDIT, EDIT_VIS_TYPES, v, apptDAO);
        assertTrue(output().contains("Type updated"));
        verify(apptDAO).updateType(1L, Appointment.TYPE_FIRST_VISIT);
    }

    @Test
    @DisplayName("visitorEdit should change type to follow-up")
    void testVisEdit_changeType_followUp() throws Exception {
        User v = mockVisitor();
        OffsetDateTime far = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5);
        Appointment a = mockConfirmedAppt(1L, T_FIRST, far);
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.singletonList(a));
        feedInput("1\n1\n1\n2\n");
        call(M_VIS_EDIT, EDIT_VIS_TYPES, v, apptDAO);
        assertTrue(output().contains("Type updated"));
        verify(apptDAO).updateType(1L, Appointment.TYPE_FOLLOW_UP);
    }

    @Test
    @DisplayName("visitorEdit should change type to virtual")
    void testVisEdit_changeType_virtual() throws Exception {
        User v = mockVisitor();
        OffsetDateTime far = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5);
        Appointment a = mockConfirmedAppt(1L, T_FIRST, far);
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.singletonList(a));
        feedInput("1\n1\n1\n3\n");
        call(M_VIS_EDIT, EDIT_VIS_TYPES, v, apptDAO);
        verify(apptDAO).updateType(1L, Appointment.TYPE_VIRTUAL);
    }

    @Test
    @DisplayName("visitorEdit should change type to group first visit")
    void testVisEdit_changeType_group_firstVisit() throws Exception {
        User v = mockVisitor();
        OffsetDateTime far = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5);
        Appointment a = mockConfirmedAppt(1L, T_FIRST, far);
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.singletonList(a));
        feedInput("1\n1\n2\n1\n");
        call(M_VIS_EDIT, EDIT_VIS_TYPES, v, apptDAO);
        verify(apptDAO).updateType(1L, Appointment.TYPE_GROUP_FIRST_VISIT);
    }

    @Test
    @DisplayName("visitorEdit should change type to group follow-up")
    void testVisEdit_changeType_group_followUp() throws Exception {
        User v = mockVisitor();
        OffsetDateTime far = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5);
        Appointment a = mockConfirmedAppt(1L, T_FIRST, far);
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.singletonList(a));
        feedInput("1\n1\n2\n2\n");
        call(M_VIS_EDIT, EDIT_VIS_TYPES, v, apptDAO);
        verify(apptDAO).updateType(1L, Appointment.TYPE_GROUP_FOLLOW_UP);
    }

    @Test
    @DisplayName("visitorEdit should change type to group virtual")
    void testVisEdit_changeType_group_virtual() throws Exception {
        User v = mockVisitor();
        OffsetDateTime far = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5);
        Appointment a = mockConfirmedAppt(1L, T_FIRST, far);
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.singletonList(a));
        feedInput("1\n1\n2\n3\n");
        call(M_VIS_EDIT, EDIT_VIS_TYPES, v, apptDAO);
        verify(apptDAO).updateType(1L, Appointment.TYPE_GROUP_VIRTUAL);
    }

    @Test
    @DisplayName("visitorEdit should reject participant count change for non-group appointments")
    void testVisEdit_count_notGroup() throws Exception {
        User v = mockVisitor();
        OffsetDateTime far = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5);
        Appointment a = mockConfirmedAppt(1L, T_FIRST, far);
        when(a.isGroup()).thenReturn(false);
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.singletonList(a));
        feedInput("1\n2\n");
        call(M_VIS_EDIT, EDIT_VIS_TYPES, v, apptDAO);
        assertTrue(output().contains("Only Group appointments"));
    }

    @Test
    @DisplayName("visitorEdit should update participant count for group appointments")
    void testVisEdit_count_success() throws Exception {
        User v = mockVisitor();
        OffsetDateTime far = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5);
        Appointment a = mockConfirmedAppt(1L, T_GRP_FIRST, far);
        when(a.isGroup()).thenReturn(true);
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.singletonList(a));
        feedInput("1\n2\n3\n");
        call(M_VIS_EDIT, EDIT_VIS_TYPES, v, apptDAO);
        assertTrue(output().contains("Visitor count updated"));
        verify(apptDAO).updateParticipants(1L, 3);
    }

    @Test
    @DisplayName("visitorEdit should handle invalid choice")
    void testVisEdit_invalidChoice() throws Exception {
        User v = mockVisitor();
        OffsetDateTime far = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5);
        Appointment a = mockConfirmedAppt(1L, T_FIRST, far);
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.singletonList(a));
        feedInput("1\n9\n");
        call(M_VIS_EDIT, EDIT_VIS_TYPES, v, apptDAO);
        assertTrue(output().contains("Invalid"));
    }

    // ================================================================
    //  VISITOR TESTS: Cancel Appointment
    // ================================================================

    private static final Class<?>[] CANCEL_VIS_TYPES =
        {User.class, AppointmentDAO.class, TimeSlotDAO.class};

    @Test
    @DisplayName("visitorCancel should show message when no future appointments exist")
    void testVisCancel_noFuture() throws Exception {
        User v = mockVisitor();
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.emptyList());
        call(M_VIS_CANCEL, CANCEL_VIS_TYPES, v, apptDAO, slotDAO);
        assertTrue(output().contains("No future appointments to cancel"));
    }

    @Test
    @DisplayName("visitorCancel should block cancellation within 24 hours")
    void testVisCancel_within24h_blocked() throws Exception {
        User v = mockVisitor();
        OffsetDateTime soon = OffsetDateTime.now(ZoneOffset.UTC).plusHours(2);
        Appointment a = mockConfirmedAppt(1L, T_FIRST, soon);
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.singletonList(a));
        feedInput("1\n");
        call(M_VIS_CANCEL, CANCEL_VIS_TYPES, v, apptDAO, slotDAO);
        assertTrue(output().contains("cannot cancel"));
        assertTrue(output().contains("200 NIS"));
    }

    @Test
    @DisplayName("visitorCancel should abort on user rejection")
    void testVisCancel_aborted() throws Exception {
        User v = mockVisitor();
        OffsetDateTime far = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5);
        Appointment a = mockConfirmedAppt(1L, T_FIRST, far);
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.singletonList(a));
        feedInput("1\nno\n");
        call(M_VIS_CANCEL, CANCEL_VIS_TYPES, v, apptDAO, slotDAO);
        assertTrue(output().contains("Aborted"));
        verify(apptDAO, never()).updateStatus(anyLong(), anyString());
    }

    @Test
    @DisplayName("visitorCancel should cancel appointment without slot")
    void testVisCancel_success_noSlot() throws Exception {
        User v = mockVisitor();
        OffsetDateTime far = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5);
        Appointment a = mockConfirmedAppt(1L, T_FIRST, far);
        when(a.getSlotId()).thenReturn(null);
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.singletonList(a));
        feedInput("1\nyes\n");
        call(M_VIS_CANCEL, CANCEL_VIS_TYPES, v, apptDAO, slotDAO);
        assertTrue(output().contains("Appointment canceled"));
        verify(apptDAO).updateStatus(1L, Appointment.STATUS_CANCELED);
        verify(slotDAO, never()).updateAvailability(anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("visitorCancel should release slot when canceling")
    void testVisCancel_success_withSlot() throws Exception {
        User v = mockVisitor();
        OffsetDateTime far = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5);
        Appointment a = mockConfirmedAppt(1L, T_FIRST, far);
        when(a.getSlotId()).thenReturn(42L);
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.singletonList(a));
        feedInput("1\nyes\n");
        call(M_VIS_CANCEL, CANCEL_VIS_TYPES, v, apptDAO, slotDAO);
        assertTrue(output().contains("Slot is now free"));
        verify(slotDAO).updateAvailability(42L, true);
    }
}
