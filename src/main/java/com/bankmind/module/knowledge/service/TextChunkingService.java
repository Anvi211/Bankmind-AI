package com.bankmind.module.knowledge.service;

import com.bankmind.module.knowledge.entity.Document;
import com.bankmind.module.knowledge.entity.DocumentChunk;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TextChunkingService {

    private static final int DEFAULT_CHUNK_SIZE = 1000;
    private static final int DEFAULT_OVERLAP = 200;

    private static final Pattern HEADING_PATTERN = Pattern.compile(
            "^(?:(?:#+\\s+)|(?:Section\\s+\\d+)|(?:Chapter\\s+\\d+)|(?:PART\\s+[I|V|X|L|C|D|M]+)|(?:Clause\\s+\\d+)).*",
            Pattern.CASE_INSENSITIVE
    );

    public List<DocumentChunk> chunkDocument(Document document, List<DocumentExtractionService.ExtractedPage> pages) {
        List<DocumentChunk> chunks = new ArrayList<>();
        int chunkIndex = 0;

        for (DocumentExtractionService.ExtractedPage page : pages) {
            String text = page.getText();
            if (text == null || text.trim().isEmpty()) {
                continue;
            }

            int pageNumber = page.getPageNumber();
            int textLength = text.length();

            if (textLength <= DEFAULT_CHUNK_SIZE) {
                String sectionTitle = findNearestHeading(text, 0);
                int tokens = estimateTokenCount(text);
                chunks.add(DocumentChunk.builder()
                        .document(document)
                        .chunkText(text.trim())
                        .pageNumber(pageNumber)
                        .chunkIndex(chunkIndex++)
                        .sectionTitle(sectionTitle)
                        .charOffsetStart(0)
                        .charOffsetEnd(textLength)
                        .tokenCount(tokens)
                        .embeddingModelVersion("text-embedding-3-small-v1")
                        .build());
            } else {
                int start = 0;
                while (start < textLength) {
                    int end = Math.min(start + DEFAULT_CHUNK_SIZE, textLength);
                    String chunkText = text.substring(start, end);
                    String sectionTitle = findNearestHeading(text, start);
                    int tokens = estimateTokenCount(chunkText);

                    chunks.add(DocumentChunk.builder()
                            .document(document)
                            .chunkText(chunkText.trim())
                            .pageNumber(pageNumber)
                            .chunkIndex(chunkIndex++)
                            .sectionTitle(sectionTitle)
                            .charOffsetStart(start)
                            .charOffsetEnd(end)
                            .tokenCount(tokens)
                            .embeddingModelVersion("text-embedding-3-small-v1")
                            .build());

                    if (end == textLength) {
                        break;
                    }
                    start += (DEFAULT_CHUNK_SIZE - DEFAULT_OVERLAP);
                }
            }
        }

        return chunks;
    }

    private String findNearestHeading(String text, int chunkStartOffset) {
        String beforeText = text.substring(0, chunkStartOffset);
        String[] lines = beforeText.split("\\r?\\n");
        
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            
            Matcher matcher = HEADING_PATTERN.matcher(line);
            if (matcher.matches()) {
                return line.replaceAll("^#+\\s+", "");
            }
            
            if (line.length() < 80 && line.equals(line.toUpperCase()) && line.matches("^[A-Z\\s\\d\\p{Punct}]+$")) {
                return line;
            }
        }
        
        return null;
    }

    private int estimateTokenCount(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        String[] words = text.split("\\s+");
        return (int) Math.ceil(words.length * 1.3);
    }
}
