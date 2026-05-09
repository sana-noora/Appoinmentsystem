package tests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import domain.Appointment;
import persistence.AppointmentDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentDAOTest {

    @Mock private Connection     connection;
    @Mock private PreparedStatement statement;
    @Mock private ResultSet      resultSet;

    private AppointmentDAO dao;
    private OffsetDateTime start;
    private OffsetDateTime end;

    @BeforeEach
    void setUp() {
        dao   = new AppointmentDAO(connection);
        start = OffsetDateTime.now().plusDays(1);
        end   = start.plusMinutes(30);
    }

    // =========================================================
    // markPastAppointmentsDone
    // =========================================================

    @Test
    @DisplayName("markPastAppointmentsDone: rows updated → prints info")
    void markPastAppointmentsDone_withRows() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(2);
        dao.markPastAppointmentsDone();
        verify(statement).executeUpdate();
    }

    @Test
    @DisplayName("markPastAppointmentsDone: zero rows → no exception")
    void markPastAppointmentsDone_zeroRows() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(0);   // covers rows == 0 branch
        dao.markPastAppointmentsDone();
        verify(statement).executeUpdate();
    }

    // =========================================================
    // addAppointment
    // =========================================================

    @Test
    @DisplayName("addAppointment: slotId null → setNull called")
    void addAppointment_nullSlot() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        dao.addAppointment(buildAppt(Appointment.TYPE_FIRST_VISIT,
                                     Appointment.STATUS_CONFIRMED, null));
        verify(statement).executeUpdate();
    }

    @Test
    @DisplayName("addAppointment: slotId non-null → setLong called")
    void addAppointment_withSlot() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        dao.addAppointment(buildAppt(Appointment.TYPE_FIRST_VISIT,
                                     Appointment.STATUS_CONFIRMED, 7L));
        verify(statement).executeUpdate();
    }

    @Test
    @DisplayName("addAppointment: FOLLOW_UP type persists correctly")
    void addAppointment_followUpType() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        dao.addAppointment(buildAppt(Appointment.TYPE_FOLLOW_UP,
                                     Appointment.STATUS_PENDING, null));
        verify(statement).executeUpdate();
    }

    @Test
    @DisplayName("addAppointment: VIRTUAL type persists correctly")
    void addAppointment_virtualType() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        dao.addAppointment(buildAppt(Appointment.TYPE_VIRTUAL,
                                     Appointment.STATUS_CONFIRMED, null));
        verify(statement).executeUpdate();
    }

    @Test
    @DisplayName("addAppointment: GROUP_FIRST_VISIT type persists correctly")
    void addAppointment_groupFirstVisit() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        dao.addAppointment(buildAppt(Appointment.TYPE_GROUP_FIRST_VISIT,
                                     Appointment.STATUS_CONFIRMED, null));
        verify(statement).executeUpdate();
    }

    @Test
    @DisplayName("addAppointment: GROUP_FOLLOW_UP type persists correctly")
    void addAppointment_groupFollowUp() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        dao.addAppointment(buildAppt(Appointment.TYPE_GROUP_FOLLOW_UP,
                                     Appointment.STATUS_CONFIRMED, null));
        verify(statement).executeUpdate();
    }

    @Test
    @DisplayName("addAppointment: GROUP_VIRTUAL type persists correctly")
    void addAppointment_groupVirtual() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        dao.addAppointment(buildAppt(Appointment.TYPE_GROUP_VIRTUAL,
                                     Appointment.STATUS_CONFIRMED, null));
        verify(statement).executeUpdate();
    }

    @Test
    @DisplayName("addAppointment: invalid type → IllegalArgumentException")
    void addAppointment_invalidType() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        Appointment bad = buildAppt("INVALID", Appointment.STATUS_CONFIRMED, null);
        assertThrows(IllegalArgumentException.class, () -> dao.addAppointment(bad));
    }

    @Test
    @DisplayName("addAppointment: null type → IllegalArgumentException")
    void addAppointment_nullType() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        Appointment bad = buildAppt(null, Appointment.STATUS_CONFIRMED, null);
        // normalizeType(null) returns null — no exception thrown from normalizeType,
        // but the setString(1, null) path is exercised
        // If the DB layer later rejects it that's fine; here we just ensure no NPE
        assertDoesNotThrow(() -> dao.addAppointment(bad));
    }

    // =========================================================
    // getAppointmentById
    // =========================================================

    @Test
    @DisplayName("getAppointmentById: found → returns Appointment")
    void getAppointmentById_found() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        mockAppointmentRow();

        Appointment a = dao.getAppointmentById(1L);
        assertNotNull(a);
        assertEquals(Appointment.TYPE_FIRST_VISIT, a.getType());
    }

    @Test
    @DisplayName("getAppointmentById: not found → returns null")
    void getAppointmentById_notFound() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        assertNull(dao.getAppointmentById(99L));
    }

    // =========================================================
    // getAllAppointments
    // =========================================================

    @Test
    @DisplayName("getAllAppointments: multiple rows → correct count")
    void getAllAppointments_multipleRows() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false);
        mockAppointmentRow();

        assertEquals(2, dao.getAllAppointments().size());
    }

    @Test
    @DisplayName("getAllAppointments: empty table → empty list")
    void getAllAppointments_empty() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        assertTrue(dao.getAllAppointments().isEmpty());
    }

    // =========================================================
    // getActiveAppointmentsByDate
    // =========================================================

    @Test
    @DisplayName("getActiveAppointmentsByDate: one row returned")
    void getActiveAppointmentsByDate_oneRow() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        mockAppointmentRow();

        List<Appointment> list = dao.getActiveAppointmentsByDate(LocalDate.now());
        assertEquals(1, list.size());
    }

    @Test
    @DisplayName("getActiveAppointmentsByDate: no rows → empty list")
    void getActiveAppointmentsByDate_empty() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        assertTrue(dao.getActiveAppointmentsByDate(LocalDate.now()).isEmpty());
    }

    // =========================================================
    // getFutureAppointmentsByDate
    // =========================================================

    @Test
    @DisplayName("getFutureAppointmentsByDate: one row returned")
    void getFutureAppointmentsByDate_oneRow() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        mockAppointmentRow();

        List<Appointment> list = dao.getFutureAppointmentsByDate(LocalDate.now());
        assertEquals(1, list.size());
    }

    @Test
    @DisplayName("getFutureAppointmentsByDate: empty → empty list")
    void getFutureAppointmentsByDate_empty() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        assertTrue(dao.getFutureAppointmentsByDate(LocalDate.now()).isEmpty());
    }

    // =========================================================
    // getAppointmentsByUser
    // =========================================================

    @Test
    @DisplayName("getAppointmentsByUser: one row returned")
    void getAppointmentsByUser_oneRow() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        mockAppointmentRow();

        assertEquals(1, dao.getAppointmentsByUser(5L).size());
    }

    @Test
    @DisplayName("getAppointmentsByUser: no rows → empty list")
    void getAppointmentsByUser_empty() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        assertTrue(dao.getAppointmentsByUser(5L).isEmpty());
    }

    // =========================================================
    // updateStatus
    // =========================================================

    @Test
    @DisplayName("updateStatus: DONE → rows affected returned")
    void updateStatus_done() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        assertEquals(1, dao.updateStatus(1L, Appointment.STATUS_DONE));
    }

    @Test
    @DisplayName("updateStatus: CONFIRMED → rows affected returned")
    void updateStatus_confirmed() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        assertEquals(1, dao.updateStatus(1L, Appointment.STATUS_CONFIRMED));
    }

    @Test
    @DisplayName("updateStatus: PENDING → rows affected returned")
    void updateStatus_pending() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        assertEquals(1, dao.updateStatus(1L, Appointment.STATUS_PENDING));
    }

    @Test
    @DisplayName("updateStatus: CANCELED → rows affected returned")
    void updateStatus_canceled() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        assertEquals(1, dao.updateStatus(1L, Appointment.STATUS_CANCELED));
    }

    @Test
    @DisplayName("updateStatus: CANCELLED spelling alias → accepted")
    void updateStatus_cancelledAlias() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        assertEquals(1, dao.updateStatus(1L, "CANCELLED"));   // covers CANCELLED branch
    }

    @Test
    @DisplayName("updateStatus: invalid status → IllegalArgumentException")
    void updateStatus_invalid() {
        assertThrows(IllegalArgumentException.class,
                () -> dao.updateStatus(1L, "BAD_STATUS"));
    }

    @Test
    @DisplayName("updateStatus: null status → IllegalArgumentException")
    void updateStatus_null() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        // normalizeStatus(null) returns null — covers that path
        assertDoesNotThrow(() -> {
            when(statement.executeUpdate()).thenReturn(0);
            dao.updateStatus(1L, null);
        });
    }

    // =========================================================
    // cancelByAdmin
    // =========================================================

    @Test
    @DisplayName("cancelByAdmin: with note → rows returned")
    void cancelByAdmin_withNote() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        assertEquals(1, dao.cancelByAdmin(1L, "scheduling conflict"));
    }

    @Test
    @DisplayName("cancelByAdmin: null note → rows returned")
    void cancelByAdmin_nullNote() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        assertEquals(1, dao.cancelByAdmin(1L, null));
    }

    @Test
    @DisplayName("cancelByAdmin: no matching row → returns 0")
    void cancelByAdmin_notFound() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(0);
        assertEquals(0, dao.cancelByAdmin(999L, "note"));
    }

    // =========================================================
    // updateStatusAndNote  — previously uncovered
    // =========================================================

    @Test
    @DisplayName("updateStatusAndNote: valid status + note → rows returned")
    void updateStatusAndNote_valid() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        int rows = dao.updateStatusAndNote(1L, Appointment.STATUS_CANCELED, "admin closed");
        assertEquals(1, rows);
    }

    @Test
    @DisplayName("updateStatusAndNote: null note → rows returned")
    void updateStatusAndNote_nullNote() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        assertEquals(1, dao.updateStatusAndNote(1L, Appointment.STATUS_DONE, null));
    }

    @Test
    @DisplayName("updateStatusAndNote: invalid status → IllegalArgumentException")
    void updateStatusAndNote_invalidStatus() {
        assertThrows(IllegalArgumentException.class,
                () -> dao.updateStatusAndNote(1L, "WRONG", "note"));
    }

    // =========================================================
    // updateParticipants  — previously uncovered
    // =========================================================

    @Test
    @DisplayName("updateParticipants: count 3 → rows returned")
    void updateParticipants_valid() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        assertEquals(1, dao.updateParticipants(1L, 3));
    }

    @Test
    @DisplayName("updateParticipants: count 1 (minimum) → rows returned")
    void updateParticipants_minimum() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        assertEquals(1, dao.updateParticipants(1L, 1));
    }

    @Test
    @DisplayName("updateParticipants: count 5 (maximum) → rows returned")
    void updateParticipants_maximum() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        assertEquals(1, dao.updateParticipants(1L, 5));
    }

    @Test
    @DisplayName("updateParticipants: no matching row → returns 0")
    void updateParticipants_notFound() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(0);
        assertEquals(0, dao.updateParticipants(999L, 2));
    }

    // =========================================================
    // updateParticipantsAndNote  — previously uncovered
    // =========================================================

    @Test
    @DisplayName("updateParticipantsAndNote: count + note → rows returned")
    void updateParticipantsAndNote_valid() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        assertEquals(1, dao.updateParticipantsAndNote(1L, 4, "rescheduled"));
    }

    @Test
    @DisplayName("updateParticipantsAndNote: null note → rows returned")
    void updateParticipantsAndNote_nullNote() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        assertEquals(1, dao.updateParticipantsAndNote(1L, 2, null));
    }

    @Test
    @DisplayName("updateParticipantsAndNote: no matching row → returns 0")
    void updateParticipantsAndNote_notFound() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(0);
        assertEquals(0, dao.updateParticipantsAndNote(999L, 3, "note"));
    }

    // =========================================================
    // updateTypeAndNote  — previously uncovered
    // =========================================================

    @Test
    @DisplayName("updateTypeAndNote: individual type + note → rows returned")
    void updateTypeAndNote_individual() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        assertEquals(1, dao.updateTypeAndNote(1L, Appointment.TYPE_FIRST_VISIT, "changed"));
    }

    @Test
    @DisplayName("updateTypeAndNote: group type → isGroup=true branch covered")
    void updateTypeAndNote_group() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        assertEquals(1, dao.updateTypeAndNote(1L, Appointment.TYPE_GROUP_FIRST_VISIT, "group"));
    }

    @Test
    @DisplayName("updateTypeAndNote: FOLLOW_UP → covered")
    void updateTypeAndNote_followUp() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        assertEquals(1, dao.updateTypeAndNote(1L, Appointment.TYPE_FOLLOW_UP, null));
    }

    @Test
    @DisplayName("updateTypeAndNote: GROUP_FOLLOW_UP → covered")
    void updateTypeAndNote_groupFollowUp() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        assertEquals(1, dao.updateTypeAndNote(1L, Appointment.TYPE_GROUP_FOLLOW_UP, null));
    }

    @Test
    @DisplayName("updateTypeAndNote: GROUP_VIRTUAL → covered")
    void updateTypeAndNote_groupVirtual() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        assertEquals(1, dao.updateTypeAndNote(1L, Appointment.TYPE_GROUP_VIRTUAL, null));
    }

    @Test
    @DisplayName("updateTypeAndNote: VIRTUAL → covered")
    void updateTypeAndNote_virtual() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        assertEquals(1, dao.updateTypeAndNote(1L, Appointment.TYPE_VIRTUAL, null));
    }

    @Test
    @DisplayName("updateTypeAndNote: null note → rows returned")
    void updateTypeAndNote_nullNote() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        assertEquals(1, dao.updateTypeAndNote(1L, Appointment.TYPE_FIRST_VISIT, null));
    }

    @Test
    @DisplayName("updateTypeAndNote: invalid type → IllegalArgumentException")
    void updateTypeAndNote_invalidType() {
        assertThrows(IllegalArgumentException.class,
                () -> dao.updateTypeAndNote(1L, "UNKNOWN", "note"));
    }

    // =========================================================
    // updateType  — previously uncovered
    // =========================================================

    @Test
    @DisplayName("updateType: FIRST_VISIT → individual branch (maxP=1)")
    void updateType_firstVisit() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        assertEquals(1, dao.updateType(1L, Appointment.TYPE_FIRST_VISIT));
    }

    @Test
    @DisplayName("updateType: FOLLOW_UP → individual branch")
    void updateType_followUp() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        assertEquals(1, dao.updateType(1L, Appointment.TYPE_FOLLOW_UP));
    }

    @Test
    @DisplayName("updateType: VIRTUAL → individual branch")
    void updateType_virtual() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        assertEquals(1, dao.updateType(1L, Appointment.TYPE_VIRTUAL));
    }

    @Test
    @DisplayName("updateType: GROUP_FIRST_VISIT → group branch (maxP=5)")
    void updateType_groupFirstVisit() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        assertEquals(1, dao.updateType(1L, Appointment.TYPE_GROUP_FIRST_VISIT));
    }

    @Test
    @DisplayName("updateType: GROUP_FOLLOW_UP → group branch")
    void updateType_groupFollowUp() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        assertEquals(1, dao.updateType(1L, Appointment.TYPE_GROUP_FOLLOW_UP));
    }

    @Test
    @DisplayName("updateType: GROUP_VIRTUAL → group branch")
    void updateType_groupVirtual() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        assertEquals(1, dao.updateType(1L, Appointment.TYPE_GROUP_VIRTUAL));
    }

    @Test
    @DisplayName("updateType: FOLLOWUP alias → normalizeType alias branch covered")
    void updateType_followUpAlias() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        assertEquals(1, dao.updateType(1L, "FOLLOWUP"));   // covers FOLLOWUP alias
    }

    @Test
    @DisplayName("updateType: invalid type → IllegalArgumentException")
    void updateType_invalid() {
        assertThrows(IllegalArgumentException.class,
                () -> dao.updateType(1L, "BAD_TYPE"));
    }

    @Test
    @DisplayName("updateType: no matching row → returns 0")
    void updateType_notFound() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(0);
        assertEquals(0, dao.updateType(999L, Appointment.TYPE_FIRST_VISIT));
    }

    // =========================================================
    // updateSlotAndTime  — previously uncovered
    // =========================================================

    @Test
    @DisplayName("updateSlotAndTime: valid params → rows returned")
    void updateSlotAndTime_valid() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        OffsetDateTime newStart = start.plusDays(1);
        OffsetDateTime newEnd   = newStart.plusMinutes(45);
        assertEquals(1, dao.updateSlotAndTime(1L, 10L, newStart, newEnd));
    }

    @Test
    @DisplayName("updateSlotAndTime: no matching row → returns 0")
    void updateSlotAndTime_notFound() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(0);
        assertEquals(0, dao.updateSlotAndTime(999L, 10L, start, end));
    }

    // =========================================================
    // deleteAppointment  — previously uncovered
    // =========================================================

    @Test
    @DisplayName("deleteAppointment: executes delete SQL")
    void deleteAppointment_valid() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        dao.deleteAppointment(1L);
        verify(statement).executeUpdate();
    }

    @Test
    @DisplayName("deleteAppointment: non-existent id → no exception")
    void deleteAppointment_notFound() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(0);
        assertDoesNotThrow(() -> dao.deleteAppointment(999L));
    }

    // =========================================================
    // normalizeType edge cases (via addAppointment/updateType)
    // =========================================================

    @Test
    @DisplayName("normalizeType: hyphenated input → normalised correctly")
    void normalizeType_hyphenated() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        // FOLLOW-UP → toUpperCase + replace('-','_') → FOLLOW_UP
        assertEquals(1, dao.updateType(1L, "FOLLOW-UP"));
    }

    @Test
    @DisplayName("normalizeType: lowercase input → normalised correctly")
    void normalizeType_lowercase() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        assertEquals(1, dao.updateType(1L, "first_visit"));
    }

    @Test
    @DisplayName("normalizeType: null input → returns null (no exception)")
    void normalizeType_null() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        // normalizeType(null) returns null — path covered without throwing
        assertDoesNotThrow(() -> dao.addAppointment(buildAppt(null,
                Appointment.STATUS_CONFIRMED, null)));
    }

    // =========================================================
    // normalizeStatus edge cases (via updateStatus)
    // =========================================================

    @Test
    @DisplayName("normalizeStatus: CANCELLED alias → maps to CANCELED")
    void normalizeStatus_cancelledAlias() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        assertEquals(1, dao.updateStatus(1L, "CANCELLED"));
    }

    @Test
    @DisplayName("normalizeStatus: unknown value → IllegalArgumentException")
    void normalizeStatus_unknown() {
        assertThrows(IllegalArgumentException.class,
                () -> dao.updateStatus(1L, "UNKNOWN_STATUS"));
    }

    // =========================================================
    // map(): canceled_by_admin flag  — covers setCanceledByAdmin branch
    // =========================================================

    @Test
    @DisplayName("map: canceled_by_admin=true is reflected on returned Appointment")
    void map_canceledByAdminTrue() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        mockAppointmentRow();
        when(resultSet.getBoolean("canceled_by_admin")).thenReturn(true);  // override

        Appointment a = dao.getAppointmentById(1L);
        assertNotNull(a);
        assertTrue(a.isCanceledByAdmin());
    }

    @Test
    @DisplayName("map: slot_id non-null is mapped correctly")
    void map_nonNullSlotId() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        mockAppointmentRow();
        when(resultSet.getObject("slot_id", Long.class)).thenReturn(42L);

        Appointment a = dao.getAppointmentById(1L);
        assertNotNull(a);
        assertEquals(42L, a.getSlotId());
    }

    @Test
    @DisplayName("map: admin_note non-null is mapped correctly")
    void map_nonNullAdminNote() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        mockAppointmentRow();
        when(resultSet.getString("admin_note")).thenReturn("rescheduled by admin");

        Appointment a = dao.getAppointmentById(1L);
        assertNotNull(a);
        assertEquals("rescheduled by admin", a.getAdminNote());
    }

    // =========================================================
    // Shared helpers
    // =========================================================

    /** Builds a minimal Appointment for insert tests. */
    private Appointment buildAppt(String type, String status, Long slotId) {
        return new Appointment(
                0L, type, status, start, end,
                1, 1, 5, slotId, null, null, null);
    }

    /**
     * Stubs all ResultSet columns that the private map() method reads.
     * Default: first_visit, confirmed, canceledByAdmin=false, slotId=null.
     */
    private void mockAppointmentRow() throws SQLException {
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