-- KLYN Stage 3 status compatibility migration.
-- Run after V4 when V4 has already been applied to an existing MySQL database.

ALTER TABLE bulk_import_jobs
    MODIFY COLUMN status VARCHAR(30) NOT NULL;