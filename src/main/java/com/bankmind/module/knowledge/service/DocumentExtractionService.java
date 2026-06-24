package com.bankmind.module.knowledge.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentExtractionService {

    public static class ExtractedPage {
        private final int pageNumber;
        private final String text;

        public ExtractedPage(int pageNumber, String text) {
            this.pageNumber = pageNumber;
            this.text = text;
        }

        public int getPageNumber() {
            return pageNumber;
        }

        public String getText() {
            return text;
        }
    }

    public List<ExtractedPage> extractTextPageByPage(InputStream inputStream, String contentType) throws Exception {
        List<ExtractedPage> pages = new ArrayList<>();
        byte[] bytes = inputStream.readAllBytes();

        if ("application/pdf".equalsIgnoreCase(contentType) || (contentType == null && isPdfHeader(bytes))) {
            // Read using PDFBox Loader
            try (PDDocument document = Loader.loadPDF(bytes)) {
                int totalPages = document.getNumberOfPages();
                PDFTextStripper stripper = new PDFTextStripper();
                for (int i = 1; i <= totalPages; i++) {
                    stripper.setStartPage(i);
                    stripper.setEndPage(i);
                    String pageText = stripper.getText(document);
                    pages.add(new ExtractedPage(i, pageText));
                }
            }
        } else {
            // General parsing via Tika
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();
            AutoDetectParser parser = new AutoDetectParser();

            try (InputStream bis = new ByteArrayInputStream(bytes)) {
                parser.parse(bis, handler, metadata, context);
            }
            String fullText = handler.toString();
            pages.add(new ExtractedPage(1, fullText));
        }

        return pages;
    }

    private boolean isPdfHeader(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return false;
        }
        return bytes[0] == 0x25 && // %
               bytes[1] == 0x50 && // P
               bytes[2] == 0x44 && // D
               bytes[3] == 0x46;   // F
    }
}
