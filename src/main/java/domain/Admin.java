package domain;

import persistence.AppointmentDAO;
import persistence.TimeSlotDAO;
import persistence.ScheduleDAO;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public class Admin extends User {

    private final AppointmentDAO appointmentDAO;
    private final TimeSlotDAO timeSlotDAO;
    private final ScheduleDAO scheduleDAO;

    private Admin(Builder builder) {
        super(
            builder.id,
            builder.name,
            builder.email,
            builder.phoneNumber,
            builder.username,
            Role.ADMIN
        );
        this.appointmentDAO = builder.appointmentDAO;
        this.timeSlotDAO    = builder.timeSlotDAO;
        this.scheduleDAO    = builder.scheduleDAO;
    }

    // ====== Builder Class ======
    public static class Builder {
        private String id;
        private String name;
        private String email;
        private String phoneNumber;
        private String username;

        private AppointmentDAO appointmentDAO;
        private TimeSlotDAO timeSlotDAO;
        private ScheduleDAO scheduleDAO;

        public Builder setId(String id) { this.id = id; return this; }
        public Builder setName(String name) { this.name = name; return this; }
        public Builder setEmail(String email) { this.email = email; return this; }
        public Builder setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; return this; }
        public Builder setUsername(String username) { this.username = username; return this; }
        public Builder setAppointmentDAO(AppointmentDAO d) { this.appointmentDAO = d; return this; }
        public Builder setTimeSlotDAO(TimeSlotDAO d) { this.timeSlotDAO = d; return this; }
        public Builder setScheduleDAO(ScheduleDAO d) { this.scheduleDAO = d; return this; }
        public Admin build() {
            return new Admin(this);
        }
    }
    // ====== End Builder =======

    public List<Schedule> viewWorkDays() throws SQLException {
        return scheduleDAO.getAllSchedules();
    }

    public long addWorkDay(LocalDate workDate) throws SQLException {
        return scheduleDAO.addSchedule(new Schedule(workDate));
    }

    public List<TimeSlot> viewAvailableSlots() throws SQLException {
        return timeSlotDAO.getAvailableSlots();
    }

    public List<TimeSlot> viewAvailableSlotsByDay(long scheduleId) throws SQLException {
        return timeSlotDAO.getAvailableSlotsByScheduleId(scheduleId);
    }

    public long addSlot(long scheduleId, OffsetDateTime start, OffsetDateTime end)
            throws SQLException {
        return timeSlotDAO.addTimeSlot(new TimeSlot(scheduleId, start, end, true));
    }

    public boolean setSlotAvailability(long slotId, boolean available) throws SQLException {
        return timeSlotDAO.updateAvailability(slotId, available) > 0;
    }

    public boolean cancelAppointmentWithNote(long appointmentId, String note,
                                             TimeSlotDAO tsDAO) throws SQLException {
        Appointment a = appointmentDAO.getAppointmentById(appointmentId);
        if (a == null) return false;
        appointmentDAO.updateStatusAndNote(appointmentId, Appointment.STATUS_CANCELED, note);
        if (a.getSlotId() != null) tsDAO.updateAvailability(a.getSlotId(), true);
        return true;
    }

    public boolean modifyAppointmentWithNote(long appointmentId, int newCount, String note)
            throws SQLException {
        if (appointmentDAO.getAppointmentById(appointmentId) == null) return false;
        appointmentDAO.updateParticipantsAndNote(appointmentId, newCount, note);
        return true;
    }

    public boolean validateDuration(Appointment a) {
        long min = java.time.Duration.between(a.getStartTime(), a.getEndTime()).toMinutes();
        return min > 0 && min <= 60;
    }

    public boolean validateCapacity(Appointment a) {
        return a.getParticipantsCount() >= 1 &&
               a.getParticipantsCount() <= a.getMaxParticipants();
    }

    public boolean validateType(Appointment a) {
        String t = a.getType();
        return Appointment.TYPE_FIRST_VISIT.equalsIgnoreCase(t)
            || Appointment.TYPE_FOLLOW_UP.equalsIgnoreCase(t)
            || Appointment.TYPE_VIRTUAL.equalsIgnoreCase(t)
            || Appointment.TYPE_GROUP_FIRST_VISIT.equalsIgnoreCase(t)
            || Appointment.TYPE_GROUP_FOLLOW_UP.equalsIgnoreCase(t)
            || Appointment.TYPE_GROUP_VIRTUAL.equalsIgnoreCase(t);
    }
}