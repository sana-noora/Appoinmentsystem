package testUser;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import domain.User;
import persistence.UserDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserDAOTest {

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    private UserDAO userDAO;

    @BeforeEach
    void setUp() {
        userDAO = new UserDAO(connection);
    }

    @Test
    void addUser_shouldExecuteInsert() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);

        User user = new User(
                "1",
                "Ali",
                "ali@test.com",
                "123456",
                "aliuser",
                User.Role.PATIENT
        );

        userDAO.addUser(user, "password123");

        verify(preparedStatement).executeUpdate();
    }

    @Test
    void login_shouldReturnUserWhenCredentialsAreValid() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery())
                .thenReturn(resultSet);
        when(resultSet.next())
                .thenReturn(true);

        String rawPassword = "secret";
        String hash = BCrypt.hashpw(rawPassword, BCrypt.gensalt());

        when(resultSet.getString("password_hash")).thenReturn(hash);
        when(resultSet.getString("id")).thenReturn("1");
        when(resultSet.getString("name")).thenReturn("Ali");
        when(resultSet.getString("email")).thenReturn("ali@test.com");
        when(resultSet.getString("phone_number")).thenReturn("123456");
        when(resultSet.getString("username")).thenReturn("aliuser");
        when(resultSet.getString("role")).thenReturn("PATIENT");

        Optional<User> result = userDAO.login("aliuser", rawPassword);

        assertTrue(result.isPresent());
        assertEquals("aliuser", result.get().getUsername());
    }

    @Test
    void login_shouldReturnEmptyWhenPasswordIsInvalid() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery())
                .thenReturn(resultSet);
        when(resultSet.next())
                .thenReturn(true);

        String hash = BCrypt.hashpw("correct", BCrypt.gensalt());
        when(resultSet.getString("password_hash")).thenReturn(hash);

        Optional<User> result = userDAO.login("aliuser", "wrong");

        assertFalse(result.isPresent());
    }

    @Test
    void login_shouldReturnEmptyWhenUserNotFound() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery())
                .thenReturn(resultSet);
        when(resultSet.next())
                .thenReturn(false);

        Optional<User> result = userDAO.login("unknown", "pass");

        assertFalse(result.isPresent());
    }

    @Test
    void getUserById_shouldReturnUser() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery())
                .thenReturn(resultSet);
        when(resultSet.next())
                .thenReturn(true);

        when(resultSet.getString("id")).thenReturn("1");
        when(resultSet.getString("name")).thenReturn("Ali");
        when(resultSet.getString("email")).thenReturn("ali@test.com");
        when(resultSet.getString("phone_number")).thenReturn("123456");
        when(resultSet.getString("username")).thenReturn("aliuser");
        when(resultSet.getString("role")).thenReturn("PATIENT");

        Optional<User> result = userDAO.getUserById("1");

        assertTrue(result.isPresent());
        assertEquals("Ali", result.get().getName());
    }

    @Test
    void getUserById_shouldReturnEmptyWhenNotFound() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery())
                .thenReturn(resultSet);
        when(resultSet.next())
                .thenReturn(false);

        Optional<User> result = userDAO.getUserById("99");

        assertFalse(result.isPresent());
    }
}
