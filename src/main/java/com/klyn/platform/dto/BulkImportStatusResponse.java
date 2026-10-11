package com.klyn.platform.dto;

import java.time.LocalDate;

import com.klyn.platform.entity.BulkImportJob;

public record BulkImportStatusResponse(Long jobId, String exchangeCode, String sourceFileName, String status,
		int totalInstruments, int completed, int pending, int unsupported, int unverified, int failed, int recordsSaved,
		LocalDate mostRecentImportedTradingDate) {

	public static BulkImportStatusResponse from(BulkImportJob job) {
		return new BulkImportStatusResponse(job.getId(), job.getExchangeCode(), job.getSourceFileName(),
				job.getStatus().name(), job.getTotalInstruments(), job.getCompleted(), job.getPending(),
				job.getUnsupported(), job.getUnverified(), job.getFailed(), job.getRecordsSaved(), job.getLatestTradingDate());
	}
}
