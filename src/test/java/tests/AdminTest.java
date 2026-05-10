package tests;


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
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminTest {

    @Mock private AppointmentDAO appointmentDAO;
    @Mock private TimeSlotDAO timeSlotDAO;
    @Mock private ScheduleDAO scheduleDAO;

    private Admin admin;

    @BeforeEach
    void setUp() {
        admin = new Admin.Builder()
                .setId("1")
                .setName("Admin")
                .setEmail("admin@test.com")
                .setPhoneNumber("123456")
                .setUsername("adminuser")
                .setAppointmentDAO(appointmentDAO)
                .setTimeSlotDAO(timeSlotDAO)
                .setScheduleDAO(scheduleDAO)
                .build();
    }

    // -------------------- Work days --------------------

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

    // -------------------- Slots --------------------

    @Test
    void viewAvailableSlots_shouldReturnSlots() throws Exception {
        when(timeSlotDAO.getAvailableSlots())
                .thenReturn(Arrays.asList(new TimeSlot()));

        List<TimeSlot> result = admin.viewAvailableSlots();
        assertEquals(1, result.size());
    }

    @Test
    void viewAvailableSlotsByDay_shouldReturnSlots() throws Exception {
        when(timeSlotDAO.getAvailableSlotsByScheduleId(5L))
                .thenReturn(Arrays.asList(new TimeSlot()));

        List<TimeSlot> result = admin.viewAvailableSlotsByDay(5L);
        assertEquals(1, result.size());
    }

    @Test
    void addSlot_shouldCallDAO() throws Exception {
        admin.addSlot(3L, OffsetDateTime.now(), OffsetDateTime.now().plusMinutes(30));
        verify(timeSlotDAO).addTimeSlot(any(TimeSlot.class));
    }

    @Test
    void setSlotAvailability_shouldReturnTrueWhenUpdated() throws Exception {
        when(timeSlotDAO.updateAvailability(1L, false)).thenReturn(1);
        assertTrue(admin.setSlotAvailability(1L, false));
    }

    @Test
    void setSlotAvailability_shouldReturnFalseWhenNotUpdated() throws Exception {
        when(timeSlotDAO.updateAvailability(1L, false)).thenReturn(0);
        assertFalse(admin.setSlotAvailability(1L, false));
    }

    // -------------------- Cancel appointment --------------------

    @Test
    void cancelAppointmentWithNote_shouldCancelAndFreeSlot() throws Exception {
        Appointment a = mock(Appointment.class);
        when(a.getSlotId()).thenReturn(4L);
        when(appointmentDAO.getAppointmentById(10L)).thenReturn(a);

        boolean result = admin.cancelAppointmentWithNote(10L, "note", timeSlotDAO);

        assertTrue(result);
        verify(appointmentDAO)
                .updateStatusAndNote(10L, Appointment.STATUS_CANCELED, "note");
        verify(timeSlotDAO).updateAvailability(4L, true);
    }

    @Test
    void cancelAppointmentWithNote_shouldReturnFalseWhenNotFound() throws Exception {
        when(appointmentDAO.getAppointmentById(10L)).thenReturn(null);
        assertFalse(admin.cancelAppointmentWithNote(10L, "note", timeSlotDAO));
    }

    // -------------------- Modify appointment --------------------

    @Test
    void modifyAppointmentWithNote_shouldUpdateWhenExists() throws Exception {
        Appointment a = mock(Appointment.class);
        when(appointmentDAO.getAppointmentById(7L)).thenReturn(a);

        boolean result = admin.modifyAppointmentWithNote(7L, 4, "note");

        assertTrue(result);
        verify(appointmentDAO).updateParticipantsAndNote(7L, 4, "note");
    }

    @Test
    void modifyAppointmentWithNote_shouldReturnFalseWhenNotFound() throws Exception {
        when(appointmentDAO.getAppointmentById(7L)).thenReturn(null);
        assertFalse(admin.modifyAppointmentWithNote(7L, 4, "note"));
    }

    // -------------------- Validation --------------------

    @Test
    void validateDuration_shouldReturnTrueIfWithin60Minutes() {
        Appointment a = mock(Appointment.class);
        OffsetDateTime s = OffsetDateTime.now();
        OffsetDateTime e = s.plusMinutes(30);

        when(a.getStartTime()).thenReturn(s);
        when(a.getEndTime()).thenReturn(e);

        assertTrue(admin.validateDuration(a));
    }

    @Test
    void validateDuration_shouldReturnFalseIfOver60Minutes() {
        Appointment a = mock(Appointment.class);
        OffsetDateTime s = OffsetDateTime.now();

        when(a.getStartTime()).thenReturn(s);
        when(a.getEndTime()).thenReturn(s.plusMinutes(90));

        assertFalse(admin.validateDuration(a));
    }

    @Test
    void validateCapacity_shouldReturnTrueIfWithinLimit() {
        Appointment a = mock(Appointment.class);
        when(a.getParticipantsCount()).thenReturn(3);
        when(a.getMaxParticipants()).thenReturn(5);

        assertTrue(admin.validateCapacity(a));
    }

    @Test
    void validateCapacity_shouldReturnFalseIfExceeded() {
        Appointment a = mock(Appointment.class);
        when(a.getParticipantsCount()).thenReturn(6);
        when(a.getMaxParticipants()).thenReturn(5);

        assertFalse(admin.validateCapacity(a));
    }

    @Test
    void validateType_shouldReturnTrueForValidType() {
        Appointment a = mock(Appointment.class);
        when(a.getType()).thenReturn(Appointment.TYPE_VIRTUAL);

        assertTrue(admin.validateType(a));
    }

    @Test
    void validateType_shouldReturnFalseForInvalidType() {
        Appointment a = mock(Appointment.class);
        when(a.getType()).thenReturn("INVALID");

        assertFalse(admin.validateType(a));
    }
}