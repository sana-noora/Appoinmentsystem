package testUser;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import domain.User;
import persistence.UserDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserDAOTest {

    // =====================================================
    // Constants - لتجنب hardcoded values
    // =====================================================
    private static final String TEST_USERNAME     = "aliuser";
    private static final String TEST_RAW_PASS     =
            System.getenv().getOrDefault("TEST_RAW_PASS", "t3stP@ssw0rd!");
    private static final String TEST_CORRECT_PASS =
            System.getenv().getOrDefault("TEST_CORRECT_PASS", "c0rr3ctP@ss!");
    private static final String TEST_WRONG_PASS   =
            System.getenv().getOrDefault("TEST_WRONG_PASS", "wr0ngP@ss!");

    @Mock private Connection        connection;
    @Mock private PreparedStatement preparedStatement;
    @Mock private ResultSet         resultSet;

    private UserDAO userDAO;

    @BeforeEach
    void setUp() {
        userDAO = new UserDAO(connection);
    }

    // =====================================================
    // addUser
    // =====================================================

    @Test
    void addUser_shouldExecuteInsert() throws Exception {
        when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);

        User user = new User(
                "1",
                "Ali",
                "ali@test.com",
                "123456",
                TEST_USERNAME,
                User.Role.VISITOR
        );

        userDAO.addUser(user, TEST_RAW_PASS);

        verify(preparedStatement).executeUpdate();
        verify(preparedStatement).setString(eq(6), eq("VISITOR"));
    }

    // =====================================================
    // login
    // =====================================================

    @Test
    void login_shouldReturnUserWhenCredentialsValid() throws Exception {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        String hash = BCrypt.hashpw(TEST_RAW_PASS, BCrypt.gensalt());

        when(resultSet.getString("password_hash")).thenReturn(hash);
        when(resultSet.getInt("id")).thenReturn(1);
        when(resultSet.getString("name")).thenReturn("Ali");
        when(resultSet.getString("email")).thenReturn("ali@test.com");
        when(resultSet.getString("phone_number")).thenReturn("123456");
        when(resultSet.getString("username")).thenReturn(TEST_USERNAME);
        when(resultSet.getString("role")).thenReturn("VISITOR");

        Optional<User> result = userDAO.login(TEST_USERNAME, TEST_RAW_PASS);

        assertTrue(result.isPresent());
        assertEquals(TEST_USERNAME, result.get().getUsername());
        assertTrue(result.get().isLoggedIn());
    }

    @Test
    void login_shouldReturnEmptyWhenPasswordInvalid() throws Exception {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        String hash = BCrypt.hashpw(TEST_CORRECT_PASS, BCrypt.gensalt());
        when(resultSet.getString("password_hash")).thenReturn(hash);

        Optional<User> result = userDAO.login(TEST_USERNAME, TEST_WRONG_PASS);

        assertFalse(result.isPresent());
    }

    @Test
    void login_shouldReturnEmptyWhenUserNotFound() throws Exception {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Optional<User> result = userDAO.login("unknown", TEST_WRONG_PASS);

        assertFalse(result.isPresent());
    }

    // =====================================================
    // getUserById
    // =====================================================

    @Test
    void getUserById_shouldReturnUserWhenExists() throws Exception {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        when(resultSet.getInt("id")).thenReturn(1);
        when(resultSet.getString("name")).thenReturn("Ali");
        when(resultSet.getString("email")).thenReturn("ali@test.com");
        when(resultSet.getString("phone_number")).thenReturn("123456");
        when(resultSet.getString("username")).thenReturn(TEST_USERNAME);
        when(resultSet.getString("role")).thenReturn("VISITOR");

        Optional<User> result = userDAO.getUserById("1");

        assertTrue(result.isPresent());
        assertEquals("1", result.get().getId());
        assertEquals("Ali", result.get().getName());
    }

    @Test
    void getUserById_shouldReturnEmptyWhenNotFound() throws Exception {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Optional<User> result = userDAO.getUserById("99");

        assertFalse(result.isPresent());
    }

    // =====================================================
    // getAllUsers
    // =====================================================

    @Test
    void getAllUsers_shouldReturnListOfUsers() throws Exception {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getInt("id")).thenReturn(1, 2);
        when(resultSet.getString("name")).thenReturn("Ali", "Noora");
        when(resultSet.getString("email")).thenReturn("ali@test.com", "admin@test.com");
        when(resultSet.getString("phone_number")).thenReturn("123", "456");
        when(resultSet.getString("username"))
                .thenReturn(TEST_USERNAME, "adminuser");
        when(resultSet.getString("role")).thenReturn("VISITOR", "ADMIN");

        List<User> users = userDAO.getAllUsers();

        assertEquals(2, users.size());
        assertEquals("Ali", users.get(0).getName());
        assertEquals(User.Role.VISITOR, users.get(0).getRole());
        assertEquals("Noora", users.get(1).getName());
        assertEquals(User.Role.ADMIN, users.get(1).getRole());
    }
}
