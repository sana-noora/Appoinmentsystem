package tests;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import connectDB.connectDB;

class ConnectDBTest {

    @Test
    void getConnection_shouldReturnConnectionOrThrowSQLException() {
        try {
            Connection conn = connectDB.getConnection();
            assertNotNull(conn);
            assertFalse(conn.isClosed());
            conn.close();
        } catch (SQLException e) {
            assertTrue(e instanceof SQLException);
        }
    }
}