package com.klyn.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MarketDataImportRequest(
		@NotBlank @Size(max = 32) String symbol,
		@NotBlank @Size(max = 20) String exchangeCode) {
}
