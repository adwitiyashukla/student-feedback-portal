-- =====================================================================
-- V1 : Baseline schema for the Student Feedback Portal
--
-- Every structural change is a versioned, checksummed Flyway migration;
-- the schema is never edited in place against a running database.
--
-- Charset is utf8mb4 throughout so the full Unicode range, including
-- four-byte characters, round-trips intact.
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- departments
-- ---------------------------------------------------------------------
CREATE TABLE departments (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(20)  NOT NULL,
    name        VARCHAR(120) NOT NULL,
    description VARCHAR(500) NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    CONSTRAINT pk_departments PRIMARY KEY (id),
    CONSTRAINT uk_departments_code UNIQUE (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- users  (single authentication table; students and admins extend it)
--
-- A single identity table for every role, so there is one hashing policy,
-- one lockout policy and one audit trail rather than one set per role.
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    email                 VARCHAR(160) NOT NULL,
    password_hash         VARCHAR(100) NOT NULL,
    full_name             VARCHAR(120) NOT NULL,
    phone                 VARCHAR(20)  NULL,
    role                  VARCHAR(20)  NOT NULL,
    enabled               BOOLEAN      NOT NULL DEFAULT TRUE,
    account_locked        BOOLEAN      NOT NULL DEFAULT FALSE,
    failed_login_attempts INT          NOT NULL DEFAULT 0,
    lock_expires_at       DATETIME(6)  NULL,
    last_login_at         DATETIME(6)  NULL,
    created_at            DATETIME(6)  NOT NULL,
    updated_at            DATETIME(6)  NOT NULL,
    version               BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('STUDENT', 'ADMIN', 'SUPER_ADMIN'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX ix_users_role ON users (role);

-- ---------------------------------------------------------------------
-- students  (joined-table inheritance from users)
-- ---------------------------------------------------------------------
CREATE TABLE students (
    user_id       BIGINT      NOT NULL,
    roll_number   VARCHAR(30) NOT NULL,
    department_id BIGINT      NOT NULL,
    program       VARCHAR(60) NULL,
    batch_year    INT         NULL,
    semester      INT         NULL,
    CONSTRAINT pk_students PRIMARY KEY (user_id),
    CONSTRAINT uk_students_roll_number UNIQUE (roll_number),
    CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_students_department FOREIGN KEY (department_id) REFERENCES departments (id),
    CONSTRAINT ck_students_semester CHECK (semester IS NULL OR (semester BETWEEN 1 AND 12))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX ix_students_department ON students (department_id);

-- ---------------------------------------------------------------------
-- admins  (joined-table inheritance from users)
-- ---------------------------------------------------------------------
CREATE TABLE admins (
    user_id       BIGINT      NOT NULL,
    employee_code VARCHAR(30) NOT NULL,
    department_id BIGINT      NOT NULL,
    designation   VARCHAR(80) NULL,
    CONSTRAINT pk_admins PRIMARY KEY (user_id),
    CONSTRAINT uk_admins_employee_code UNIQUE (employee_code),
    CONSTRAINT fk_admins_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_admins_department FOREIGN KEY (department_id) REFERENCES departments (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX ix_admins_department ON admins (department_id);

-- ---------------------------------------------------------------------
-- feedback
--
-- The ticket number is generated server-side and unique-constrained, and
-- the workflow columns are only writable through the service layer - never
-- from a client-supplied field.
-- ---------------------------------------------------------------------
CREATE TABLE feedback (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    ticket_number       VARCHAR(24)  NOT NULL,
    title               VARCHAR(150) NOT NULL,
    description         TEXT         NOT NULL,
    category            VARCHAR(30)  NOT NULL,
    priority            VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    status              VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    submitted_by_id     BIGINT       NOT NULL,
    assigned_to_id      BIGINT       NULL,
    department_id       BIGINT       NOT NULL,
    anonymous           BOOLEAN      NOT NULL DEFAULT FALSE,
    sentiment_label     VARCHAR(20)  NULL,
    sentiment_score     DOUBLE       NULL,
    suggested_category  VARCHAR(30)  NULL,
    suggested_priority  VARCHAR(20)  NULL,
    analysis_confidence DOUBLE       NULL,
    analysed_at         DATETIME(6)  NULL,
    due_at              DATETIME(6)  NULL,
    resolved_at         DATETIME(6)  NULL,
    closed_at           DATETIME(6)  NULL,
    satisfaction_rating INT          NULL,
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,
    version             BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_feedback PRIMARY KEY (id),
    CONSTRAINT uk_feedback_ticket_number UNIQUE (ticket_number),
    CONSTRAINT fk_feedback_submitted_by FOREIGN KEY (submitted_by_id) REFERENCES students (user_id),
    CONSTRAINT fk_feedback_assigned_to FOREIGN KEY (assigned_to_id) REFERENCES admins (user_id),
    CONSTRAINT fk_feedback_department FOREIGN KEY (department_id) REFERENCES departments (id),
    CONSTRAINT ck_feedback_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'AWAITING_STUDENT', 'RESOLVED', 'CLOSED', 'REJECTED')),
    CONSTRAINT ck_feedback_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    CONSTRAINT ck_feedback_rating CHECK (satisfaction_rating IS NULL OR (satisfaction_rating BETWEEN 1 AND 5))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Query patterns the dashboards actually use.
CREATE INDEX ix_feedback_status_created ON feedback (status, created_at);
CREATE INDEX ix_feedback_submitter_created ON feedback (submitted_by_id, created_at);
CREATE INDEX ix_feedback_department_status ON feedback (department_id, status);
CREATE INDEX ix_feedback_assignee_status ON feedback (assigned_to_id, status);
CREATE INDEX ix_feedback_due_at ON feedback (due_at);
CREATE FULLTEXT INDEX ft_feedback_text ON feedback (title, description);

-- ---------------------------------------------------------------------
-- feedback_comments  (threaded conversation)
--
-- A real thread, so a ticket can carry an ongoing conversation rather
-- than a single reply field.
-- ---------------------------------------------------------------------
CREATE TABLE feedback_comments (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    feedback_id   BIGINT      NOT NULL,
    author_id     BIGINT      NOT NULL,
    body          TEXT        NOT NULL,
    internal_note BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at    DATETIME(6) NOT NULL,
    CONSTRAINT pk_feedback_comments PRIMARY KEY (id),
    CONSTRAINT fk_comments_feedback FOREIGN KEY (feedback_id) REFERENCES feedback (id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX ix_comments_feedback_created ON feedback_comments (feedback_id, created_at);

-- ---------------------------------------------------------------------
-- feedback_status_history  (immutable audit trail of the workflow)
-- ---------------------------------------------------------------------
CREATE TABLE feedback_status_history (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    feedback_id   BIGINT      NOT NULL,
    from_status   VARCHAR(20) NULL,
    to_status     VARCHAR(20) NOT NULL,
    changed_by_id BIGINT      NOT NULL,
    note          VARCHAR(500) NULL,
    changed_at    DATETIME(6) NOT NULL,
    CONSTRAINT pk_status_history PRIMARY KEY (id),
    CONSTRAINT fk_history_feedback FOREIGN KEY (feedback_id) REFERENCES feedback (id) ON DELETE CASCADE,
    CONSTRAINT fk_history_changed_by FOREIGN KEY (changed_by_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX ix_history_feedback_changed ON feedback_status_history (feedback_id, changed_at);

-- ---------------------------------------------------------------------
-- attachments  (local disk in dev, S3 in production)
-- ---------------------------------------------------------------------
CREATE TABLE attachments (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    feedback_id    BIGINT       NOT NULL,
    file_name      VARCHAR(255) NOT NULL,
    content_type   VARCHAR(120) NOT NULL,
    size_bytes     BIGINT       NOT NULL,
    storage_key    VARCHAR(512) NOT NULL,
    uploaded_by_id BIGINT       NOT NULL,
    created_at     DATETIME(6)  NOT NULL,
    CONSTRAINT pk_attachments PRIMARY KEY (id),
    CONSTRAINT fk_attachments_feedback FOREIGN KEY (feedback_id) REFERENCES feedback (id) ON DELETE CASCADE,
    CONSTRAINT fk_attachments_uploader FOREIGN KEY (uploaded_by_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX ix_attachments_feedback ON attachments (feedback_id);

-- ---------------------------------------------------------------------
-- notifications
-- ---------------------------------------------------------------------
CREATE TABLE notifications (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    recipient_id BIGINT       NOT NULL,
    type         VARCHAR(40)  NOT NULL,
    title        VARCHAR(150) NOT NULL,
    message      VARCHAR(500) NOT NULL,
    link         VARCHAR(255) NULL,
    is_read      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   DATETIME(6)  NOT NULL,
    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX ix_notifications_recipient_read ON notifications (recipient_id, is_read, created_at);

-- ---------------------------------------------------------------------
-- refresh_tokens  (only the SHA-256 hash is stored, never the token)
-- ---------------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    token_hash  VARCHAR(64)  NOT NULL,
    user_id     BIGINT       NOT NULL,
    issued_at   DATETIME(6)  NOT NULL,
    expires_at  DATETIME(6)  NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    user_agent  VARCHAR(255) NULL,
    ip_address  VARCHAR(45)  NULL,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX ix_refresh_tokens_user ON refresh_tokens (user_id, revoked);
CREATE INDEX ix_refresh_tokens_expires ON refresh_tokens (expires_at);

-- ---------------------------------------------------------------------
-- audit_logs
-- ---------------------------------------------------------------------
CREATE TABLE audit_logs (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    actor_id    BIGINT       NULL,
    actor_email VARCHAR(160) NULL,
    action      VARCHAR(60)  NOT NULL,
    entity_type VARCHAR(60)  NOT NULL,
    entity_id   VARCHAR(60)  NULL,
    details     TEXT         NULL,
    ip_address  VARCHAR(45)  NULL,
    created_at  DATETIME(6)  NOT NULL,
    CONSTRAINT pk_audit_logs PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX ix_audit_logs_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX ix_audit_logs_created ON audit_logs (created_at);
