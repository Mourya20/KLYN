package com.klyn.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyCreateRequest(
		@NotBlank @Size(max = 32) String symbol,
		@NotBlank @Size(max = 200) String name,
		@NotBlank @Size(max = 20) String exchangeCode,
		@NotBlank @Size(max = 10) String currency,
		@Size(max = 100) String sector,
		@Size(max = 64) String providerSymbol,
		Boolean active) {
}
