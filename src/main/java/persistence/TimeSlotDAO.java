package persistence;

import domain.TimeSlot;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;


public class TimeSlotDAO {
    private final Connection connection;

    public TimeSlotDAO(Connection connection) {
        this.connection = connection;
    }

    
    public void addTimeSlot(TimeSlot slot) throws SQLException {
        String sql = "INSERT INTO time_slots (schedule_id, start_time, end_time, available) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, slot.getScheduleId());
            stmt.setObject(2, slot.getStartTime());
            stmt.setObject(3, slot.getEndTime());
            stmt.setBoolean(4, slot.isAvailable());
            stmt.executeUpdate();
        }
    }

    
    public TimeSlot getTimeSlotById(long id) throws SQLException {
        String sql = "SELECT * FROM time_slots WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToTimeSlot(rs);
                }
            }
        }
        return null;
    }

    
    public List<TimeSlot> getAllTimeSlots() throws SQLException {
        List<TimeSlot> slots = new ArrayList<>();
        String sql = "SELECT * FROM time_slots";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                slots.add(mapResultSetToTimeSlot(rs));
            }
        }
        return slots;
    }

   
    public List<TimeSlot> getAvailableSlots() throws SQLException {
        List<TimeSlot> slots = new ArrayList<>();
        String sql = "SELECT * FROM time_slots WHERE available = TRUE";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                slots.add(mapResultSetToTimeSlot(rs));
            }
        }
        return slots;
    }

    
    public void updateAvailability(long id, boolean available) throws SQLException {
        String sql = "UPDATE time_slots SET available = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBoolean(1, available);
            stmt.setLong(2, id);
            stmt.executeUpdate();
        }
    }

  
    public void deleteTimeSlot(long id) throws SQLException {
        String sql = "DELETE FROM time_slots WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
    }

    
    private TimeSlot mapResultSetToTimeSlot(ResultSet rs) throws SQLException {
        return new TimeSlot(
                rs.getLong("id"),
                rs.getLong("schedule_id"),
                rs.getObject("start_time", OffsetDateTime.class),
                rs.getObject("end_time", OffsetDateTime.class),
                rs.getBoolean("available")
        );
    }
}
