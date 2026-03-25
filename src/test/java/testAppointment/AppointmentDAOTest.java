package testAppointment;


import domain.Appointment;
import persistence.AppointmentDAO;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
@ExtendWith(MockitoExtension.class)
class AppointmentDAOTest {
	 @Mock
	    private Connection connection;

	    @Mock
	    private PreparedStatement preparedStatement;

	    @Mock
	    private ResultSet resultSet;

	    private AppointmentDAO appointmentDAO;

	    @BeforeEach
	    void setUp() throws Exception {
	        appointmentDAO = new AppointmentDAO(connection);
	    }

	    @Test
	       void addAppointment_shouldExecuteInsert() throws Exception {

	           when(connection.prepareStatement(anyString()))
	                   .thenReturn(preparedStatement);

	           Appointment appointment = new Appointment(
	                   "URGENT",
	                   "PENDING",
	                   OffsetDateTime.now(),
	                   OffsetDateTime.now().plusHours(1),
	                   1,
	                   5,
	                   10L,
	                   null
	           );

	           appointmentDAO.addAppointment(appointment);

	           verify(connection).prepareStatement(anyString());
	           verify(preparedStatement).executeUpdate();
	       }

	    @Test
	    void addAppointment_shouldNormalizeFollowUpType() throws Exception {

	        when(connection.prepareStatement(anyString()))
	                .thenReturn(preparedStatement);

	        Appointment appointment = new Appointment(
	                "follow-up",
	                "pending",
	                OffsetDateTime.now(),
	                OffsetDateTime.now().plusHours(1),
	                1,
	                4,
	                1L,
	                null
	        );

	        appointmentDAO.addAppointment(appointment);

	        verify(preparedStatement).setString(1, "FOLLOW_UP");
	    }
	    @Test
	    void addAppointment_shouldThrowExceptionForInvalidType() {

	        Appointment appointment = new Appointment(
	                "INVALID_TYPE",
	                "PENDING",
	                OffsetDateTime.now(),
	                OffsetDateTime.now(),
	                1,
	                5,
	                1L,
	                null
	        );

	        assertThrows(IllegalArgumentException.class, () ->
	                appointmentDAO.addAppointment(appointment)
	        );
	    }
	    @Test
void getAppointmentById_shouldReturnAppointment() throws Exception {

        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery())
                .thenReturn(resultSet);
        when(resultSet.next())
                .thenReturn(true);

        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getString("type")).thenReturn("URGENT");
        when(resultSet.getString("status")).thenReturn("CONFIRMED");
        when(resultSet.getObject(eq("start_time"), eq(OffsetDateTime.class)))
                .thenReturn(OffsetDateTime.now());
        when(resultSet.getObject(eq("end_time"), eq(OffsetDateTime.class)))
                .thenReturn(OffsetDateTime.now().plusHours(1));
        when(resultSet.getInt("participants_count")).thenReturn(2);
        when(resultSet.getInt("max_participants")).thenReturn(5);
        when(resultSet.getLong("created_by")).thenReturn(100L);
        when(resultSet.getObject(eq("slot_id"), eq(Long.class)))
                .thenReturn(null);
        when(resultSet.getObject(eq("created_at"), eq(OffsetDateTime.class)))
                .thenReturn(OffsetDateTime.now());
        when(resultSet.getObject(eq("updated_at"), eq(OffsetDateTime.class)))
                .thenReturn(OffsetDateTime.now());

        Appointment result = appointmentDAO.getAppointmentById(1L);

        assertNotNull(result);
        assertEquals("URGENT", result.getType());
        assertEquals("CONFIRMED", result.getStatus());
        assertEquals(2, result.getParticipantsCount());
    }

@Test
  void updateStatus_shouldUpdateSuccessfully() throws Exception {

      when(connection.prepareStatement(anyString()))
              .thenReturn(preparedStatement);

      appointmentDAO.updateStatus(5L, "CONFIRMED");

      verify(preparedStatement).executeUpdate();
  }
@Test
void updateStatus_shouldNormalizeLowerCaseStatus() throws Exception {

    when(connection.prepareStatement(anyString()))
            .thenReturn(preparedStatement);

    appointmentDAO.updateStatus(1L, "canceled");

    verify(preparedStatement).setString(1, "CANCELED");
}
@Test
void updateStatus_shouldThrowExceptionForInvalidStatus() {

    assertThrows(IllegalArgumentException.class, () ->
            appointmentDAO.updateStatus(1L, "DONE")
    );
}
@Test
   void deleteAppointment_shouldDeleteRow() throws Exception {

       when(connection.prepareStatement(anyString()))
               .thenReturn(preparedStatement);

       appointmentDAO.deleteAppointment(10L);

       verify(preparedStatement).executeUpdate();
   }

@Test
void getAllAppointments_shouldReturnListOfAppointments() throws Exception {

    when(connection.prepareStatement(anyString()))
            .thenReturn(preparedStatement);
    when(preparedStatement.executeQuery())
            .thenReturn(resultSet);

    // rs.next(): مرتين (row1, row2) وبعدين false
    when(resultSet.next())
            .thenReturn(true, true, false);

    // Mock القيم
    when(resultSet.getLong("id")).thenReturn(1L, 2L);
    when(resultSet.getString("type")).thenReturn("URGENT", "GROUP");
    when(resultSet.getString("status")).thenReturn("PENDING", "CONFIRMED");
    when(resultSet.getObject(eq("start_time"), eq(OffsetDateTime.class)))
            .thenReturn(OffsetDateTime.now());
    when(resultSet.getObject(eq("end_time"), eq(OffsetDateTime.class)))
            .thenReturn(OffsetDateTime.now().plusHours(1));
    when(resultSet.getInt("participants_count")).thenReturn(1, 3);
    when(resultSet.getInt("max_participants")).thenReturn(5, 10);
    when(resultSet.getLong("created_by")).thenReturn(10L);
    when(resultSet.getObject(eq("slot_id"), eq(Long.class)))
            .thenReturn(null);
    when(resultSet.getObject(eq("created_at"), eq(OffsetDateTime.class)))
            .thenReturn(OffsetDateTime.now());
    when(resultSet.getObject(eq("updated_at"), eq(OffsetDateTime.class)))
            .thenReturn(OffsetDateTime.now());

    List<Appointment> result = appointmentDAO.getAllAppointments();

    assertEquals(2, result.size());
    assertEquals("URGENT", result.get(0).getType());
    assertEquals("GROUP", result.get(1).getType());
}
@Test
void updateStatus_shouldExecuteUpdate() throws Exception {

    when(connection.prepareStatement(anyString()))
            .thenReturn(preparedStatement);

    appointmentDAO.updateStatus(5L, "confirmed");

    verify(preparedStatement).setString(eq(1), eq("CONFIRMED"));
    verify(preparedStatement).executeUpdate();
}
@Test
void updateParticipants_shouldUpdateCount() throws Exception {

    when(connection.prepareStatement(anyString()))
            .thenReturn(preparedStatement);

    appointmentDAO.updateParticipants(3L, 7);

    verify(preparedStatement).setInt(1, 7);
    verify(preparedStatement).executeUpdate();
}

}
