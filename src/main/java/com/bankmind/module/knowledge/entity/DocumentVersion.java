package com.bankmind.module.knowledge.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_versions", uniqueConstraints = {
    @UniqueConstraint(name = "ux_dv_doc_version", columnNames = {"document_id", "version_number"})
}, indexes = {
    @Index(name = "idx_dv_doc", columnList = "document_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "file_path", nullable = false, columnDefinition = "TEXT")
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "ingestion_status", nullable = false)
    private IngestionStatus ingestionStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum IngestionStatus {
        PENDING, PROCESSING, COMPLETE, FAILED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (ingestionStatus == null) {
            ingestionStatus = IngestionStatus.PENDING;
        }
    }
}
