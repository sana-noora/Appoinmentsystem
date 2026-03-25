package testAppointment;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import domain.Appointment;
import java.time.OffsetDateTime;

class AppointmentTest {

	  private OffsetDateTime start;
	    private OffsetDateTime end;


	 @BeforeEach
	    void setUp() {
	        start = OffsetDateTime.now();
	        end = start.plusHours(1);
	    }




	  @Test
	    void constructorWithoutId_shouldSetFieldsCorrectly() {

	        Appointment appointment = new Appointment(
	                "URGENT",
	                "PENDING",
	                start,
	                end,
	                2,
	                5,
	                10L,
	                null
	        );

	        assertEquals("URGENT", appointment.getType());
	        assertEquals("PENDING", appointment.getStatus());
	        assertEquals(start, appointment.getStartTime());
	        assertEquals(end, appointment.getEndTime());
	        assertEquals(2, appointment.getParticipantsCount());
	        assertEquals(5, appointment.getMaxParticipants());
	        assertEquals(10L, appointment.getCreatedBy());
	        assertNull(appointment.getSlotId());
	    }

	  @Test
	     void constructorWithId() {

	         OffsetDateTime createdAt = OffsetDateTime.now();
	         OffsetDateTime updatedAt = OffsetDateTime.now();

	         Appointment appointment = new Appointment(
	                 1L,
	                 "GROUP",
	                 "CONFIRMED",
	                 start,
	                 end,
	                 3,
	                 10,
	                 20L,
	                 5L,
	                 createdAt,
	                 updatedAt
	         );

	         assertEquals(1L, appointment.getId());
	         assertEquals("GROUP", appointment.getType());
	         assertEquals("CONFIRMED", appointment.getStatus());
	         assertEquals(3, appointment.getParticipantsCount());
	         assertEquals(10, appointment.getMaxParticipants());
	         assertEquals(20L, appointment.getCreatedBy());
	         assertEquals(5L, appointment.getSlotId());
	         assertEquals(createdAt, appointment.getCreatedAt());
	         assertEquals(updatedAt, appointment.getUpdatedAt());
	     }

	  @Test
	     void settersAndGetters() {

	         Appointment appointment = new Appointment();

	         appointment.setType("FOLLOW_UP");
	         appointment.setStatus("CANCELED");
	         appointment.setStartTime(start);
	         appointment.setEndTime(end);
	         appointment.setParticipantsCount(4);
	         appointment.setMaxParticipants(8);
	         appointment.setSlotId(3L);

	         OffsetDateTime createdAt = OffsetDateTime.now();
	         OffsetDateTime updatedAt = OffsetDateTime.now();

	         appointment.setCreatedAt(createdAt);
	         appointment.setUpdatedAt(updatedAt);

	         assertEquals("FOLLOW_UP", appointment.getType());
	         assertEquals("CANCELED", appointment.getStatus());
	         assertEquals(start, appointment.getStartTime());
	         assertEquals(end, appointment.getEndTime());
	         assertEquals(4, appointment.getParticipantsCount());
	         assertEquals(8, appointment.getMaxParticipants());
	         assertEquals(3L, appointment.getSlotId());
	         assertEquals(createdAt, appointment.getCreatedAt());
	         assertEquals(updatedAt, appointment.getUpdatedAt());
	     }

	  @Test
	     void toStringtest() {

	         Appointment appointment = new Appointment(
	                 "URGENT",
	                 "PENDING",
	                 start,
	                 end,
	                 1,
	                 5,
	                 9L,
	                 null
	         );

	         String result = appointment.toString();

	         assertTrue(result.contains("URGENT"));
	         assertTrue(result.contains("PENDING"));
	         assertTrue(result.contains("participantsCount=1"));
	         assertTrue(result.contains("maxParticipants=5"));
	     }


}
