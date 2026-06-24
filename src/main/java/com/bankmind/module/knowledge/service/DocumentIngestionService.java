package com.bankmind.module.knowledge.service;

import com.bankmind.module.knowledge.entity.*;
import com.bankmind.module.knowledge.repository.*;
import com.bankmind.module.knowledge.dto.DocumentUploadResponse;
import com.bankmind.exception.ResourceNotFoundException;

import com.bankmind.module.knowledge.service.EmbeddingService;
import com.bankmind.module.ai.service.QdrantService;
import com.bankmind.module.ai.dto.QdrantPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.bankmind.module.ai.dto.QdrantPoint;
import com.bankmind.module.ai.service.QdrantService;
import com.bankmind.module.knowledge.service.EmbeddingService;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DocumentIngestionService {

    private final KnowledgeSourceRepository knowledgeSourceRepository;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final SourceSyncLogRepository sourceSyncLogRepository;
    private final DocumentExtractionService documentExtractionService;
    private final TextChunkingService textChunkingService;

    private final EmbeddingService embeddingService;
    private final QdrantService qdrantService;

    private static final String UPLOAD_DIR = "uploads";

    public DocumentIngestionService(
        KnowledgeSourceRepository knowledgeSourceRepository,
        DocumentRepository documentRepository,
        DocumentChunkRepository documentChunkRepository,
        SourceSyncLogRepository sourceSyncLogRepository,
        DocumentExtractionService documentExtractionService,
        TextChunkingService textChunkingService,
        EmbeddingService embeddingService,
        QdrantService qdrantService) {

    this.knowledgeSourceRepository = knowledgeSourceRepository;
    this.documentRepository = documentRepository;
    this.documentChunkRepository = documentChunkRepository;
    this.sourceSyncLogRepository = sourceSyncLogRepository;
    this.documentExtractionService = documentExtractionService;
    this.textChunkingService = textChunkingService;

    this.embeddingService = embeddingService;
    this.qdrantService = qdrantService;
}

    @Transactional
    public DocumentUploadResponse ingestDocument(Long sourceId, MultipartFile file) throws Exception {
        KnowledgeSource source = knowledgeSourceRepository.findById(sourceId)
                .orElseThrow(() -> new ResourceNotFoundException("KnowledgeSource not found with id: " + sourceId));

        Long currentTenantId = com.bankmind.util.TenantContext.getTenantId();
        if (currentTenantId != null && !currentTenantId.equals(source.getTenantId())) {
            throw new com.bankmind.exception.TenantAccessDeniedException("Access denied to knowledge source tenant: " + source.getTenantId());
        }

        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            originalFilename = "unnamed_document";
        }
        String uniqueFilename = UUID.randomUUID().toString() + "_" + originalFilename;
        Path filePath = uploadPath.resolve(uniqueFilename);

        try (InputStream is = file.getInputStream()) {
            Files.copy(is, filePath, StandardCopyOption.REPLACE_EXISTING);
        }

        SourceSyncLog syncLog = SourceSyncLog.builder()
                .source(source)
                .triggeredBy(SourceSyncLog.TriggeredBy.MANUAL)
                .startedAt(LocalDateTime.now())
                .documentsFound(1)
                .documentsProcessed(0)
                .documentsFailed(0)
                .status(SourceSyncLog.SyncStatus.RUNNING)
                .build();
        syncLog = sourceSyncLogRepository.save(syncLog);

        log.info("Starting ingestion for file: {} with knowledge source ID: {}", file.getOriginalFilename(), sourceId);
        try {
            Document document = Document.builder()
                    .source(source)
                    .title(originalFilename)
                    .filePath(filePath.toString().replace("\\", "/")) // normalize to forward slashes
                    .build();
            document = documentRepository.save(document);

            log.info("Created document record with ID: {} in MySQL", document.getId());

            List<DocumentExtractionService.ExtractedPage> pages;
            try (InputStream is = Files.newInputStream(filePath)) {
                pages = documentExtractionService.extractTextPageByPage(is, file.getContentType());
            }

            log.info("Extracted {} page(s) from document using extraction service", pages.size());

            List<DocumentChunk> chunks = textChunkingService.chunkDocument(document, pages);
            documentChunkRepository.saveAll(chunks);

            log.info("Generated and saved {} chunks for document ID: {}", chunks.size(), document.getId());

            List<QdrantPoint> qdrantPoints = new ArrayList<>();

for (DocumentChunk chunk : chunks) {

    float[] embedding =
            embeddingService.embed(chunk.getChunkText());

    Map<String, Object> payload = new HashMap<>();
    payload.put("chunkId", chunk.getId());
    payload.put("documentId", document.getId());
    payload.put("pageNumber", chunk.getPageNumber());
    payload.put("text", chunk.getChunkText());

    qdrantPoints.add(
            new QdrantPoint(
                    chunk.getId(),
                    embedding,
                    payload
            )
    );
}

qdrantService.upsertPoints(
        "bankmind_documents",
        qdrantPoints
);

log.info(
        "Indexed {} chunks into Qdrant",
        qdrantPoints.size()
);


            syncLog.setDocumentsProcessed(1);
            syncLog.setStatus(SourceSyncLog.SyncStatus.COMPLETE);
            syncLog.setCompletedAt(LocalDateTime.now());
            sourceSyncLogRepository.save(syncLog);

            log.info("Successfully completed ingestion for document: {} (ID: {})", originalFilename, document.getId());

            return DocumentUploadResponse.builder()
                    .documentId(document.getId())
                    .title(document.getTitle())
                    .filePath(document.getFilePath())
                    .chunksCreated(chunks.size())
                    .build();

        } catch (Exception e) {
            log.error("Failed to ingest document: {} due to error: {}", file.getOriginalFilename(), e.getMessage(), e);
            syncLog.setDocumentsFailed(1);
            syncLog.setStatus(SourceSyncLog.SyncStatus.FAILED);
            syncLog.setErrorMessage(e.getMessage());
            syncLog.setCompletedAt(LocalDateTime.now());
            sourceSyncLogRepository.save(syncLog);
            throw e;
        }
    }
}
