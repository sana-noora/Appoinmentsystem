package testtimeslot;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import domain.TimeSlot;
import persistence.TimeSlotDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TimeSlotDAOTest {

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    private TimeSlotDAO timeSlotDAO;

    private OffsetDateTime startTime;
    private OffsetDateTime endTime;

    @BeforeEach
    void setUp() {
        timeSlotDAO = new TimeSlotDAO(connection);
        startTime = OffsetDateTime.now();
        endTime = startTime.plusHours(1);
    }

    @Test
    void addTimeSlot_shouldExecuteInsert() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);

        TimeSlot slot = new TimeSlot(1L, startTime, endTime, true);

        timeSlotDAO.addTimeSlot(slot);

        verify(preparedStatement).executeUpdate();
    }
    @Test
    void getAvailableSlotsByScheduleId_shouldReturnOnlyMatchingScheduleSlots() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery())
                .thenReturn(resultSet);

        when(resultSet.next()).thenReturn(true, true, false);

        OffsetDateTime startTime = OffsetDateTime.now();
        OffsetDateTime endTime = startTime.plusHours(1);

        when(resultSet.getLong("id")).thenReturn(1L, 2L);
        when(resultSet.getLong("schedule_id")).thenReturn(10L);
        when(resultSet.getObject("start_time", OffsetDateTime.class))
                .thenReturn(startTime);
        when(resultSet.getObject("end_time", OffsetDateTime.class))
                .thenReturn(endTime);
        when(resultSet.getBoolean("available")).thenReturn(true);

        List<TimeSlot> slots = timeSlotDAO.getAvailableSlotsByScheduleId(10L);

        assertEquals(2, slots.size());
        assertEquals(10L, slots.get(0).getScheduleId());
        assertTrue(slots.get(0).isAvailable());
    }
    @Test
    void getTimeSlotById_shouldReturnTimeSlot() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery())
                .thenReturn(resultSet);
        when(resultSet.next())
                .thenReturn(true);

        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getLong("schedule_id")).thenReturn(5L);
        when(resultSet.getObject("start_time", OffsetDateTime.class))
                .thenReturn(startTime);
        when(resultSet.getObject("end_time", OffsetDateTime.class))
                .thenReturn(endTime);
        when(resultSet.getBoolean("available")).thenReturn(true);

        TimeSlot slot = timeSlotDAO.getTimeSlotById(1L);

        assertNotNull(slot);
        assertEquals(1L, slot.getId());
        assertTrue(slot.isAvailable());
    }

    @Test
    void getTimeSlotById_shouldReturnNullIfNotFound() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery())
                .thenReturn(resultSet);
        when(resultSet.next())
                .thenReturn(false);

        TimeSlot slot = timeSlotDAO.getTimeSlotById(99L);

        assertNull(slot);
    }

    @Test
    void getAllTimeSlots_shouldReturnList() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery())
                .thenReturn(resultSet);

        when(resultSet.next()).thenReturn(true, true, false);

        when(resultSet.getLong("id")).thenReturn(1L, 2L);
        when(resultSet.getLong("schedule_id")).thenReturn(5L);
        when(resultSet.getObject("start_time", OffsetDateTime.class))
                .thenReturn(startTime);
        when(resultSet.getObject("end_time", OffsetDateTime.class))
                .thenReturn(endTime);
        when(resultSet.getBoolean("available")).thenReturn(true);

        List<TimeSlot> slots = timeSlotDAO.getAllTimeSlots();

        assertEquals(2, slots.size());
    }

    @Test
    void getAvailableSlots_shouldReturnOnlyAvailable() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery())
                .thenReturn(resultSet);

        when(resultSet.next()).thenReturn(true, false);

        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getLong("schedule_id")).thenReturn(3L);
        when(resultSet.getObject("start_time", OffsetDateTime.class))
                .thenReturn(startTime);
        when(resultSet.getObject("end_time", OffsetDateTime.class))
                .thenReturn(endTime);
        when(resultSet.getBoolean("available")).thenReturn(true);

        List<TimeSlot> slots = timeSlotDAO.getAvailableSlots();

        assertEquals(1, slots.size());
        assertTrue(slots.get(0).isAvailable());
    }

    @Test
    void updateAvailability_shouldExecuteUpdate() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);

        timeSlotDAO.updateAvailability(1L, false);

        verify(preparedStatement).executeUpdate();
    }

    @Test
    void deleteTimeSlot_shouldExecuteDelete() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);

        timeSlotDAO.deleteTimeSlot(4L);

        verify(preparedStatement).executeUpdate();
    }
}
