package connectDB;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class connectDB {

    private static final String URL  =
            "jdbc:postgresql://localhost:5432/appointment_system";

    private static final String USER =
            System.getenv("DB_USER");

    private static final String PASS =
            System.getenv("DB_PASS");

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}

