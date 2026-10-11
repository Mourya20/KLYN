package com.klyn.platform.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.klyn.platform.dto.MarketDataImportRequest;
import com.klyn.platform.dto.MarketDataImportResponse;
import com.klyn.platform.dto.BulkImportStatusResponse;
import com.klyn.platform.service.BulkMarketDataImportService;
import com.klyn.platform.service.MarketPriceService;
import com.klyn.platform.service.NseUniverseImportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/market-data")
public class MarketDataController {

	private final MarketPriceService marketPriceService;
	private final NseUniverseImportService nseUniverseImportService;
	private final BulkMarketDataImportService bulkMarketDataImportService;

	public MarketDataController(MarketPriceService marketPriceService, NseUniverseImportService nseUniverseImportService,
			BulkMarketDataImportService bulkMarketDataImportService) {
		this.marketPriceService = marketPriceService;
		this.nseUniverseImportService = nseUniverseImportService;
		this.bulkMarketDataImportService = bulkMarketDataImportService;
	}

	@PostMapping("/import")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Import daily OHLCV data for a configured company symbol")
	@SecurityRequirement(name = "bearerAuth")
	public MarketDataImportResponse importData(@Valid @RequestBody MarketDataImportRequest request) {
		return marketPriceService.importDailyData(request);
	}

	@PostMapping(value = "/bulk-import/nse", consumes = "multipart/form-data")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Create a bulk import job from an official NSE equity listing CSV")
	@SecurityRequirement(name = "bearerAuth")
	public BulkImportStatusResponse createNseBulkImport(@RequestParam("file") MultipartFile file) {
		return nseUniverseImportService.createNseJob(file);
	}

	@PostMapping("/bulk-import/{jobId}/run")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Process a controlled batch of mapped instruments")
	@SecurityRequirement(name = "bearerAuth")
	public BulkImportStatusResponse runBulkImport(@PathVariable Long jobId,
			@RequestParam(required = false) Integer limit,
			@RequestParam(defaultValue = "false") boolean retryFailed) {
		return bulkMarketDataImportService.run(jobId, limit, retryFailed);
	}

	@GetMapping("/bulk-import/{jobId}")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Get bulk import progress")
	@SecurityRequirement(name = "bearerAuth")
	public BulkImportStatusResponse getBulkImportStatus(@PathVariable Long jobId) {
		return bulkMarketDataImportService.status(jobId);
	}
}
