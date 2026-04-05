package testAppointment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import domain.Appointment;
import persistence.AppointmentDAO;

import java.sql.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentDAOTest {

    @Mock private Connection connection;
    @Mock private PreparedStatement statement;
    @Mock private ResultSet resultSet;

    private AppointmentDAO dao;

    private OffsetDateTime start;
    private OffsetDateTime end;

    @BeforeEach
    void setUp() {
        dao = new AppointmentDAO(connection);
        start = OffsetDateTime.now().plusDays(1);
        end = start.plusMinutes(30);
    }

    // =====================================================
    // markPastAppointmentsDone
    // =====================================================

    @Test
    void markPastAppointmentsDone_shouldExecuteUpdate() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(2);

        dao.markPastAppointmentsDone();

        verify(statement).executeUpdate();
    }

    // =====================================================
    // addAppointment
    // =====================================================

    @Test
    void addAppointment_shouldExecuteInsert() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(statement);

        Appointment a = new Appointment(
                0L,
                Appointment.TYPE_FIRST_VISIT,
                Appointment.STATUS_CONFIRMED,
                start,
                end,
                1,
                1,
                5,
                null,
                null,
                null,
                null
        );

        dao.addAppointment(a);

        verify(statement).executeUpdate();
    }

    // =====================================================
    // getAppointmentById
    // =====================================================

    @Test
    void getAppointmentById_shouldReturnAppointment() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(statement);
        when(statement.executeQuery())
                .thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        mockAppointmentRow();

        Appointment a = dao.getAppointmentById(1L);

        assertNotNull(a);
        assertEquals(Appointment.TYPE_FIRST_VISIT, a.getType());
    }

    @Test
    void getAppointmentById_shouldReturnNullWhenNotFound() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(statement);
        when(statement.executeQuery())
                .thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        assertNull(dao.getAppointmentById(99L));
    }

    // =====================================================
    // getAllAppointments
    // =====================================================

    @Test
    void getAllAppointments_shouldReturnList() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(statement);
        when(statement.executeQuery())
                .thenReturn(resultSet);

        when(resultSet.next()).thenReturn(true, true, false);
        mockAppointmentRow();

        List<Appointment> list = dao.getAllAppointments();

        assertEquals(2, list.size());
    }

    // =====================================================
    // getActiveAppointmentsByDate
    // =====================================================

    @Test
    void getActiveAppointmentsByDate_shouldReturnList() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(statement);
        when(statement.executeQuery())
                .thenReturn(resultSet);

        when(resultSet.next()).thenReturn(true, false);
        mockAppointmentRow();

        List<Appointment> list =
                dao.getActiveAppointmentsByDate(LocalDate.now());

        assertEquals(1, list.size());
    }

    // =====================================================
    // getFutureAppointmentsByDate
    // =====================================================

    @Test
    void getFutureAppointmentsByDate_shouldReturnList() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(statement);
        when(statement.executeQuery())
                .thenReturn(resultSet);

        when(resultSet.next()).thenReturn(true, false);
        mockAppointmentRow();

        List<Appointment> list =
                dao.getFutureAppointmentsByDate(LocalDate.now());

        assertEquals(1, list.size());
    }

    // =====================================================
    // getAppointmentsByUser
    // =====================================================

    @Test
    void getAppointmentsByUser_shouldReturnList() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(statement);
        when(statement.executeQuery())
                .thenReturn(resultSet);

        when(resultSet.next()).thenReturn(true, false);
        mockAppointmentRow();

        List<Appointment> list = dao.getAppointmentsByUser(5L);

        assertEquals(1, list.size());
    }

    // =====================================================
    // updateStatus
    // =====================================================

    @Test
    void updateStatus_shouldReturnRows() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);

        int rows =
                dao.updateStatus(1L, Appointment.STATUS_DONE);

        assertEquals(1, rows);
    }

    // =====================================================
    // cancelByAdmin
    // =====================================================

    @Test
    void cancelByAdmin_shouldReturnRows() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);

        int rows = dao.cancelByAdmin(1L, "note");

        assertEquals(1, rows);
    }

    // =====================================================
    // updateStatusAndNote
    // =====================================================

    @Test
    void updateStatusAndNote_shouldReturnRows() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);

        int rows =
                dao.updateStatusAndNote(1L,
                        Appointment.STATUS_CONFIRMED,
                        "note");

        assertEquals(1, rows);
    }

    // =====================================================
    // updateParticipants
    // =====================================================

    @Test
    void updateParticipants_shouldReturnRows() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);

        int rows = dao.updateParticipants(1L, 4);

        assertEquals(1, rows);
    }

    // =====================================================
    // updateParticipantsAndNote
    // =====================================================

    @Test
    void updateParticipantsAndNote_shouldReturnRows() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);

        int rows = dao.updateParticipantsAndNote(1L, 3, "note");

        assertEquals(1, rows);
    }

    // =====================================================
    // updateTypeAndNote
    // =====================================================

    @Test
    void updateTypeAndNote_shouldReturnRows() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);

        int rows = dao.updateTypeAndNote(
                1L,
                Appointment.TYPE_GROUP_FIRST_VISIT,
                "note"
        );

        assertEquals(1, rows);
    }

    // =====================================================
    // updateType
    // =====================================================

    @Test
    void updateType_shouldReturnRows() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);

        int rows = dao.updateType(1L, Appointment.TYPE_VIRTUAL);

        assertEquals(1, rows);
    }

    // =====================================================
    // updateSlotAndTime
    // =====================================================

    @Test
    void updateSlotAndTime_shouldReturnRows() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);

        int rows = dao.updateSlotAndTime(
                1L,
                10L,
                start.plusHours(1),
                end.plusHours(1)
        );

        assertEquals(1, rows);
    }

    // =====================================================
    // deleteAppointment
    // =====================================================

    @Test
    void deleteAppointment_shouldExecuteDelete() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(statement);

        dao.deleteAppointment(3L);

        verify(statement).executeUpdate();
    }

    // =====================================================
    // normalizeType — invalid
    // =====================================================

    @Test
    void addAppointment_shouldThrowWhenTypeInvalid() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(statement);

        Appointment a = new Appointment(
                0L,
                "INVALID",
                Appointment.STATUS_CONFIRMED,
                start,
                end,
                1,
                1,
                5,
                null,
                null,
                null,
                null
        );

        assertThrows(IllegalArgumentException.class,
                () -> dao.addAppointment(a));
    }

    // =====================================================
    // normalizeStatus — invalid
    // =====================================================

    @Test
    void updateStatus_shouldThrowWhenStatusInvalid() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(statement);

        assertThrows(IllegalArgumentException.class,
                () -> dao.updateStatus(1L, "BAD"));
    }

    // =====================================================
    // helper
    // =====================================================

    private void mockAppointmentRow() throws Exception {
        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getString("type"))
                .thenReturn(Appointment.TYPE_FIRST_VISIT);
        when(resultSet.getString("status"))
                .thenReturn(Appointment.STATUS_CONFIRMED);
        when(resultSet.getObject("start_time", OffsetDateTime.class))
                .thenReturn(start);
        when(resultSet.getObject("end_time", OffsetDateTime.class))
                .thenReturn(end);
        when(resultSet.getInt("participants_count")).thenReturn(1);
        when(resultSet.getInt("max_participants")).thenReturn(1);
        when(resultSet.getInt("created_by")).thenReturn(5);
        when(resultSet.getObject("slot_id", Long.class))
                .thenReturn(null);
        when(resultSet.getString("admin_note"))
                .thenReturn(null);
        when(resultSet.getObject("created_at", OffsetDateTime.class))
                .thenReturn(start.minusDays(1));
        when(resultSet.getObject("updated_at", OffsetDateTime.class))
                .thenReturn(start.minusHours(1));
        when(resultSet.getBoolean("canceled_by_admin"))
                .thenReturn(false);
    }
}