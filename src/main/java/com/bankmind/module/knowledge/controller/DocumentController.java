package com.bankmind.module.knowledge.controller;

import com.bankmind.module.knowledge.dto.DocumentUploadResponse;
import com.bankmind.module.knowledge.service.DocumentIngestionService;
import com.bankmind.module.knowledge.service.EmbeddingService;
import com.bankmind.module.ai.service.QdrantService;
import com.bankmind.module.ai.dto.QdrantPoint;
import com.bankmind.module.knowledge.repository.DocumentRepository;
import com.bankmind.module.knowledge.repository.DocumentChunkRepository;
import com.bankmind.module.knowledge.entity.Document;
import com.bankmind.module.knowledge.entity.DocumentChunk;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentIngestionService documentIngestionService;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final EmbeddingService embeddingService;
    private final QdrantService qdrantService;

    public DocumentController(DocumentIngestionService documentIngestionService,
                              DocumentRepository documentRepository,
                              DocumentChunkRepository documentChunkRepository,
                              EmbeddingService embeddingService,
                              QdrantService qdrantService) {
        this.documentIngestionService = documentIngestionService;
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.embeddingService = embeddingService;
        this.qdrantService = qdrantService;
    }

    @GetMapping("/test/qdrant")
    public ResponseEntity<Map<String, Object>> testQdrant() {
        Map<String, Object> result = new HashMap<>();
        String collectionName = "bankmind_documents";

        try {
            // 1. Existence check & creation
            boolean existsBefore = qdrantService.collectionExists(collectionName);
            result.put("collectionExistsBefore", existsBefore);

            if (!existsBefore) {
                qdrantService.createCollection(collectionName, embeddingService.getDimension());
            }
            boolean existsAfter = qdrantService.collectionExists(collectionName);
            result.put("collectionExistsAfter", existsAfter);

            // 2. Generate vector and metadata
            String text = "BankMind loan approval policy";
            float[] vector = embeddingService.embed(text);

            Map<String, Object> payload = new HashMap<>();
            payload.put("title", "Test Policy");
            payload.put("tenantId", 1L);
            payload.put("text", text);

            long pointId = 999L;
            QdrantPoint point = new QdrantPoint(pointId, vector, payload);

            // 3. Upsert
            qdrantService.upsertPoint(collectionName, point);
            result.put("upsertedSuccess", true);

            // 4. Retrieve
            Optional<QdrantPoint> retrievedOpt = qdrantService.retrievePoint(collectionName, pointId);
            boolean found = retrievedOpt.isPresent();
            result.put("retrievedFound", found);

            if (found) {
                QdrantPoint retrieved = retrievedOpt.get();
                result.put("retrievedId", retrieved.getId());
                result.put("retrievedPayload", retrieved.getPayload());
                result.put("retrievedVectorDimension", retrieved.getVector().length);

                // Check first 10 values
                java.util.List<Float> origFirst10 = new java.util.ArrayList<>();
                java.util.List<Float> retFirst10 = new java.util.ArrayList<>();
                for (int i = 0; i < Math.min(10, vector.length); i++) {
                    origFirst10.add(vector[i]);
                    retFirst10.add(retrieved.getVector()[i]);
                }
                result.put("originalFirst10", origFirst10);
                result.put("retrievedFirst10", retFirst10);

                // Compare vectors
                boolean vectorMatch = true;
                if (vector.length != retrieved.getVector().length) {
                    vectorMatch = false;
                } else {
                    for (int i = 0; i < vector.length; i++) {
                        if (Float.compare(vector[i], retrieved.getVector()[i]) != 0) {
                            vectorMatch = false;
                            break;
                        }
                    }
                }
                result.put("vectorMatch", vectorMatch);
            }
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/test/embed")
    public ResponseEntity<Map<String, Object>> testEmbed(@RequestParam("text") String text) {
        Map<String, Object> result = new HashMap<>();

        float[] vec1 = embeddingService.embed(text);
        float[] vec2 = embeddingService.embed(text);
        float[] vecDifferent = embeddingService.embed(text + " suffix different");

        result.put("provider", embeddingService.getProviderName());
        result.put("dimension", vec1.length);

        // Get first 10 values
        java.util.List<Float> first10Run1 = new java.util.ArrayList<>();
        java.util.List<Float> first10Run2 = new java.util.ArrayList<>();
        java.util.List<Float> first10Different = new java.util.ArrayList<>();
        for (int i = 0; i < Math.min(10, vec1.length); i++) {
            first10Run1.add(vec1[i]);
            first10Run2.add(vec2[i]);
            first10Different.add(vecDifferent[i]);
        }
        result.put("first10ValuesRun1", first10Run1);
        result.put("first10ValuesRun2", first10Run2);
        result.put("first10ValuesDifferent", first10Different);

        // Compare runs
        boolean runsMatch = true;
        if (vec1.length != vec2.length) {
            runsMatch = false;
        } else {
            for (int i = 0; i < vec1.length; i++) {
                if (Float.compare(vec1[i], vec2[i]) != 0) {
                    runsMatch = false;
                    break;
                }
            }
        }
        result.put("runsMatch", runsMatch);

        // Compare different
        boolean differentDiffers = false;
        if (vec1.length != vecDifferent.length) {
            differentDiffers = true;
        } else {
            for (int i = 0; i < vec1.length; i++) {
                if (Float.compare(vec1[i], vecDifferent[i]) != 0) {
                    differentDiffers = true;
                    break;
                }
            }
        }
        result.put("differentDiffers", differentDiffers);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/upload")
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            @RequestParam("sourceId") Long sourceId,
            @RequestParam("file") MultipartFile file) throws Exception {

        DocumentUploadResponse response = documentIngestionService.ingestDocument(sourceId, file);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/test/chunks")
    public ResponseEntity<Map<String, Object>> getChunks() {
        Map<String, Object> result = new HashMap<>();
        List<Document> documents = documentRepository.findAll();
        List<DocumentChunk> chunks = documentChunkRepository.findAll();

        result.put("documentCount", documents.size());
        result.put("chunkCount", chunks.size());

        List<Map<String, Object>> documentsInfo = documents.stream().map(doc -> {
            Map<String, Object> docMap = new HashMap<>();
            docMap.put("id", doc.getId());
            docMap.put("title", doc.getTitle());
            docMap.put("filePath", doc.getFilePath());
            return docMap;
        }).collect(Collectors.toList());
        result.put("documents", documentsInfo);

        List<Map<String, Object>> chunksInfo = chunks.stream().map(c -> {
            Map<String, Object> cMap = new HashMap<>();
            cMap.put("id", c.getId());
            cMap.put("documentId", c.getDocument().getId());
            cMap.put("chunkText", c.getChunkText());
            cMap.put("pageNumber", c.getPageNumber());
            cMap.put("chunkIndex", c.getChunkIndex());
            cMap.put("charOffsetStart", c.getCharOffsetStart());
            cMap.put("charOffsetEnd", c.getCharOffsetEnd());
            cMap.put("tokenCount", c.getTokenCount());
            return cMap;
        }).collect(Collectors.toList());
        result.put("chunks", chunksInfo);

        return ResponseEntity.ok(result);
    }
    @GetMapping("/test/qdrant-point/{id}")
public ResponseEntity<?> getPoint(@PathVariable Long id) {

    return ResponseEntity.ok(
            qdrantService.retrievePoint(
                    "bankmind_documents",
                    id
            )
    );
}
}
