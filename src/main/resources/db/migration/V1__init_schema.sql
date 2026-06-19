-- BankMind AI — V1__init_schema.sql
-- Baseline schema initialization

SET FOREIGN_KEY_CHECKS = 0;

-- ── 1. users ─────────────────────────────────────────────────────────────────
CREATE TABLE users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name     VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    phone_number  VARCHAR(255) NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(50) NULL,
    status        VARCHAR(50) NULL,
    email_verified BOOLEAN NULL DEFAULT FALSE,
    created_at    TIMESTAMP NULL,
    updated_at    TIMESTAMP NULL,
    UNIQUE KEY ux_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 2. otp_tokens ────────────────────────────────────────────────────────────
CREATE TABLE otp_tokens (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    code       VARCHAR(6) NOT NULL,
    user_id    BIGINT NOT NULL,
    purpose    VARCHAR(50) NOT NULL,
    used       BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_otp_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_otp_token_code (code),
    INDEX idx_otp_token_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 3. refresh_tokens ────────────────────────────────────────────────────────
CREATE TABLE refresh_tokens (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    token      VARCHAR(512) NOT NULL,
    user_id    BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NULL,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY ux_refresh_tokens_token (token),
    INDEX idx_refresh_token_token (token),
    INDEX idx_refresh_token_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 4. knowledge_sources ─────────────────────────────────────────────────────
CREATE TABLE knowledge_sources (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    source_type     VARCHAR(50) NOT NULL,
    location_path   VARCHAR(2048) NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    cron_expression VARCHAR(100) NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_ks_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 5. documents ─────────────────────────────────────────────────────────────
CREATE TABLE documents (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_id  BIGINT NOT NULL,
    title      VARCHAR(255) NOT NULL,
    file_path  TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_documents_source FOREIGN KEY (source_id) REFERENCES knowledge_sources(id) ON DELETE CASCADE,
    INDEX idx_docs_source (source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 6. document_chunks ────────────────────────────────────────────────────────
CREATE TABLE document_chunks (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id       BIGINT NOT NULL,
    chunk_text        TEXT NOT NULL,
    page_number       INT NOT NULL,
    char_offset_start INT NOT NULL,
    char_offset_end   INT NOT NULL,
    token_count       INT NOT NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_document_chunks_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    INDEX idx_chunks_doc (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 7. chat_sessions ─────────────────────────────────────────────────────────
CREATE TABLE chat_sessions (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    title      VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_sessions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_sessions_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 8. chat_messages ─────────────────────────────────────────────────────────
CREATE TABLE chat_messages (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id       BIGINT NOT NULL,
    role             VARCHAR(50) NOT NULL,
    message_text     TEXT NOT NULL,
    confidence_score DECIMAL(5,2) NULL,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_messages_session FOREIGN KEY (session_id) REFERENCES chat_sessions(id) ON DELETE CASCADE,
    INDEX idx_messages_session (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 9. loan_eligibility_requests ─────────────────────────────────────────────
CREATE TABLE loan_eligibility_requests (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id            BIGINT NOT NULL,
    loan_type          VARCHAR(50) NOT NULL,
    rule_name_applied  VARCHAR(255) NULL,
    salary             DECIMAL(15,2) NOT NULL,
    cibil_score        INT NOT NULL,
    foir               DECIMAL(5,2) NOT NULL,
    age                INT NOT NULL,
    employment_type    VARCHAR(50) NOT NULL,
    eligible           BOOLEAN NOT NULL,
    reason             TEXT NULL,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ler_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_ler_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 10. regulatory_analyses ──────────────────────────────────────────────────
CREATE TABLE regulatory_analyses (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    title            VARCHAR(255) NOT NULL,
    from_document_id BIGINT NOT NULL,
    to_document_id   BIGINT NOT NULL,
    summary          TEXT NULL,
    status           VARCHAR(50) NOT NULL,
    created_by       BIGINT NOT NULL,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ra_from_doc FOREIGN KEY (from_document_id) REFERENCES documents(id) ON DELETE CASCADE,
    CONSTRAINT fk_ra_to_doc FOREIGN KEY (to_document_id) REFERENCES documents(id) ON DELETE CASCADE,
    CONSTRAINT fk_ra_creator FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT,
    INDEX idx_ra_creator (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 11. audit_logs ───────────────────────────────────────────────────────────
CREATE TABLE audit_logs (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NULL,
    action     VARCHAR(255) NOT NULL,
    details    TEXT NULL,
    ip_address VARCHAR(45) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_al_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_al_user (user_id),
    INDEX idx_al_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 12. Seed data ────────────────────────────────────────────────────────────
-- Seed default administrator / system user with id = 1
INSERT INTO users (id, full_name, email, phone_number, password_hash, role, status, email_verified, created_at, updated_at)
VALUES (1, 'System Administrator', 'admin@bankmind.ai', NULL, '$2a$10$vK3N6H6Z6G9k9k8Q8y8y8e8e8e8e8e8e8e8e8e8e8e8e8e8e8e8e8', 'ADMIN', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

SET FOREIGN_KEY_CHECKS = 1;
