package persistence;

import domain.Appointment;

import java.sql.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for {@link Appointment} entities.
 *
 * <p>Handles all database operations for the {@code appointments} table,
 * including insert, query, status updates, participant updates, type updates,
 * slot rescheduling, admin-note writes, and automatic marking of past
 * appointments as {@code DONE}.</p>
 *
 * <p>All appointment-type and status strings are normalized through private
 * helper methods before being sent to the database, so callers can safely
 * pass the constants defined in {@link Appointment}.</p>
 *
 * @author Student
 * @version 1.0
 */
public class AppointmentDAO {

    private final Connection connection;

    /**
     * Constructs an AppointmentDAO backed by the given JDBC connection.
     *
     * @param connection active connection to the PostgreSQL database
     */
    public AppointmentDAO(Connection connection) {
        this.connection = connection;
    }

    /**
     * Marks every CONFIRMED or PENDING appointment whose end_time is in the
     * past as DONE. Call once at application startup.
     *
     * @throws SQLException if a database error occurs
     */
    public void markPastAppointmentsDone() throws SQLException {
        String sql = "UPDATE appointments " +
                     "SET status='DONE'::booking_status, updated_at=? " +
                     "WHERE end_time < NOW() AND status IN ('CONFIRMED','PENDING')";
        try (PreparedStatement s = connection.prepareStatement(sql)) {
            s.setObject(1, OffsetDateTime.now());
            int rows = s.executeUpdate();
            if (rows > 0)
                System.out.println("[INFO] " + rows + " past appointment(s) marked DONE.");
        }
    }

    /**
     * Inserts a new appointment. created_at and updated_at are set automatically.
     *
     * @param appointment appointment to persist (id field is ignored)
     * @throws SQLException if a database error occurs
     */
    public void addAppointment(Appointment appointment) throws SQLException {
        String sql = "INSERT INTO appointments " +
                     "(type,status,start_time,end_time,participants_count," +
                     " max_participants,created_by,slot_id,created_at,updated_at) " +
                     "VALUES(?::appointment_type,?::booking_status,?,?,?,?,?,?,?,?)";
        try (PreparedStatement s = connection.prepareStatement(sql)) {
            s.setString(1, normalizeType(appointment.getType()));
            s.setString(2, normalizeStatus(appointment.getStatus()));
            s.setObject(3, appointment.getStartTime());
            s.setObject(4, appointment.getEndTime());
            s.setInt(5, appointment.getParticipantsCount());
            s.setInt(6, appointment.getMaxParticipants());
            s.setInt(7, (int) appointment.getCreatedBy());
            if (appointment.getSlotId() != null)
                s.setLong(8, appointment.getSlotId());
            else
                s.setNull(8, Types.BIGINT);
            s.setObject(9, OffsetDateTime.now());
            s.setObject(10, OffsetDateTime.now());
            s.executeUpdate();
        }
    }

