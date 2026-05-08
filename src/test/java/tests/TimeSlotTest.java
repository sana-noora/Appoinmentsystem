package tests;

import static org.junit.jupiter.api.Assertions.*;

import domain.TimeSlot;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TimeSlotTest {

    private OffsetDateTime startTime;
    private OffsetDateTime endTime;

    @BeforeEach
    void setUp() {
        startTime = OffsetDateTime.now();
        endTime = startTime.plusHours(1);
    }

    // =====================================================
    // Constructors
    // =====================================================

    @Test
    void defaultConstructor_shouldCreateObjectWithDefaultValues() {
        TimeSlot timeSlot = new TimeSlot();

        assertNotNull(timeSlot);
        assertEquals(0L, timeSlot.getId());
        assertEquals(0L, timeSlot.getScheduleId());
        assertNull(timeSlot.getStartTime());
        assertNull(timeSlot.getEndTime());
        assertFalse(timeSlot.isAvailable());
    }

    @Test
    void constructorWithId_shouldSetAllFieldsCorrectly() {
        TimeSlot timeSlot = new TimeSlot(
                1L,
                10L,
                startTime,
                endTime,
                true
        );

        assertEquals(1L, timeSlot.getId());
        assertEquals(10L, timeSlot.getScheduleId());
        assertEquals(startTime, timeSlot.getStartTime());
        assertEquals(endTime, timeSlot.getEndTime());
        assertTrue(timeSlot.isAvailable());
    }

    @Test
    void constructorWithoutId_shouldSetFieldsExceptId() {
        TimeSlot timeSlot = new TimeSlot(
                15L,
                startTime,
                endTime,
                false
        );

        assertEquals(0L, timeSlot.getId());
        assertEquals(15L, timeSlot.getScheduleId());
        assertEquals(startTime, timeSlot.getStartTime());
        assertEquals(endTime, timeSlot.getEndTime());
        assertFalse(timeSlot.isAvailable());
    }

    // =====================================================
    // Getters & Setters
    // =====================================================

    @Test
    void settersAndGetters_shouldWorkCorrectly() {
        TimeSlot timeSlot = new TimeSlot();

        timeSlot.setId(7L);
        timeSlot.setScheduleId(20L);
        timeSlot.setStartTime(startTime);
        timeSlot.setEndTime(endTime);
        timeSlot.setAvailable(true);

        assertEquals(7L, timeSlot.getId());
        assertEquals(20L, timeSlot.getScheduleId());
        assertEquals(startTime, timeSlot.getStartTime());
        assertEquals(endTime, timeSlot.getEndTime());
        assertTrue(timeSlot.isAvailable());
    }

    @Test
    void setAvailable_shouldToggleAvailability() {
        TimeSlot timeSlot = new TimeSlot();
        assertFalse(timeSlot.isAvailable());

        timeSlot.setAvailable(true);
        assertTrue(timeSlot.isAvailable());

        timeSlot.setAvailable(false);
        assertFalse(timeSlot.isAvailable());
    }

    // =====================================================
    // toString
    // =====================================================

    @Test
    void toString_shouldContainIdTimesAndAvailability() {
        TimeSlot timeSlot = new TimeSlot(
                3L,
                8L,
                startTime,
                endTime,
                true
        );

        String result = timeSlot.toString();

        assertNotNull(result);
        assertTrue(result.contains("id=3"));
        assertTrue(result.contains(startTime.toString()));
        assertTrue(result.contains(endTime.toString()));
        assertTrue(result.contains("available=true"));
    }
}