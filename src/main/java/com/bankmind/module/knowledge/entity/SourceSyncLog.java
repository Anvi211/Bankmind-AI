package com.bankmind.module.knowledge.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "source_sync_logs", indexes = {
    @Index(name = "idx_ssl_source", columnList = "source_id, started_at"),
    @Index(name = "idx_ssl_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SourceSyncLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private KnowledgeSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "triggered_by", nullable = false)
    private TriggeredBy triggeredBy;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "documents_found", nullable = false)
    private Integer documentsFound;

    @Column(name = "documents_processed", nullable = false)
    private Integer documentsProcessed;

    @Column(name = "documents_failed", nullable = false)
    private Integer documentsFailed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SyncStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public enum TriggeredBy {
        SCHEDULED, MANUAL
    }

    public enum SyncStatus {
        RUNNING, COMPLETE, PARTIAL, FAILED
    }
}
