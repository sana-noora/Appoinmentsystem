package main;

import java.sql.*;
import java.time.OffsetDateTime;


public class Main {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/appointment_system";
        String user = "postgres";
        String password = "123456";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("Connected to PostgreSQL successfully!");

            String sql = "\r\n"
            		+ "  SELECT id,\r\n"
            		+ "                   type,\r\n"
            		+ "                   status,\r\n"
            		+ "                   start_time,\r\n"
            		+ "                   end_time,\r\n"
            		+ "                   participants_count,\r\n"
            		+ "                   max_participants,\r\n"
            		+ "                   created_by,\r\n"
            		+ "                   slot_id\r\n"
            		+ "            FROM appointments\r\n"
            		+ "            ORDER BY id ASC\r\n"
            		+ "";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {

            	   long id = rs.getLong("id");
            	                String type = rs.getString("type");
            	                String status = rs.getString("status");
            	                OffsetDateTime start = rs.getObject("start_time", OffsetDateTime.class);
            	                OffsetDateTime end   = rs.getObject("end_time", OffsetDateTime.class);
            	                int participants = rs.getInt("participants_count");
            	                int maxParticipants = rs.getInt("max_participants");
            	                long createdBy = rs.getLong("created_by");
            	                Long slotId = rs.getObject("slot_id") == null ? null : rs.getLong("slot_id");

            	                System.out.println(
            	                        String.format(
            	                                "id=%d | type=%s | status=%s | start=%s | end=%s | participants=%d/%d | created_by=%d | slot_id=%s",
            	                                id, type, status,
            	                                start, end,
            	                                participants, maxParticipants,
            	                                createdBy,
            	                                (slotId == null ? "NULL" : slotId.toString())
            	                        )
            	                );

            }
        } catch (Exception e) {
            System.out.println("Connection failed!");
            e.printStackTrace();
        }
    }
}
