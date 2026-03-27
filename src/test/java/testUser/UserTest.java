package testUser;

import static org.junit.jupiter.api.Assertions.*;

import domain.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User(
                "1",
                "Ali",
                "ali@test.com",
                "123456",
                "aliuser",
                User.Role.PATIENT
        );
    }

    @Test
    void constructor_shouldSetAllFields() {
        assertEquals("1", user.getId());
        assertEquals("Ali", user.getName());
        assertEquals("ali@test.com", user.getEmail());
        assertEquals("123456", user.getPhoneNumber());
        assertEquals("aliuser", user.getUsername());
        assertEquals(User.Role.PATIENT, user.getRole());
        assertFalse(user.isLoggedIn());
    }

    @Test
    void markLoggedIn_shouldSetLoggedInTrue() {
        user.markLoggedIn();
        assertTrue(user.isLoggedIn());
    }

    @Test
    void logout_shouldSetLoggedInFalse() {
        user.markLoggedIn();
        user.logout();
        assertFalse(user.isLoggedIn());
    }

    @Test
    void role_shouldBeCorrect() {
        assertEquals(User.Role.PATIENT, user.getRole());
    }
}
