package domain;


import java.time.OffsetDateTime;

public class TimeSlot {
    private long id;
    private long scheduleId;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private boolean available;

public TimeSlot() {
    }

    public TimeSlot(long id, long scheduleId, OffsetDateTime startTime, OffsetDateTime endTime, boolean available) {
        this.setId(id);
        this.scheduleId = scheduleId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.available = available;
    }

    public TimeSlot(long scheduleId, OffsetDateTime startTime, OffsetDateTime endTime, boolean available) {
        this.scheduleId = scheduleId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.available = available;
    }

	public long getId() {return id;}
	public void setId(long id) {this.id = id;}
    public long getScheduleId() {return scheduleId;}
    public void setScheduleId(long scheduleId) { this.scheduleId = scheduleId;}
    public OffsetDateTime getStartTime() {return startTime;}
    public void setStartTime(OffsetDateTime startTime) { this.startTime = startTime;}
    public OffsetDateTime getEndTime() { return endTime;}
    public void setEndTime(OffsetDateTime endTime) {  this.endTime = endTime;}
    public boolean isAvailable() {return available;}
    public void setAvailable(boolean available) { this.available = available;}
         

    @Override
       public String toString() {
           return "TimeSlot{" +
                   "id=" + id +
                   ", scheduleId=" + scheduleId +
                   ", startTime=" + startTime +
                   ", endTime=" + endTime +
                   ", available=" + available +
                   '}';
       }

	

    
}
