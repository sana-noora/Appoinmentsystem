package persistence;

import domain.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.Optional;

public class UserDAO {
    private final Connection connection;

    public UserDAO(Connection connection) {
        this.connection = connection;
    }

    public void addUser(User user, String rawPassword) throws SQLException {
        String sql = "INSERT INTO users (id, name, email, phone_number, username, password_hash, role) VALUES (?, ?, ?, ?, ?, ?, ?)";

        String passwordHash = BCrypt.hashpw(rawPassword, BCrypt.gensalt());

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getId());
            stmt.setString(2, user.getName());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getPhoneNumber());
            stmt.setString(5, user.getUsername());
            stmt.setString(6, passwordHash);
            stmt.setString(7, user.getRole().name());
            stmt.executeUpdate();
        }
    }

    public Optional<User> login(String username, String plainPassword) throws SQLException {
        String sql = "SELECT id, name, email, phone_number, username, password_hash, role FROM users WHERE username = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return Optional.empty();

                String hashed = rs.getString("password_hash");
                if (!BCrypt.checkpw(plainPassword, hashed)) return Optional.empty();

                User user = new User(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("phone_number"),
                        rs.getString("username"),
                        User.Role.valueOf(rs.getString("role"))
                );
                user.markLoggedIn();
                return Optional.of(user);
            }
        }
    }

    public Optional<User> getUserById(String id) throws SQLException {
        String sql = "SELECT id, name, email, phone_number, username, role FROM users WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return Optional.empty();

                User user = new User(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("phone_number"),
                        rs.getString("username"),
                        User.Role.valueOf(rs.getString("role"))
                );
                return Optional.of(user);
            }
        }
    }
}
