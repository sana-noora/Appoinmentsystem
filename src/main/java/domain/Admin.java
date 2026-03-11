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
                 AppointmentDAO appointmentDAO, TimeSlotDAO timeSlotDAO, ScheduleDAO scheduleDAO) {
        super(id, name, email, phoneNumber, username, Role.ADMIN);
        this.appointmentDAO = appointmentDAO;
        this.timeSlotDAO = timeSlotDAO;
        this.scheduleDAO = scheduleDAO;
    }

    public List<Schedule> viewWorkDays() throws SQLException {
        return scheduleDAO.getAllSchedules();
    }

    public void addWorkDay(LocalDate workDate) throws SQLException {
        Schedule schedule = new Schedule(0L, workDate, null);
        scheduleDAO.addSchedule(schedule);
    }

    public List<TimeSlot> viewAvailableSlots() throws SQLException {
        return timeSlotDAO.getAvailableSlots();
    }

    public List<TimeSlot> viewAvailableSlotsByDay(long scheduleId) throws SQLException {
        return timeSlotDAO.getAvailableSlotsByScheduleId(scheduleId);
    }

    public void addSlot(long scheduleId, OffsetDateTime start, OffsetDateTime end) throws SQLException {
        TimeSlot slot = new TimeSlot(0L, scheduleId, start, end, true);
        timeSlotDAO.addTimeSlot(slot);
    }

    public void setSlotAvailability(long slotId, boolean available) throws SQLException {
        timeSlotDAO.updateAvailability(slotId, available);
    }

    public void cancelAppointment(long appointmentId) throws SQLException {
        appointmentDAO.updateStatus(appointmentId, "CANCELED");
        System.out.println("Appointment " + appointmentId + " has been canceled.");
    }

    public void modifyAppointment(long appointmentId, int newCount) throws SQLException {
        appointmentDAO.updateParticipants(appointmentId, newCount);
        System.out.println("Appointment " + appointmentId + " participants updated to " + newCount);
    }

    public boolean validateDuration(Appointment appointment) {
        long minutes = java.time.Duration.between(
                appointment.getStartTime(), appointment.getEndTime()).toMinutes();
        return minutes <= 30;
    }

    public boolean validateCapacity(Appointment appointment) {
        return appointment.getParticipantsCount() <= appointment.getMaxParticipants();
    }
}