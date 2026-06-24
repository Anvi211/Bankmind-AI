package com.bankmind.module.knowledge.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_chunks", indexes = {
    @Index(name = "idx_chunks_doc", columnList = "document_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "chunk_text", nullable = false, columnDefinition = "TEXT")
    private String chunkText;

    @Column(name = "page_number", nullable = false)
    private Integer pageNumber;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(name = "section_title", length = 500)
    private String sectionTitle;

    @Column(name = "char_offset_start", nullable = false)
    private Integer charOffsetStart;

    @Column(name = "char_offset_end", nullable = false)
    private Integer charOffsetEnd;

    @Column(name = "highlight_coords", columnDefinition = "json")
    private String highlightCoords;

    @Column(name = "token_count", nullable = false)
    private Integer tokenCount;

    @Column(name = "embedding_model_version", nullable = false, length = 100)
    private String embeddingModelVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (embeddingModelVersion == null) {
            embeddingModelVersion = "text-embedding-3-small-v1";
        }
    }
}
