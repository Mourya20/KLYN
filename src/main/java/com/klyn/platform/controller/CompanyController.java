package com.klyn.platform.controller;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.klyn.platform.dto.CompanyCreateRequest;
import com.klyn.platform.dto.CompanyResponse;
import com.klyn.platform.dto.MarketPriceResponse;
import com.klyn.platform.service.CompanyService;
import com.klyn.platform.service.MarketPriceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@Validated
@RestController
@RequestMapping("/api")
public class CompanyController {

	private final CompanyService companyService;
	private final MarketPriceService marketPriceService;

	public CompanyController(CompanyService companyService, MarketPriceService marketPriceService) {
		this.companyService = companyService;
		this.marketPriceService = marketPriceService;
	}

	@GetMapping("/companies")
	@Operation(summary = "List companies", description = "List active companies with pagination.")
	@SecurityRequirement(name = "bearerAuth")
	public Page<CompanyResponse> listCompanies(
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) int size) {
		return companyService.listCompanies(page, size);
	}

	@GetMapping("/companies/{id}")
	@Operation(summary = "Fetch company by id")
	@SecurityRequirement(name = "bearerAuth")
	public CompanyResponse getCompany(@PathVariable Long id) {
		return companyService.getCompany(id);
	}

	@GetMapping("/companies/search")
	@Operation(summary = "Search companies by name or symbol")
	@SecurityRequirement(name = "bearerAuth")
	public Page<CompanyResponse> searchCompanies(
			@RequestParam String q,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) int size) {
		return companyService.searchCompanies(q, page, size);
	}

	@PostMapping("/companies")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Register a company/instrument", description = "Creates a configured company record for a provider-trusted symbol and exchange.")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<CompanyResponse> createCompany(@Valid @RequestBody CompanyCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(companyService.createCompany(request));
	}

	@GetMapping("/companies/{id}/prices")
	@Operation(summary = "Retrieve historical OHLCV prices for a company")
	@SecurityRequirement(name = "bearerAuth")
	public Page<MarketPriceResponse> getCompanyPrices(
			@PathVariable Long id,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Parameter(description = "Start date in ISO format, e.g. 2026-10-01") LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Parameter(description = "End date in ISO format, e.g. 2026-10-31") LocalDate to,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) int size) {
		return marketPriceService.getHistoricalPrices(id, from, to, page, size);
	}

	@GetMapping("/companies/{id}/latest-price")
	@Operation(summary = "Retrieve the latest stored price for a company")
	@SecurityRequirement(name = "bearerAuth")
	public MarketPriceResponse getLatestPrice(@PathVariable Long id) {
		return marketPriceService.getLatestPrice(id);
	}
}