    /**
     * Retrieves a single appointment by primary key.
     *
     * @param id appointment ID
     * @return the matching Appointment, or null if not found
     * @throws SQLException if a database error occurs
     */
    public Appointment getAppointmentById(long id) throws SQLException {
        String sql = "SELECT * FROM appointments WHERE id=?";
        try (PreparedStatement s = connection.prepareStatement(sql)) {
            s.setLong(1, id);
            try (ResultSet rs = s.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /**
     * Returns all appointments ordered by start_time ascending.
     *
     * @return list of all appointments
     * @throws SQLException if a database error occurs
     */
    public List<Appointment> getAllAppointments() throws SQLException {
        List<Appointment> list = new ArrayList<>();
        String sql = "SELECT * FROM appointments ORDER BY start_time ASC";
        try (PreparedStatement s = connection.prepareStatement(sql);
             ResultSet rs = s.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    /**
     * Returns active (CONFIRMED) appointments for a specific work day,
     * excluding DONE, CANCELED appointments.
     *
     * @param date the work day date
     * @return list of active appointments on that day
     * @throws SQLException if a database error occurs
     */
    public List<Appointment> getActiveAppointmentsByDate(LocalDate date) throws SQLException {
        List<Appointment> list = new ArrayList<>();
        String sql = "SELECT a.* FROM appointments a " +
                     "WHERE DATE(a.start_time AT TIME ZONE 'UTC') = ? " +
                     "  AND a.status IN ('CONFIRMED','PENDING') " +
                     "ORDER BY a.start_time ASC";
        try (PreparedStatement s = connection.prepareStatement(sql)) {
            s.setObject(1, date);
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /**
     * Returns future active appointments for a specific work day (used by admin cancel/edit).
     *
     * @param date the work day date
     * @return list of future CONFIRMED appointments on that day
     * @throws SQLException if a database error occurs
     */
    public List<Appointment> getFutureAppointmentsByDate(LocalDate date) throws SQLException {
        List<Appointment> list = new ArrayList<>();
        String sql = "SELECT a.* FROM appointments a " +
                     "WHERE DATE(a.start_time AT TIME ZONE 'UTC') = ? " +
                     "  AND a.start_time > NOW() " +
                     "  AND a.status IN ('CONFIRMED','PENDING') " +
                     "ORDER BY a.start_time ASC";
        try (PreparedStatement s = connection.prepareStatement(sql)) {
            s.setObject(1, date);
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /**
     * Returns all appointments for a specific user, ordered by start_time.
     *
     * @param userId numeric user ID
     * @return list of appointments for that user
     * @throws SQLException if a database error occurs
     */
    public List<Appointment> getAppointmentsByUser(long userId) throws SQLException {
        List<Appointment> list = new ArrayList<>();
        String sql = "SELECT * FROM appointments WHERE created_by=? ORDER BY start_time ASC";
        try (PreparedStatement s = connection.prepareStatement(sql)) {
            s.setInt(1, (int) userId);
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /**
     * Updates only the status of an appointment.
     *
     * @param id        appointment ID
     * @param newStatus STATUS_* constant
     * @return rows affected
     * @throws SQLException if a database error occurs
     */
    public int updateStatus(long id, String newStatus) throws SQLException {
        String sql = "UPDATE appointments SET status=?::booking_status,updated_at=? WHERE id=?";
        try (PreparedStatement s = connection.prepareStatement(sql)) {
            s.setString(1, normalizeStatus(newStatus));
            s.setObject(2, OffsetDateTime.now());
            s.setLong(3, id);
            return s.executeUpdate();
        }
    }

    /**
     * Cancels an appointment (admin), stores the reason note, frees the slot,
     * and marks canceled_by_admin = true so the user sees who canceled.
     *
     * @param id     appointment ID
     * @param note   admin reason (may be null)
     * @return rows affected
     * @throws SQLException if a database error occurs
     */
    public int cancelByAdmin(long id, String note) throws SQLException {
        String sql = "UPDATE appointments " +
                     "SET status='CANCELED'::booking_status, " +
                     "    admin_note=?, canceled_by_admin=TRUE, updated_at=? " +
                     "WHERE id=?";
        try (PreparedStatement s = connection.prepareStatement(sql)) {
            s.setString(1, note);
            s.setObject(2, OffsetDateTime.now());
            s.setLong(3, id);
            return s.executeUpdate();
        }
    }

    /**
     * Updates status and sets an admin note.
     *
     * @param id        appointment ID
     * @param newStatus STATUS_* constant
     * @param note      admin note
     * @return rows affected
     * @throws SQLException if a database error occurs
     */
    public int updateStatusAndNote(long id, String newStatus, String note) throws SQLException {
        String sql = "UPDATE appointments " +
                     "SET status=?::booking_status,admin_note=?,updated_at=? WHERE id=?";
        try (PreparedStatement s = connection.prepareStatement(sql)) {
            s.setString(1, normalizeStatus(newStatus));
            s.setString(2, note);
            s.setObject(3, OffsetDateTime.now());
            s.setLong(4, id);
            return s.executeUpdate();
        }
    }

    /**
     * Updates participant count.
     *
     * @param id       appointment ID
     * @param newCount new count
     * @return rows affected
     * @throws SQLException if a database error occurs
     */
    public int updateParticipants(long id, int newCount) throws SQLException {
        String sql = "UPDATE appointments SET participants_count=?,updated_at=? WHERE id=?";
        try (PreparedStatement s = connection.prepareStatement(sql)) {
            s.setInt(1, newCount);
            s.setObject(2, OffsetDateTime.now());
            s.setLong(3, id);
            return s.executeUpdate();
        }
    }

    /**
     * Updates participant count and sets an admin note.
     *
     * @param id       appointment ID
     * @param newCount new count
     * @param note     admin note
     * @return rows affected
     * @throws SQLException if a database error occurs
     */
    public int updateParticipantsAndNote(long id, int newCount, String note) throws SQLException {
        String sql = "UPDATE appointments " +
                     "SET participants_count=?,admin_note=?,updated_at=? WHERE id=?";
        try (PreparedStatement s = connection.prepareStatement(sql)) {
            s.setInt(1, newCount);
            s.setString(2, note);
            s.setObject(3, OffsetDateTime.now());
            s.setLong(4, id);
            return s.executeUpdate();
        }
    }

    /**
     * Updates appointment type and optionally sets an admin note.
     *
     * @param id      appointment ID
     * @param newType TYPE_* constant
     * @param note    admin note (may be null)
     * @return rows affected
     * @throws SQLException if a database error occurs
     */
    public int updateTypeAndNote(long id, String newType, String note) throws SQLException {
        String sql = "UPDATE appointments " +
                     "SET type=?::appointment_type,admin_note=?,updated_at=? WHERE id=?";
        try (PreparedStatement s = connection.prepareStatement(sql)) {
            s.setString(1, normalizeType(newType));
            s.setString(2, note);
            s.setObject(3, OffsetDateTime.now());
            s.setLong(4, id);
            return s.executeUpdate();
        }
    }

    /**
     * Updates appointment type.
     *
     * @param id      appointment ID
     * @param newType TYPE_* constant
     * @return rows affected
     * @throws SQLException if a database error occurs
     */
    public int updateType(long id, String newType) throws SQLException {
        String sql = "UPDATE appointments SET type=?::appointment_type,updated_at=? WHERE id=?";
        try (PreparedStatement s = connection.prepareStatement(sql)) {
            s.setString(1, normalizeType(newType));
            s.setObject(2, OffsetDateTime.now());
            s.setLong(3, id);
            return s.executeUpdate();
        }
    }

    /**
     * Reschedules to a different slot.
     *
     * @param id        appointment ID
     * @param newSlotId new time slot ID
     * @param newStart  new start time
     * @param newEnd    new end time
     * @return rows affected
     * @throws SQLException if a database error occurs
     */
    public int updateSlotAndTime(long id, long newSlotId,
                                  OffsetDateTime newStart, OffsetDateTime newEnd)
            throws SQLException {
        String sql = "UPDATE appointments " +
                     "SET slot_id=?,start_time=?,end_time=?,updated_at=? WHERE id=?";
        try (PreparedStatement s = connection.prepareStatement(sql)) {
            s.setLong(1, newSlotId);
            s.setObject(2, newStart);
            s.setObject(3, newEnd);
            s.setObject(4, OffsetDateTime.now());
            s.setLong(5, id);
            return s.executeUpdate();
        }
    }

    /**
     * Permanently deletes an appointment record.
     *
     * @param id appointment ID
     * @throws SQLException if a database error occurs
     */
    public void deleteAppointment(long id) throws SQLException {
        String sql = "DELETE FROM appointments WHERE id=?";
        try (PreparedStatement s = connection.prepareStatement(sql)) {
            s.setLong(1, id);
            s.executeUpdate();
        }
    }

    // ── private helpers ──────────────────────────────────────────────

    private Appointment map(ResultSet rs) throws SQLException {
        Appointment a = new Appointment(
                rs.getLong("id"),
                rs.getString("type"),
                rs.getString("status"),
                rs.getObject("start_time", OffsetDateTime.class),
                rs.getObject("end_time",   OffsetDateTime.class),
                rs.getInt("participants_count"),
                rs.getInt("max_participants"),
                rs.getInt("created_by"),
                rs.getObject("slot_id", Long.class),
                rs.getString("admin_note"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class));
        a.setCanceledByAdmin(rs.getBoolean("canceled_by_admin"));
        return a;
    }

    private String normalizeType(String input) {
        if (input == null) return null;
        String v = input.trim().toUpperCase().replace('-','_').replace(' ','_');
        switch (v) {
            case "FIRST_VISIT":       return "FIRST_VISIT";
            case "FOLLOW_UP":
            case "FOLLOWUP":          return "FOLLOW_UP";
            case "VIRTUAL":           return "VIRTUAL";
            case "GROUP_FIRST_VISIT": return "GROUP_FIRST_VISIT";
            case "GROUP_FOLLOW_UP":   return "GROUP_FOLLOW_UP";
            case "GROUP_VIRTUAL":     return "GROUP_VIRTUAL";
            default: throw new IllegalArgumentException("Unknown type: " + input);
        }
    }

    private String normalizeStatus(String input) {
        if (input == null) return null;
        switch (input.trim().toUpperCase()) {
            case "PENDING":   return "PENDING";
            case "CONFIRMED": return "CONFIRMED";
            case "CANCELED":
            case "CANCELLED": return "CANCELED";
            case "DONE":      return "DONE";
            default: throw new IllegalArgumentException("Unknown status: " + input);
        }
    }
}