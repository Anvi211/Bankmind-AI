package com.bankmind.module.knowledge.repository;

import com.bankmind.module.knowledge.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findBySourceId(Long sourceId);
}
