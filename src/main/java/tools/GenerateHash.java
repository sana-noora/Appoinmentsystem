package tools;
import org.mindrot.jbcrypt.BCrypt;

public class GenerateHash {
    public static void main(String[] args) {
        String plainPassword = "noora123"; // غيّريها حسب المستخدم
        String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
       System.out.println("Hashed: " + hashedPassword);
    }
}
