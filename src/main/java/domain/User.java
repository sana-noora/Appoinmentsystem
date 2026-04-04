package domain;

public class User {

    public enum Role { ADMIN, PATIENT }

    private String id;
    private String name;
    private String email;
    private String phoneNumber;
    private String username;
    private Role role;
    private boolean loggedIn;

    public User(String id, String name, String email, String phoneNumber,
                String username, Role role) {
        this.id = id; this.name = name; this.email = email;
        this.phoneNumber = phoneNumber; this.username = username;
        this.role = role; this.loggedIn = false;
    }

    public void logout()      { this.loggedIn = false; }
    public boolean isLoggedIn() { return loggedIn; }
    public void markLoggedIn()  { this.loggedIn = true; }

    public String getId()          { return id; }
    public String getName()        { return name; }
    public String getEmail()       { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getUsername()    { return username; }
    public Role   getRole()        { return role; }
}