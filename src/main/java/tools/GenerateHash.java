package tools;

import org.mindrot.jbcrypt.BCrypt;

public class GenerateHash {

    public static void main(String[] args) {

        // Read the password from environment variable instead of hardcoding it
        String plainPassword = System.getenv("PLAIN_PASSWORD");

        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalStateException(
                "Environment variable PLAIN_PASSWORD is not set"
            );
        }

        String hashedPassword = BCrypt.hashpw(
                plainPassword,
                BCrypt.gensalt()
        );

        System.out.println("Hashed: " + hashedPassword);
    }
}
