-- KLYN Stage 3 identity migration.
-- Run this script once against an existing MySQL database before starting the
-- application with the instrument-based entity model. It preserves existing rows.

CREATE TABLE instruments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_id BIGINT NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    exchange_code VARCHAR(20) NOT NULL,
    trading_currency VARCHAR(10) NOT NULL,
    provider_symbol VARCHAR(64) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_instrument_exchange_symbol UNIQUE (exchange_code, symbol),
    CONSTRAINT fk_instrument_company FOREIGN KEY (company_id) REFERENCES companies (id),
    INDEX idx_instrument_company (company_id),
    INDEX idx_instrument_symbol (symbol)
);

INSERT INTO instruments (company_id, symbol, exchange_code, trading_currency, provider_symbol, active, created_at, updated_at)
SELECT c.id, c.symbol, c.exchange_code, c.currency, c.symbol, c.active, c.created_at, c.updated_at
FROM companies c
LEFT JOIN instruments i ON i.company_id = c.id
WHERE i.id IS NULL;

ALTER TABLE market_prices
    ADD COLUMN instrument_id BIGINT NULL,
    ADD COLUMN data_source VARCHAR(40) NOT NULL DEFAULT 'LEGACY_UNSPECIFIED',
    ADD COLUMN price_adjustment VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    ADD COLUMN provider_time_zone VARCHAR(64) NULL;

UPDATE market_prices mp
JOIN instruments i ON i.company_id = mp.company_id
SET mp.instrument_id = i.id;

ALTER TABLE market_prices
    MODIFY COLUMN instrument_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_market_price_instrument FOREIGN KEY (instrument_id) REFERENCES instruments (id);

ALTER TABLE market_prices
    DROP FOREIGN KEY fk_market_price_company,
    DROP INDEX uk_company_trading_date,
    DROP INDEX idx_market_price_company_date,
    ADD CONSTRAINT uk_instrument_trading_date UNIQUE (instrument_id, trading_date),
    ADD INDEX idx_market_price_instrument_date (instrument_id, trading_date),
    DROP COLUMN company_id;

-- Existing prices are deliberately marked UNKNOWN rather than being described
-- as adjusted or provider-verified. New imports write explicit source metadata.
