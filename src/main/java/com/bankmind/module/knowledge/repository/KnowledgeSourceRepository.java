package com.bankmind.module.knowledge.repository;

import com.bankmind.module.knowledge.entity.KnowledgeSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeSourceRepository extends JpaRepository<KnowledgeSource, Long> {
    List<KnowledgeSource> findByTenantId(Long tenantId);
    List<KnowledgeSource> findByTenantIdAndActive(Long tenantId, Boolean active);
}
