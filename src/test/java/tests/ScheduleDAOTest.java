package tests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import domain.Schedule;
import persistence.ScheduleDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScheduleDAOTest {

    @Mock private Connection connection;
    @Mock private PreparedStatement preparedStatement;
    @Mock private ResultSet resultSet;

    private ScheduleDAO scheduleDAO;

    @BeforeEach
    void setUp() {
        scheduleDAO = new ScheduleDAO(connection);
    }

    // =====================================================
    // addSchedule
    // =====================================================

    @Test
    void addSchedule_shouldReturnGeneratedId() throws Exception {
        ResultSet keys = mock(ResultSet.class);

        when(connection.prepareStatement(anyString(), anyInt()))
                .thenReturn(preparedStatement);
        when(preparedStatement.getGeneratedKeys())
                .thenReturn(keys);
        when(keys.next()).thenReturn(true);
        when(keys.getLong(1)).thenReturn(3L);

        Schedule schedule = new Schedule(LocalDate.of(2026, 3, 25));

        long id = scheduleDAO.addSchedule(schedule);

        assertEquals(3L, id);
    }

    @Test
    void addSchedule_shouldThrowExceptionWhenNoGeneratedKeyReturned()
            throws Exception {

        ResultSet keys = mock(ResultSet.class);

        when(connection.prepareStatement(anyString(), anyInt()))
                .thenReturn(preparedStatement);
        when(preparedStatement.getGeneratedKeys())
                .thenReturn(keys);
        when(keys.next()).thenReturn(false);

        Schedule schedule = new Schedule(LocalDate.of(2026, 3, 25));

        SQLException ex = assertThrows(SQLException.class, () -> {
            scheduleDAO.addSchedule(schedule);
        });

        assertTrue(ex.getMessage().contains("No key returned"));
    }

    // =====================================================
    // existsByDate
    // =====================================================

    @Test
    void existsByDate_shouldReturnTrueWhenExists() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery())
                .thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        assertTrue(scheduleDAO.existsByDate(LocalDate.now()));
    }

    @Test
    void existsByDate_shouldReturnFalseWhenNotExists() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery())
                .thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        assertFalse(scheduleDAO.existsByDate(LocalDate.now()));
    }

    // =====================================================
    // getScheduleById
    // =====================================================

    @Test
    void getScheduleById_shouldReturnSchedule() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery())
                .thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        mockScheduleRow(1L);

        Schedule schedule = scheduleDAO.getScheduleById(1L);

        assertNotNull(schedule);
        assertEquals(1L, schedule.getId());
    }

    @Test
    void getScheduleById_shouldReturnNullWhenNotFound() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery())
                .thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        assertNull(scheduleDAO.getScheduleById(99L));
    }

    // =====================================================
    // getScheduleByDate
    // =====================================================

    @Test
    void getScheduleByDate_shouldReturnSchedule() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery())
                .thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        mockScheduleRow(2L);

        Schedule schedule =
                scheduleDAO.getScheduleByDate(LocalDate.of(2026, 4, 5));

        assertNotNull(schedule);
        assertEquals(2L, schedule.getId());
    }

    @Test
    void getScheduleByDate_shouldReturnNullWhenNotFound() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery())
                .thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        assertNull(
                scheduleDAO.getScheduleByDate(LocalDate.of(2026, 4, 5))
        );
    }

    // =====================================================
    // getAllSchedules
    // =====================================================

    @Test
    void getAllSchedules_shouldReturnList() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery())
                .thenReturn(resultSet);

        when(resultSet.next()).thenReturn(true, true, false);
        mockScheduleRow(1L);

        List<Schedule> schedules = scheduleDAO.getAllSchedules();

        assertEquals(2, schedules.size());
    }

    // =====================================================
    // getFutureSchedules
    // =====================================================

    @Test
    void getFutureSchedules_shouldReturnOnlyFutureSchedules()
            throws Exception {

        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery())
                .thenReturn(resultSet);

        when(resultSet.next()).thenReturn(true, false);

        when(resultSet.getLong("id")).thenReturn(5L);
        when(resultSet.getObject("work_date", LocalDate.class))
                .thenReturn(LocalDate.now().plusDays(5));
        when(resultSet.getObject("created_at", OffsetDateTime.class))
                .thenReturn(OffsetDateTime.now());

        List<Schedule> schedules = scheduleDAO.getFutureSchedules();

        assertEquals(1, schedules.size());
        assertFalse(
                schedules.get(0).getWorkDate()
                        .isBefore(LocalDate.now(ZoneId.systemDefault()))
        );
    }

    // =====================================================
    // deleteSchedule
    // =====================================================

    @Test
    void deleteSchedule_shouldExecuteDelete() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);

        scheduleDAO.deleteSchedule(10L);

        verify(preparedStatement).executeUpdate();
    }

    // =====================================================
    // helper
    // =====================================================

    private void mockScheduleRow(long id) throws Exception {
        when(resultSet.getLong("id")).thenReturn(id);
        when(resultSet.getObject("work_date", LocalDate.class))
                .thenReturn(LocalDate.of(2026, 4, 5));
        when(resultSet.getObject("created_at", OffsetDateTime.class))
                .thenReturn(OffsetDateTime.now());
    }
}
