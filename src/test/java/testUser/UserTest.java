package testUser;

import static org.junit.jupiter.api.Assertions.*;

import domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class UserTest {

    private User visitor;
    private User admin;

    @BeforeEach
    void setUp() {
        visitor = new User(
                "1",
                "Ali",
                "ali@test.com",
                "123456",
                "aliuser",
                User.Role.VISITOR
        );

        admin = new User(
                "2",
                "Noora",
                "admin@test.com",
                "0599999999",
                "adminuser",
                User.Role.ADMIN
        );
    }

    @Test
    void constructor_shouldSetAllFields_forVisitor() {
        assertEquals("1", visitor.getId());
        assertEquals("Ali", visitor.getName());
        assertEquals("ali@test.com", visitor.getEmail());
        assertEquals("123456", visitor.getPhoneNumber());
        assertEquals("aliuser", visitor.getUsername());
        assertEquals(User.Role.VISITOR, visitor.getRole());
        assertFalse(visitor.isLoggedIn());
    }

    @Test
    void constructor_shouldSetAllFields_forAdmin() {
        assertEquals("2", admin.getId());
        assertEquals("Noora", admin.getName());
        assertEquals("admin@test.com", admin.getEmail());
        assertEquals("0599999999", admin.getPhoneNumber());
        assertEquals("adminuser", admin.getUsername());
        assertEquals(User.Role.ADMIN, admin.getRole());
        assertFalse(admin.isLoggedIn());
    }

    @Test
    void markLoggedIn_shouldSetLoggedInTrue_forVisitorAndAdmin() {
        visitor.markLoggedIn();
        admin.markLoggedIn();

        assertTrue(visitor.isLoggedIn());
        assertTrue(admin.isLoggedIn());
    }

    @Test
    void logout_shouldSetLoggedInFalse_forVisitorAndAdmin() {
        visitor.markLoggedIn();
        admin.markLoggedIn();

        visitor.logout();
        admin.logout();

        assertFalse(visitor.isLoggedIn());
        assertFalse(admin.isLoggedIn());
    }

    @Test
    void roles_shouldBeDifferentBetweenAdminAndVisitor() {
        assertEquals(User.Role.VISITOR, visitor.getRole());
        assertEquals(User.Role.ADMIN, admin.getRole());
        assertNotEquals(visitor.getRole(), admin.getRole());
    }
    @Test
    void valueOf_shouldThrowExceptionWhenRoleIsLowercase() {
        assertThrows(IllegalArgumentException.class, () -> {
            User.Role.valueOf("visitor");
        });
    }
    
}