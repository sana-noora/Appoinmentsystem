package persistence;

import domain.Schedule;
import java.sql.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class ScheduleDAO {
    private final Connection connection;

    public ScheduleDAO(Connection connection) {
        this.connection = connection;
    }

    public void addSchedule(Schedule schedule) throws SQLException {
        String sql = "INSERT INTO schedules (work_date, created_at) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, schedule.getWorkDate());
            stmt.setObject(2, OffsetDateTime.now());
            stmt.executeUpdate();
        }
    }

    public Schedule getScheduleById(long id) throws SQLException {
        String sql = "SELECT * FROM schedules WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToSchedule(rs);
                }
            }
        }
        return null;
    }

    public List<Schedule> getAllSchedules() throws SQLException {
        List<Schedule> schedules = new ArrayList<>();
        String sql = "SELECT * FROM schedules ORDER BY work_date ASC";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                schedules.add(mapResultSetToSchedule(rs));
            }
        }
        return schedules;
    }

    public void deleteSchedule(long id) throws SQLException {
        String sql = "DELETE FROM schedules WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
    }

    private Schedule mapResultSetToSchedule(ResultSet rs) throws SQLException {
        return new Schedule(
                rs.getLong("id"),
                rs.getObject("work_date", LocalDate.class),
                rs.getObject("created_at", OffsetDateTime.class)
        );
    }
}