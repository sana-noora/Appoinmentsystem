package domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public class Schedule {

    private long id;
    private LocalDate workDate;
    private OffsetDateTime createdAt;

    public Schedule() {}

    public Schedule(long id, LocalDate workDate, OffsetDateTime createdAt) {
        this.id = id; this.workDate = workDate; this.createdAt = createdAt;
    }

    public Schedule(LocalDate workDate) { this.workDate = workDate; }

    public long getId()                         { return id; }
    public void setId(long id)                  { this.id = id; }
    public LocalDate getWorkDate()              { return workDate; }
    public void setWorkDate(LocalDate d)        { this.workDate = d; }
    public OffsetDateTime getCreatedAt()        { return createdAt; }

    @Override
    public String toString() {
        return "Schedule{id=" + id + ", workDate=" + workDate + "}";
    }
}