package tests;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.*;
import java.time.ZoneOffset;
import java.util.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import domain.*;
import persistence.*;

class MainTest {

    private InputStream           originalIn;
    private PrintStream           originalOut;
    private ByteArrayOutputStream outContent;

    // ── shared mocks ────────────────────────────────────────────────
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
        setSc(new java.util.Scanner(originalIn));
    }

    // ================================================================
    //  REFLECTION HELPERS
    // ================================================================

    private void setSc(java.util.Scanner sc) throws Exception {
        Field f = main.Main.class.getDeclaredField("sc");
        f.setAccessible(true);
        f.set(null, sc);
    }

    private void feedInput(String input) throws Exception {
        setSc(new java.util.Scanner(new ByteArrayInputStream(input.getBytes())));
    }

    private Object callPrivate(String method, Class<?>[] types, Object... args)
            throws Exception {
        Method m = main.Main.class.getDeclaredMethod(method, types);
        m.setAccessible(true);
        return m.invoke(null, args);
    }

    private Object callPrivateWithDAOs(String method, Class<?>[] types, Object... args)
            throws Exception {
        Method m = main.Main.class.getDeclaredMethod(method, types);
        m.setAccessible(true);
        return m.invoke(null, args);
    }

    private String output() { return outContent.toString(); }

    // ================================================================
    //  friendlyType – all 6 + edge cases
    // ================================================================

    @ParameterizedTest(name = "friendlyType({0}) = {1}")
    @CsvSource({
        "FIRST_VISIT,       Individual – First Visit",
        "FOLLOW_UP,         Individual – Follow-up",
        "VIRTUAL,           Individual – Virtual",
        "GROUP_FIRST_VISIT, Group – First Visit",
        "GROUP_FOLLOW_UP,   Group – Follow-up",
        "GROUP_VIRTUAL,     Group – Virtual"
    })
    void testFriendlyType_KnownTypes(String input, String expected) throws Exception {
        assertEquals(expected.trim(),
            callPrivate("friendlyType", new Class[]{String.class}, input.trim()));
    }

    @Test void testFriendlyType_Null() throws Exception {
        assertEquals("Unknown",
            callPrivate("friendlyType", new Class[]{String.class}, (Object) null));
    }

    @Test void testFriendlyType_Unknown() throws Exception {
        assertEquals("XYZ",
            callPrivate("friendlyType", new Class[]{String.class}, "XYZ"));
    }

    @Test void testFriendlyType_EmptyString() throws Exception {
        assertEquals("",
            callPrivate("friendlyType", new Class[]{String.class}, ""));
    }

    @Test void testFriendlyType_LowercaseStillMatches() throws Exception {
        assertEquals("Individual – First Visit",
            callPrivate("friendlyType", new Class[]{String.class}, "first_visit"));
    }

    // ================================================================
    //  isWithin24h – full boundary coverage
    // ================================================================

    @Test void testIsWithin24h_WithinOneHour() throws Exception {
        assertTrue((boolean) callPrivate("isWithin24h", new Class[]{OffsetDateTime.class},
            OffsetDateTime.now(ZoneOffset.UTC).plusHours(1)));
    }

    @Test void testIsWithin24h_23Hours() throws Exception {
        assertTrue((boolean) callPrivate("isWithin24h", new Class[]{OffsetDateTime.class},
            OffsetDateTime.now(ZoneOffset.UTC).plusHours(23)));
    }

    @Test void testIsWithin24h_25Hours() throws Exception {
        assertFalse((boolean) callPrivate("isWithin24h", new Class[]{OffsetDateTime.class},
            OffsetDateTime.now(ZoneOffset.UTC).plusHours(25)));
    }

    @Test void testIsWithin24h_Past() throws Exception {
        assertTrue((boolean) callPrivate("isWithin24h", new Class[]{OffsetDateTime.class},
            OffsetDateTime.now(ZoneOffset.UTC).minusHours(5)));
    }

    @Test void testIsWithin24h_FarFuture() throws Exception {
        assertFalse((boolean) callPrivate("isWithin24h", new Class[]{OffsetDateTime.class},
            OffsetDateTime.now(ZoneOffset.UTC).plusDays(10)));
    }

    @Test void testIsWithin24h_ExactlyNow() throws Exception {
        assertTrue((boolean) callPrivate("isWithin24h", new Class[]{OffsetDateTime.class},
            OffsetDateTime.now(ZoneOffset.UTC)));
    }

    @Test void testIsWithin24h_JustOutside() throws Exception {
        assertFalse((boolean) callPrivate("isWithin24h", new Class[]{OffsetDateTime.class},
            OffsetDateTime.now(ZoneOffset.UTC).plusHours(24).plusMinutes(1)));
    }

    @Test void testIsWithin24h_JustInside() throws Exception {
        assertTrue((boolean) callPrivate("isWithin24h", new Class[]{OffsetDateTime.class},
            OffsetDateTime.now(ZoneOffset.UTC).plusHours(23).plusMinutes(59)));
    }

    // ================================================================
    //  sortGroup
    // ================================================================

    @Test void testSortGroup_Done() throws Exception {
        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn(Appointment.STATUS_DONE);
        assertEquals(0, callPrivate("sortGroup", new Class[]{Appointment.class}, a));
    }

    @Test void testSortGroup_Canceled() throws Exception {
        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CANCELED);
        assertEquals(1, callPrivate("sortGroup", new Class[]{Appointment.class}, a));
    }

    @Test void testSortGroup_Confirmed() throws Exception {
        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);
        assertEquals(2, callPrivate("sortGroup", new Class[]{Appointment.class}, a));
    }

    @Test void testSortGroup_Unknown() throws Exception {
        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn("ANYTHING");
        assertEquals(2, callPrivate("sortGroup", new Class[]{Appointment.class}, a));
    }

    @Test void testSortGroup_Null() throws Exception {
        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn(null);
        assertEquals(2, callPrivate("sortGroup", new Class[]{Appointment.class}, a));
    }

    @Test void testSortGroup_OrderCorrect() throws Exception {
        Appointment d = mock(Appointment.class); when(d.getStatus()).thenReturn(Appointment.STATUS_DONE);
        Appointment c = mock(Appointment.class); when(c.getStatus()).thenReturn(Appointment.STATUS_CANCELED);
        Appointment f = mock(Appointment.class); when(f.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);
        int gd = (int) callPrivate("sortGroup", new Class[]{Appointment.class}, d);
        int gc = (int) callPrivate("sortGroup", new Class[]{Appointment.class}, c);
        int gf = (int) callPrivate("sortGroup", new Class[]{Appointment.class}, f);
        assertTrue(gd < gc && gc < gf);
    }

    // ================================================================
    //  fmtTime
    // ================================================================

    @Test void testFmtTime_Null() throws Exception {
        assertEquals("N/A",
            callPrivate("fmtTime", new Class[]{OffsetDateTime.class}, new Object[]{null}));
    }

    @Test void testFmtTime_NonNull() throws Exception {
        String r = (String) callPrivate("fmtTime", new Class[]{OffsetDateTime.class},
            OffsetDateTime.now(ZoneOffset.UTC));
        assertNotNull(r); assertFalse(r.isBlank());
    }

    @Test void testFmtTime_ContainsDayName() throws Exception {
        String r = (String) callPrivate("fmtTime", new Class[]{OffsetDateTime.class},
            OffsetDateTime.now(ZoneOffset.UTC));
        assertTrue(r.matches("(?i)(Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday).*"));
    }

    @Test void testFmtTime_KnownDate() throws Exception {
        String r = (String) callPrivate("fmtTime", new Class[]{OffsetDateTime.class},
            OffsetDateTime.of(2026, 4, 6, 0, 0, 0, 0, ZoneOffset.UTC));
        assertTrue(r.contains("2026") && r.contains("Apr"));
    }

    @Test void testFmtTime_ContainsTime() throws Exception {
        String r = (String) callPrivate("fmtTime", new Class[]{OffsetDateTime.class},
            OffsetDateTime.of(2026, 1, 1, 10, 30, 0, 0, ZoneOffset.UTC));
        assertNotNull(r);
        // Time portion should be present (HH:mm pattern)
        assertTrue(r.matches(".*\\d{2}:\\d{2}.*"));
    }

    // ================================================================
    //  readInt – full boundary coverage
    // ================================================================

    @Test void testReadInt_Valid() throws Exception {
        feedInput("3\n");
        assertEquals(3, callPrivate("readInt", new Class[]{int.class, int.class}, 1, 5));
    }

    @Test void testReadInt_Min() throws Exception {
        feedInput("1\n");
        assertEquals(1, callPrivate("readInt", new Class[]{int.class, int.class}, 1, 5));
    }

    @Test void testReadInt_Max() throws Exception {
        feedInput("5\n");
        assertEquals(5, callPrivate("readInt", new Class[]{int.class, int.class}, 1, 5));
    }

    @Test void testReadInt_BelowMin() throws Exception {
        feedInput("0\n");
        assertEquals(-1, callPrivate("readInt", new Class[]{int.class, int.class}, 1, 5));
    }

    @Test void testReadInt_AboveMax() throws Exception {
        feedInput("99\n");
        assertEquals(-1, callPrivate("readInt", new Class[]{int.class, int.class}, 1, 5));
    }

    @Test void testReadInt_NonNumeric() throws Exception {
        feedInput("abc\n");
        assertEquals(-1, callPrivate("readInt", new Class[]{int.class, int.class}, 1, 5));
    }

    @Test void testReadInt_Empty() throws Exception {
        feedInput("\n");
        assertEquals(-1, callPrivate("readInt", new Class[]{int.class, int.class}, 1, 5));
    }

    @Test void testReadInt_Negative() throws Exception {
        feedInput("-1\n");
        assertEquals(-1, callPrivate("readInt", new Class[]{int.class, int.class}, 1, 5));
    }

    @Test void testReadInt_Float() throws Exception {
        feedInput("2.5\n");
        assertEquals(-1, callPrivate("readInt", new Class[]{int.class, int.class}, 1, 5));
    }

    @Test void testReadInt_WithSpaces() throws Exception {
        feedInput("  3  \n");
        assertEquals(3, callPrivate("readInt", new Class[]{int.class, int.class}, 1, 5));
    }

    // ================================================================
    //  readLong
    // ================================================================

    @Test void testReadLong_Valid() throws Exception {
        feedInput("42\n");
        assertEquals(42L, callPrivate("readLong", new Class[0]));
    }

    @Test void testReadLong_NonNumeric() throws Exception {
        feedInput("xyz\n");
        assertEquals(-1L, callPrivate("readLong", new Class[0]));
    }

    @Test void testReadLong_Negative() throws Exception {
        feedInput("-5\n");
        assertEquals(-5L, callPrivate("readLong", new Class[0]));
    }

    @Test void testReadLong_Large() throws Exception {
        feedInput("9999999999\n");
        assertEquals(9999999999L, callPrivate("readLong", new Class[0]));
    }

    @Test void testReadLong_Empty() throws Exception {
        feedInput("\n");
        assertEquals(-1L, callPrivate("readLong", new Class[0]));
    }

    @Test void testReadLong_Zero() throws Exception {
        feedInput("0\n");
        assertEquals(0L, callPrivate("readLong", new Class[0]));
    }

    @Test void testReadLong_WithSpaces() throws Exception {
        feedInput("  7  \n");
        assertEquals(7L, callPrivate("readLong", new Class[0]));
    }

    // ================================================================
    //  printBanner
    // ================================================================

    @Test void testPrintBanner() throws Exception {
        callPrivate("printBanner", new Class[0]);
        String out = output();
        assertFalse(out.isBlank());
        assertTrue(out.contains("Appointment Scheduling System"));
    }

    // ================================================================
    //  adminViewAppointments – empty schedules
    // ================================================================

    @Test
    void testAdminViewAppointments_NoWorkDays() throws Exception {
        when(schedDAO.getFutureSchedules()).thenReturn(Collections.emptyList());
        callPrivateWithDAOs("adminViewAppointments",
            new Class[]{AppointmentDAO.class, ScheduleDAO.class, UserDAO.class},
            apptDAO, schedDAO, userDAO);
        assertTrue(output().contains("No work days in system"));
    }

    @Test
    void testAdminViewAppointments_InvalidDayChoice() throws Exception {
        Schedule s = mock(Schedule.class);
        when(s.getWorkDate()).thenReturn(java.time.LocalDate.now().plusDays(1));
        when(schedDAO.getFutureSchedules()).thenReturn(List.of(s));
        feedInput("99\n"); // out of range
        callPrivateWithDAOs("adminViewAppointments",
            new Class[]{AppointmentDAO.class, ScheduleDAO.class, UserDAO.class},
            apptDAO, schedDAO, userDAO);
        assertTrue(output().contains("Invalid input"));
    }

    @Test
    void testAdminViewAppointments_NoAppointmentsOnDay() throws Exception {
        Schedule s = mock(Schedule.class);
        java.time.LocalDate d = java.time.LocalDate.now().plusDays(1);
        when(s.getWorkDate()).thenReturn(d);
        when(s.getId()).thenReturn(1L);
        when(schedDAO.getFutureSchedules()).thenReturn(List.of(s));
        when(apptDAO.getActiveAppointmentsByDate(d)).thenReturn(Collections.emptyList());
        feedInput("1\n");
        callPrivateWithDAOs("adminViewAppointments",
            new Class[]{AppointmentDAO.class, ScheduleDAO.class, UserDAO.class},
            apptDAO, schedDAO, userDAO);
        assertTrue(output().contains("No active appointments"));
    }

    @Test
    void testAdminViewAppointments_WithAppointments() throws Exception {
        Schedule s = mock(Schedule.class);
        java.time.LocalDate d = java.time.LocalDate.now().plusDays(1);
        when(s.getWorkDate()).thenReturn(d);
        when(schedDAO.getFutureSchedules()).thenReturn(List.of(s));

        Appointment a = mock(Appointment.class);
        when(a.getId()).thenReturn(1L);
        when(a.getStartTime()).thenReturn(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(a.getEndTime()).thenReturn(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1).plusMinutes(30));
        when(a.getType()).thenReturn("FIRST_VISIT");
        when(a.getCreatedBy()).thenReturn(1L);
        when(apptDAO.getActiveAppointmentsByDate(d)).thenReturn(List.of(a));
        when(userDAO.getUserById("1")).thenReturn(Optional.empty());

        feedInput("1\n");
        callPrivateWithDAOs("adminViewAppointments",
            new Class[]{AppointmentDAO.class, ScheduleDAO.class, UserDAO.class},
            apptDAO, schedDAO, userDAO);
        assertTrue(output().contains("Individual – First Visit"));
    }

    // ================================================================
    //  adminCancelAppointment
    // ================================================================

    @Test
    void testAdminCancelAppointment_NoFutureDays() throws Exception {
        when(schedDAO.getFutureSchedules()).thenReturn(Collections.emptyList());
        callPrivateWithDAOs("adminCancelAppointment",
            new Class[]{AppointmentDAO.class, TimeSlotDAO.class, ScheduleDAO.class, UserDAO.class},
            apptDAO, slotDAO, schedDAO, userDAO);
        assertTrue(output().contains("No future work days"));
    }

    @Test
    void testAdminCancelAppointment_InvalidDayInput() throws Exception {
        Schedule s = mock(Schedule.class);
        when(s.getWorkDate()).thenReturn(java.time.LocalDate.now().plusDays(1));
        when(schedDAO.getFutureSchedules()).thenReturn(List.of(s));
        feedInput("abc\n");
        callPrivateWithDAOs("adminCancelAppointment",
            new Class[]{AppointmentDAO.class, TimeSlotDAO.class, ScheduleDAO.class, UserDAO.class},
            apptDAO, slotDAO, schedDAO, userDAO);
        assertTrue(output().contains("Invalid input"));
    }

    @Test
    void testAdminCancelAppointment_NoAppointmentsOnDay() throws Exception {
        Schedule s = mock(Schedule.class);
        java.time.LocalDate d = java.time.LocalDate.now().plusDays(1);
        when(s.getWorkDate()).thenReturn(d);
        when(schedDAO.getFutureSchedules()).thenReturn(List.of(s));
        when(apptDAO.getFutureAppointmentsByDate(d)).thenReturn(Collections.emptyList());
        feedInput("1\n");
        callPrivateWithDAOs("adminCancelAppointment",
            new Class[]{AppointmentDAO.class, TimeSlotDAO.class, ScheduleDAO.class, UserDAO.class},
            apptDAO, slotDAO, schedDAO, userDAO);
        assertTrue(output().contains("No future appointments on that day"));
    }

    @Test
    void testAdminCancelAppointment_AppointmentNotFound() throws Exception {
        Schedule s = mock(Schedule.class);
        java.time.LocalDate d = java.time.LocalDate.now().plusDays(1);
        when(s.getWorkDate()).thenReturn(d);
        when(schedDAO.getFutureSchedules()).thenReturn(List.of(s));

        Appointment a = mock(Appointment.class);
        when(a.getId()).thenReturn(10L);
        when(a.getStartTime()).thenReturn(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(a.getEndTime()).thenReturn(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1).plusMinutes(30));
        when(a.getType()).thenReturn("FOLLOW_UP");
        when(a.getCreatedBy()).thenReturn(1L);
        when(apptDAO.getFutureAppointmentsByDate(d)).thenReturn(List.of(a));
        when(userDAO.getUserById(anyString())).thenReturn(Optional.empty());
        when(apptDAO.getAppointmentById(99L)).thenReturn(null);
        feedInput("1\n99\n");
        callPrivateWithDAOs("adminCancelAppointment",
            new Class[]{AppointmentDAO.class, TimeSlotDAO.class, ScheduleDAO.class, UserDAO.class},
            apptDAO, slotDAO, schedDAO, userDAO);
        assertTrue(output().contains("Appointment not found or already past"));
    }

    @Test
    void testAdminCancelAppointment_Success() throws Exception {
        Schedule s = mock(Schedule.class);
        java.time.LocalDate d = java.time.LocalDate.now().plusDays(1);
        when(s.getWorkDate()).thenReturn(d);
        when(schedDAO.getFutureSchedules()).thenReturn(List.of(s));

        OffsetDateTime future = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2);
        Appointment a = mock(Appointment.class);
        when(a.getId()).thenReturn(5L);
        when(a.getStartTime()).thenReturn(future);
        when(a.getEndTime()).thenReturn(future.plusMinutes(30));
        when(a.getType()).thenReturn("FIRST_VISIT");
        when(a.getCreatedBy()).thenReturn(1L);
        when(a.getSlotId()).thenReturn(null);
        when(apptDAO.getFutureAppointmentsByDate(d)).thenReturn(List.of(a));
        when(apptDAO.getAppointmentById(5L)).thenReturn(a);
        when(userDAO.getUserById(anyString())).thenReturn(Optional.empty());

        feedInput("1\n5\nTest reason\n");
        callPrivateWithDAOs("adminCancelAppointment",
            new Class[]{AppointmentDAO.class, TimeSlotDAO.class, ScheduleDAO.class, UserDAO.class},
            apptDAO, slotDAO, schedDAO, userDAO);
        assertTrue(output().contains("Appointment canceled"));
        verify(apptDAO).cancelByAdmin(5L, "Test reason");
    }

    // ================================================================
    //  adminAddWorkDay
    // ================================================================

    @Test
    void testAdminAddWorkDay_InvalidFormat() throws Exception {
        when(schedDAO.getFutureSchedules()).thenReturn(Collections.emptyList());
        feedInput("not-a-date\n");
        callPrivateWithDAOs("adminAddWorkDay",
            new Class[]{ScheduleDAO.class}, schedDAO);
        assertTrue(output().contains("Invalid format"));
    }

    @Test
    void testAdminAddWorkDay_PastDate() throws Exception {
        when(schedDAO.getFutureSchedules()).thenReturn(Collections.emptyList());
        feedInput("2020-01-01\n");
        callPrivateWithDAOs("adminAddWorkDay",
            new Class[]{ScheduleDAO.class}, schedDAO);
        assertTrue(output().contains("Date must be today or in the future"));
    }

    @Test
    void testAdminAddWorkDay_AlreadyExists() throws Exception {
        when(schedDAO.getFutureSchedules()).thenReturn(Collections.emptyList());
        java.time.LocalDate future = java.time.LocalDate.now().plusDays(5);
        when(schedDAO.existsByDate(future)).thenReturn(true);
        feedInput(future.toString() + "\n");
        callPrivateWithDAOs("adminAddWorkDay",
            new Class[]{ScheduleDAO.class}, schedDAO);
        assertTrue(output().contains("already exists"));
    }

    @Test
    void testAdminAddWorkDay_Success() throws Exception {
        when(schedDAO.getFutureSchedules()).thenReturn(Collections.emptyList());
        java.time.LocalDate future = java.time.LocalDate.now().plusDays(3);
        when(schedDAO.existsByDate(future)).thenReturn(false);
        feedInput(future.toString() + "\n");
        callPrivateWithDAOs("adminAddWorkDay",
            new Class[]{ScheduleDAO.class}, schedDAO);
        assertTrue(output().contains("Work day added successfully"));
    }

    // ================================================================
    //  adminViewDaySlots
    // ================================================================

    @Test
    void testAdminViewDaySlots_NoWorkDays() throws Exception {
        when(schedDAO.getFutureSchedules()).thenReturn(Collections.emptyList());
        callPrivateWithDAOs("adminViewDaySlots",
            new Class[]{TimeSlotDAO.class, ScheduleDAO.class}, slotDAO, schedDAO);
        assertTrue(output().contains("No work days"));
    }

    @Test
    void testAdminViewDaySlots_NoSlots() throws Exception {
        Schedule s = mock(Schedule.class);
        when(s.getWorkDate()).thenReturn(java.time.LocalDate.now().plusDays(1));
        when(s.getId()).thenReturn(1L);
        when(schedDAO.getFutureSchedules()).thenReturn(List.of(s));
        when(slotDAO.getAllSlotsBySchedule(1L)).thenReturn(Collections.emptyList());
        feedInput("1\n");
        callPrivateWithDAOs("adminViewDaySlots",
            new Class[]{TimeSlotDAO.class, ScheduleDAO.class}, slotDAO, schedDAO);
        assertTrue(output().contains("No slots for that day"));
    }

    @Test
    void testAdminViewDaySlots_WithAvailableSlot() throws Exception {
        Schedule s = mock(Schedule.class);
        when(s.getWorkDate()).thenReturn(java.time.LocalDate.now().plusDays(1));
        when(s.getId()).thenReturn(1L);
        when(schedDAO.getFutureSchedules()).thenReturn(List.of(s));

        TimeSlot slot = mock(TimeSlot.class);
        when(slot.isAvailable()).thenReturn(true);
        when(slot.getStartTime()).thenReturn(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(slotDAO.getAllSlotsBySchedule(1L)).thenReturn(List.of(slot));

        feedInput("1\n");
        callPrivateWithDAOs("adminViewDaySlots",
            new Class[]{TimeSlotDAO.class, ScheduleDAO.class}, slotDAO, schedDAO);
        assertTrue(output().contains("Available"));
    }

    @Test
    void testAdminViewDaySlots_WithBookedSlot() throws Exception {
        Schedule s = mock(Schedule.class);
        when(s.getWorkDate()).thenReturn(java.time.LocalDate.now().plusDays(1));
        when(s.getId()).thenReturn(1L);
        when(schedDAO.getFutureSchedules()).thenReturn(List.of(s));

        TimeSlot slot = mock(TimeSlot.class);
        when(slot.isAvailable()).thenReturn(false);
        when(slot.getId()).thenReturn(10L);
        when(slot.getStartTime()).thenReturn(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(slotDAO.getAllSlotsBySchedule(1L)).thenReturn(List.of(slot));
        when(slotDAO.getBookedByUsername(10L)).thenReturn("testuser");

        feedInput("1\n");
        callPrivateWithDAOs("adminViewDaySlots",
            new Class[]{TimeSlotDAO.class, ScheduleDAO.class}, slotDAO, schedDAO);
        assertTrue(output().contains("Booked") && output().contains("testuser"));
    }

    // ================================================================
    //  adminAddSlot
    // ================================================================

    @Test
    void testAdminAddSlot_NoWorkDays() throws Exception {
        when(schedDAO.getFutureSchedules()).thenReturn(Collections.emptyList());
        callPrivateWithDAOs("adminAddSlot",
            new Class[]{TimeSlotDAO.class, ScheduleDAO.class}, slotDAO, schedDAO);
        assertTrue(output().contains("No work days"));
    }

    @Test
    void testAdminAddSlot_InvalidHour() throws Exception {
        Schedule s = mock(Schedule.class);
        when(s.getWorkDate()).thenReturn(java.time.LocalDate.now().plusDays(1));
        when(s.getId()).thenReturn(1L);
        when(schedDAO.getFutureSchedules()).thenReturn(List.of(s));
        when(slotDAO.getAllSlotsBySchedule(1L)).thenReturn(Collections.emptyList());
        feedInput("1\n99\n"); // 99 is out of 0-23
        callPrivateWithDAOs("adminAddSlot",
            new Class[]{TimeSlotDAO.class, ScheduleDAO.class}, slotDAO, schedDAO);
        assertTrue(output().contains("Invalid input"));
    }

    @Test
    void testAdminAddSlot_SlotAlreadyExists() throws Exception {
        Schedule s = mock(Schedule.class);
        java.time.LocalDate d = java.time.LocalDate.now().plusDays(1);
        when(s.getWorkDate()).thenReturn(d);
        when(s.getId()).thenReturn(1L);
        when(schedDAO.getFutureSchedules()).thenReturn(List.of(s));
        when(slotDAO.getAllSlotsBySchedule(1L)).thenReturn(Collections.emptyList());
        when(slotDAO.existsByScheduleAndStart(anyLong(), any())).thenReturn(true);
        feedInput("1\n9\n");
        callPrivateWithDAOs("adminAddSlot",
            new Class[]{TimeSlotDAO.class, ScheduleDAO.class}, slotDAO, schedDAO);
        assertTrue(output().contains("already exists"));
    }

    @Test
    void testAdminAddSlot_Success() throws Exception {
        Schedule s = mock(Schedule.class);
        java.time.LocalDate d = java.time.LocalDate.now().plusDays(1);
        when(s.getWorkDate()).thenReturn(d);
        when(s.getId()).thenReturn(1L);
        when(schedDAO.getFutureSchedules()).thenReturn(List.of(s));
        when(slotDAO.getAllSlotsBySchedule(1L)).thenReturn(Collections.emptyList());
        when(slotDAO.existsByScheduleAndStart(anyLong(), any())).thenReturn(false);
        feedInput("1\n10\n");
        callPrivateWithDAOs("adminAddSlot",
            new Class[]{TimeSlotDAO.class, ScheduleDAO.class}, slotDAO, schedDAO);
        assertTrue(output().contains("Slot added"));
    }

    // ================================================================
    //  adminViewAllUsers
    // ================================================================

    @Test
    void testAdminViewAllUsers_Empty() throws Exception {
        when(userDAO.getAllUsers()).thenReturn(Collections.emptyList());
        callPrivateWithDAOs("adminViewAllUsers",
            new Class[]{UserDAO.class}, userDAO);
        assertTrue(output().contains("All Users"));
    }

    @Test
    void testAdminViewAllUsers_WithUsers() throws Exception {
        User u = mock(User.class);
        when(u.getId()).thenReturn("1");
        when(u.getName()).thenReturn("Test User");
        when(u.getEmail()).thenReturn("test@test.com");
        when(u.getPhoneNumber()).thenReturn("0501234567");
        when(u.getRole()).thenReturn(User.Role.VISITOR);
        when(userDAO.getAllUsers()).thenReturn(List.of(u));
        callPrivateWithDAOs("adminViewAllUsers",
            new Class[]{UserDAO.class}, userDAO);
        assertTrue(output().contains("Test User"));
    }

    // ================================================================
    //  visitorMyAppointments
    // ================================================================

    @Test
    void testVisitorMyAppointments_Empty() throws Exception {
        User visitor = mock(User.class);
        when(visitor.getId()).thenReturn("1");
        when(apptDAO.getAppointmentsByUser(1L)).thenReturn(Collections.emptyList());
        callPrivateWithDAOs("visitorMyAppointments",
            new Class[]{User.class, AppointmentDAO.class}, visitor, apptDAO);
        assertTrue(output().contains("You have no appointments"));
    }

    @Test
    void testVisitorMyAppointments_OnlySelfCanceled() throws Exception {
        User visitor = mock(User.class);
        when(visitor.getId()).thenReturn("1");
        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CANCELED);
        when(a.isCanceledByAdmin()).thenReturn(false);
        when(apptDAO.getAppointmentsByUser(1L)).thenReturn(List.of(a));
        callPrivateWithDAOs("visitorMyAppointments",
            new Class[]{User.class, AppointmentDAO.class}, visitor, apptDAO);
        assertTrue(output().contains("You have no appointments"));
    }

    @Test
    void testVisitorMyAppointments_ShowDone() throws Exception {
        User visitor = mock(User.class);
        when(visitor.getId()).thenReturn("1");
        OffsetDateTime t = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn(Appointment.STATUS_DONE);
        when(a.isCanceledByAdmin()).thenReturn(false);
        when(a.getStartTime()).thenReturn(t);
        when(a.getEndTime()).thenReturn(t.plusMinutes(30));
        when(a.getType()).thenReturn("FOLLOW_UP");
        when(a.isGroup()).thenReturn(false);
        when(a.getAdminNote()).thenReturn(null);
        when(apptDAO.getAppointmentsByUser(1L)).thenReturn(List.of(a));
        callPrivateWithDAOs("visitorMyAppointments",
            new Class[]{User.class, AppointmentDAO.class}, visitor, apptDAO);
        assertTrue(output().contains("DONE"));
    }

    @Test
    void testVisitorMyAppointments_ShowCanceledByAdmin() throws Exception {
        User visitor = mock(User.class);
        when(visitor.getId()).thenReturn("1");
        OffsetDateTime t = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CANCELED);
        when(a.isCanceledByAdmin()).thenReturn(true);
        when(a.getStartTime()).thenReturn(t);
        when(a.getEndTime()).thenReturn(t.plusMinutes(30));
        when(a.getType()).thenReturn("VIRTUAL");
        when(a.isGroup()).thenReturn(false);
        when(a.getAdminNote()).thenReturn("Admin note here");
        when(apptDAO.getAppointmentsByUser(1L)).thenReturn(List.of(a));
        callPrivateWithDAOs("visitorMyAppointments",
            new Class[]{User.class, AppointmentDAO.class}, visitor, apptDAO);
        assertTrue(output().contains("CANCELED by Admin"));
        assertTrue(output().contains("Admin note here"));
    }

    @Test
    void testVisitorMyAppointments_UpcomingWithin24h() throws Exception {
        User visitor = mock(User.class);
        when(visitor.getId()).thenReturn("1");
        OffsetDateTime t = OffsetDateTime.now(ZoneOffset.UTC).plusHours(2);
        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);
        when(a.isCanceledByAdmin()).thenReturn(false);
        when(a.getStartTime()).thenReturn(t);
        when(a.getEndTime()).thenReturn(t.plusMinutes(30));
        when(a.getType()).thenReturn("FIRST_VISIT");
        when(a.isGroup()).thenReturn(false);
        when(a.getAdminNote()).thenReturn(null);
        when(apptDAO.getAppointmentsByUser(1L)).thenReturn(List.of(a));
        callPrivateWithDAOs("visitorMyAppointments",
            new Class[]{User.class, AppointmentDAO.class}, visitor, apptDAO);
        assertTrue(output().contains("Less than 24h remaining"));
    }

    @Test
    void testVisitorMyAppointments_UpcomingFar() throws Exception {
        User visitor = mock(User.class);
        when(visitor.getId()).thenReturn("1");
        OffsetDateTime t = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5);
        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);
        when(a.isCanceledByAdmin()).thenReturn(false);
        when(a.getStartTime()).thenReturn(t);
        when(a.getEndTime()).thenReturn(t.plusMinutes(30));
        when(a.getType()).thenReturn("FIRST_VISIT");
        when(a.isGroup()).thenReturn(false);
        when(a.getAdminNote()).thenReturn(null);
        when(apptDAO.getAppointmentsByUser(1L)).thenReturn(List.of(a));
        callPrivateWithDAOs("visitorMyAppointments",
            new Class[]{User.class, AppointmentDAO.class}, visitor, apptDAO);
        assertTrue(output().contains("[Upcoming]"));
    }

    @Test
    void testVisitorMyAppointments_GroupAppointment() throws Exception {
        User visitor = mock(User.class);
        when(visitor.getId()).thenReturn("1");
        OffsetDateTime t = OffsetDateTime.now(ZoneOffset.UTC).plusDays(3);
        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);
        when(a.isCanceledByAdmin()).thenReturn(false);
        when(a.getStartTime()).thenReturn(t);
        when(a.getEndTime()).thenReturn(t.plusMinutes(45));
        when(a.getType()).thenReturn("GROUP_FIRST_VISIT");
        when(a.isGroup()).thenReturn(true);
        when(a.getParticipantsCount()).thenReturn(3);
        when(a.getAdminNote()).thenReturn(null);
        when(apptDAO.getAppointmentsByUser(1L)).thenReturn(List.of(a));
        callPrivateWithDAOs("visitorMyAppointments",
            new Class[]{User.class, AppointmentDAO.class}, visitor, apptDAO);
        assertTrue(output().contains("visitors: 3"));
    }

    // ================================================================
    //  visitorEdit
    // ================================================================

    @Test
    void testVisitorEdit_NoFutureAppointments() throws Exception {
        User visitor = mock(User.class);
        when(visitor.getId()).thenReturn("1");
        when(apptDAO.getAppointmentsByUser(1L)).thenReturn(Collections.emptyList());
        callPrivateWithDAOs("visitorEdit",
            new Class[]{User.class, AppointmentDAO.class}, visitor, apptDAO);
        assertTrue(output().contains("No future appointments to edit"));
    }

    @Test
    void testVisitorEdit_Within24h_Blocked() throws Exception {
        User visitor = mock(User.class);
        when(visitor.getId()).thenReturn("1");
        OffsetDateTime soon = OffsetDateTime.now(ZoneOffset.UTC).plusHours(2);
        Appointment a = mock(Appointment.class);
        when(a.getId()).thenReturn(1L);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);
        when(a.getStartTime()).thenReturn(soon);
        when(a.getEndTime()).thenReturn(soon.plusMinutes(30));
        when(a.getType()).thenReturn("FIRST_VISIT");
        when(apptDAO.getAppointmentsByUser(1L)).thenReturn(List.of(a));
        feedInput("1\n");
        callPrivateWithDAOs("visitorEdit",
            new Class[]{User.class, AppointmentDAO.class}, visitor, apptDAO);
        assertTrue(output().contains("cannot edit"));
        assertTrue(output().contains("100 NIS"));
    }

    @Test
    void testVisitorEdit_AppointmentNotFound() throws Exception {
        User visitor = mock(User.class);
        when(visitor.getId()).thenReturn("1");
        OffsetDateTime far = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5);
        Appointment a = mock(Appointment.class);
        when(a.getId()).thenReturn(1L);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);
        when(a.getStartTime()).thenReturn(far);
        when(a.getEndTime()).thenReturn(far.plusMinutes(30));
        when(a.getType()).thenReturn("FIRST_VISIT");
        when(apptDAO.getAppointmentsByUser(1L)).thenReturn(List.of(a));
        feedInput("999\n"); // wrong ID
        callPrivateWithDAOs("visitorEdit",
            new Class[]{User.class, AppointmentDAO.class}, visitor, apptDAO);
        assertTrue(output().contains("not found or not yours"));
    }

    @Test
    void testVisitorEdit_ChangeType_Individual_FirstVisit() throws Exception {
        User visitor = mock(User.class);
        when(visitor.getId()).thenReturn("1");
        OffsetDateTime far = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5);
        Appointment a = mock(Appointment.class);
        when(a.getId()).thenReturn(1L);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);
        when(a.getStartTime()).thenReturn(far);
        when(a.getEndTime()).thenReturn(far.plusMinutes(30));
        when(a.getType()).thenReturn("FOLLOW_UP");
        when(apptDAO.getAppointmentsByUser(1L)).thenReturn(List.of(a));
        feedInput("1\n1\n1\n1\n"); // ID=1, choice=1(type), cat=1(individual), vt=1(first visit)
        callPrivateWithDAOs("visitorEdit",
            new Class[]{User.class, AppointmentDAO.class}, visitor, apptDAO);
        assertTrue(output().contains("Type updated"));
    }

    @Test
    void testVisitorEdit_ChangeVisitorCount_NotGroup() throws Exception {
        User visitor = mock(User.class);
        when(visitor.getId()).thenReturn("1");
        OffsetDateTime far = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5);
        Appointment a = mock(Appointment.class);
        when(a.getId()).thenReturn(1L);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);
        when(a.getStartTime()).thenReturn(far);
        when(a.getEndTime()).thenReturn(far.plusMinutes(30));
        when(a.getType()).thenReturn("FIRST_VISIT");
        when(a.isGroup()).thenReturn(false);
        when(apptDAO.getAppointmentsByUser(1L)).thenReturn(List.of(a));
        feedInput("1\n2\n"); // ID=1, choice=2(count) but not group
        callPrivateWithDAOs("visitorEdit",
            new Class[]{User.class, AppointmentDAO.class}, visitor, apptDAO);
        assertTrue(output().contains("Only Group appointments"));
    }

    @Test
    void testVisitorEdit_ChangeVisitorCount_Success() throws Exception {
        User visitor = mock(User.class);
        when(visitor.getId()).thenReturn("1");
        OffsetDateTime far = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5);
        Appointment a = mock(Appointment.class);
        when(a.getId()).thenReturn(1L);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);
        when(a.getStartTime()).thenReturn(far);
        when(a.getEndTime()).thenReturn(far.plusMinutes(30));
        when(a.getType()).thenReturn("GROUP_FIRST_VISIT");
        when(a.isGroup()).thenReturn(true);
        when(apptDAO.getAppointmentsByUser(1L)).thenReturn(List.of(a));
        feedInput("1\n2\n3\n"); // ID=1, choice=2(count), count=3
        callPrivateWithDAOs("visitorEdit",
            new Class[]{User.class, AppointmentDAO.class}, visitor, apptDAO);
        assertTrue(output().contains("Visitor count updated"));
    }

    // ================================================================
    //  visitorCancel
    // ================================================================

    @Test
    void testVisitorCancel_NoFutureAppointments() throws Exception {
        User visitor = mock(User.class);
        when(visitor.getId()).thenReturn("1");
        when(apptDAO.getAppointmentsByUser(1L)).thenReturn(Collections.emptyList());
        callPrivateWithDAOs("visitorCancel",
            new Class[]{User.class, AppointmentDAO.class, TimeSlotDAO.class},
            visitor, apptDAO, slotDAO);
        assertTrue(output().contains("No future appointments to cancel"));
    }

    @Test
    void testVisitorCancel_Within24h_Blocked() throws Exception {
        User visitor = mock(User.class);
        when(visitor.getId()).thenReturn("1");
        OffsetDateTime soon = OffsetDateTime.now(ZoneOffset.UTC).plusHours(2);
        Appointment a = mock(Appointment.class);
        when(a.getId()).thenReturn(1L);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);
        when(a.getStartTime()).thenReturn(soon);
        when(a.getEndTime()).thenReturn(soon.plusMinutes(30));
        when(a.getType()).thenReturn("FIRST_VISIT");
        when(apptDAO.getAppointmentsByUser(1L)).thenReturn(List.of(a));
        feedInput("1\n");
        callPrivateWithDAOs("visitorCancel",
            new Class[]{User.class, AppointmentDAO.class, TimeSlotDAO.class},
            visitor, apptDAO, slotDAO);
        assertTrue(output().contains("cannot cancel"));
        assertTrue(output().contains("200 NIS"));
    }

    @Test
    void testVisitorCancel_Aborted() throws Exception {
        User visitor = mock(User.class);
        when(visitor.getId()).thenReturn("1");
        OffsetDateTime far = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5);
        Appointment a = mock(Appointment.class);
        when(a.getId()).thenReturn(1L);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);
        when(a.getStartTime()).thenReturn(far);
        when(a.getEndTime()).thenReturn(far.plusMinutes(30));
        when(a.getType()).thenReturn("FIRST_VISIT");
        when(apptDAO.getAppointmentsByUser(1L)).thenReturn(List.of(a));
        feedInput("1\nno\n"); // confirm = no
        callPrivateWithDAOs("visitorCancel",
            new Class[]{User.class, AppointmentDAO.class, TimeSlotDAO.class},
            visitor, apptDAO, slotDAO);
        assertTrue(output().contains("Aborted"));
    }

    @Test
    void testVisitorCancel_Success_NoSlot() throws Exception {
        User visitor = mock(User.class);
        when(visitor.getId()).thenReturn("1");
        OffsetDateTime far = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5);
        Appointment a = mock(Appointment.class);
        when(a.getId()).thenReturn(1L);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);
        when(a.getStartTime()).thenReturn(far);
        when(a.getEndTime()).thenReturn(far.plusMinutes(30));
        when(a.getType()).thenReturn("FIRST_VISIT");
        when(a.getSlotId()).thenReturn(null);
        when(apptDAO.getAppointmentsByUser(1L)).thenReturn(List.of(a));
        feedInput("1\nyes\n");
        callPrivateWithDAOs("visitorCancel",
            new Class[]{User.class, AppointmentDAO.class, TimeSlotDAO.class},
            visitor, apptDAO, slotDAO);
        assertTrue(output().contains("Appointment canceled"));
        verify(apptDAO).updateStatus(1L, Appointment.STATUS_CANCELED);
        verify(slotDAO, never()).updateAvailability(anyLong(), anyBoolean());
    }

    @Test
    void testVisitorCancel_Success_WithSlot() throws Exception {
        User visitor = mock(User.class);
        when(visitor.getId()).thenReturn("1");
        OffsetDateTime far = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5);
        Appointment a = mock(Appointment.class);
        when(a.getId()).thenReturn(1L);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);
        when(a.getStartTime()).thenReturn(far);
        when(a.getEndTime()).thenReturn(far.plusMinutes(30));
        when(a.getType()).thenReturn("FIRST_VISIT");
        when(a.getSlotId()).thenReturn(42L);
        when(apptDAO.getAppointmentsByUser(1L)).thenReturn(List.of(a));
        feedInput("1\nyes\n");
        callPrivateWithDAOs("visitorCancel",
            new Class[]{User.class, AppointmentDAO.class, TimeSlotDAO.class},
            visitor, apptDAO, slotDAO);
        assertTrue(output().contains("Slot is now free"));
        verify(slotDAO).updateAvailability(42L, true);
    }

    // ================================================================
    //  visitorBook – edge cases
    // ================================================================

    @Test
    void testVisitorBook_NoWorkDays() throws Exception {
        User visitor = mock(User.class);
        when(visitor.getId()).thenReturn("1");
        when(schedDAO.getFutureSchedules()).thenReturn(Collections.emptyList());
        callPrivateWithDAOs("visitorBook",
            new Class[]{User.class, AppointmentDAO.class, TimeSlotDAO.class, ScheduleDAO.class},
            visitor, apptDAO, slotDAO, schedDAO);
        assertTrue(output().contains("No available work days"));
    }

    @Test
    void testVisitorBook_NoAvailableSlots() throws Exception {
        User visitor = mock(User.class);
        when(visitor.getId()).thenReturn("1");
        Schedule s = mock(Schedule.class);
        when(s.getWorkDate()).thenReturn(java.time.LocalDate.now().plusDays(1));
        when(s.getId()).thenReturn(1L);
        when(schedDAO.getFutureSchedules()).thenReturn(List.of(s));
        when(slotDAO.getAvailableSlotsByScheduleId(1L)).thenReturn(Collections.emptyList());
        feedInput("1\n");
        callPrivateWithDAOs("visitorBook",
            new Class[]{User.class, AppointmentDAO.class, TimeSlotDAO.class, ScheduleDAO.class},
            visitor, apptDAO, slotDAO, schedDAO);
        assertTrue(output().contains("No available slots"));
    }

    @Test
    void testVisitorBook_InvalidCategory() throws Exception {
        User visitor = mock(User.class);
        when(visitor.getId()).thenReturn("1");
        Schedule s = mock(Schedule.class);
        when(s.getWorkDate()).thenReturn(java.time.LocalDate.now().plusDays(1));
        when(s.getId()).thenReturn(1L);
        when(schedDAO.getFutureSchedules()).thenReturn(List.of(s));
        TimeSlot slot = mock(TimeSlot.class);
        when(slot.getStartTime()).thenReturn(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(slotDAO.getAvailableSlotsByScheduleId(1L)).thenReturn(List.of(slot));
        feedInput("1\n1\n30\n9\n"); // day=1, slot=1, duration=30, category=9(invalid)
        callPrivateWithDAOs("visitorBook",
            new Class[]{User.class, AppointmentDAO.class, TimeSlotDAO.class, ScheduleDAO.class},
            visitor, apptDAO, slotDAO, schedDAO);
        assertTrue(output().contains("Invalid category"));
    }

    @Test
    void testVisitorBook_Individual_FirstVisit_Success() throws Exception {
        User visitor = mock(User.class);
        when(visitor.getId()).thenReturn("1");
        Schedule s = mock(Schedule.class);
        java.time.LocalDate d = java.time.LocalDate.now().plusDays(1);
        when(s.getWorkDate()).thenReturn(d);
        when(s.getId()).thenReturn(1L);
        when(schedDAO.getFutureSchedules()).thenReturn(List.of(s));
        TimeSlot slot = mock(TimeSlot.class);
        when(slot.getId()).thenReturn(5L);
        when(slot.getStartTime()).thenReturn(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(slotDAO.getAvailableSlotsByScheduleId(1L)).thenReturn(List.of(slot));
        // day=1, slot=1, duration=30, category=1(individual), type=1(first visit)
        feedInput("1\n1\n30\n1\n1\n");
        callPrivateWithDAOs("visitorBook",
            new Class[]{User.class, AppointmentDAO.class, TimeSlotDAO.class, ScheduleDAO.class},
            visitor, apptDAO, slotDAO, schedDAO);
        assertTrue(output().contains("booked successfully"));
        verify(apptDAO).addAppointment(any());
        verify(slotDAO).updateAvailability(5L, false);
    }

    @Test
    void testVisitorBook_Group_FollowUp_Success() throws Exception {
        User visitor = mock(User.class);
        when(visitor.getId()).thenReturn("1");
        Schedule s = mock(Schedule.class);
        java.time.LocalDate d = java.time.LocalDate.now().plusDays(1);
        when(s.getWorkDate()).thenReturn(d);
        when(s.getId()).thenReturn(1L);
        when(schedDAO.getFutureSchedules()).thenReturn(List.of(s));
        TimeSlot slot = mock(TimeSlot.class);
        when(slot.getId()).thenReturn(5L);
        when(slot.getStartTime()).thenReturn(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(slotDAO.getAvailableSlotsByScheduleId(1L)).thenReturn(List.of(slot));
        // day=1, slot=1, duration=45, category=2(group), count=3, type=2(follow-up)
        feedInput("1\n1\n45\n2\n3\n2\n");
        callPrivateWithDAOs("visitorBook",
            new Class[]{User.class, AppointmentDAO.class, TimeSlotDAO.class, ScheduleDAO.class},
            visitor, apptDAO, slotDAO, schedDAO);
        assertTrue(output().contains("booked successfully"));
        assertTrue(output().contains("Visitors : 3"));
    }

    // ================================================================
    //  adminEditAppointment – edge cases
    // ================================================================

    @Test
    void testAdminEditAppointment_NoFutureDays() throws Exception {
        when(schedDAO.getFutureSchedules()).thenReturn(Collections.emptyList());
        callPrivateWithDAOs("adminEditAppointment",
            new Class[]{AppointmentDAO.class, ScheduleDAO.class, UserDAO.class},
            apptDAO, schedDAO, userDAO);
        assertTrue(output().contains("No future work days"));
    }

    @Test
    void testAdminEditAppointment_InvalidChoice() throws Exception {
        Schedule s = mock(Schedule.class);
        java.time.LocalDate d = java.time.LocalDate.now().plusDays(1);
        when(s.getWorkDate()).thenReturn(d);
        when(schedDAO.getFutureSchedules()).thenReturn(List.of(s));

        OffsetDateTime future = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2);
        Appointment a = mock(Appointment.class);
        when(a.getId()).thenReturn(1L);
        when(a.getStartTime()).thenReturn(future);
        when(a.getEndTime()).thenReturn(future.plusMinutes(30));
        when(a.getType()).thenReturn("FIRST_VISIT");
        when(a.getCreatedBy()).thenReturn(1L);
        when(apptDAO.getFutureAppointmentsByDate(d)).thenReturn(List.of(a));
        when(apptDAO.getAppointmentById(1L)).thenReturn(a);
        when(userDAO.getUserById(anyString())).thenReturn(Optional.empty());

        feedInput("1\n1\n9\n"); // choice=9(invalid edit option)
        callPrivateWithDAOs("adminEditAppointment",
            new Class[]{AppointmentDAO.class, ScheduleDAO.class, UserDAO.class},
            apptDAO, schedDAO, userDAO);
        assertTrue(output().contains("Invalid choice"));
    }

    @Test
    void testAdminEditAppointment_ChangeType_Success() throws Exception {
        Schedule s = mock(Schedule.class);
        java.time.LocalDate d = java.time.LocalDate.now().plusDays(1);
        when(s.getWorkDate()).thenReturn(d);
        when(schedDAO.getFutureSchedules()).thenReturn(List.of(s));

        OffsetDateTime future = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2);
        Appointment a = mock(Appointment.class);
        when(a.getId()).thenReturn(1L);
        when(a.getStartTime()).thenReturn(future);
        when(a.getEndTime()).thenReturn(future.plusMinutes(30));
        when(a.getType()).thenReturn("FIRST_VISIT");
        when(a.getCreatedBy()).thenReturn(1L);
        when(apptDAO.getFutureAppointmentsByDate(d)).thenReturn(List.of(a));
        when(apptDAO.getAppointmentById(1L)).thenReturn(a);
        when(userDAO.getUserById(anyString())).thenReturn(Optional.empty());

        // day=1, apptId=1, edit=1(type), cat=2(group), vt=3(virtual), note=""
        feedInput("1\n1\n1\n2\n3\n\n");
        callPrivateWithDAOs("adminEditAppointment",
            new Class[]{AppointmentDAO.class, ScheduleDAO.class, UserDAO.class},
            apptDAO, schedDAO, userDAO);
        assertTrue(output().contains("Appointment updated"));
        verify(apptDAO).updateTypeAndNote(eq(1L), eq(Appointment.TYPE_GROUP_VIRTUAL), isNull());
    }
}