package testschedule;

import static org.junit.jupiter.api.Assertions.*;

import domain.Schedule;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScheduleTest {

    private LocalDate workDate;
    private OffsetDateTime createdAt;

    @BeforeEach
    void setUp() {
        workDate = LocalDate.of(2026, 3, 25);
        createdAt = OffsetDateTime.now();
    }

    @Test
    void defaultConstructor_shouldCreateEmptyObject() {
        Schedule schedule = new Schedule();

        assertNotNull(schedule);
        assertEquals(0L, schedule.getId());
        assertNull(schedule.getWorkDate());
        assertNull(schedule.getCreatedAt());
    }

    @Test
    void constructorWithId_shouldSetAllFields() {
        Schedule schedule = new Schedule(1L, workDate, createdAt);

        assertEquals(1L, schedule.getId());
        assertEquals(workDate, schedule.getWorkDate());
        assertEquals(createdAt, schedule.getCreatedAt());
    }

    @Test
    void constructorWithWorkDate_shouldSetWorkDateOnly() {
        Schedule schedule = new Schedule(workDate);

        assertEquals(workDate, schedule.getWorkDate());
        assertEquals(0L, schedule.getId());
        assertNull(schedule.getCreatedAt());
    }

    @Test
    void settersAndGetters_shouldWorkCorrectly() {
        Schedule schedule = new Schedule();

        schedule.setId(5L);
        schedule.setWorkDate(workDate);

        assertEquals(5L, schedule.getId());
        assertEquals(workDate, schedule.getWorkDate());
    }

    @Test
    void toString_shouldContainImportantFields() {
        Schedule schedule = new Schedule(2L, workDate, createdAt);

        String result = schedule.toString();

        assertTrue(result.contains("id=2"));
        assertTrue(result.contains(workDate.toString()));
        assertTrue(result.contains("createdAt"));
    }
}