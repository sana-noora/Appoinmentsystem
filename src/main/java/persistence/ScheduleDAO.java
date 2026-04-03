package persistence;

import domain.Schedule;

import java.sql.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class ScheduleDAO {

    private final Connection connection;

    public ScheduleDAO(Connection connection) { this.connection = connection; }

    public long addSchedule(Schedule schedule) throws SQLException {
        String sql = "INSERT INTO schedules(work_date,created_at) VALUES(?,?)";
        try (PreparedStatement s = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setObject(1, schedule.getWorkDate());
            s.setObject(2, OffsetDateTime.now());
            s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
                throw new SQLException("No key returned for schedule insert.");
            }
        }
    }

    public boolean existsByDate(LocalDate date) throws SQLException {
        String sql = "SELECT 1 FROM schedules WHERE work_date=?";
        try (PreparedStatement s = connection.prepareStatement(sql)) {
            s.setObject(1, date);
            try (ResultSet rs = s.executeQuery()) { return rs.next(); }
        }
    }

    public Schedule getScheduleById(long id) throws SQLException {
        String sql = "SELECT * FROM schedules WHERE id=?";
        try (PreparedStatement s = connection.prepareStatement(sql)) {
            s.setLong(1, id);
            try (ResultSet rs = s.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public Schedule getScheduleByDate(LocalDate date) throws SQLException {
        String sql = "SELECT * FROM schedules WHERE work_date=?";
        try (PreparedStatement s = connection.prepareStatement(sql)) {
            s.setObject(1, date);
            try (ResultSet rs = s.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    // All schedules (past + future)
    public List<Schedule> getAllSchedules() throws SQLException {
        List<Schedule> list = new ArrayList<>();
        String sql = "SELECT * FROM schedules ORDER BY work_date ASC";
        try (PreparedStatement s = connection.prepareStatement(sql);
             ResultSet rs = s.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    // Only today + future schedules
    public List<Schedule> getFutureSchedules() throws SQLException {
        List<Schedule> list = new ArrayList<>();
        String sql = "SELECT * FROM schedules WHERE work_date >= CURRENT_DATE ORDER BY work_date ASC";
        try (PreparedStatement s = connection.prepareStatement(sql);
             ResultSet rs = s.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public void deleteSchedule(long id) throws SQLException {
        String sql = "DELETE FROM schedules WHERE id=?";
        try (PreparedStatement s = connection.prepareStatement(sql)) {
            s.setLong(1, id);
            s.executeUpdate();
        }
    }

    private Schedule map(ResultSet rs) throws SQLException {
        return new Schedule(rs.getLong("id"),
                            rs.getObject("work_date",  LocalDate.class),
                            rs.getObject("created_at", OffsetDateTime.class));
    }
}