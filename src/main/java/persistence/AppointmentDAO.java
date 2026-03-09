package persistence;

import domain.Appointment;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;


public class AppointmentDAO {
    private final Connection connection;

    public AppointmentDAO(Connection connection) {
        this.connection = connection;
    }

   
    public void addAppointment(Appointment appointment) throws SQLException {
        String sql = "INSERT INTO appointments (type, status, start_time, end_time, participants_count, max_participants, created_by, slot_id, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, appointment.getType());
            stmt.setString(2, appointment.getStatus());
            stmt.setObject(3, appointment.getStartTime());
            stmt.setObject(4, appointment.getEndTime());
            stmt.setInt(5, appointment.getParticipantsCount());
            stmt.setInt(6, appointment.getMaxParticipants());
            stmt.setLong(7, appointment.getCreatedBy());
            stmt.setObject(8, appointment.getSlotId());
            stmt.setObject(9, OffsetDateTime.now());
            stmt.setObject(10, OffsetDateTime.now());
            stmt.executeUpdate();
        }
    }

    
    public Appointment getAppointmentById(long id) throws SQLException {
        String sql = "SELECT * FROM appointments WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAppointment(rs);
                }
            }
        }
        return null;
    }

    
    public List<Appointment> getAllAppointments() throws SQLException {
        List<Appointment> appointments = new ArrayList<>();
        String sql = "SELECT * FROM appointments";

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                appointments.add(mapResultSetToAppointment(rs));
            }
        }
        return appointments;
    }

    
    public void updateStatus(long appointmentId, String newStatus) throws SQLException {
        String sql = "UPDATE appointments SET status = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, newStatus);
            stmt.setObject(2, OffsetDateTime.now());
            stmt.setLong(3, appointmentId);
            stmt.executeUpdate();
        }
    }

    
    public void updateParticipants(long appointmentId, int newCount) throws SQLException {
        String sql = "UPDATE appointments SET participants_count = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, newCount);
            stmt.setObject(2, OffsetDateTime.now());
            stmt.setLong(3, appointmentId);
            stmt.executeUpdate();
        }
    }

    
    public void deleteAppointment(long appointmentId) throws SQLException {
        String sql = "DELETE FROM appointments WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, appointmentId);
            stmt.executeUpdate();
        }
    }

    
    private Appointment mapResultSetToAppointment(ResultSet rs) throws SQLException {
        return new Appointment(
                rs.getLong("id"),
                rs.getString("type"),
                rs.getString("status"),
                rs.getObject("start_time", OffsetDateTime.class),
                rs.getObject("end_time", OffsetDateTime.class),
                rs.getInt("participants_count"),
                rs.getInt("max_participants"),
                rs.getLong("created_by"),
                rs.getObject("slot_id", Long.class),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class)
        );
    }
}
