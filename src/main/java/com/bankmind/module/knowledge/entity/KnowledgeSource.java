package com.bankmind.module.knowledge.entity;

import com.bankmind.util.TenantContext;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge_sources", indexes = {
    @Index(name = "idx_ks_tenant", columnList = "tenant_id"),
    @Index(name = "idx_ks_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnowledgeSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false)
    private String name;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @Column(name = "location_path", nullable = false, length = 2048)
    private String locationPath;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "cron_expression", length = 100)
    private String cronExpression;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (tenantId == null) {
            tenantId = TenantContext.getTenantId() != null ? TenantContext.getTenantId() : 1L;
        }
        if (active == null) {
            active = true;
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
