package appointment;

import static org.junit.jupiter.api.Assertions.*;

import domain.Appointment;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AppointmentTest {

    private OffsetDateTime start;
    private OffsetDateTime end;

    @BeforeEach
    void setUp() {
        start = OffsetDateTime.now();
        end = start.plusMinutes(30);
    }

    // =====================================================
    // Constructors
    // =====================================================

    @Test
    void defaultConstructor_shouldCreateEmptyObject() {
        Appointment a = new Appointment();

        assertNotNull(a);
        assertEquals(0L, a.getId());
        assertNull(a.getType());
        assertNull(a.getStatus());
        assertNull(a.getStartTime());
        assertNull(a.getEndTime());
        assertEquals(0, a.getParticipantsCount());
        assertEquals(0, a.getMaxParticipants());
        assertEquals(0, a.getCreatedBy());
        assertNull(a.getSlotId());
        assertNull(a.getAdminNote());
        assertFalse(a.isCanceledByAdmin());
        assertNull(a.getCreatedAt());
        assertNull(a.getUpdatedAt());
    }

    @Test
    void fullConstructor_shouldSetAllFields() {
        Appointment a = new Appointment(
                5L,
                Appointment.TYPE_FIRST_VISIT,
                Appointment.STATUS_CONFIRMED,
                start,
                end,
                1,
                1,
                10L,
                3L,
                "note",
                start.minusDays(1),
                end.plusDays(1)
        );

        assertEquals(5L, a.getId());
        assertEquals(Appointment.TYPE_FIRST_VISIT, a.getType());
        assertEquals(Appointment.STATUS_CONFIRMED, a.getStatus());
        assertEquals(start, a.getStartTime());
        assertEquals(end, a.getEndTime());
        assertEquals(1, a.getParticipantsCount());
        assertEquals(1, a.getMaxParticipants());
        assertEquals(10L, a.getCreatedBy());
        assertEquals(3L, a.getSlotId());
        assertEquals("note", a.getAdminNote());
        assertEquals(start.minusDays(1), a.getCreatedAt());
        assertEquals(end.plusDays(1), a.getUpdatedAt());
    }

    @Test
    void constructorWithoutId_shouldSetCoreFields() {
        Appointment a = new Appointment(
                Appointment.TYPE_VIRTUAL,
                Appointment.STATUS_PENDING,
                start,
                end,
                1,
                1,
                7L,
                null
        );

        assertEquals(Appointment.TYPE_VIRTUAL, a.getType());
        assertEquals(Appointment.STATUS_PENDING, a.getStatus());
        assertEquals(start, a.getStartTime());
        assertEquals(end, a.getEndTime());
        assertEquals(1, a.getParticipantsCount());
        assertEquals(1, a.getMaxParticipants());
        assertEquals(7L, a.getCreatedBy());
        assertNull(a.getSlotId());
    }

    // =====================================================
    // Getters & Setters
    // =====================================================

    @Test
    void settersAndGetters_shouldWorkCorrectly() {
        Appointment a = new Appointment();

        a.setType(Appointment.TYPE_GROUP_FIRST_VISIT);
        a.setStatus(Appointment.STATUS_CONFIRMED);
        a.setStartTime(start);
        a.setEndTime(end);
        a.setParticipantsCount(4);
        a.setMaxParticipants(5);
        a.setSlotId(12L);
        a.setAdminNote("admin note");
        a.setCanceledByAdmin(true);

        assertEquals(Appointment.TYPE_GROUP_FIRST_VISIT, a.getType());
        assertEquals(Appointment.STATUS_CONFIRMED, a.getStatus());
        assertEquals(start, a.getStartTime());
        assertEquals(end, a.getEndTime());
        assertEquals(4, a.getParticipantsCount());
        assertEquals(5, a.getMaxParticipants());
        assertEquals(12L, a.getSlotId());
        assertEquals("admin note", a.getAdminNote());
        assertTrue(a.isCanceledByAdmin());
    }

    // =====================================================
    // isGroup
    // =====================================================

    @Test
    void isGroup_shouldReturnTrueForGroupTypes() {
        Appointment a = new Appointment();
        a.setType(Appointment.TYPE_GROUP_VIRTUAL);

        assertTrue(a.isGroup());
    }

    @Test
    void isGroup_shouldReturnFalseForIndividualTypes() {
        Appointment a = new Appointment();
        a.setType(Appointment.TYPE_FIRST_VISIT);

        assertFalse(a.isGroup());
    }

    @Test
    void isGroup_shouldReturnFalseWhenTypeIsNull() {
        Appointment a = new Appointment();

        assertFalse(a.isGroup());
    }

    // =====================================================
    // toString
    // =====================================================

    @Test
    void toString_shouldContainImportantFields() {
        Appointment a = new Appointment(
                9L,
                Appointment.TYPE_FOLLOW_UP,
                Appointment.STATUS_CONFIRMED,
                start,
                end,
                1,
                1,
                5L,
                null,
                null,
                null,
                null
        );

        String result = a.toString();

        assertNotNull(result);
        assertTrue(result.contains("id=9"));
        assertTrue(result.contains("type=" + Appointment.TYPE_FOLLOW_UP));
        assertTrue(result.contains("status=" + Appointment.STATUS_CONFIRMED));
        assertTrue(result.contains(start.toString()));
    }
}