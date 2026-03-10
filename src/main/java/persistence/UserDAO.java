package persistence;

import domain.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;


public class UserDAO {
    private final Connection connection;

    public UserDAO(Connection connection) {
        this.connection = connection;
    }

   
    public void addUser(User user) throws SQLException {
        String sql = "INSERT INTO users (id, name, email, phone_number, username, password_hash, role) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getId());
            stmt.setString(2, user.getName());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getPhoneNumber());
            stmt.setString(5, user.getUsername());
            stmt.setString(6, BCrypt.hashpw(user.getUsername(), BCrypt.gensalt())); // أو من الـ User مباشرة
            stmt.setString(7, user.getRole().name());
            stmt.executeUpdate();
        }
    }

    
    public User login(String username, String plainPassword) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String hashedPassword = rs.getString("password_hash");
                    if (BCrypt.checkpw(plainPassword, hashedPassword)) {
                        return new User(
                                rs.getString("id"),
                                rs.getString("name"),
                                rs.getString("email"),
                                rs.getString("phone_number"),
                                rs.getString("username"),
                                plainPassword,
                                User.Role.valueOf(rs.getString("role"))
                        );
                    }
                }
            }
        }
        return null; 
    }

    
    public User getUserById(String id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getString("id"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getString("phone_number"),
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            User.Role.valueOf(rs.getString("role"))
                    );
                }
            }
        }
        return null;
    }
}
