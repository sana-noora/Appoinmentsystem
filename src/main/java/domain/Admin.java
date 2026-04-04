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

    public Admin(String id, String name, String email, String phoneNumber,
                 String username,
                 AppointmentDAO appointmentDAO,
                 TimeSlotDAO timeSlotDAO,
                 ScheduleDAO scheduleDAO) {
        super(id, name, email, phoneNumber, username, Role.ADMIN);
        this.appointmentDAO = appointmentDAO;
        this.timeSlotDAO    = timeSlotDAO;
        this.scheduleDAO    = scheduleDAO;
    }

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