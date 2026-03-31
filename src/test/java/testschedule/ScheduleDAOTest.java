package testschedule;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import domain.Schedule;
import persistence.ScheduleDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScheduleDAOTest {

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    private ScheduleDAO scheduleDAO;

    @BeforeEach
    void setUp() {
        scheduleDAO = new ScheduleDAO(connection);
    }

    @Test
    void addSchedule_shouldExecuteInsert() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);

        Schedule schedule = new Schedule(LocalDate.of(2026, 3, 25));

        scheduleDAO.addSchedule(schedule);

        verify(preparedStatement).executeUpdate();
    }

    @Test
    void getScheduleById_shouldReturnSchedule() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery())
                .thenReturn(resultSet);
        when(resultSet.next())
                .thenReturn(true);

        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getObject("work_date", LocalDate.class))
                .thenReturn(LocalDate.of(2026, 3, 25));
        when(resultSet.getObject("created_at", OffsetDateTime.class))
                .thenReturn(OffsetDateTime.now());

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
        when(resultSet.next())
                .thenReturn(false);

        Schedule schedule = scheduleDAO.getScheduleById(99L);

        assertNull(schedule);
    }

    @Test
    void getAllSchedules_shouldReturnList() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery())
                .thenReturn(resultSet);

        when(resultSet.next()).thenReturn(true, true, false);

        when(resultSet.getLong("id")).thenReturn(1L, 2L);
        when(resultSet.getObject("work_date", LocalDate.class))
                .thenReturn(
                        LocalDate.of(2026, 3, 25),
                        LocalDate.of(2026, 3, 26)
                );
        when(resultSet.getObject("created_at", OffsetDateTime.class))
                .thenReturn(OffsetDateTime.now());

        List<Schedule> schedules = scheduleDAO.getAllSchedules();

        assertEquals(2, schedules.size());
    }

    @Test
    void deleteSchedule_shouldExecuteDelete() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);

        scheduleDAO.deleteSchedule(5L);

        verify(preparedStatement).executeUpdate();
    }
}