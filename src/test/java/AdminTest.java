import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import domain.Admin;
import domain.Appointment;
import domain.Schedule;
import domain.TimeSlot;
import persistence.AppointmentDAO;
import persistence.TimeSlotDAO;
import persistence.ScheduleDAO;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.anyLong;

@ExtendWith(MockitoExtension.class)
class AdminTest {

    @Mock
    private AppointmentDAO appointmentDAO;

    @Mock
    private TimeSlotDAO timeSlotDAO;

    @Mock
    private ScheduleDAO scheduleDAO;

    private Admin admin;

    @BeforeEach
    void setUp() {
        admin = new Admin(
                "1",
                "Admin",
                "admin@test.com",
                "123456",
                "adminuser",
                appointmentDAO,
                timeSlotDAO,
                scheduleDAO
        );
    }

    @Test
    void viewWorkDays_shouldReturnSchedules() throws Exception {
        when(scheduleDAO.getAllSchedules())
                .thenReturn(Arrays.asList(new Schedule()));

        List<Schedule> result = admin.viewWorkDays();

        assertEquals(1, result.size());
    }

    @Test
    void addWorkDay_shouldCallDAO() throws Exception {
        admin.addWorkDay(LocalDate.now());

        verify(scheduleDAO).addSchedule(any(Schedule.class));
    }

    @Test
    void viewAvailableSlots_shouldReturnSlots() throws Exception {
        when(timeSlotDAO.getAvailableSlots())
                .thenReturn(Arrays.asList(new TimeSlot()));

        List<TimeSlot> result = admin.viewAvailableSlots();

        assertEquals(1, result.size());
    }

    @Test
    void viewAvailableSlotsByDay_shouldReturnSlots() throws Exception {
        when(timeSlotDAO.getAvailableSlotsByScheduleId(anyLong()))
                .thenReturn(Arrays.asList(new TimeSlot()));

        List<TimeSlot> result = admin.viewAvailableSlotsByDay(5L);

        assertEquals(1, result.size());
    }

    @Test
    void addSlot_shouldCallDAO() throws Exception {
        admin.addSlot(
                3L,
                OffsetDateTime.now(),
                OffsetDateTime.now().plusMinutes(30)
        );

        verify(timeSlotDAO).addTimeSlot(any(TimeSlot.class));
    }

    @Test
    void setSlotAvailability_shouldCallDAO() throws Exception {
        admin.setSlotAvailability(1L, false);

        verify(timeSlotDAO).updateAvailability(1L, false);
    }

    @Test
    void cancelAppointment_shouldUpdateStatus() throws Exception {
        admin.cancelAppointment(10L);

        verify(appointmentDAO).updateStatus(10L, "CANCELED");
    }

    @Test
    void modifyAppointment_shouldUpdateParticipants() throws Exception {
        admin.modifyAppointment(7L, 4);

        verify(appointmentDAO).updateParticipants(7L, 4);
    }

    @Test
    void validateDuration_shouldReturnTrueIfWithin30Minutes() {
        Appointment appointment = mock(Appointment.class);

        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = start.plusMinutes(30);

        when(appointment.getStartTime()).thenReturn(start);
        when(appointment.getEndTime()).thenReturn(end);

        assertTrue(admin.validateDuration(appointment));
    }

    @Test
    void validateDuration_shouldReturnFalseIfOver30Minutes() {
        Appointment appointment = mock(Appointment.class);

        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = start.plusMinutes(45);

        when(appointment.getStartTime()).thenReturn(start);
        when(appointment.getEndTime()).thenReturn(end);

        assertFalse(admin.validateDuration(appointment));
    }

    @Test
    void validateCapacity_shouldReturnTrueIfWithinLimit() {
        Appointment appointment = mock(Appointment.class);

        when(appointment.getParticipantsCount()).thenReturn(3);
        when(appointment.getMaxParticipants()).thenReturn(5);

        assertTrue(admin.validateCapacity(appointment));
    }

    @Test
    void validateCapacity_shouldReturnFalseIfExceeded() {
        Appointment appointment = mock(Appointment.class);

        when(appointment.getParticipantsCount()).thenReturn(6);
        when(appointment.getMaxParticipants()).thenReturn(5);

        assertFalse(admin.validateCapacity(appointment));
    }
}