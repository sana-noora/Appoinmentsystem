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
 *
 * KEY FIX: Main.sc is no longer "static final". We call Main.setScanner(new Scanner(...))
 * in feedInput() so every test gets a fresh Scanner that reads from its own ByteArrayInputStream.
 * This eliminates the NoSuchElementException that occurred because the old static final Scanner
 * kept pointing to an exhausted stream after the first test consumed it.
 */
class MainTest {

    // ================================================================
    //  Method name constants
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

    private static final Logger logger = Logger.getLogger(MainTest.class.getName());

    // ================================================================
    //  Fixtures
    // ================================================================
    private InputStream           originalIn;
    private PrintStream           originalOut;
    private ByteArrayOutputStream out;

    private AppointmentDAO apptDAO;
    private TimeSlotDAO    slotDAO;
    private ScheduleDAO    schedDAO;
    private UserDAO        userDAO;

    @BeforeEach
    void setUp() {
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
    void tearDown() {
        System.setIn(originalIn);
        System.setOut(originalOut);
        // Restore a clean Scanner on System.in so Main is left in a good state
        main.Main.setScanner(new java.util.Scanner(originalIn));
        if (out != null) {
            try { out.close(); }
            catch (IOException e) { logger.log(Level.WARNING, "Error closing output stream", e); }
        }
    }

    // ================================================================
    //  Helpers
    // ================================================================

    /**
     * Creates a fresh Scanner over the given input and injects it into Main.
     * Must be called BEFORE the method under test is invoked.
     */
    private void feedInput(String input) {
        ByteArrayInputStream bais = new ByteArrayInputStream(input.getBytes());
        main.Main.setScanner(new java.util.Scanner(bais));
    }

    /** Invokes a private static method on Main via reflection. */
    private Object call(String method, Class<?>[] types, Object... args) throws Exception {
        Method m = main.Main.class.getDeclaredMethod(method, types);
        m.setAccessible(true);
        return m.invoke(null, args);
    }

    /** Returns captured stdout. */
    private String output() {
        return out != null ? out.toString() : "";
    }

    // ── Mock helpers ─────────────────────────────────────────────────

    /** Sets up a mock Schedule N days in the future and wires it to schedDAO. */
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

    private Appointment mockAppt(long id, String type, OffsetDateTime start) {
        Appointment a = mock(Appointment.class);
        when(a.getId()).thenReturn(id);
        when(a.getStartTime()).thenReturn(start);
        when(a.getEndTime()).thenReturn(start.plusMinutes(30));
        when(a.getType()).thenReturn(type);
        when(a.getCreatedBy()).thenReturn(LONG_UID);
        return a;
    }

    private Appointment mockConfirmedAppt(long id, String type, OffsetDateTime start) {
        Appointment a = mockAppt(id, type, start);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);
        return a;
    }

    private User mockVisitor() {
        User v = mock(User.class);
        when(v.getId()).thenReturn(UID);
        when(v.isLoggedIn()).thenReturn(false);
        return v;
    }

    private TimeSlot mockSlot(long id, OffsetDateTime start) {
        TimeSlot s = mock(TimeSlot.class);
        when(s.getId()).thenReturn(id);
        when(s.getStartTime()).thenReturn(start);
        return s;
    }

    // ================================================================
    //  friendlyType
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
    @DisplayName("friendlyType: all known types + unknown passthrough")
    void testFriendlyType_allTypes(String input, String expected) throws Exception {
        Object result = call(M_FRIENDLY, new Class[]{String.class}, input.trim());
        assertEquals(expected.trim(), result);
    }

    @Test
    @DisplayName("friendlyType: null → 'Unknown'")
    void testFriendlyType_null() throws Exception {
        assertEquals("Unknown", call(M_FRIENDLY, new Class[]{String.class}, (Object) null));
    }

    @Test
    @DisplayName("friendlyType: lowercase handled via toUpperCase")
    void testFriendlyType_lowercase() throws Exception {
        assertEquals("Individual – First Visit",
                call(M_FRIENDLY, new Class[]{String.class}, "first_visit"));
    }

    @Test
    @DisplayName("friendlyType: empty string → empty string (passthrough)")
    void testFriendlyType_empty() throws Exception {
        assertEquals("", call(M_FRIENDLY, new Class[]{String.class}, ""));
    }

    // ================================================================
    //  isWithin24h
    // ================================================================

    @Test void testWithin24h_true_1h() throws Exception {
        assertTrue((boolean) call(M_WITHIN24H, new Class[]{OffsetDateTime.class},
                OffsetDateTime.now(ZoneOffset.UTC).plusHours(1)));
    }

    @Test void testWithin24h_true_23h() throws Exception {
        assertTrue((boolean) call(M_WITHIN24H, new Class[]{OffsetDateTime.class},
                OffsetDateTime.now(ZoneOffset.UTC).plusHours(23)));
    }

    @Test void testWithin24h_false_25h() throws Exception {
        assertFalse((boolean) call(M_WITHIN24H, new Class[]{OffsetDateTime.class},
                OffsetDateTime.now(ZoneOffset.UTC).plusHours(25)));
    }

    @Test void testWithin24h_past() throws Exception {
        assertTrue((boolean) call(M_WITHIN24H, new Class[]{OffsetDateTime.class},
                OffsetDateTime.now(ZoneOffset.UTC).minusHours(5)));
    }

