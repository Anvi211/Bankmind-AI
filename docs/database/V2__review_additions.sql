-- BankMind AI — V2__review_additions.sql
-- Phase 0 architecture review additions
-- Run after V1__init_schema.sql

SET FOREIGN_KEY_CHECKS = 0;

-- ── 1. Add tenant isolation to users and knowledge_sources ──────────────────

ALTER TABLE users
    ADD COLUMN tenant_id BIGINT NULL DEFAULT 1 AFTER id,
    ADD INDEX idx_users_tenant (tenant_id);

ALTER TABLE knowledge_sources
    ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 AFTER id,
    ADD INDEX idx_ks_tenant (tenant_id);

-- ── 2. login_events ─────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS login_events (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT       NULL,
    email        VARCHAR(255) NOT NULL,
    ip_address   VARCHAR(45)  NOT NULL,
    user_agent   VARCHAR(500) NULL,
    outcome      ENUM('SUCCESS','WRONG_PASSWORD','ACCOUNT_LOCKED',
                      'OTP_FAILED','ACCOUNT_NOT_VERIFIED') NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_le_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_le_user (user_id, created_at),
    INDEX idx_le_ip   (ip_address, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 3. source_sync_logs ─────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS source_sync_logs (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_id           BIGINT      NOT NULL,
    triggered_by        ENUM('SCHEDULED','MANUAL') NOT NULL DEFAULT 'SCHEDULED',
    started_at          TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at        TIMESTAMP   NULL,
    documents_found     INT         NOT NULL DEFAULT 0,
    documents_processed INT         NOT NULL DEFAULT 0,
    documents_failed    INT         NOT NULL DEFAULT 0,
    status              ENUM('RUNNING','COMPLETE','PARTIAL','FAILED') NOT NULL DEFAULT 'RUNNING',
    error_message       TEXT        NULL,
    CONSTRAINT fk_ssl_source FOREIGN KEY (source_id) REFERENCES knowledge_sources(id) ON DELETE CASCADE,
    INDEX idx_ssl_source (source_id, started_at),
    INDEX idx_ssl_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 4. document_versions ────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS document_versions (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id      BIGINT      NOT NULL,
    version_number   INT         NOT NULL,
    content_hash     VARCHAR(64) NOT NULL,
    file_path        TEXT        NOT NULL,
    ingestion_status ENUM('PENDING','PROCESSING','COMPLETE','FAILED') NOT NULL DEFAULT 'PENDING',
    created_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_dv_doc FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    UNIQUE KEY ux_dv_doc_version (document_id, version_number),
    INDEX idx_dv_doc (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 5. Add three columns to document_chunks ─────────────────────────────────

ALTER TABLE document_chunks
    ADD COLUMN section_title           VARCHAR(500)  NULL
        COMMENT 'Nearest heading above this chunk from parsed document'
        AFTER page_number,
    ADD COLUMN highlight_coords        JSON          NULL
        COMMENT 'PDF bounding box: {x1,y1,x2,y2,page} from PDFBox TextStripper'
        AFTER char_offset_end,
    ADD COLUMN embedding_model_version VARCHAR(100)  NOT NULL
        DEFAULT 'text-embedding-3-small-v1'
        COMMENT 'Model used to generate this chunk embedding'
        AFTER token_count;

-- ── 6. evidence_references ──────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS evidence_references (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    chat_message_id   BIGINT         NOT NULL,
    document_chunk_id BIGINT         NOT NULL,
    page_number       INT            NOT NULL,
    evidence_text     TEXT           NOT NULL,
    similarity_score  DECIMAL(6,4)   NOT NULL,
    highlight_coords  JSON           NULL,
    citation_rank     TINYINT        NOT NULL DEFAULT 1,
    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_er_message FOREIGN KEY (chat_message_id)
        REFERENCES chat_messages(id) ON DELETE CASCADE,
    CONSTRAINT fk_er_chunk FOREIGN KEY (document_chunk_id)
        REFERENCES document_chunks(id) ON DELETE CASCADE,
    INDEX idx_er_message (chat_message_id),
    INDEX idx_er_chunk   (document_chunk_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 7. loan_policy_rules ────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS loan_policy_rules (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_type                ENUM('HOME','PERSONAL','EDUCATION','AUTO','BUSINESS') NOT NULL,
    rule_name                VARCHAR(255)      NOT NULL,
    minimum_salary           DECIMAL(15,2)     NOT NULL DEFAULT 0,
    minimum_cibil            SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    maximum_foir             DECIMAL(5,2)      NOT NULL DEFAULT 100.00,
    minimum_age              TINYINT UNSIGNED  NOT NULL DEFAULT 18,
    maximum_age              TINYINT UNSIGNED  NOT NULL DEFAULT 70,
    employment_types_allowed JSON              NOT NULL
        COMMENT 'e.g. ["SALARIED","SELF_EMPLOYED"]',
    effective_from           DATE              NOT NULL,
    effective_to             DATE              NULL
        COMMENT 'NULL means no end date (currently effective)',
    status                   ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_by               BIGINT            NOT NULL,
    created_at               TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_lpr_user FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX idx_lpr_type_status (loan_type, status, effective_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 8. Seed default loan policy rules (v1 baseline) ─────────────────────────

INSERT INTO loan_policy_rules
    (loan_type, rule_name, minimum_salary, minimum_cibil, maximum_foir,
     minimum_age, maximum_age, employment_types_allowed, effective_from, status, created_by)
VALUES
    ('HOME',     'Home Loan - Default',      25000, 700, 50.00, 21, 65,
     '["SALARIED","SELF_EMPLOYED","BUSINESS"]', '2025-01-01', 'ACTIVE', 1),
    ('PERSONAL', 'Personal Loan - Default',  15000, 650, 50.00, 21, 60,
     '["SALARIED"]', '2025-01-01', 'ACTIVE', 1),
    ('EDUCATION','Education Loan - Default',      0, 600, 60.00, 18, 40,
     '["SALARIED","SELF_EMPLOYED"]', '2025-01-01', 'ACTIVE', 1),
    ('AUTO',     'Auto Loan - Default',      15000, 650, 55.00, 21, 65,
     '["SALARIED","SELF_EMPLOYED","BUSINESS","RETIRED"]', '2025-01-01', 'ACTIVE', 1),
    ('BUSINESS', 'Business Loan - Default',  50000, 680, 65.00, 25, 65,
     '["SELF_EMPLOYED","BUSINESS"]', '2025-01-01', 'ACTIVE', 1);

SET FOREIGN_KEY_CHECKS = 1;
