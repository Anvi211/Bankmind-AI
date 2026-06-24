-- BankMind AI — V3__knowledge_baseline.sql
-- Phase 2 baseline verification and seeding

INSERT INTO knowledge_sources (id, tenant_id, name, source_type, location_path, active, cron_expression, created_at, updated_at)
VALUES (1, 1, 'Default System Uploads', 'UPLOAD', 'uploads', TRUE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE name = 'Default System Uploads';
