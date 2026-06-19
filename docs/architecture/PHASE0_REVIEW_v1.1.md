# BankMind AI — Phase 0 Architecture Review v1.1

> **Review type**: Principal Architect pre-implementation review
> **Triggered by**: Final review before Phase 1 build
> **Version**: 1.1.0 | Status: Approved for implementation

---

## Review Summary

Seven architectural gaps identified and resolved. Four new database tables added. Two
existing tables enhanced. Architecture label corrected from "API Gateway" to "Modular
Monolith." Evidence viewer metadata confirmed sufficient with improvements. Total table
count: 15.

---

## Part 1 — Architectural Gaps Found and Resolved

### Gap 1 · Wrong architecture label (HIGH)

**Problem**: Phase 0 spec described an "API Gateway" as the entry point. An API Gateway
is a dedicated infrastructure component (Kong, AWS API Gateway, Spring Cloud Gateway)
that sits in front of multiple independent services. BankMind v1 has exactly one backend
application. Labelling it an "API Gateway" would mislead every developer on the team.

**Resolution**: The entry point is a `Spring Security FilterChain` inside a
**Spring Boot modular monolith**. Each feature domain (auth, knowledge, AI, loan) is a
package module with its own controller, service, and repository. They share one JVM,
one datasource, one application context. No inter-service HTTP calls, no service
discovery, no gateway routing.

**Future-proofing**: When multi-bank or high-scale extraction is needed, individual
modules can be extracted into independent Spring Boot services and a real API gateway
added at that point — the clean package boundaries already drawn make that extraction
tractable.

---

### Gap 2 · No multi-bank isolation layer (MEDIUM)

**Problem**: The spec targets eventual multi-bank support but had no tenant isolation
mechanism. Adding it post-launch requires a migration across every table that holds
bank-specific data.

**Resolution**: A nullable `tenant_id BIGINT` column is added now to `users` and
`knowledge_sources`. For v1 single-bank deployment, all rows carry the same
`tenant_id = 1`. Application-level filtering (`WHERE tenant_id = ?`) is enforced in
all service queries via a `TenantContext` thread-local populated by the JWT filter.
This requires zero schema changes when a second bank is onboarded.

---

### Gap 3 · No sync observability (HIGH)

**Problem**: The knowledge source management module had no record of what happened
during each sync run. If ingestion fails at 3 AM, there is no way for the admin
dashboard to show which sources errored, how many documents were found vs processed,
or how long the run took.

**Resolution**: New `source_sync_logs` table. Every scheduler run opens a log record
on start and closes it on completion or error. The admin dashboard reads this table
directly — no log scraping required.

---

### Gap 4 · No document versioning (HIGH)

**Problem**: Regulatory documents are updated over time. RBI circular v2 supersedes v1.
Without versioning, re-ingesting a document silently overwrites the previous content,
making it impossible to compare "current vs previous" — a core regulatory intelligence
feature.

**Resolution**: New `document_versions` table. Each re-ingestion of a source URL
creates a new version row linked to the parent document. The parent `documents` row
represents the canonical document identity. Policy comparison queries join on
`version_number`.

---

### Gap 5 · Evidence grounding was implicit, not persisted (HIGH)

**Problem**: When an AI answer is returned, the source citations were stored only as
a JSON array of chunk IDs in `chat_messages.source_chunk_ids`. This meant:
(a) the evidence viewer had to re-query Qdrant at render time, and
(b) there was no auditable record of exactly what evidence supported exactly what answer.

**Resolution**: New `evidence_references` table. Each row links one `chat_message` to
one `document_chunk` with the `similarity_score`, `page_number`, `evidence_text`, and
`highlight_coords` captured at answer generation time. This table is the explainability
audit trail: regulators can query "what evidence did the system use to answer this
question on this date?"

---

### Gap 6 · Loan rules were hardcoded, not data-driven (CRITICAL)

**Problem**: Loan eligibility criteria (CIBIL ≥ 700, age 21–65, FOIR ≤ 50%) were
described as Java constants in a service class. RBI updates these thresholds. A code
change + redeploy for every regulatory update is operationally unacceptable.

**Resolution**: New `loan_policy_rules` table. Rules are configured by ADMIN users via
the admin dashboard. The `LoanEligibilityService` reads active rules at query time.
`effective_from` / `effective_to` date range columns support scheduled rule changes
(e.g. a new threshold that takes effect on a future date). `employment_types_allowed`
is a JSON array so it handles any combination without schema changes.

