package domain;
import org.mindrot.jbcrypt.BCrypt;

public class User {
    private String id;
    private String name;
    private String email;
    private String phoneNumber;
    private String username;
    private String passwordHash; 
    private Role role;
    private boolean loggedIn;

    public User(String id, String name, String email, String phoneNumber,
                String username, String plainPassword, Role role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.username = username;
        this.passwordHash = hashPassword(plainPassword); 
        this.role = role;
        this.loggedIn = false;
    }

    private String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    public boolean login(String username, String plainPassword) {
        if (this.username.equals(username) && BCrypt.checkpw(plainPassword, this.passwordHash)) {
            this.loggedIn = true;
            return true;
        }
        return false;
    }

    public void logout() {
        this.loggedIn = false;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getUsername() { return username; }
    public Role getRole() { return role; }

    public enum Role {
        ADMIN, PATIENT
    }
}
