package persistence;

import domain.TimeSlot;

import java.sql.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class TimeSlotDAO {

    private final Connection connection;

    public TimeSlotDAO(Connection connection) { this.connection = connection; }

    public long addTimeSlot(TimeSlot slot) throws SQLException {
        String sql = "INSERT INTO time_slots(schedule_id,start_time,end_time,available) " +
                     "VALUES(?,?,?,?)";
        try (PreparedStatement s = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setLong(1, slot.getScheduleId());
            s.setObject(2, slot.getStartTime());
            s.setObject(3, slot.getEndTime());
            s.setBoolean(4, slot.isAvailable());
            s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
                throw new SQLException("No key returned for time_slot insert.");
            }
        }
    }

    // Check if a slot with same schedule + start hour already exists
    public boolean existsByScheduleAndStart(long scheduleId, OffsetDateTime start)
            throws SQLException {
        String sql = "SELECT 1 FROM time_slots WHERE schedule_id=? AND start_time=?";
        try (PreparedStatement s = connection.prepareStatement(sql)) {
            s.setLong(1, scheduleId);
            s.setObject(2, start);
            try (ResultSet rs = s.executeQuery()) { return rs.next(); }
        }
    }

    public TimeSlot getTimeSlotById(long id) throws SQLException {
        String sql = "SELECT * FROM time_slots WHERE id=?";
        try (PreparedStatement s = connection.prepareStatement(sql)) {
            s.setLong(1, id);
            try (ResultSet rs = s.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<TimeSlot> getAllTimeSlots() throws SQLException {
        List<TimeSlot> list = new ArrayList<>();
        String sql = "SELECT * FROM time_slots ORDER BY start_time ASC";
        try (PreparedStatement s = connection.prepareStatement(sql);
             ResultSet rs = s.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    // All slots for a schedule (available + booked)
    public List<TimeSlot> getAllSlotsBySchedule(long scheduleId) throws SQLException {
        List<TimeSlot> list = new ArrayList<>();
        String sql = "SELECT * FROM time_slots WHERE schedule_id=? ORDER BY start_time ASC";
        try (PreparedStatement s = connection.prepareStatement(sql)) {
            s.setLong(1, scheduleId);
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    // All slots for a date (joins schedules)
    public List<TimeSlot> getAllSlotsByDate(LocalDate date) throws SQLException {
        List<TimeSlot> list = new ArrayList<>();
        String sql = "SELECT ts.* FROM time_slots ts " +
                     "JOIN schedules sc ON sc.id=ts.schedule_id " +
                     "WHERE sc.work_date=? ORDER BY ts.start_time ASC";
        try (PreparedStatement s = connection.prepareStatement(sql)) {
            s.setObject(1, date);
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    // Only available future slots
    public List<TimeSlot> getAvailableSlots() throws SQLException {
        List<TimeSlot> list = new ArrayList<>();
        String sql = "SELECT * FROM time_slots WHERE available=TRUE AND end_time>NOW() " +
                     "ORDER BY start_time ASC";
        try (PreparedStatement s = connection.prepareStatement(sql);
             ResultSet rs = s.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<TimeSlot> getAvailableSlotsByScheduleId(long scheduleId) throws SQLException {
        List<TimeSlot> list = new ArrayList<>();
        String sql = "SELECT * FROM time_slots " +
                     "WHERE schedule_id=? AND available=TRUE AND end_time>NOW() " +
                     "ORDER BY start_time ASC";
        try (PreparedStatement s = connection.prepareStatement(sql)) {
            s.setLong(1, scheduleId);
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public int updateAvailability(long id, boolean available) throws SQLException {
        String sql = "UPDATE time_slots SET available=? WHERE id=?";
        try (PreparedStatement s = connection.prepareStatement(sql)) {
            s.setBoolean(1, available);
            s.setLong(2, id);
            return s.executeUpdate();
        }
    }

    // Get the username of who booked a slot (returns null if available)
    public String getBookedByUsername(long slotId) throws SQLException {
        String sql = "SELECT u.username FROM appointments a " +
                     "JOIN users u ON u.id = a.created_by " +
                     "WHERE a.slot_id=? AND a.status IN ('CONFIRMED','PENDING') LIMIT 1";
        try (PreparedStatement s = connection.prepareStatement(sql)) {
            s.setLong(1, slotId);
            try (ResultSet rs = s.executeQuery()) {
                return rs.next() ? rs.getString("username") : null;
            }
        }
    }

    public void deleteTimeSlot(long id) throws SQLException {
        String sql = "DELETE FROM time_slots WHERE id=?";
        try (PreparedStatement s = connection.prepareStatement(sql)) {
            s.setLong(1, id);
            s.executeUpdate();
        }
    }

    private TimeSlot map(ResultSet rs) throws SQLException {
        return new TimeSlot(rs.getLong("id"), rs.getLong("schedule_id"),
                            rs.getObject("start_time", OffsetDateTime.class),
                            rs.getObject("end_time",   OffsetDateTime.class),
                            rs.getBoolean("available"));
    }
}