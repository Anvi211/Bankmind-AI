package com.bankmind.module.knowledge.repository;

import com.bankmind.module.knowledge.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {
    List<DocumentChunk> findByDocumentId(Long documentId);
    void deleteByDocumentId(Long documentId);
    List<DocumentChunk> findByIdIn(List<Long> ids);
}
