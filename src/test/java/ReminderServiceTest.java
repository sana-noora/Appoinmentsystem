

import domain.Appointment;
import domain.User;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import persistence.AppointmentDAO;
import persistence.UserDAO;
import service.ReminderService;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

class ReminderServiceTest {

    private AppointmentDAO appointmentDAO;
    private UserDAO        userDAO;
    private ReminderService reminderService;

    private static MockedStatic<io.github.cdimascio.dotenv.Dotenv> dotenvStatic;

    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        io.github.cdimascio.dotenv.Dotenv dotenvMock =
                mock(io.github.cdimascio.dotenv.Dotenv.class);
        when(dotenvMock.get("EMAIL_USERNAME")).thenReturn("test@gmail.com");
        when(dotenvMock.get("EMAIL_PASSWORD")).thenReturn("testpassword");

        dotenvStatic = Mockito.mockStatic(io.github.cdimascio.dotenv.Dotenv.class);
        dotenvStatic.when(io.github.cdimascio.dotenv.Dotenv::load).thenReturn(dotenvMock);
    }

    @AfterAll
    static void tearDownAfterClass() throws Exception {
        dotenvStatic.close();
    }

    @BeforeEach
    void setUp() throws Exception {
        appointmentDAO  = mock(AppointmentDAO.class);
        userDAO         = mock(UserDAO.class);
        reminderService = new ReminderService(appointmentDAO, userDAO);
    }

    @AfterEach
    void tearDown() throws Exception {
    }

    @Test
    void sendReminders_noAppointments_printsNoRemindersMessage() throws SQLException {
        when(appointmentDAO.getAllAppointments()).thenReturn(Collections.emptyList());

        reminderService.sendReminders();

        verify(appointmentDAO, times(1)).getAllAppointments();
        verifyNoInteractions(userDAO);
    }

    @Test
    void sendReminders_appointmentNotConfirmed_skipped() throws SQLException {
        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn("PENDING");

        when(appointmentDAO.getAllAppointments()).thenReturn(List.of(a));

        reminderService.sendReminders();

        verifyNoInteractions(userDAO);
    }

    @Test
    void sendReminders_confirmedButStartTimeNull_skipped() throws SQLException {
        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);
        when(a.getStartTime()).thenReturn(null);

        when(appointmentDAO.getAllAppointments()).thenReturn(List.of(a));

        reminderService.sendReminders();

        verifyNoInteractions(userDAO);
    }

    @Test
    void sendReminders_confirmedButStartTimeOutsideWindow_skipped() throws SQLException {
        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);
        when(a.getStartTime()).thenReturn(OffsetDateTime.now(ZoneOffset.UTC).plusHours(48));

        when(appointmentDAO.getAllAppointments()).thenReturn(List.of(a));

        reminderService.sendReminders();

        verifyNoInteractions(userDAO);
    }

    @Test
    void sendReminders_confirmedStartTimeAlreadyPast_skipped() throws SQLException {
        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);
        when(a.getStartTime()).thenReturn(OffsetDateTime.now(ZoneOffset.UTC).minusHours(1));

        when(appointmentDAO.getAllAppointments()).thenReturn(List.of(a));

        reminderService.sendReminders();

        verifyNoInteractions(userDAO);
    }

    @Test
    void sendReminders_confirmedWithinWindow_userNotFound_skipped() throws SQLException {
        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);
        when(a.getStartTime()).thenReturn(OffsetDateTime.now(ZoneOffset.UTC).plusHours(12));
        when(a.getCreatedBy()).thenReturn((long) 99);

        when(appointmentDAO.getAllAppointments()).thenReturn(List.of(a));
        when(userDAO.getUserById("99")).thenReturn(Optional.empty());

        reminderService.sendReminders();

        verify(userDAO, times(1)).getUserById("99");
    }

    @Test
    void sendReminders_confirmedWithinWindow_userEmailNull_skipped() throws SQLException {
        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);
        when(a.getStartTime()).thenReturn(OffsetDateTime.now(ZoneOffset.UTC).plusHours(12));
        when(a.getCreatedBy()).thenReturn((long) 1);

        User u = mock(User.class);
        when(u.getEmail()).thenReturn(null);

        when(appointmentDAO.getAllAppointments()).thenReturn(List.of(a));
        when(userDAO.getUserById("1")).thenReturn(Optional.of(u));

        reminderService.sendReminders();

        verify(userDAO, times(1)).getUserById("1");
    }

    @Test
    void sendReminders_confirmedWithinWindow_userEmailBlank_skipped() throws SQLException {
        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);
        when(a.getStartTime()).thenReturn(OffsetDateTime.now(ZoneOffset.UTC).plusHours(12));
        when(a.getCreatedBy()).thenReturn((long) 2);

        User u = mock(User.class);
        when(u.getEmail()).thenReturn("   ");

        when(appointmentDAO.getAllAppointments()).thenReturn(List.of(a));
        when(userDAO.getUserById("2")).thenReturn(Optional.of(u));

        reminderService.sendReminders();

        verify(userDAO, times(1)).getUserById("2");
    }

    @Test
    void sendReminders_daoThrowsSQLException_handledGracefully() throws SQLException {
        when(appointmentDAO.getAllAppointments()).thenThrow(new SQLException("DB down"));

        reminderService.sendReminders();

        verify(appointmentDAO, times(1)).getAllAppointments();
    }

    @Test
    void sendReminders_multipleAppointments_onlyConfirmedWithinWindowProcessed()
            throws SQLException {
        OffsetDateTime inWindow = OffsetDateTime.now(ZoneOffset.UTC).plusHours(10);

        Appointment confirmed = mock(Appointment.class);
        when(confirmed.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);
        when(confirmed.getStartTime()).thenReturn(inWindow);
        when(confirmed.getCreatedBy()).thenReturn((long) 10);

        Appointment pending = mock(Appointment.class);
        when(pending.getStatus()).thenReturn("PENDING");

        Appointment tooLate = mock(Appointment.class);
        when(tooLate.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);
        when(tooLate.getStartTime()).thenReturn(
                OffsetDateTime.now(ZoneOffset.UTC).plusHours(30));

        when(appointmentDAO.getAllAppointments())
                .thenReturn(List.of(confirmed, pending, tooLate));
        when(userDAO.getUserById("10")).thenReturn(Optional.empty());

        reminderService.sendReminders();

        verify(userDAO, times(1)).getUserById("10");
    }

    @Test
    void sendReminders_groupAppointmentWithinWindow_userLookedUp() throws SQLException {
        OffsetDateTime inWindow = OffsetDateTime.now(ZoneOffset.UTC).plusHours(6);

        Appointment group = mock(Appointment.class);
        when(group.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);
        when(group.getStartTime()).thenReturn(inWindow);
        when(group.getCreatedBy()).thenReturn((long) 5);
        when(group.isGroup()).thenReturn(true);
        when(group.getParticipantsCount()).thenReturn(4);

        when(appointmentDAO.getAllAppointments()).thenReturn(List.of(group));
        when(userDAO.getUserById("5")).thenReturn(Optional.empty());

        reminderService.sendReminders();

        verify(userDAO, times(1)).getUserById("5");
    }

    @Test
    void sendReminders_appointmentExactlyAt24hBoundary_withinWindow() throws SQLException {
        OffsetDateTime boundary = OffsetDateTime.now(ZoneOffset.UTC).plusHours(23).plusMinutes(59);

        Appointment a = mock(Appointment.class);
        when(a.getStatus()).thenReturn(Appointment.STATUS_CONFIRMED);
        when(a.getStartTime()).thenReturn(boundary);
        when(a.getCreatedBy()).thenReturn((long) 7);

        when(appointmentDAO.getAllAppointments()).thenReturn(List.of(a));
        when(userDAO.getUserById("7")).thenReturn(Optional.empty());

        reminderService.sendReminders();

        verify(userDAO, times(1)).getUserById("7");
    }
}