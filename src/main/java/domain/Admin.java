package domain;

import persistence.AppointmentDAO;
import persistence.TimeSlotDAO;

import java.sql.SQLException;
import java.util.List;


public class Admin extends User {

    private final AppointmentDAO appointmentDAO;
    private final TimeSlotDAO timeSlotDAO;

    public Admin(String id, String name, String email, String phoneNumber,
                 String username, String plainPassword,
                 AppointmentDAO appointmentDAO, TimeSlotDAO timeSlotDAO) {
        super(id, name, email, phoneNumber, username, plainPassword, Role.ADMIN);
        this.appointmentDAO = appointmentDAO;
        this.timeSlotDAO = timeSlotDAO;
    }



    public List<TimeSlot> viewAvailableSlots() throws SQLException {
        return timeSlotDAO.getAvailableSlots();
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
