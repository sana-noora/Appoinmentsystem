package testtimeslot;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import domain.TimeSlot;
import persistence.TimeSlotDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TimeSlotDAOTest {

    @Mock private Connection connection;
    @Mock private PreparedStatement preparedStatement;
    @Mock private ResultSet resultSet;

    private TimeSlotDAO timeSlotDAO;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;

    @BeforeEach
    void setUp() {
        timeSlotDAO = new TimeSlotDAO(connection);
        startTime = OffsetDateTime.now();
        endTime = startTime.plusHours(1);
    }

    // =====================================================
    // addTimeSlot — SUCCESS
    // =====================================================

    @Test
    void addTimeSlot_shouldReturnGeneratedId() throws Exception {
        ResultSet keys = mock(ResultSet.class);

        when(connection.prepareStatement(anyString(), anyInt()))
                .thenReturn(preparedStatement);
        when(preparedStatement.getGeneratedKeys())
                .thenReturn(keys);
        when(keys.next()).thenReturn(true);
        when(keys.getLong(1)).thenReturn(5L);

        TimeSlot slot = new TimeSlot(3L, startTime, endTime, true);

        long id = timeSlotDAO.addTimeSlot(slot);

        assertEquals(5L, id);
        verify(preparedStatement).executeUpdate();
    }

    // =====================================================
    // addTimeSlot — NO GENERATED KEY (EDGE CASE ✅)
    // =====================================================

    @Test
    void addTimeSlot_shouldThrowExceptionWhenNoGeneratedKeyReturned()
            throws Exception {

        ResultSet keys = mock(ResultSet.class);

        when(connection.prepareStatement(anyString(), anyInt()))
                .thenReturn(preparedStatement);
        when(preparedStatement.getGeneratedKeys())
                .thenReturn(keys);
        when(keys.next()).thenReturn(false);

        TimeSlot slot = new TimeSlot(3L, startTime, endTime, true);

        SQLException ex = assertThrows(SQLException.class, () -> {
            timeSlotDAO.addTimeSlot(slot);
        });

        assertTrue(ex.getMessage().contains("No key returned"));
    }

    // =====================================================
    // existsByScheduleAndStart
    // =====================================================

    @Test
    void existsByScheduleAndStart_shouldReturnTrueWhenExists()
            throws Exception {

        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery())
                .thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        assertTrue(timeSlotDAO.existsByScheduleAndStart(1L, startTime));
    }

    @Test
    void existsByScheduleAndStart_shouldReturnFalseWhenNotExists()
            throws Exception {

        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery())
                .thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        assertFalse(timeSlotDAO.existsByScheduleAndStart(1L, startTime));
    }

    // =====================================================
    // getTimeSlotById
    // =====================================================

    @Test
    void getTimeSlotById_shouldReturnSlot() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery())
                .thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        mockSlotRow(1L, 10L, true);

        TimeSlot slot = timeSlotDAO.getTimeSlotById(1L);

        assertNotNull(slot);
        assertEquals(1L, slot.getId());
        assertTrue(slot.isAvailable());
    }

    @Test
    void getTimeSlotById_shouldReturnNullWhenNotFound() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery())
                .thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        assertNull(timeSlotDAO.getTimeSlotById(99L));
    }

    // =====================================================
    // getAllTimeSlots
    // =====================================================

    @Test
    void getAllTimeSlots_shouldReturnList() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery())
                .thenReturn(resultSet);

        when(resultSet.next()).thenReturn(true, true, false);
        mockSlotRow(1L, 7L, true);

        List<TimeSlot> slots = timeSlotDAO.getAllTimeSlots();

        assertEquals(2, slots.size());
    }

    // =====================================================
    // getAllSlotsBySchedule
    // =====================================================

    @Test
    void getAllSlotsBySchedule_shouldReturnList() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery())
                .thenReturn(resultSet);

        when(resultSet.next()).thenReturn(true, false);
        mockSlotRow(2L, 8L, true);

        List<TimeSlot> slots = timeSlotDAO.getAllSlotsBySchedule(8L);

        assertEquals(1, slots.size());
        assertEquals(8L, slots.get(0).getScheduleId());
    }

    // =====================================================
    // getAllSlotsByDate
    // =====================================================

    @Test
    void getAllSlotsByDate_shouldReturnList() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery())
                .thenReturn(resultSet);

        when(resultSet.next()).thenReturn(true, false);
        mockSlotRow(3L, 9L, true);

        List<TimeSlot> slots =
                timeSlotDAO.getAllSlotsByDate(LocalDate.now());

        assertEquals(1, slots.size());
    }

    // =====================================================
    // getAvailableSlots
    // =====================================================

    @Test
    void getAvailableSlots_shouldReturnOnlyAvailable() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery())
                .thenReturn(resultSet);

        when(resultSet.next()).thenReturn(true, false);
        mockSlotRow(4L, 11L, true);

        List<TimeSlot> slots = timeSlotDAO.getAvailableSlots();

        assertEquals(1, slots.size());
        assertTrue(slots.get(0).isAvailable());
    }

    // =====================================================
    // getAvailableSlotsByScheduleId
    // =====================================================

    @Test
    void getAvailableSlotsByScheduleId_shouldReturnSlots() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery())
                .thenReturn(resultSet);

        when(resultSet.next()).thenReturn(true, true, false);
        mockSlotRow(6L, 12L, true);

        List<TimeSlot> slots =
                timeSlotDAO.getAvailableSlotsByScheduleId(12L);

        assertEquals(2, slots.size());
    }

    // =====================================================
    // updateAvailability
    // =====================================================

    @Test
    void updateAvailability_shouldReturnRowsUpdated() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate())
                .thenReturn(1);

        int rows = timeSlotDAO.updateAvailability(5L, false);

        assertEquals(1, rows);
    }

    // =====================================================
    // getBookedByUsername
    // =====================================================

    @Test
    void getBookedByUsername_shouldReturnUsername() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery())
                .thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("username")).thenReturn("aliuser");

        String user = timeSlotDAO.getBookedByUsername(7L);

        assertEquals("aliuser", user);
    }

    @Test
    void getBookedByUsername_shouldReturnNullWhenNoBooking()
            throws Exception {

        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery())
                .thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        assertNull(timeSlotDAO.getBookedByUsername(7L));
    }

    // =====================================================
    // deleteTimeSlot
    // =====================================================

    @Test
    void deleteTimeSlot_shouldExecuteDelete() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);

        timeSlotDAO.deleteTimeSlot(9L);

        verify(preparedStatement).executeUpdate();
    }

    // =====================================================
    // helper
    // =====================================================

    private void mockSlotRow(long id, long scheduleId, boolean available)
            throws Exception {

        when(resultSet.getLong("id")).thenReturn(id);
        when(resultSet.getLong("schedule_id")).thenReturn(scheduleId);
        when(resultSet.getObject("start_time", OffsetDateTime.class))
                .thenReturn(startTime);
        when(resultSet.getObject("end_time", OffsetDateTime.class))
                .thenReturn(endTime);
        when(resultSet.getBoolean("available")).thenReturn(available);
    }
}