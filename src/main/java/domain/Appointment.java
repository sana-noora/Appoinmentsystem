package domain;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;




public class Appointment {
private  String id; 
 private  String userId;
 private  LocalDateTime dateTime; 
 private  int durationMinutes;
 private  String type;
 private  int maxParticipants;
 private Status status;

public Appointment(String id, String userId, LocalDateTime dateTime, 
                      int durationMinutes, String type, int maxParticipants) {

	  if (id == null || id.trim().isEmpty()) {
	            throw new IllegalArgumentException("id must not be null or blank");
	        }
	        if (userId == null || userId.trim().isEmpty()) {
	            throw new IllegalArgumentException("userId must not be null or blank");
	        }
	        if (dateTime == null) {
	            throw new IllegalArgumentException("dateTime must not be null");
	        }
	        if (durationMinutes <= 0) {
	            throw new IllegalArgumentException("durationMinutes must be > 0");
	        }
	        if (type == null || type.trim().isEmpty()) {
	            throw new IllegalArgumentException("type must not be null or blank");
	        }
	        if (maxParticipants <= 0) {
	            throw new IllegalArgumentException("maxParticipants must be > 0");
	        }

	        this.id = id;
	        this.userId = userId;
	        this.dateTime = dateTime;
	        this.durationMinutes = durationMinutes;
	        this.type = type;
	        this.maxParticipants = maxParticipants;
	        this.status = Status.CONFIRMED;

	      

    }


   public String getId() { return id; }
   public String getUserId() {  return userId;}
   public LocalDateTime getDateTime() { return dateTime; }
   public int getDurationMinutes() { return durationMinutes; }
   public String getType() { return type; } 
   public int getMaxParticipants() {return maxParticipants; }
   public Status getStatus() { return status;}
   public void setStatus(Status status) { this.status = status;}
public String toString() {
        return "Appointment{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", dateTime=" + dateTime +
                ", durationMinutes=" + durationMinutes +
                ", type='" + type + '\'' +
                ", maxParticipants=" + maxParticipants +
                ", status=" + status +
                '}';
    }


  public enum Status {
	               CONFIRMED,  
	               CANCELLED     
	           }


}