    @Test void testWithin24h_farFuture() throws Exception {
        assertFalse((boolean) call(M_WITHIN24H, new Class[]{OffsetDateTime.class},
                OffsetDateTime.now(ZoneOffset.UTC).plusDays(10)));
    }

    @Test void testWithin24h_justInside() throws Exception {
        assertTrue((boolean) call(M_WITHIN24H, new Class[]{OffsetDateTime.class},
                OffsetDateTime.now(ZoneOffset.UTC).plusHours(23).plusMinutes(59)));
    }

    @Test void testWithin24h_justOutside() throws Exception {
        assertFalse((boolean) call(M_WITHIN24H, new Class[]{OffsetDateTime.class},
                OffsetDateTime.now(ZoneOffset.UTC).plusHours(24).plusMinutes(1)));
    }

    // ================================================================
    //  sortGroup
    // ================================================================

    @Test void testSort_done() throws Exception {
        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn(Appointment.STATUS_DONE);
        assertEquals(0, call(M_SORT, new Class[]{Appointment.class}, a));
    }

    @Test void testSort_canceled() throws Exception {
        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CANCELED);
        assertEquals(1, call(M_SORT, new Class[]{Appointment.class}, a));
    }

    @Test void testSort_confirmed() throws Exception {
        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);
        assertEquals(2, call(M_SORT, new Class[]{Appointment.class}, a));
    }

    @Test void testSort_nullStatus() throws Exception {
        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn(null);
        assertEquals(2, call(M_SORT, new Class[]{Appointment.class}, a));
    }

    @Test
    @DisplayName("sortGroup order: DONE(0) < CANCELED(1) < CONFIRMED(2)")
    void testSort_order() throws Exception {
        Appointment done = mock(Appointment.class); when(done.getStatus()).thenReturn(Appointment.STATUS_DONE);
        Appointment canceled = mock(Appointment.class); when(canceled.getStatus()).thenReturn(Appointment.STATUS_CANCELED);
        Appointment confirmed = mock(Appointment.class); when(confirmed.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);

        int gd = (int) call(M_SORT, new Class[]{Appointment.class}, done);
        int gc = (int) call(M_SORT, new Class[]{Appointment.class}, canceled);
        int gf = (int) call(M_SORT, new Class[]{Appointment.class}, confirmed);

        assertTrue(gd < gc && gc < gf);
    }

    // ================================================================
    //  fmtTime
    // ================================================================

    @Test void testFmt_null() throws Exception {
        assertEquals("N/A", call(M_FMT, new Class[]{OffsetDateTime.class}, new Object[]{null}));
    }

    @Test void testFmt_nonNull() throws Exception {
        String r = (String) call(M_FMT, new Class[]{OffsetDateTime.class}, OffsetDateTime.now(ZoneOffset.UTC));
        assertNotNull(r);
        assertFalse(r.trim().isEmpty());
    }

    @Test void testFmt_containsDayName() throws Exception {
        String r = (String) call(M_FMT, new Class[]{OffsetDateTime.class}, OffsetDateTime.now(ZoneOffset.UTC));
        assertTrue(r.matches("(?i).*(Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday).*"));
    }

    @Test void testFmt_knownDate() throws Exception {
        String r = (String) call(M_FMT, new Class[]{OffsetDateTime.class},
                OffsetDateTime.of(2026, 4, 6, 0, 0, 0, 0, ZoneOffset.UTC));
        assertTrue(r.contains("2026") && r.contains("Apr"));
    }

    @Test void testFmt_containsTime() throws Exception {
        String r = (String) call(M_FMT, new Class[]{OffsetDateTime.class},
                OffsetDateTime.of(2026, 1, 1, 10, 30, 0, 0, ZoneOffset.UTC));
        assertTrue(r.matches(".*\\d{2}:\\d{2}.*"));
    }

    // ================================================================
    //  readInt
    // ================================================================

    @Test void testReadInt_valid() throws Exception {
        feedInput("3\n");
        assertEquals(3, call(M_READ_INT, new Class[]{int.class, int.class}, 1, 5));
    }

    @Test void testReadInt_min() throws Exception {
        feedInput("1\n");
        assertEquals(1, call(M_READ_INT, new Class[]{int.class, int.class}, 1, 5));
    }

    @Test void testReadInt_max() throws Exception {
        feedInput("5\n");
        assertEquals(5, call(M_READ_INT, new Class[]{int.class, int.class}, 1, 5));
    }

    @Test void testReadInt_below() throws Exception {
        feedInput("0\n");
        assertEquals(-1, call(M_READ_INT, new Class[]{int.class, int.class}, 1, 5));
        assertTrue(output().contains("Invalid input"));
    }

    @Test void testReadInt_above() throws Exception {
        feedInput("99\n");
        assertEquals(-1, call(M_READ_INT, new Class[]{int.class, int.class}, 1, 5));
        assertTrue(output().contains("Invalid input"));
    }

    @Test void testReadInt_text() throws Exception {
        feedInput("abc\n");
        assertEquals(-1, call(M_READ_INT, new Class[]{int.class, int.class}, 1, 5));
        assertTrue(output().contains("Invalid input"));
    }

    @Test void testReadInt_empty() throws Exception {
        feedInput("\n");
        assertEquals(-1, call(M_READ_INT, new Class[]{int.class, int.class}, 1, 5));
    }

    @Test void testReadInt_negative() throws Exception {
        feedInput("-1\n");
        assertEquals(-1, call(M_READ_INT, new Class[]{int.class, int.class}, 1, 5));
    }

    @Test void testReadInt_float() throws Exception {
        feedInput("2.5\n");
        assertEquals(-1, call(M_READ_INT, new Class[]{int.class, int.class}, 1, 5));
    }

    @Test void testReadInt_spaces() throws Exception {
        feedInput("  3  \n");
        assertEquals(3, call(M_READ_INT, new Class[]{int.class, int.class}, 1, 5));
    }

    // ================================================================
    //  readLong
    // ================================================================

    @Test void testReadLong_valid() throws Exception {
        feedInput("42\n");
        assertEquals(42L, call(M_READ_LONG, new Class[0]));
    }

    @Test void testReadLong_text() throws Exception {
        feedInput("xyz\n");
        assertEquals(-1L, call(M_READ_LONG, new Class[0]));
        assertTrue(output().contains("Invalid ID"));
    }

    @Test void testReadLong_negative() throws Exception {
        feedInput("-5\n");
        assertEquals(-5L, call(M_READ_LONG, new Class[0]));
    }

    @Test void testReadLong_large() throws Exception {
        feedInput("9999999999\n");
        assertEquals(9999999999L, call(M_READ_LONG, new Class[0]));
    }

    @Test void testReadLong_empty() throws Exception {
        feedInput("\n");
        assertEquals(-1L, call(M_READ_LONG, new Class[0]));
    }

    @Test void testReadLong_zero() throws Exception {
        feedInput("0\n");
        assertEquals(0L, call(M_READ_LONG, new Class[0]));
    }

    @Test void testReadLong_spaces() throws Exception {
        feedInput("  7  \n");
        assertEquals(7L, call(M_READ_LONG, new Class[0]));
    }

    // ================================================================
    //  printBanner
    // ================================================================

    @Test void testPrintBanner() throws Exception {
        feedInput("");
        call(M_BANNER, new Class[0]);
        assertTrue(output().contains("Appointment Scheduling System"));
    }

    // ================================================================
    //  Admin: View Appointments
    // ================================================================

    private static final Class<?>[] VIEW_APPT_TYPES =
        {AppointmentDAO.class, ScheduleDAO.class, UserDAO.class};

    @Test void testAdminView_noWorkDays() throws Exception {
        when(schedDAO.getFutureSchedules()).thenReturn(Collections.emptyList());
        feedInput("");
        call(M_ADMIN_VIEW, VIEW_APPT_TYPES, apptDAO, schedDAO, userDAO);
        assertTrue(output().contains("No work days in system"));
    }

    @Test void testAdminView_invalidChoice() throws Exception {
        setupSchedule(1);
        feedInput("99\n");
        call(M_ADMIN_VIEW, VIEW_APPT_TYPES, apptDAO, schedDAO, userDAO);
        assertTrue(output().contains("Invalid input"));
    }

    @Test void testAdminView_noAppointments() throws Exception {
        LocalDate d = setupSchedule(1);
        when(apptDAO.getActiveAppointmentsByDate(d)).thenReturn(Collections.emptyList());
        feedInput("1\n");
        call(M_ADMIN_VIEW, VIEW_APPT_TYPES, apptDAO, schedDAO, userDAO);
        assertTrue(output().contains("No active appointments"));
    }

    @Test void testAdminView_withAppointments() throws Exception {
        LocalDate d = setupSchedule(1);
        Appointment a = mockAppt(1L, T_FIRST, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(apptDAO.getActiveAppointmentsByDate(d)).thenReturn(Collections.singletonList(a));
        when(userDAO.getUserById(UID)).thenReturn(Optional.empty());
        feedInput("1\n");
        call(M_ADMIN_VIEW, VIEW_APPT_TYPES, apptDAO, schedDAO, userDAO);
        assertTrue(output().contains("Individual – First Visit"));
    }

    @Test void testAdminView_showsUsername() throws Exception {
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
    //  Admin: Cancel Appointment
    // ================================================================

    private static final Class<?>[] CANCEL_TYPES =
        {AppointmentDAO.class, TimeSlotDAO.class, ScheduleDAO.class, UserDAO.class};

    @Test void testAdminCancel_noWorkDays() throws Exception {
        when(schedDAO.getFutureSchedules()).thenReturn(Collections.emptyList());
        feedInput("");
        call(M_ADMIN_CANCEL, CANCEL_TYPES, apptDAO, slotDAO, schedDAO, userDAO);
        assertTrue(output().contains("No future work days"));
    }

    @Test void testAdminCancel_invalidDay() throws Exception {
        setupSchedule(1);
        feedInput("abc\n");
        call(M_ADMIN_CANCEL, CANCEL_TYPES, apptDAO, slotDAO, schedDAO, userDAO);
        assertTrue(output().contains("Invalid input"));
    }

    @Test void testAdminCancel_noAppointments() throws Exception {
        LocalDate d = setupSchedule(1);
        when(apptDAO.getFutureAppointmentsByDate(d)).thenReturn(Collections.emptyList());
        feedInput("1\n");
        call(M_ADMIN_CANCEL, CANCEL_TYPES, apptDAO, slotDAO, schedDAO, userDAO);
        assertTrue(output().contains("No future appointments on that day"));
    }

    @Test void testAdminCancel_notFound() throws Exception {
        LocalDate d = setupSchedule(1);
        Appointment a = mockAppt(10L, T_FOLLOW, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(apptDAO.getFutureAppointmentsByDate(d)).thenReturn(Collections.singletonList(a));
        when(userDAO.getUserById(anyString())).thenReturn(Optional.empty());
        when(apptDAO.getAppointmentById(99L)).thenReturn(null);
        feedInput("1\n99\n");
        call(M_ADMIN_CANCEL, CANCEL_TYPES, apptDAO, slotDAO, schedDAO, userDAO);
        assertTrue(output().contains("Appointment not found or already past"));
    }

    @Test void testAdminCancel_success_noSlot() throws Exception {
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

    @Test void testAdminCancel_success_withSlot() throws Exception {
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

    @Test void testAdminCancel_emptyNote() throws Exception {
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
    //  Admin: Edit Appointment
    // ================================================================

    private static final Class<?>[] EDIT_TYPES =
        {AppointmentDAO.class, ScheduleDAO.class, UserDAO.class};

    @Test void testAdminEdit_noWorkDays() throws Exception {
        when(schedDAO.getFutureSchedules()).thenReturn(Collections.emptyList());
        feedInput("");
        call(M_ADMIN_EDIT, EDIT_TYPES, apptDAO, schedDAO, userDAO);
        assertTrue(output().contains("No future work days"));
    }

    @Test void testAdminEdit_noAppointments() throws Exception {
        LocalDate d = setupSchedule(1);
        when(apptDAO.getFutureAppointmentsByDate(d)).thenReturn(Collections.emptyList());
        feedInput("1\n");
        call(M_ADMIN_EDIT, EDIT_TYPES, apptDAO, schedDAO, userDAO);
        assertTrue(output().contains("No future appointments on that day"));
    }

    @Test void testAdminEdit_invalidChoice() throws Exception {
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

    @Test void testAdminEdit_changeType_individual_firstVisit() throws Exception {
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

    @Test void testAdminEdit_changeType_group_virtual() throws Exception {
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

    @Test void testAdminEdit_changeType_individual_followUp() throws Exception {
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

    @Test void testAdminEdit_changeType_individual_virtual() throws Exception {
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

    @Test void testAdminEdit_changeType_group_firstVisit() throws Exception {
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

    @Test void testAdminEdit_changeType_group_followUp() throws Exception {
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

    @Test void testAdminEdit_changeCount_notGroup() throws Exception {
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

    @Test void testAdminEdit_changeCount_success() throws Exception {
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
    //  Admin: Add Work Day
    // ================================================================

    private static final Class<?>[] WORKDAY_TYPES = {ScheduleDAO.class};

    @Test void testWorkday_invalidFormat() throws Exception {
        when(schedDAO.getFutureSchedules()).thenReturn(Collections.emptyList());
        feedInput("not-a-date\n");
        call(M_ADMIN_WORKDAY, WORKDAY_TYPES, schedDAO);
        assertTrue(output().contains("Invalid format"));
    }

    @Test void testWorkday_pastDate() throws Exception {
        when(schedDAO.getFutureSchedules()).thenReturn(Collections.emptyList());
        feedInput("2020-01-01\n");
        call(M_ADMIN_WORKDAY, WORKDAY_TYPES, schedDAO);
        assertTrue(output().contains("Date must be today or in the future"));
    }

    @Test void testWorkday_alreadyExists() throws Exception {
        when(schedDAO.getFutureSchedules()).thenReturn(Collections.emptyList());
        LocalDate future = LocalDate.now().plusDays(5);
        when(schedDAO.existsByDate(future)).thenReturn(true);
        feedInput(future + "\n");
        call(M_ADMIN_WORKDAY, WORKDAY_TYPES, schedDAO);
        assertTrue(output().contains("already exists"));
    }

    @Test void testWorkday_success() throws Exception {
        when(schedDAO.getFutureSchedules()).thenReturn(Collections.emptyList());
        LocalDate future = LocalDate.now().plusDays(3);
        when(schedDAO.existsByDate(future)).thenReturn(false);
        feedInput(future + "\n");
        call(M_ADMIN_WORKDAY, WORKDAY_TYPES, schedDAO);
        assertTrue(output().contains("Work day added successfully"));
        verify(schedDAO).addSchedule(any(Schedule.class));
    }

    @Test void testWorkday_showsExistingDays() throws Exception {
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
    //  Admin: View Day Slots
    // ================================================================

    private static final Class<?>[] SLOTS_TYPES = {TimeSlotDAO.class, ScheduleDAO.class};

    @Test void testSlots_noWorkDays() throws Exception {
        when(schedDAO.getFutureSchedules()).thenReturn(Collections.emptyList());
        feedInput("");
        call(M_ADMIN_SLOTS, SLOTS_TYPES, slotDAO, schedDAO);
        assertTrue(output().contains("No work days"));
    }

    @Test void testSlots_noSlots() throws Exception {
        setupSchedule(1);
        when(slotDAO.getAllSlotsBySchedule(1L)).thenReturn(Collections.emptyList());
        feedInput("1\n");
        call(M_ADMIN_SLOTS, SLOTS_TYPES, slotDAO, schedDAO);
        assertTrue(output().contains("No slots for that day"));
    }

    @Test void testSlots_available() throws Exception {
        setupSchedule(1);
        TimeSlot slot = mockSlot(5L, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(slot.isAvailable()).thenReturn(true);
        when(slotDAO.getAllSlotsBySchedule(1L)).thenReturn(Collections.singletonList(slot));
        feedInput("1\n");
        call(M_ADMIN_SLOTS, SLOTS_TYPES, slotDAO, schedDAO);
        assertTrue(output().contains("Available"));
    }

    @Test void testSlots_booked_withUsername() throws Exception {
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

    @Test void testSlots_booked_noUsername() throws Exception {
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
    //  Admin: Add Slot
    // ================================================================

    private static final Class<?>[] ADD_SLOT_TYPES = {TimeSlotDAO.class, ScheduleDAO.class};

    @Test void testAddSlot_noWorkDays() throws Exception {
        when(schedDAO.getFutureSchedules()).thenReturn(Collections.emptyList());
        feedInput("");
        call(M_ADMIN_ADD_SLOT, ADD_SLOT_TYPES, slotDAO, schedDAO);
        assertTrue(output().contains("No work days"));
    }

    @Test void testAddSlot_invalidHour() throws Exception {
        setupSchedule(1);
        when(slotDAO.getAllSlotsBySchedule(1L)).thenReturn(Collections.emptyList());
        feedInput("1\n99\n");
        call(M_ADMIN_ADD_SLOT, ADD_SLOT_TYPES, slotDAO, schedDAO);
        assertTrue(output().contains("Invalid input"));
    }

    @Test void testAddSlot_alreadyExists() throws Exception {
        setupSchedule(1);
        when(slotDAO.getAllSlotsBySchedule(1L)).thenReturn(Collections.emptyList());
        when(slotDAO.existsByScheduleAndStart(anyLong(), any())).thenReturn(true);
        feedInput("1\n9\n");
        call(M_ADMIN_ADD_SLOT, ADD_SLOT_TYPES, slotDAO, schedDAO);
        assertTrue(output().contains("already exists"));
    }

    @Test void testAddSlot_success() throws Exception {
        setupSchedule(1);
        when(slotDAO.getAllSlotsBySchedule(1L)).thenReturn(Collections.emptyList());
        when(slotDAO.existsByScheduleAndStart(anyLong(), any())).thenReturn(false);
        feedInput("1\n10\n");
        call(M_ADMIN_ADD_SLOT, ADD_SLOT_TYPES, slotDAO, schedDAO);
        assertTrue(output().contains("Slot added"));
        verify(slotDAO).addTimeSlot(any(TimeSlot.class));
    }

    @Test void testAddSlot_showsExistingSlots() throws Exception {
        setupSchedule(1);
        TimeSlot existing = mockSlot(1L, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(slotDAO.getAllSlotsBySchedule(1L)).thenReturn(Collections.singletonList(existing));
        when(slotDAO.existsByScheduleAndStart(anyLong(), any())).thenReturn(false);
        feedInput("1\n14\n");
        call(M_ADMIN_ADD_SLOT, ADD_SLOT_TYPES, slotDAO, schedDAO);
        assertTrue(output().contains("Slot added"));
    }

    // ================================================================
    //  Admin: Add User
    // ================================================================

    private static final Class<?>[] ADD_USER_TYPES = {UserDAO.class};

    @Test void testAdminAddUser_visitor() throws Exception {
        feedInput("John Doe\njohn@test.com\n0501234567\njohnuser\npassword123\n2\n");
        call("adminAddUser", ADD_USER_TYPES, userDAO);
        assertTrue(output().contains("User registered successfully"));
        verify(userDAO).addUser(any(User.class), eq("password123"));
    }

    @Test void testAdminAddUser_admin() throws Exception {
        feedInput("Admin User\nadmin@test.com\n0509999999\nadminuser\nadminpass\n1\n");
        call("adminAddUser", ADD_USER_TYPES, userDAO);
        assertTrue(output().contains("User registered successfully"));
        verify(userDAO).addUser(any(User.class), eq("adminpass"));
    }

    // ================================================================
    //  Admin: View All Users
    // ================================================================

    private static final Class<?>[] VIEW_USERS_TYPES = {UserDAO.class};

    @Test void testViewUsers_empty() throws Exception {
        when(userDAO.getAllUsers()).thenReturn(Collections.emptyList());
        feedInput("");
        call("adminViewAllUsers", VIEW_USERS_TYPES, userDAO);
        assertTrue(output().contains("All Users"));
    }

    @Test void testViewUsers_withData() throws Exception {
        User u = mock(User.class);
        when(u.getId()).thenReturn(UID);
        when(u.getName()).thenReturn("Test User");
        when(u.getEmail()).thenReturn("test@test.com");
        when(u.getPhoneNumber()).thenReturn("0501234567");
        when(u.getRole()).thenReturn(User.Role.VISITOR);
        when(userDAO.getAllUsers()).thenReturn(Collections.singletonList(u));
        feedInput("");
        call("adminViewAllUsers", VIEW_USERS_TYPES, userDAO);
        assertTrue(output().contains("Test User"));
        assertTrue(output().contains("test@test.com"));
        assertTrue(output().contains("VISITOR"));
    }

    @Test void testViewUsers_adminRole() throws Exception {
        User u = mock(User.class);
        when(u.getId()).thenReturn("2");
        when(u.getName()).thenReturn("Admin Name");
        when(u.getEmail()).thenReturn("admin@test.com");
        when(u.getPhoneNumber()).thenReturn("0509999999");
        when(u.getRole()).thenReturn(User.Role.ADMIN);
        when(userDAO.getAllUsers()).thenReturn(Collections.singletonList(u));
        feedInput("");
        call("adminViewAllUsers", VIEW_USERS_TYPES, userDAO);
        assertTrue(output().contains("Admin Name"));
        assertTrue(output().contains("ADMIN"));
    }

    // ================================================================
    //  Visitor: Book Appointment
    // ================================================================

    private static final Class<?>[] BOOK_TYPES =
        {User.class, AppointmentDAO.class, TimeSlotDAO.class, ScheduleDAO.class};

    @Test void testBook_noWorkDays() throws Exception {
        User v = mockVisitor();
        when(schedDAO.getFutureSchedules()).thenReturn(Collections.emptyList());
        feedInput("");
        call(M_VIS_BOOK, BOOK_TYPES, v, apptDAO, slotDAO, schedDAO);
        assertTrue(output().contains("No available work days"));
    }

    @Test void testBook_noSlots() throws Exception {
        User v = mockVisitor();
        setupSchedule(1);
        when(slotDAO.getAvailableSlotsByScheduleId(1L)).thenReturn(Collections.emptyList());
        feedInput("1\n");
        call(M_VIS_BOOK, BOOK_TYPES, v, apptDAO, slotDAO, schedDAO);
        assertTrue(output().contains("No available slots"));
    }

    @Test void testBook_invalidCategory() throws Exception {
        User v = mockVisitor();
        setupSchedule(1);
        TimeSlot slot = mockSlot(5L, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(slotDAO.getAvailableSlotsByScheduleId(1L)).thenReturn(Collections.singletonList(slot));
        feedInput("1\n1\n30\n9\n");
        call(M_VIS_BOOK, BOOK_TYPES, v, apptDAO, slotDAO, schedDAO);
        assertTrue(output().contains("Invalid category"));
    }

    @Test void testBook_individual_firstVisit() throws Exception {
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

    @Test void testBook_individual_followUp() throws Exception {
        User v = mockVisitor();
        setupSchedule(1);
        TimeSlot slot = mockSlot(5L, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(slotDAO.getAvailableSlotsByScheduleId(1L)).thenReturn(Collections.singletonList(slot));
        feedInput("1\n1\n30\n1\n2\n");
        call(M_VIS_BOOK, BOOK_TYPES, v, apptDAO, slotDAO, schedDAO);
        assertTrue(output().contains("booked successfully"));
    }

    @Test void testBook_individual_virtual() throws Exception {
        User v = mockVisitor();
        setupSchedule(1);
        TimeSlot slot = mockSlot(5L, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(slotDAO.getAvailableSlotsByScheduleId(1L)).thenReturn(Collections.singletonList(slot));
        feedInput("1\n1\n30\n1\n3\n");
        call(M_VIS_BOOK, BOOK_TYPES, v, apptDAO, slotDAO, schedDAO);
        assertTrue(output().contains("booked successfully"));
    }

    @Test void testBook_group_firstVisit() throws Exception {
        User v = mockVisitor();
        setupSchedule(1);
        TimeSlot slot = mockSlot(5L, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(slotDAO.getAvailableSlotsByScheduleId(1L)).thenReturn(Collections.singletonList(slot));
        feedInput("1\n1\n45\n2\n3\n1\n");
        call(M_VIS_BOOK, BOOK_TYPES, v, apptDAO, slotDAO, schedDAO);
        assertTrue(output().contains("booked successfully"));
        assertTrue(output().contains("Visitors : 3"));
    }

    @Test void testBook_group_followUp() throws Exception {
        User v = mockVisitor();
        setupSchedule(1);
        TimeSlot slot = mockSlot(5L, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(slotDAO.getAvailableSlotsByScheduleId(1L)).thenReturn(Collections.singletonList(slot));
        feedInput("1\n1\n45\n2\n2\n2\n");
        call(M_VIS_BOOK, BOOK_TYPES, v, apptDAO, slotDAO, schedDAO);
        assertTrue(output().contains("booked successfully"));
    }

    @Test void testBook_group_virtual() throws Exception {
        User v = mockVisitor();
        setupSchedule(1);
        TimeSlot slot = mockSlot(5L, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(slotDAO.getAvailableSlotsByScheduleId(1L)).thenReturn(Collections.singletonList(slot));
        feedInput("1\n1\n45\n2\n1\n3\n");
        call(M_VIS_BOOK, BOOK_TYPES, v, apptDAO, slotDAO, schedDAO);
        assertTrue(output().contains("booked successfully"));
    }

    @Test void testBook_invalidDuration() throws Exception {
        User v = mockVisitor();
        setupSchedule(1);
        TimeSlot slot = mockSlot(5L, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(slotDAO.getAvailableSlotsByScheduleId(1L)).thenReturn(Collections.singletonList(slot));
        feedInput("1\n1\n99\n");
        call(M_VIS_BOOK, BOOK_TYPES, v, apptDAO, slotDAO, schedDAO);
        assertTrue(output().contains("Duration cannot exceed 60"));
    }

    @Test void testBook_invalidDayChoice() throws Exception {
        User v = mockVisitor();
        setupSchedule(1);
        feedInput("99\n");
        call(M_VIS_BOOK, BOOK_TYPES, v, apptDAO, slotDAO, schedDAO);
        assertTrue(output().contains("Invalid input"));
    }

    @Test void testBook_invalidSlotChoice() throws Exception {
        User v = mockVisitor();
        setupSchedule(1);
        TimeSlot slot = mockSlot(5L, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(slotDAO.getAvailableSlotsByScheduleId(1L)).thenReturn(Collections.singletonList(slot));
        feedInput("1\n99\n");
        call(M_VIS_BOOK, BOOK_TYPES, v, apptDAO, slotDAO, schedDAO);
        assertTrue(output().contains("Invalid input"));
    }

    @Test void testBook_confirmationDetails() throws Exception {
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
    //  Visitor: My Appointments
    // ================================================================

    private static final Class<?>[] MY_APPT_TYPES = {User.class, AppointmentDAO.class};

    @Test void testMyAppts_empty() throws Exception {
        User v = mockVisitor();
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.emptyList());
        feedInput("");
        call(M_VIS_MINE, MY_APPT_TYPES, v, apptDAO);
        assertTrue(output().contains("You have no appointments"));
    }

    @Test void testMyAppts_onlySelfCanceled() throws Exception {
        User v = mockVisitor();
        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CANCELED);
        when(a.isCanceledByAdmin()).thenReturn(false);
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.singletonList(a));
        feedInput("");
        call(M_VIS_MINE, MY_APPT_TYPES, v, apptDAO);
        assertTrue(output().contains("You have no appointments"));
    }

    @Test void testMyAppts_done() throws Exception {
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
        feedInput("");
        call(M_VIS_MINE, MY_APPT_TYPES, v, apptDAO);
        assertTrue(output().contains("[DONE]"));
    }

    @Test void testMyAppts_canceledByAdmin_withNote() throws Exception {
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
        feedInput("");
        call(M_VIS_MINE, MY_APPT_TYPES, v, apptDAO);
        assertTrue(output().contains("CANCELED by Admin"));
        assertTrue(output().contains("Admin note here"));
    }

    @Test void testMyAppts_within24h() throws Exception {
        User v = mockVisitor();
        OffsetDateTime t = OffsetDateTime.now(ZoneOffset.UTC).plusHours(2);
        Appointment a = mockConfirmedAppt(1L, T_FIRST, t);
        when(a.isCanceledByAdmin()).thenReturn(false);
        when(a.isGroup()).thenReturn(false);
        when(a.getAdminNote()).thenReturn(null);
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.singletonList(a));
        feedInput("");
        call(M_VIS_MINE, MY_APPT_TYPES, v, apptDAO);
        assertTrue(output().contains("Less than 24h remaining"));
    }

    @Test void testMyAppts_upcoming() throws Exception {
        User v = mockVisitor();
        OffsetDateTime t = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5);
        Appointment a = mockConfirmedAppt(1L, T_FIRST, t);
        when(a.isCanceledByAdmin()).thenReturn(false);
        when(a.isGroup()).thenReturn(false);
        when(a.getAdminNote()).thenReturn(null);
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.singletonList(a));
        feedInput("");
        call(M_VIS_MINE, MY_APPT_TYPES, v, apptDAO);
        assertTrue(output().contains("[Upcoming]"));
    }

    @Test void testMyAppts_group() throws Exception {
        User v = mockVisitor();
        OffsetDateTime t = OffsetDateTime.now(ZoneOffset.UTC).plusDays(3);
        Appointment a = mockConfirmedAppt(1L, T_GRP_FIRST, t);
        when(a.isCanceledByAdmin()).thenReturn(false);
        when(a.isGroup()).thenReturn(true);
        when(a.getParticipantsCount()).thenReturn(3);
        when(a.getAdminNote()).thenReturn(null);
        when(a.getEndTime()).thenReturn(t.plusMinutes(45));
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.singletonList(a));
        feedInput("");
        call(M_VIS_MINE, MY_APPT_TYPES, v, apptDAO);
        assertTrue(output().contains("visitors: 3"));
    }

    // ================================================================
    //  Visitor: Edit Appointment
    // ================================================================

    private static final Class<?>[] EDIT_VIS_TYPES = {User.class, AppointmentDAO.class};

    @Test void testVisEdit_noFuture() throws Exception {
        User v = mockVisitor();
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.emptyList());
        feedInput("");
        call(M_VIS_EDIT, EDIT_VIS_TYPES, v, apptDAO);
        assertTrue(output().contains("No future appointments to edit"));
    }

    @Test void testVisEdit_within24h_blocked() throws Exception {
        User v = mockVisitor();
        OffsetDateTime soon = OffsetDateTime.now(ZoneOffset.UTC).plusHours(2);
        Appointment a = mockConfirmedAppt(1L, T_FIRST, soon);
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.singletonList(a));
        feedInput("1\n");
        call(M_VIS_EDIT, EDIT_VIS_TYPES, v, apptDAO);
        assertTrue(output().contains("cannot edit"));
        assertTrue(output().contains("100 NIS"));
    }

    @Test void testVisEdit_notFound() throws Exception {
        User v = mockVisitor();
        OffsetDateTime far = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5);
        Appointment a = mockConfirmedAppt(1L, T_FIRST, far);
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.singletonList(a));
        feedInput("999\n");
        call(M_VIS_EDIT, EDIT_VIS_TYPES, v, apptDAO);
        assertTrue(output().contains("not found or not yours"));
    }

    @Test void testVisEdit_changeType_firstVisit() throws Exception {
        User v = mockVisitor();
        OffsetDateTime far = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5);
        Appointment a = mockConfirmedAppt(1L, T_FOLLOW, far);
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.singletonList(a));
        feedInput("1\n1\n1\n1\n");
        call(M_VIS_EDIT, EDIT_VIS_TYPES, v, apptDAO);
        assertTrue(output().contains("Type updated"));
        verify(apptDAO).updateType(1L, Appointment.TYPE_FIRST_VISIT);
    }

    @Test void testVisEdit_changeType_followUp() throws Exception {
        User v = mockVisitor();
        OffsetDateTime far = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5);
        Appointment a = mockConfirmedAppt(1L, T_FIRST, far);
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.singletonList(a));
        feedInput("1\n1\n1\n2\n");
        call(M_VIS_EDIT, EDIT_VIS_TYPES, v, apptDAO);
        assertTrue(output().contains("Type updated"));
        verify(apptDAO).updateType(1L, Appointment.TYPE_FOLLOW_UP);
    }

    @Test void testVisEdit_changeType_virtual() throws Exception {
        User v = mockVisitor();
        OffsetDateTime far = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5);
        Appointment a = mockConfirmedAppt(1L, T_FIRST, far);
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.singletonList(a));
        feedInput("1\n1\n1\n3\n");
        call(M_VIS_EDIT, EDIT_VIS_TYPES, v, apptDAO);
        verify(apptDAO).updateType(1L, Appointment.TYPE_VIRTUAL);
    }

    @Test void testVisEdit_changeType_group_firstVisit() throws Exception {
        User v = mockVisitor();
        OffsetDateTime far = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5);
        Appointment a = mockConfirmedAppt(1L, T_FIRST, far);
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.singletonList(a));
        feedInput("1\n1\n2\n1\n");
        call(M_VIS_EDIT, EDIT_VIS_TYPES, v, apptDAO);
        verify(apptDAO).updateType(1L, Appointment.TYPE_GROUP_FIRST_VISIT);
    }

    @Test void testVisEdit_changeType_group_followUp() throws Exception {
        User v = mockVisitor();
        OffsetDateTime far = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5);
        Appointment a = mockConfirmedAppt(1L, T_FIRST, far);
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.singletonList(a));
        feedInput("1\n1\n2\n2\n");
        call(M_VIS_EDIT, EDIT_VIS_TYPES, v, apptDAO);
        verify(apptDAO).updateType(1L, Appointment.TYPE_GROUP_FOLLOW_UP);
    }

    @Test void testVisEdit_changeType_group_virtual() throws Exception {
        User v = mockVisitor();
        OffsetDateTime far = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5);
        Appointment a = mockConfirmedAppt(1L, T_FIRST, far);
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.singletonList(a));
        feedInput("1\n1\n2\n3\n");
        call(M_VIS_EDIT, EDIT_VIS_TYPES, v, apptDAO);
        verify(apptDAO).updateType(1L, Appointment.TYPE_GROUP_VIRTUAL);
    }

    @Test void testVisEdit_count_notGroup() throws Exception {
        User v = mockVisitor();
        OffsetDateTime far = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5);
        Appointment a = mockConfirmedAppt(1L, T_FIRST, far);
        when(a.isGroup()).thenReturn(false);
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.singletonList(a));
        feedInput("1\n2\n");
        call(M_VIS_EDIT, EDIT_VIS_TYPES, v, apptDAO);
        assertTrue(output().contains("Only Group appointments"));
    }

    @Test void testVisEdit_count_success() throws Exception {
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

    @Test void testVisEdit_invalidChoice() throws Exception {
        User v = mockVisitor();
        OffsetDateTime far = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5);
        Appointment a = mockConfirmedAppt(1L, T_FIRST, far);
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.singletonList(a));
        feedInput("1\n9\n");
        call(M_VIS_EDIT, EDIT_VIS_TYPES, v, apptDAO);
        assertTrue(output().contains("Invalid"));
    }

    // ================================================================
    //  Visitor: Cancel Appointment
    // ================================================================

    private static final Class<?>[] CANCEL_VIS_TYPES =
        {User.class, AppointmentDAO.class, TimeSlotDAO.class};

    @Test void testVisCancel_noFuture() throws Exception {
        User v = mockVisitor();
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.emptyList());
        feedInput("");
        call(M_VIS_CANCEL, CANCEL_VIS_TYPES, v, apptDAO, slotDAO);
        assertTrue(output().contains("No future appointments to cancel"));
    }

    @Test void testVisCancel_within24h_blocked() throws Exception {
        User v = mockVisitor();
        OffsetDateTime soon = OffsetDateTime.now(ZoneOffset.UTC).plusHours(2);
        Appointment a = mockConfirmedAppt(1L, T_FIRST, soon);
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.singletonList(a));
        feedInput("1\n");
        call(M_VIS_CANCEL, CANCEL_VIS_TYPES, v, apptDAO, slotDAO);
        assertTrue(output().contains("cannot cancel"));
        assertTrue(output().contains("200 NIS"));
    }

    @Test void testVisCancel_aborted() throws Exception {
        User v = mockVisitor();
        OffsetDateTime far = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5);
        Appointment a = mockConfirmedAppt(1L, T_FIRST, far);
        when(apptDAO.getAppointmentsByUser(LONG_UID)).thenReturn(Collections.singletonList(a));
        feedInput("1\nno\n");
        call(M_VIS_CANCEL, CANCEL_VIS_TYPES, v, apptDAO, slotDAO);
        assertTrue(output().contains("Aborted"));
        verify(apptDAO, never()).updateStatus(anyLong(), anyString());
    }

    @Test void testVisCancel_success_noSlot() throws Exception {
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

    @Test void testVisCancel_success_withSlot() throws Exception {
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