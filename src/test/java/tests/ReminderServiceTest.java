package tests;

import domain.Appointment;
import domain.User;
import org.junit.jupiter.api.*;
import persistence.AppointmentDAO;
import persistence.UserDAO;
import service.ReminderService;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.mockito.Mockito.*;

class ReminderServiceTest {

    private AppointmentDAO appointmentDAO;
    private UserDAO userDAO;
    private ReminderService reminderService;

    @BeforeEach
    void setUp() {
        appointmentDAO = mock(AppointmentDAO.class);
        userDAO = mock(UserDAO.class);

        // ✅ لا static mocking
        reminderService = new ReminderService(appointmentDAO, userDAO);
    }

    @Test
    void sendReminders_noAppointments_skipped() throws SQLException {
        when(appointmentDAO.getAllAppointments())
                .thenReturn(Collections.emptyList());

        reminderService.sendReminders();

        verify(appointmentDAO).getAllAppointments();
        verifyNoInteractions(userDAO);
    }

    @Test
    void sendReminders_appointmentNotConfirmed_skipped() throws SQLException {
        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn("PENDING");

        when(appointmentDAO.getAllAppointments())
                .thenReturn(Collections.singletonList(a));

        reminderService.sendReminders();

        verifyNoInteractions(userDAO);
    }

    @Test
    void sendReminders_confirmedButStartTimeNull_skipped() throws SQLException {
        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);
        when(a.getStartTime()).thenReturn(null);

        when(appointmentDAO.getAllAppointments())
                .thenReturn(Collections.singletonList(a));

        reminderService.sendReminders();

        verifyNoInteractions(userDAO);
    }

    @Test
    void sendReminders_confirmedOutsideWindow_skipped() throws SQLException {
        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);
        when(a.getStartTime())
                .thenReturn(OffsetDateTime.now(ZoneOffset.UTC).plusHours(48));

        when(appointmentDAO.getAllAppointments())
                .thenReturn(Collections.singletonList(a));

        reminderService.sendReminders();

        verifyNoInteractions(userDAO);
    }

    @Test
    void sendReminders_confirmedPast_skipped() throws SQLException {
        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);
        when(a.getStartTime())
                .thenReturn(OffsetDateTime.now(ZoneOffset.UTC).minusHours(1));

        when(appointmentDAO.getAllAppointments())
                .thenReturn(Collections.singletonList(a));

        reminderService.sendReminders();

        verifyNoInteractions(userDAO);
    }

    @Test
    void sendReminders_confirmedWithinWindow_userNotFound() throws SQLException {
        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);
        when(a.getStartTime())
                .thenReturn(OffsetDateTime.now(ZoneOffset.UTC).plusHours(12));
        when(a.getCreatedBy()).thenReturn(99L);

        when(appointmentDAO.getAllAppointments())
                .thenReturn(Collections.singletonList(a));
        when(userDAO.getUserById("99")).thenReturn(Optional.empty());

        reminderService.sendReminders();

        verify(userDAO).getUserById("99");
    }

    @Test
    void sendReminders_userEmailBlank_skipped() throws SQLException {
        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);
        when(a.getStartTime())
                .thenReturn(OffsetDateTime.now(ZoneOffset.UTC).plusHours(12));
        when(a.getCreatedBy()).thenReturn(2L);

        User u = mock(User.class);
        when(u.getEmail()).thenReturn("   ");

        when(appointmentDAO.getAllAppointments())
                .thenReturn(Collections.singletonList(a));
        when(userDAO.getUserById("2")).thenReturn(Optional.of(u));

        reminderService.sendReminders();

        verify(userDAO).getUserById("2");
    }

    @Test
    void sendReminders_daoThrowsSQLException_handledGracefully() throws SQLException {
        when(appointmentDAO.getAllAppointments())
                .thenThrow(new SQLException("DB down"));

        reminderService.sendReminders();

        verify(appointmentDAO).getAllAppointments();
    }

    @Test
    void sendReminders_multipleAppointments_onlyOneProcessed() throws SQLException {
        OffsetDateTime inWindow = OffsetDateTime.now(ZoneOffset.UTC).plusHours(10);

        Appointment confirmed = mock(Appointment.class);
        when(confirmed.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);
        when(confirmed.getStartTime()).thenReturn(inWindow);
        when(confirmed.getCreatedBy()).thenReturn(10L);

        Appointment pending = mock(Appointment.class);
        when(pending.getStatus()).thenReturn("PENDING");

        when(appointmentDAO.getAllAppointments())
                .thenReturn(Arrays.asList(confirmed, pending));
        when(userDAO.getUserById("10")).thenReturn(Optional.empty());

        reminderService.sendReminders();

        verify(userDAO).getUserById("10");
    }
    
    @Test
    void sendReminders_confirmedWithinWindow_validUser_sendsEmail() throws SQLException {
        OffsetDateTime inWindow = OffsetDateTime.now(ZoneOffset.UTC).plusHours(5);

        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);
        when(a.getStartTime()).thenReturn(inWindow);
        when(a.getEndTime()).thenReturn(inWindow.plusMinutes(30));
        when(a.getCreatedBy()).thenReturn(1L);
        when(a.getType()).thenReturn("FIRST_VISIT");
        when(a.isGroup()).thenReturn(false);

        User u = mock(User.class);
        when(u.getEmail()).thenReturn("test@example.com");
        when(u.getName()).thenReturn("Noora");

        when(appointmentDAO.getAllAppointments()).thenReturn(Collections.singletonList(a));
        when(userDAO.getUserById("1")).thenReturn(Optional.of(u));

        ReminderService spyService = spy(reminderService);
        doNothing().when(spyService).sendEmail(any(User.class), any(Appointment.class));

        spyService.sendReminders();

        verify(spyService).sendEmail(u, a);
    }
    
    @Test
    void friendlyType_allVariantsCovered() {
        Assertions.assertEquals("Individual – First Visit", reminderService.friendlyType("FIRST_VISIT"));
        Assertions.assertEquals("Individual – Follow-up", reminderService.friendlyType("FOLLOW_UP"));
        Assertions.assertEquals("Individual – Virtual", reminderService.friendlyType("VIRTUAL"));
        Assertions.assertEquals("Group – First Visit", reminderService.friendlyType("GROUP_FIRST_VISIT"));
        Assertions.assertEquals("Group – Follow-up", reminderService.friendlyType("GROUP_FOLLOW_UP"));
        Assertions.assertEquals("Group – Virtual", reminderService.friendlyType("GROUP_VIRTUAL"));
        Assertions.assertEquals("Unknown", reminderService.friendlyType(null));
        Assertions.assertEquals("OTHER", reminderService.friendlyType("OTHER"));
    }



    @Test
    void sendReminders_boundaryJustUnder24h() throws SQLException {
        OffsetDateTime boundary =
                OffsetDateTime.now(ZoneOffset.UTC).plusHours(23).plusMinutes(59);

        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);
        when(a.getStartTime()).thenReturn(boundary);
        when(a.getCreatedBy()).thenReturn(7L);

        when(appointmentDAO.getAllAppointments())
                .thenReturn(Collections.singletonList(a));
        when(userDAO.getUserById("7")).thenReturn(Optional.empty());

        reminderService.sendReminders();

        verify(userDAO).getUserById("7");
    }
}