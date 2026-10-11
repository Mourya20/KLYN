# KLYN

KLYN is a Spring Boot backend using MySQL, Spring Security, JWT, OpenAPI, and a provider-backed market-data foundation.

## Configuration

Copy the variable names from `.env.example` into your operating-system environment, IDE run configuration, or deployment secret store. Spring Boot does not automatically load a local `.env` file.

Required variables:

```text
DB_USERNAME
DB_PASSWORD
JWT_SECRET
ALPHA_VANTAGE_API_KEY
```

Optional variable:

```text
JWT_EXPIRATION=3600000

```

`JWT_SECRET` must be at least 32 characters long. Never commit real credentials or a `.env` file.

## Authentication API

```text
POST /api/auth/register
POST /api/auth/login
GET  /api/users/me       (Authorization: Bearer <JWT>)
```

Swagger UI is available at `/swagger-ui.html` when the application is running.

## Market Data API

All market-data endpoints require a JWT. Market-data ingestion is restricted to users with the `ADMIN` role.

```text
GET  /api/companies
GET  /api/companies/search?q=reliance
GET  /api/companies/{id}/prices
GET  /api/companies/{id}/latest-price
POST /api/companies                         (ADMIN)
POST /api/market-data/import                 (ADMIN)
POST /api/market-data/bulk-import/nse        (ADMIN, multipart file upload)
POST /api/market-data/bulk-import/{jobId}/run (ADMIN, optional `limit` and `retryFailed`)
GET  /api/market-data/bulk-import/{jobId}    (ADMIN)
```

`Company` represents an issuer. `Instrument` represents a symbol listing on a specific exchange, and a company can have multiple instruments. Prices are unique per instrument and trading date.

The Alpha Vantage provider uses `alpha.vantage.output-size=compact` by default, which returns only the latest 100 daily records. Set it to `full` when the provider plan supports full history; this does not guarantee unlimited history. `alpha.vantage.connect-timeout-ms` and `alpha.vantage.response-timeout-ms` control HTTP connection and response timeouts.

Alpha Vantage metadata such as `US/Eastern` is stored as provider metadata only. It is not treated as proof of an Indian exchange, currency, or timezone mapping. The configured instrument's `providerSymbol` is the explicit symbol mapping used for ingestion, and provider exchange verification is not claimed when Alpha Vantage does not return one.

## Existing Database Migration

For an existing MySQL database, run [V3__split_companies_into_instruments.sql](src/main/resources/db/migration/V3__split_companies_into_instruments.sql) once before starting the updated application. The script creates one instrument from each legacy company listing, backfills `market_prices.instrument_id`, preserves existing prices, and marks legacy source/adjustment semantics as `LEGACY_UNSPECIFIED` and `UNKNOWN`. Back up the database and review the generated mapping before execution. Do not run it more than once.

New imports are idempotent: repeated identical records are skipped, while corrected records for the same instrument/date update the stored row.

## NSE Bulk Import

Download the current official NSE equity listing CSV from the NSE India reports/download area and upload that file; do not treat this workflow as a claim that every NSE listing is supported by Alpha Vantage. The parser accepts the official `SYMBOL`, `NAME OF COMPANY`, and `SERIES` columns and imports only `EQ` rows. It also accepts an optional `PROVIDER SYMBOL` column from a reviewed mapping file. Rows without a provider mapping remain `unverified` and are not requested from Alpha Vantage. Rows explicitly marked `UNSUPPORTED` remain visible in the job summary.

1. Create a job with an admin JWT:

```cmd
curl -X POST http://localhost:8080/api/market-data/bulk-import/nse -H "Authorization: Bearer <ADMIN_JWT>" -F "file=@nse-equity-list.csv"
```

2. Run at most the configured number of provider requests. Set `retryFailed=true` to retry failed items:

```cmd
curl -X POST "http://localhost:8080/api/market-data/bulk-import/<JOB_ID>/run?limit=5" -H "Authorization: Bearer <ADMIN_JWT>"
curl -X POST "http://localhost:8080/api/market-data/bulk-import/<JOB_ID>/run?limit=5&retryFailed=true" -H "Authorization: Bearer <ADMIN_JWT>"
```

3. Poll the status endpoint. A rate limit changes the job to `PAUSED` and leaves the current item `pending`; rerun it later. Jobs with only `unverified` or `unsupported` items finish as `COMPLETED_WITH_SKIPPED`. Successful provider responses set the instrument mapping to `RESPONDING`, not `VERIFIED`; exchange and currency verification requires a separately reviewed mapping. Completed instruments are not requested again. The default controls are `market-data.batch.max-requests-per-run=5` and `market-data.batch.delay-between-requests-ms=12000`, matching Alpha Vantage's commonly documented free-tier limit. Confirm the quota for the configured account before changing them.

For an existing database, run [V4__add_bulk_import_tracking.sql](src/main/resources/db/migration/V4__add_bulk_import_tracking.sql) after V3. V4 adds provider mapping state and the resumable job/item ledger. If V4 was already applied, run [V5__expand_stage3_status_values.sql](src/main/resources/db/migration/V5__expand_stage3_status_values.sql) before using the new job status. These SQL files are manual migrations because this project does not install a migration runner.

Never place real API keys, database passwords, JWT secrets, or `.env` files in source control. `.env.example` contains placeholders only.

## Verification

```cmd
mvnw.cmd clean test
mvnw.cmd clean package
```