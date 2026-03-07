package connectDB;
import java.sql.*;
public class connectDB {
	 public static void main(String[] args) {

	        String url = "jdbc:postgresql://localhost:5432/appointment_system"; 
	        String user = "postgres"; 
	        String password = "123456";

	        try {
	            Connection conn = DriverManager.getConnection(url, user, password);
	            System.out.println("Connected to PostgreSQL database!");
	        } catch (SQLException e) {
	            System.out.println("Connection failed!");
	            e.printStackTrace();
	        }
	    }
	

}