---

### Gap 7 · No login event history (MEDIUM)

**Problem**: The `audit_logs` table tracks mutations but was not purpose-built for
security-relevant authentication events. Detecting brute force, impossible travel, or
account takeover requires a dedicated, indexed login event stream.

**Resolution**: New `login_events` table with `user_id`, `ip_address`, `user_agent`,
`outcome` (SUCCESS / WRONG_PASSWORD / ACCOUNT_LOCKED / OTP_FAILED), and timestamp.
Indexed on `(user_id, created_at)` for per-user history queries and on `ip_address`
for IP-based rate limiting checks.

---

## Part 2 — Evidence Viewer Metadata Review

### Requirement: The evidence viewer must support four operations

1. **Open PDF** — `document_chunks.document_id` → `documents.file_path`
2. **Jump to page** — `document_chunks.page_number`
3. **Highlight paragraph** — `document_chunks.highlight_coords` (NEW, JSON bounding box)
4. **Show evidence excerpt** — `document_chunks.chunk_text`

### v1.0 gap assessment

| Feature | v1.0 status | Issue |
|---|---|---|
| Open PDF | ✅ Present | `document_id` links to `documents.file_path` |
| Jump to page | ✅ Present | `page_number` stored per chunk |
| Highlight paragraph | ❌ Missing | No coordinate data stored |
| Show evidence excerpt | ✅ Present | `chunk_text` stored |
| Section context | ❌ Missing | No `section_title` — citations had no heading context |
| Re-embed safety | ❌ Missing | No `embedding_model_version` — changing models silently breaks search |

### v1.1 additions to `document_chunks`

```
highlight_coords         JSON    NULL
  -- Format: {"x1": 72, "y1": 340, "x2": 540, "y2": 362, "page": 14}
  -- Populated by PDFBox TextStripper with position extraction
  -- Used by PDF.js custom highlight layer in the Evidence Viewer

section_title            VARCHAR(500) NULL
  -- Nearest heading above this chunk (extracted during parsing)
  -- Displayed in citation card: "Section 4.2 — Eligibility Criteria"
  -- Improves answer readability significantly

embedding_model_version  VARCHAR(100) NOT NULL DEFAULT 'text-embedding-3-small-v1'
  -- Stored per chunk at embedding time
  -- When the model is upgraded, stale chunks are identified by version mismatch
  -- Re-embedding job filters: WHERE embedding_model_version != 'current'
```

### Evidence grounding data flow (updated)

```
User clicks citation in chat
    ↓
GET /chunks/{chunkId}/evidence
    ↓
Returns: page_number, highlight_coords, chunk_text, section_title,
         document_id, file_path, similarity_score
    ↓
Frontend opens PDF viewer at page_number
    ↓
PDF.js renders page, applies highlight_coords bounding box overlay
    ↓
Sidebar shows: section_title + chunk_text + similarity_score + source name
```

---

## Part 3 — Updated Database Schema (15 tables)

### New table: `login_events`

