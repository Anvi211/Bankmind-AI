-- BankMind AI — V4__add_chunk_index.sql
-- Add chunk_index to document_chunks to support sequential chunk tracking

ALTER TABLE document_chunks
    ADD COLUMN chunk_index INT NOT NULL AFTER page_number;
