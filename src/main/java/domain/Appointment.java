package domain;

import java.time.OffsetDateTime;

public class Appointment {

    private long id;
    private String type;
    private String status;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private int participantsCount;
    private int maxParticipants;
    private long createdBy;
    private Long slotId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Appointment() {
    }

    public Appointment(long id,
                       String type,
                       String status,
                       OffsetDateTime startTime,
                       OffsetDateTime endTime,
                       int participantsCount,
                       int maxParticipants,
                       long createdBy,
                       Long slotId,
                       OffsetDateTime createdAt,
                       OffsetDateTime updatedAt) {
        this.id = id;
        this.type = type;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.participantsCount = participantsCount;
        this.maxParticipants = maxParticipants;
        this.createdBy = createdBy;
        this.slotId = slotId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Appointment(String type,
                       String status,
                       OffsetDateTime startTime,
                       OffsetDateTime endTime,
                       int participantsCount,
                       int maxParticipants,
                       long createdBy,
                       Long slotId) {
        this.type = type;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.participantsCount = participantsCount;
        this.maxParticipants = maxParticipants;
        this.createdBy = createdBy;
        this.slotId = slotId;
    }

    public long getId() { return id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public OffsetDateTime getStartTime() { return startTime; }
    public void setStartTime(OffsetDateTime startTime) { this.startTime = startTime; }
    public OffsetDateTime getEndTime() { return endTime; }
    public void setEndTime(OffsetDateTime endTime) { this.endTime = endTime; }
    public int getParticipantsCount() { return participantsCount; }
    public void setParticipantsCount(int participantsCount) { this.participantsCount = participantsCount; }
    public int getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(int maxParticipants) { this.maxParticipants = maxParticipants; }
    public long getCreatedBy() { return createdBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getSlotId() { return slotId; }
    public void setSlotId(Long slotId) { this.slotId = slotId; }

    @Override
    public String toString() {
        return "Appointment{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", status='" + status + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", participantsCount=" + participantsCount +
                ", maxParticipants=" + maxParticipants +
                ", createdBy=" + createdBy +
                ", slotId=" + slotId +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}