```sql
CREATE TABLE login_events (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT      NULL,
    email        VARCHAR(255) NOT NULL,
    ip_address   VARCHAR(45) NOT NULL,
    user_agent   VARCHAR(500) NULL,
    outcome      ENUM('SUCCESS','WRONG_PASSWORD','ACCOUNT_LOCKED',
                      'OTP_FAILED','ACCOUNT_NOT_VERIFIED') NOT NULL,
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_le_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_le_user   (user_id, created_at),
    INDEX idx_le_ip     (ip_address, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### New table: `source_sync_logs`

```sql
CREATE TABLE source_sync_logs (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_id             BIGINT      NOT NULL,
    triggered_by          ENUM('SCHEDULED','MANUAL') NOT NULL DEFAULT 'SCHEDULED',
    started_at            TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at          TIMESTAMP   NULL,
    documents_found       INT         NOT NULL DEFAULT 0,
    documents_processed   INT         NOT NULL DEFAULT 0,
    documents_failed      INT         NOT NULL DEFAULT 0,
    status                ENUM('RUNNING','COMPLETE','PARTIAL','FAILED') NOT NULL DEFAULT 'RUNNING',
    error_message         TEXT        NULL,
    CONSTRAINT fk_ssl_source FOREIGN KEY (source_id) REFERENCES knowledge_sources(id) ON DELETE CASCADE,
    INDEX idx_ssl_source (source_id, started_at),
    INDEX idx_ssl_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### New table: `document_versions`

```sql
CREATE TABLE document_versions (
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
```

### New table: `evidence_references`

```sql
CREATE TABLE evidence_references (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    chat_message_id     BIGINT         NOT NULL,
    document_chunk_id   BIGINT         NOT NULL,
    page_number         INT            NOT NULL,
    evidence_text       TEXT           NOT NULL,
    similarity_score    DECIMAL(6,4)   NOT NULL,
    highlight_coords    JSON           NULL,
    citation_rank       TINYINT        NOT NULL DEFAULT 1,
    created_at          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_er_message FOREIGN KEY (chat_message_id)   REFERENCES chat_messages(id)    ON DELETE CASCADE,
    CONSTRAINT fk_er_chunk   FOREIGN KEY (document_chunk_id) REFERENCES document_chunks(id)  ON DELETE CASCADE,
    INDEX idx_er_message (chat_message_id),
    INDEX idx_er_chunk   (document_chunk_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### New table: `loan_policy_rules`

```sql
CREATE TABLE loan_policy_rules (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_type                ENUM('HOME','PERSONAL','EDUCATION','AUTO','BUSINESS') NOT NULL,
    rule_name                VARCHAR(255) NOT NULL,
    minimum_salary           DECIMAL(15,2) NOT NULL DEFAULT 0,
    minimum_cibil            SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    maximum_foir             DECIMAL(5,2) NOT NULL DEFAULT 100.00,
    minimum_age              TINYINT UNSIGNED NOT NULL DEFAULT 18,
    maximum_age              TINYINT UNSIGNED NOT NULL DEFAULT 70,
    employment_types_allowed JSON NOT NULL,
    effective_from           DATE NOT NULL,
    effective_to             DATE NULL,
    status                   ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_by               BIGINT NOT NULL,
    created_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_lpr_user FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX idx_lpr_type_status (loan_type, status, effective_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### Altered table: `users` (add `tenant_id`)

```sql
ALTER TABLE users ADD COLUMN tenant_id BIGINT NULL DEFAULT 1 AFTER id;
ALTER TABLE users ADD INDEX idx_users_tenant (tenant_id);
```

### Altered table: `knowledge_sources` (add `tenant_id`)

```sql
ALTER TABLE knowledge_sources ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 AFTER id;
ALTER TABLE knowledge_sources ADD INDEX idx_ks_tenant (tenant_id);
```

### Altered table: `document_chunks` (add three columns)

```sql
ALTER TABLE document_chunks
    ADD COLUMN section_title            VARCHAR(500)  NULL        AFTER page_number,
    ADD COLUMN highlight_coords         JSON          NULL        AFTER char_offset_end,
    ADD COLUMN embedding_model_version  VARCHAR(100)  NOT NULL DEFAULT 'text-embedding-3-small-v1' AFTER token_count;
```

---

## Part 4 — Updated Complete Table Inventory

| # | Table | Purpose | New / Changed |
|---|---|---|---|
| 1 | `users` | Identity, roles, tenant | + `tenant_id` |
| 2 | `otp_tokens` | Email verification, password reset | No change |
| 3 | `refresh_tokens` | JWT refresh token store | No change |
| 4 | `login_events` | Security-grade auth event log | **NEW** |
| 5 | `knowledge_sources` | Registered document sources | + `tenant_id` |
| 6 | `source_sync_logs` | Per-run ingestion telemetry | **NEW** |
| 7 | `documents` | Canonical document registry | No change |
| 8 | `document_versions` | Document version history | **NEW** |
| 9 | `document_chunks` | Text chunks with position metadata | + 3 columns |
| 10 | `chat_sessions` | Conversation containers | No change |
| 11 | `chat_messages` | Individual turns (user + assistant) | No change |
| 12 | `evidence_references` | Answer-to-evidence audit trail | **NEW** |
| 13 | `loan_policy_rules` | Configurable eligibility criteria | **NEW** |
| 14 | `loan_eligibility_requests` | Loan check history | No change |
| 15 | `regulatory_analyses` | Impact analysis results | No change |
| 16 | `audit_logs` | General system event log | No change |

**Total: 16 tables** (was 11, +4 new, +1 counted correctly from v1.0 — `audit_logs` was described but not numbered)

---

## Part 5 — Updated API Catalog

### New endpoints added in v1.1

#### Sync Logs

```
GET  /sources/{id}/sync-logs
     → paginated list of source_sync_logs for a source
     → roles: ADMIN, EMPLOYEE
     → response: [{id, triggeredBy, startedAt, completedAt, documentsFound,
                   documentsProcessed, documentsFailed, status, errorMessage}]

GET  /sources/{id}/sync-logs/latest
     → most recent sync log entry
     → roles: all authenticated
```

#### Document Versions

```
GET  /documents/{id}/versions
     → version history for a document
     → roles: all authenticated
     → response: [{id, versionNumber, contentHash, ingestionStatus, createdAt}]

POST /documents/{id}/compare?fromVersion=1&toVersion=2
     → triggers regulatory impact analysis between two versions
     → roles: ADMIN, EMPLOYEE
     → response: {analysisId, status}
```

#### Evidence References

```
GET  /chat/messages/{messageId}/evidence
     → all evidence references for one AI answer
     → roles: all authenticated
     → response: [{rank, pageNumber, evidenceText, similarityScore,
                   highlightCoords, documentTitle, chunkId, downloadUrl}]
```

#### Loan Policy Rules

```
GET    /admin/loan-rules
       → list all rules, optionally filtered by loanType and status
       → roles: ADMIN, EMPLOYEE

POST   /admin/loan-rules
       → create a new policy rule
       → roles: ADMIN only
       → body: {loanType, ruleName, minimumSalary, minimumCibil, maximumFoir,
                minimumAge, maximumAge, employmentTypesAllowed, effectiveFrom, effectiveTo}

PUT    /admin/loan-rules/{id}
       → update a rule (creates audit log entry)
       → roles: ADMIN only

DELETE /admin/loan-rules/{id}
       → sets status = INACTIVE (soft delete, never physical delete for audit)
       → roles: ADMIN only
```

#### Login Events

```
GET  /admin/login-events
     → paginated login event log
     → roles: ADMIN only
     → query params: userId, ip, outcome, from, to, page, size
```

### Modified endpoints

`POST /chat/ask` — response now includes populated `evidenceReferences` array instead of raw `source_chunk_ids`. The `evidence_references` table is written server-side at answer time.

`GET /chunks/{chunkId}/evidence` — now also returns `sectionTitle`, `highlightCoords`, and `embeddingModelVersion`.

---

## Part 6 — Updated Backend Package Structure

```
com.bankmind/
├── BankMindApplication.java
│
├── config/
│   ├── SecurityConfig.java          # FilterChain, CORS, CSRF disabled for JWT
│   ├── JwtConfig.java               # Secret, expiry constants
│   ├── QdrantConfig.java            # QdrantClient bean
│   ├── LangChain4jConfig.java       # EmbeddingModel, ChatLanguageModel beans
│   ├── SchedulerConfig.java         # @EnableScheduling, thread pool
│   ├── TenantConfig.java            # TenantContext ThreadLocal + resolver
│   └── FlywayConfig.java            # Custom migration callbacks
│
├── security/
│   ├── JwtAuthFilter.java           # OncePerRequestFilter
│   ├── JwtProvider.java             # generate / validate / extract claims
│   ├── UserDetailsServiceImpl.java  # loads UserDetails from users table
│   └── RateLimitFilter.java         # IP-based rate limit on /auth/**
│
├── module/
│   │
│   ├── auth/
│   │   ├── controller/AuthController.java
│   │   ├── service/
│   │   │   ├── AuthService.java
│   │   │   ├── OtpService.java
│   │   │   └── TokenService.java
│   │   ├── repository/
│   │   │   ├── UserRepository.java
│   │   │   ├── OtpTokenRepository.java
│   │   │   ├── RefreshTokenRepository.java
│   │   │   └── LoginEventRepository.java
│   │   ├── entity/
│   │   │   ├── User.java
│   │   │   ├── OtpToken.java
│   │   │   ├── RefreshToken.java
│   │   │   └── LoginEvent.java
│   │   └── dto/
│   │       ├── RegisterRequest.java
│   │       ├── VerifyOtpRequest.java
│   │       ├── LoginRequest.java
│   │       ├── RefreshRequest.java
│   │       └── LoginResponse.java
│   │
│   ├── knowledge/
│   │   ├── controller/
│   │   │   ├── KnowledgeSourceController.java
│   │   │   └── DocumentController.java
│   │   ├── service/
│   │   │   ├── KnowledgeSourceService.java
│   │   │   ├── SyncSchedulerService.java      # @Scheduled runs, writes sync logs
│   │   │   ├── DocumentIngestionService.java  # orchestrates parse → chunk → embed
│   │   │   ├── DocumentVersionService.java
│   │   │   ├── ChunkingService.java
│   │   │   └── EmbeddingService.java
│   │   ├── repository/
│   │   │   ├── KnowledgeSourceRepository.java
│   │   │   ├── SourceSyncLogRepository.java
│   │   │   ├── DocumentRepository.java
│   │   │   ├── DocumentVersionRepository.java
│   │   │   └── DocumentChunkRepository.java
│   │   ├── entity/
│   │   │   ├── KnowledgeSource.java
│   │   │   ├── SourceSyncLog.java
│   │   │   ├── Document.java
│   │   │   ├── DocumentVersion.java
│   │   │   └── DocumentChunk.java
│   │   └── dto/ (request + response DTOs)
│   │
│   ├── ai/
│   │   ├── controller/ChatController.java
│   │   ├── service/
│   │   │   ├── RagService.java               # embedding → Qdrant → LLM → persist
│   │   │   ├── ChatService.java              # session + message management
│   │   │   ├── EvidenceService.java          # writes evidence_references rows
│   │   │   └── QdrantService.java            # upsert, search, delete
│   │   ├── repository/
│   │   │   ├── ChatSessionRepository.java
│   │   │   ├── ChatMessageRepository.java
│   │   │   └── EvidenceReferenceRepository.java
│   │   ├── entity/
│   │   │   ├── ChatSession.java
│   │   │   ├── ChatMessage.java
│   │   │   └── EvidenceReference.java
│   │   └── dto/ (AskRequest, ChatResponse, EvidenceDto)
│   │
│   ├── loan/
│   │   ├── controller/
│   │   │   ├── LoanController.java
│   │   │   └── LoanPolicyRuleController.java  # admin CRUD for rules
│   │   ├── service/
│   │   │   ├── LoanEligibilityService.java
│   │   │   └── LoanPolicyRuleService.java
│   │   ├── repository/
│   │   │   ├── LoanEligibilityRequestRepository.java
│   │   │   └── LoanPolicyRuleRepository.java
│   │   ├── entity/
│   │   │   ├── LoanEligibilityRequest.java
│   │   │   └── LoanPolicyRule.java
│   │   └── dto/
│   │
│   ├── regulatory/
│   │   ├── controller/RegulatoryController.java
│   │   ├── service/RegulatoryAnalysisService.java
│   │   ├── repository/RegulatoryAnalysisRepository.java
│   │   ├── entity/RegulatoryAnalysis.java
│   │   └── dto/
│   │
│   └── admin/
│       ├── controller/AdminController.java
│       ├── service/AdminService.java
│       ├── repository/
│       │   ├── AuditLogRepository.java
│       │   └── LoginEventRepository.java      # shared with auth module
│       ├── entity/AuditLog.java
│       └── dto/ (AuditLogDto, StatsResponse, SyncHealthResponse)
│
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   ├── ValidationException.java
│   ├── InsufficientEvidenceException.java
│   ├── DuplicateEmailException.java
│   └── TenantAccessDeniedException.java
│
└── util/
    ├── HashUtil.java           # SHA-256, BCrypt helpers
    ├── OtpUtil.java            # secure random 6-digit OTP
    ├── PaginationUtil.java     # PageRequest builder
    ├── CorrelationIdUtil.java  # MDC request correlation ID
    └── TenantContext.java      # ThreadLocal<Long> tenantId
```

---

## Part 7 — Updated Frontend Folder Structure

```
frontend/src/
│
├── router/
│   └── AppRouter.jsx            # React Router v6, protected routes by role
│
├── context/
│   ├── AuthContext.jsx          # user, tokens, login(), logout()
│   └── TenantContext.jsx        # tenantId for future multi-bank UI
│
├── hooks/
│   ├── useAuth.js
│   ├── useChat.js
│   ├── useEvidenceViewer.js     # PDF.js integration, highlight logic
│   └── useLoanCheck.js
│
├── services/                    # Axios instances, one per backend module
│   ├── api.js                   # base Axios instance, JWT interceptor
│   ├── authService.js
│   ├── sourceService.js
│   ├── documentService.js
│   ├── chatService.js
│   ├── evidenceService.js       # fetches evidence_references by messageId
│   ├── loanService.js
│   └── adminService.js
│
├── pages/
│   ├── LoginPage.jsx
│   ├── RegisterPage.jsx
│   ├── OtpPage.jsx
│   ├── ChatPage.jsx
│   ├── EvidencePage.jsx
│   ├── LoanPage.jsx
│   ├── SourcesPage.jsx
│   ├── RegIntelPage.jsx
│   └── AdminPage.jsx
│
└── components/
    ├── auth/
    │   ├── LoginForm.jsx
    │   ├── RegisterForm.jsx
    │   └── OtpForm.jsx
    │
    ├── chat/
    │   ├── ChatWindow.jsx
    │   ├── MessageBubble.jsx
    │   ├── CitationCard.jsx      # shows source, page, similarity score
    │   ├── ConfidenceBadge.jsx   # colour-coded: green ≥80, amber 50-79, red <50
    │   └── InsufficientEvidenceBanner.jsx
    │
    ├── evidence/
    │   ├── EvidencePanel.jsx     # split view: chat left, PDF right
    │   ├── PdfViewer.jsx         # react-pdf-viewer wrapper
    │   └── HighlightLayer.jsx    # PDF.js custom layer using highlight_coords
    │
    ├── loan/
    │   ├── EligibilityForm.jsx
    │   ├── EligibilityResult.jsx
    │   └── PolicyReferenceList.jsx
    │
    ├── sources/
    │   ├── SourceTable.jsx
    │   ├── AddSourceModal.jsx
    │   └── SyncLogDrawer.jsx     # shows source_sync_logs per source
    │
    ├── admin/
    │   ├── AuditLogTable.jsx
    │   ├── StatsCards.jsx
    │   ├── SourceHealthTable.jsx
    │   ├── LoanRulesTable.jsx    # CRUD for loan_policy_rules
    │   └── LoginEventsTable.jsx
    │
    └── shared/
        ├── ProtectedRoute.jsx
        ├── RoleGate.jsx
        ├── PageHeader.jsx
        ├── ErrorBoundary.jsx
        └── LoadingSpinner.jsx
```

---

## Part 8 — Final Phase 0 Sign-off Report

### Checklist

| Item | v1.0 | v1.1 | Notes |
|---|---|---|---|
| Feature list frozen | ✅ | ✅ | No scope creep |
| Use cases documented | ✅ | ✅ | UC-010 updated with login events |
| Architecture diagram | ✅ | ✅ | Corrected to modular monolith |
| Database schema complete | ✅ | ✅ | 16 tables, all indexed |
| API endpoints specified | ✅ | ✅ | +6 new endpoints |
| Non-functional requirements | ✅ | ✅ | Unchanged |
| Folder structure | ✅ | ✅ | Module-per-domain structure |
| Evidence viewer metadata | ❌ | ✅ | highlight_coords, section_title added |
| Sync observability | ❌ | ✅ | source_sync_logs added |
| Document versioning | ❌ | ✅ | document_versions added |
| Explainable AI trail | ❌ | ✅ | evidence_references added |
| Configurable loan rules | ❌ | ✅ | loan_policy_rules added |
| Security event log | ❌ | ✅ | login_events added |
| Multi-bank isolation | ❌ | ✅ | tenant_id added (nullable, v1 default=1) |
| Embedding model versioning | ❌ | ✅ | embedding_model_version on chunks |

**16 / 16 items complete. Phase 0 approved.**

---

### Principal Architect notes for Phase 1

1. Implement `TenantContext` and the JWT tenant claim in Phase 1 itself — it is far
   cheaper to thread it through from the first commit than to add it later.

2. The `login_events` table must be written to by `AuthService` in Phase 1. Do not
   defer this to Phase 10 (admin dashboard). The data must exist before the UI is built.

3. Flyway migrations must be numbered sequentially. V1 creates all base tables. Any
   Phase 0 review changes that alter existing tables go into V2. Phase 1 additions
   (if any schema work is needed) start at V3. Never re-order or edit committed
   migrations.

4. The `LoanPolicyRuleService` in Phase 8 must query rules as:
   `WHERE loan_type = ? AND status = 'ACTIVE' AND effective_from <= CURDATE()
   AND (effective_to IS NULL OR effective_to >= CURDATE())`
   This is the only correct query for date-bounded rules.

5. All `@Scheduled` sync jobs must write to `source_sync_logs` within a
   try-finally block — the log record must be closed even if the job throws.

**Ready to proceed to Phase 1 — Authentication Module.**
