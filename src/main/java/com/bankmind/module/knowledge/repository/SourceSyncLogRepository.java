package com.bankmind.module.knowledge.repository;

import com.bankmind.module.knowledge.entity.SourceSyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SourceSyncLogRepository extends JpaRepository<SourceSyncLog, Long> {
    List<SourceSyncLog> findBySourceId(Long sourceId);
    Optional<SourceSyncLog> findTopBySourceIdOrderByStartedAtDesc(Long sourceId);
}
