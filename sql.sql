
CREATE TYPE booking_status AS ENUM ('PENDING', 'CONFIRMED', 'CANCELED');
CREATE TYPE appointment_type AS ENUM ('URGENT','FOLLOW_UP','ASSESSMENT','VIRTUAL','IN_PERSON','INDIVIDUAL','GROUP');



CREATE TABLE schedules (
  id           BIGSERIAL PRIMARY KEY,
  work_date    DATE            NOT NULL,
  created_at   TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);
ALTER TABLE schedules ADD CONSTRAINT uq_schedules_work_date UNIQUE (work_date);

CREATE TABLE time_slots (
  id            BIGSERIAL PRIMARY KEY,
  schedule_id   BIGINT         NOT NULL REFERENCES schedules(id) ON DELETE CASCADE,
  start_time    TIMESTAMPTZ    NOT NULL,
  end_time      TIMESTAMPTZ    NOT NULL,
  available     BOOLEAN        NOT NULL DEFAULT TRUE,
  CONSTRAINT chk_slot_time CHECK (end_time > start_time)
);

CREATE TABLE appointments (
  id                   BIGSERIAL PRIMARY KEY,
  type                 appointment_type NOT NULL,
  status               booking_status   NOT NULL DEFAULT 'CONFIRMED',
  start_time           TIMESTAMPTZ      NOT NULL,
  end_time             TIMESTAMPTZ      NOT NULL,
  participants_count   INT              NOT NULL DEFAULT 1,
  max_participants     INT              NOT NULL DEFAULT 1,
  created_by           BIGINT           NOT NULL REFERENCES users(id),
  slot_id              BIGINT           REFERENCES time_slots(id),
  created_at           TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
  updated_at           TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
  CONSTRAINT chk_appt_time CHECK (end_time > start_time),
  CONSTRAINT chk_capacity  CHECK (
    participants_count >= 1
    AND max_participants >= 1
    AND participants_count <= max_participants
  )
);


