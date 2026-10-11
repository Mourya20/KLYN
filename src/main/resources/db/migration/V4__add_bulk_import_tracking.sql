-- KLYN Stage 3 bulk import tracking migration.
-- Run after V3 against an existing MySQL database.

ALTER TABLE instruments
    MODIFY COLUMN provider_symbol VARCHAR(64) NULL,
    ADD COLUMN provider_mapping_status VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED',
    ADD COLUMN provider_mapping_note VARCHAR(255) NULL;

UPDATE instruments
SET provider_mapping_status = 'UNVERIFIED',
    provider_mapping_note = 'Legacy mapping requires provider response verification.'
WHERE provider_mapping_status = 'UNVERIFIED';

CREATE TABLE bulk_import_jobs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    exchange_code VARCHAR(20) NOT NULL,
    source_file_name VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    total_instruments INT NOT NULL DEFAULT 0,
    completed INT NOT NULL DEFAULT 0,
    pending INT NOT NULL DEFAULT 0,
    unsupported INT NOT NULL DEFAULT 0,
    unverified INT NOT NULL DEFAULT 0,
    failed INT NOT NULL DEFAULT 0,
    records_saved INT NOT NULL DEFAULT 0,
    latest_trading_date DATE NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE bulk_import_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    job_id BIGINT NOT NULL,
    instrument_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    records_saved INT NOT NULL DEFAULT 0,
    latest_trading_date DATE NULL,
    error_message VARCHAR(500) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_bulk_import_job_instrument UNIQUE (job_id, instrument_id),
    CONSTRAINT fk_bulk_item_job FOREIGN KEY (job_id) REFERENCES bulk_import_jobs (id),
    CONSTRAINT fk_bulk_item_instrument FOREIGN KEY (instrument_id) REFERENCES instruments (id)
);
