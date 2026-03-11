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
        String sql = "INSERT INTO appointments " +
                     "(type, status, start_time, end_time, participants_count, max_participants, created_by, slot_id, created_at, updated_at) " +
                     "VALUES (?::appointment_type, ?::booking_status, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, normalizeAppointmentType(appointment.getType()));
            stmt.setString(2, normalizeStatus(appointment.getStatus()));
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
        String sql = "UPDATE appointments SET status = ?::booking_status, updated_at = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, normalizeStatus(newStatus));
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
                rs.getObject("start_time", java.time.OffsetDateTime.class),
                rs.getObject("end_time", java.time.OffsetDateTime.class),
                rs.getInt("participants_count"),
                rs.getInt("max_participants"),
                rs.getLong("created_by"),
                rs.getObject("slot_id", Long.class),
                rs.getObject("created_at", java.time.OffsetDateTime.class),
                rs.getObject("updated_at", java.time.OffsetDateTime.class)
        );
    }

    private String normalizeAppointmentType(String input) {
        if (input == null) return null;
        String v = input.trim().toUpperCase();
        switch (v) {
            case "URGENT":
            case "FOLLOW_UP":
            case "GROUP":
                return v;
            default:
                if (v.equals("FOLLOW-UP")) return "FOLLOW_UP";
                if (v.equals("FOLLOWUP")) return "FOLLOW_UP";
                throw new IllegalArgumentException("Unsupported appointment type: " + input);
        }
    }

    private String normalizeStatus(String input) {
        if (input == null) return null;
        String v = input.trim().toUpperCase();
        switch (v) {
            case "PENDING":
            case "CONFIRMED":
            case "CANCELED":
                return v;
            default:
                throw new IllegalArgumentException("Unsupported status: " + input);
        }
    }
}