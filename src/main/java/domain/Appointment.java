package domain;

import java.time.OffsetDateTime;

public class Appointment {

    public static final String TYPE_FIRST_VISIT       = "FIRST_VISIT";
    public static final String TYPE_FOLLOW_UP         = "FOLLOW_UP";
    public static final String TYPE_VIRTUAL           = "VIRTUAL";
    public static final String TYPE_GROUP_FIRST_VISIT = "GROUP_FIRST_VISIT";
    public static final String TYPE_GROUP_FOLLOW_UP   = "GROUP_FOLLOW_UP";
    public static final String TYPE_GROUP_VIRTUAL     = "GROUP_VIRTUAL";

    public static final String STATUS_PENDING   = "PENDING";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_CANCELED  = "CANCELED";
    public static final String STATUS_DONE      = "DONE";

    private long id;
    private String type;
    private String status;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private int participantsCount;
    private int maxParticipants;
    private long createdBy;
    private Long slotId;
    private String adminNote;
    private boolean canceledByAdmin;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Appointment() {}

    public Appointment(long id, String type, String status,
                       OffsetDateTime startTime, OffsetDateTime endTime,
                       int participantsCount, int maxParticipants,
                       long createdBy, Long slotId, String adminNote,
                       OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id; this.type = type; this.status = status;
        this.startTime = startTime; this.endTime = endTime;
        this.participantsCount = participantsCount;
        this.maxParticipants = maxParticipants;
        this.createdBy = createdBy; this.slotId = slotId;
        this.adminNote = adminNote;
        this.createdAt = createdAt; this.updatedAt = updatedAt;
    }

    public Appointment(String type, String status,
                       OffsetDateTime startTime, OffsetDateTime endTime,
                       int participantsCount, int maxParticipants,
                       long createdBy, Long slotId) {
        this.type = type; this.status = status;
        this.startTime = startTime; this.endTime = endTime;
        this.participantsCount = participantsCount;
        this.maxParticipants = maxParticipants;
        this.createdBy = createdBy; this.slotId = slotId;
    }

    public long getId()                        { return id; }
    public String getType()                    { return type; }
    public void setType(String t)              { this.type = t; }
    public String getStatus()                  { return status; }
    public void setStatus(String s)            { this.status = s; }
    public OffsetDateTime getStartTime()       { return startTime; }
    public void setStartTime(OffsetDateTime t) { this.startTime = t; }
    public OffsetDateTime getEndTime()         { return endTime; }
    public void setEndTime(OffsetDateTime t)   { this.endTime = t; }
    public int getParticipantsCount()          { return participantsCount; }
    public void setParticipantsCount(int c)    { this.participantsCount = c; }
    public int getMaxParticipants()            { return maxParticipants; }
    public void setMaxParticipants(int m)      { this.maxParticipants = m; }
    public long getCreatedBy()                 { return createdBy; }
    public Long getSlotId()                    { return slotId; }
    public void setSlotId(Long s)              { this.slotId = s; }
    public String getAdminNote()               { return adminNote; }
    public void setAdminNote(String n)         { this.adminNote = n; }
    public boolean isCanceledByAdmin()         { return canceledByAdmin; }
    public void setCanceledByAdmin(boolean b)  { this.canceledByAdmin = b; }
    public OffsetDateTime getCreatedAt()       { return createdAt; }
    public OffsetDateTime getUpdatedAt()       { return updatedAt; }

    public boolean isGroup() {
        return type != null && type.startsWith("GROUP_");
    }

    @Override
    public String toString() {
        return "Appointment{id=" + id + ", type=" + type +
               ", status=" + status + ", start=" + startTime + "}";
    }
}