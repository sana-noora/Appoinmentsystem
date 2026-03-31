package testtimeslot;

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

    @Test
    void defaultConstructor_shouldCreateEmptyObject() {
        TimeSlot timeSlot = new TimeSlot();

        assertNotNull(timeSlot);
        assertEquals(0L, timeSlot.getId());
        assertEquals(0L, timeSlot.getScheduleId());
        assertNull(timeSlot.getStartTime());
        assertNull(timeSlot.getEndTime());
        assertFalse(timeSlot.isAvailable());
    }

    @Test
    void constructorWithId_shouldSetAllFields() {
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
    void toString_shouldContainImportantFields() {
        TimeSlot timeSlot = new TimeSlot(
                3L,
                8L,
                startTime,
                endTime,
                true
        );

        String result = timeSlot.toString();

        assertTrue(result.contains("id=3"));
        assertTrue(result.contains("scheduleId=8"));
        assertTrue(result.contains("available=true"));
    }
}