package tests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.*;
import java.util.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import domain.*;
import persistence.*;

class MainTest {

    // ================================================================
    // Constants
    // ================================================================
    private static final String METHOD_FRIENDLY_TYPE     = "friendlyType";
    private static final String METHOD_IS_WITHIN_24H     = "isWithin24h";
    private static final String METHOD_SORT_GROUP        = "sortGroup";
    private static final String METHOD_FMT_TIME          = "fmtTime";
    private static final String METHOD_READ_INT          = "readInt";
    private static final String METHOD_READ_LONG         = "readLong";
    private static final String METHOD_PRINT_BANNER      = "printBanner";

    private static final String METHOD_ADMIN_VIEW_APPT   = "adminViewAppointments";
    private static final String METHOD_ADMIN_CANCEL_APPT = "adminCancelAppointment";
    private static final String METHOD_ADMIN_ADD_WORKDAY = "adminAddWorkDay";
    private static final String METHOD_ADMIN_VIEW_SLOTS  = "adminViewDaySlots";
    private static final String METHOD_ADMIN_ADD_SLOT    = "adminAddSlot";
    private static final String METHOD_ADMIN_VIEW_USERS  = "adminViewAllUsers";
    private static final String METHOD_ADMIN_EDIT_APPT   = "adminEditAppointment";

    private static final String METHOD_VISITOR_MY_APPT   = "visitorMyAppointments";
    private static final String METHOD_VISITOR_EDIT      = "visitorEdit";
    private static final String METHOD_VISITOR_CANCEL    = "visitorCancel";
    private static final String METHOD_VISITOR_BOOK      = "visitorBook";

    private static final String USER_ID_ONE = "1";

    // ================================================================
    // Fields
    // ================================================================
    private InputStream           originalIn;
    private PrintStream           originalOut;
    private ByteArrayOutputStream outContent;

    private AppointmentDAO apptDAO;
    private TimeSlotDAO    slotDAO;
    private ScheduleDAO    schedDAO;
    private UserDAO        userDAO;

    @BeforeEach
    void setUp() {
        originalIn  = System.in;
        originalOut = System.out;
        outContent  = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        apptDAO  = mock(AppointmentDAO.class);
        slotDAO  = mock(TimeSlotDAO.class);
        schedDAO = mock(ScheduleDAO.class);
        userDAO  = mock(UserDAO.class);
    }

    @AfterEach
    void tearDown() throws Exception {
        System.setIn(originalIn);
        System.setOut(originalOut);
        setScanner(new Scanner(originalIn));
    }

    // ================================================================
    // Reflection helpers
    // ================================================================
    private void setScanner(Scanner sc) throws Exception {
        Field f = main.Main.class.getDeclaredField("sc");
        f.setAccessible(true);
        f.set(null, sc);
    }

    private void feedInput(String input) throws Exception {
        setScanner(new Scanner(new ByteArrayInputStream(input.getBytes())));
    }

    private Object callPrivate(String name, Class<?>[] types, Object... args) throws Exception {
        Method m = main.Main.class.getDeclaredMethod(name, types);
        m.setAccessible(true);
        return m.invoke(null, args);
    }

    private Object callWithDAOs(String name, Class<?>[] types, Object... args) throws Exception {
        Method m = main.Main.class.getDeclaredMethod(name, types);
        m.setAccessible(true);
        return m.invoke(null, args);
    }

    private String output() {
        return outContent.toString();
    }

    // ================================================================
    // Shared helpers (FIXED: throws Exception)
    // ================================================================
    private void setupScheduleNextDay() throws Exception {
        Schedule s = mock(Schedule.class);
        when(s.getWorkDate()).thenReturn(LocalDate.now().plusDays(1));
        when(s.getId()).thenReturn(1L);
        when(schedDAO.getFutureSchedules()).thenReturn(List.of(s));
    }

    private LocalDate setupScheduleWithDate(int plusDays) throws Exception {
        LocalDate d = LocalDate.now().plusDays(plusDays);
        Schedule s = mock(Schedule.class);
        when(s.getWorkDate()).thenReturn(d);
        when(s.getId()).thenReturn(1L);
        when(schedDAO.getFutureSchedules()).thenReturn(List.of(s));
        return d;
    }

    private Appointment mockAppointmentAt(long id, String type, OffsetDateTime start) {
        Appointment a = mock(Appointment.class);
        when(a.getId()).thenReturn(id);
        when(a.getStartTime()).thenReturn(start);
        when(a.getEndTime()).thenReturn(start.plusMinutes(30));
        when(a.getType()).thenReturn(type);
        when(a.getCreatedBy()).thenReturn(1L);
        return a;
    }

    private User mockVisitor() {
        User u = mock(User.class);
        when(u.getId()).thenReturn(USER_ID_ONE);
        return u;
    }

    private Appointment mockVisitorAppointment(long id, String type, OffsetDateTime start) {
        Appointment a = mock(Appointment.class);
        when(a.getId()).thenReturn(id);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);
        when(a.getStartTime()).thenReturn(start);
        when(a.getEndTime()).thenReturn(start.plusMinutes(30));
        when(a.getType()).thenReturn(type);
        return a;
    }

    private void setupAvailableSlot(long slotId) throws Exception {
        TimeSlot slot = mock(TimeSlot.class);
        when(slot.getId()).thenReturn(slotId);
        when(slot.getStartTime())
                .thenReturn(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(slotDAO.getAvailableSlotsByScheduleId(1L))
                .thenReturn(List.of(slot));
    }

    // ================================================================
    // Example tests (rest unchanged logic)
    // ================================================================
    @ParameterizedTest
    @CsvSource({
        "FIRST_VISIT, Individual – First Visit",
        "FOLLOW_UP, Individual – Follow-up"
    })
    void testFriendlyType(String input, String expected) throws Exception {
        assertEquals(expected,
            callPrivate(METHOD_FRIENDLY_TYPE, new Class[]{String.class}, input));
    }

    @Test
    void testPrintBanner() throws Exception {
        callPrivate(METHOD_PRINT_BANNER, new Class<?>[0]);
        assertTrue(output().contains("Appointment Scheduling System"));
    }
}