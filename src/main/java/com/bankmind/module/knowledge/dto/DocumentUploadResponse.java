package com.bankmind.module.knowledge.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadResponse {
    private Long documentId;
    private String title;
    private String filePath;
    private Integer chunksCreated;
}
