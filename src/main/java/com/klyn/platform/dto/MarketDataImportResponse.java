package com.klyn.platform.dto;

public record MarketDataImportResponse(String symbol, String exchangeCode, int recordsProcessed, int recordsInserted,
		int recordsUpdated, int recordsSkipped, String status, String message) {
}
