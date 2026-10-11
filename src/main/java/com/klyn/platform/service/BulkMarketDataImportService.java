package com.klyn.platform.service;

import org.springframework.stereotype.Service;

import com.klyn.platform.config.MarketDataBatchProperties;
import com.klyn.platform.dto.BulkImportStatusResponse;
import com.klyn.platform.dto.MarketDataImportRequest;
import com.klyn.platform.dto.MarketDataImportResponse;
import com.klyn.platform.entity.BulkImportItem;
import com.klyn.platform.entity.BulkImportItemStatus;
import com.klyn.platform.entity.BulkImportJob;
import com.klyn.platform.entity.BulkImportJobStatus;
import com.klyn.platform.entity.Instrument;
import com.klyn.platform.entity.ProviderMappingStatus;
import com.klyn.platform.exception.ResourceNotFoundException;
import com.klyn.platform.marketdata.MarketDataProviderException;
import com.klyn.platform.repository.BulkImportItemRepository;
import com.klyn.platform.repository.BulkImportJobRepository;
import com.klyn.platform.repository.InstrumentRepository;
import com.klyn.platform.repository.MarketPriceRepository;

@Service
public class BulkMarketDataImportService {

	private final BulkImportJobRepository jobRepository;
	private final BulkImportItemRepository itemRepository;
	private final InstrumentRepository instrumentRepository;
	private final MarketPriceRepository marketPriceRepository;
	private final MarketPriceService marketPriceService;
	private final NseUniverseImportService universeImportService;
	private final MarketDataBatchProperties properties;

	public BulkMarketDataImportService(BulkImportJobRepository jobRepository, BulkImportItemRepository itemRepository,
			InstrumentRepository instrumentRepository, MarketPriceRepository marketPriceRepository,
			MarketPriceService marketPriceService, NseUniverseImportService universeImportService,
			MarketDataBatchProperties properties) {
		this.jobRepository = jobRepository;
		this.itemRepository = itemRepository;
		this.instrumentRepository = instrumentRepository;
		this.marketPriceRepository = marketPriceRepository;
		this.marketPriceService = marketPriceService;
		this.universeImportService = universeImportService;
		this.properties = properties;
	}

	public BulkImportStatusResponse run(Long jobId, Integer requestedLimit, boolean retryFailed) {
		BulkImportJob job = findJob(jobId);
		if (job.getStatus() == BulkImportJobStatus.COMPLETED
				|| (job.getStatus() == BulkImportJobStatus.COMPLETED_WITH_SKIPPED && !retryFailed)) {
			return BulkImportStatusResponse.from(job);
		}
		int limit = requestedLimit == null ? properties.getMaxRequestsPerRun() : requestedLimit;
		if (limit < 1) {
			throw new IllegalArgumentException("The batch request limit must be at least 1.");
		}
		limit = Math.min(limit, properties.getMaxRequestsPerRun());
		if (retryFailed) {
			for (BulkImportItem item : itemRepository.findByJobOrderByIdAsc(job)) {
				if (item.getStatus() == BulkImportItemStatus.FAILED) {
					item.setStatus(BulkImportItemStatus.PENDING);
					item.setErrorMessage(null);
					itemRepository.save(item);
				}
			}
		}
		job.setStatus(BulkImportJobStatus.RUNNING);
		jobRepository.save(job);

		int processed = 0;
		boolean rateLimited = false;
		try {
			for (BulkImportItem item : itemRepository.findByJobOrderByIdAsc(job)) {
				if (processed >= limit || item.getStatus() != BulkImportItemStatus.PENDING) {
					continue;
				}
				try {
					if (processed > 0) {
						delayBetweenRequests();
					}
					processed++;
				MarketDataImportResponse result = marketPriceService.importDailyData(
						new MarketDataImportRequest(item.getInstrument().getSymbol(), item.getInstrument().getExchangeCode()));
				Instrument instrument = item.getInstrument();
				instrument.setProviderMappingStatus(ProviderMappingStatus.RESPONDING);
				instrument.setProviderMappingNote("Provider returned daily data for the requested symbol; exchange and currency remain unverified.");
				instrumentRepository.save(instrument);
				item.setStatus(BulkImportItemStatus.COMPLETED);
				item.setRecordsSaved(result.recordsInserted() + result.recordsUpdated());
				marketPriceRepository.findTopByInstrumentOrderByTradingDateDesc(instrument)
						.map(price -> price.getTradingDate()).ifPresent(item::setLatestTradingDate);
				item.setErrorMessage(null);
				itemRepository.save(item);
				} catch (MarketDataProviderException exception) {
					item.setStatus(exception.isRateLimited() ? BulkImportItemStatus.PENDING : BulkImportItemStatus.FAILED);
					item.setErrorMessage(exception.isRateLimited() ? "Provider rate limit reached." : "Provider request failed.");
					itemRepository.save(item);
					if (exception.isRateLimited()) {
						rateLimited = true;
						break;
					}
				} catch (IllegalArgumentException exception) {
					markFailed(item, "Provider data was invalid or did not match the instrument mapping.");
				} catch (RuntimeException exception) {
					markFailed(item, "Provider item processing failed; retry the failed item later.");
				}
			}
		} finally {
			universeImportService.refreshCounts(job);
			if (rateLimited) {
				job.setStatus(BulkImportJobStatus.PAUSED);
			} else if (job.getPending() > 0) {
				job.setStatus(BulkImportJobStatus.PENDING);
			} else if (job.getFailed() > 0) {
				job.setStatus(BulkImportJobStatus.FAILED);
			} else if (job.getUnsupported() > 0 || job.getUnverified() > 0) {
				job.setStatus(BulkImportJobStatus.COMPLETED_WITH_SKIPPED);
			} else {
				job.setStatus(BulkImportJobStatus.COMPLETED);
			}
			jobRepository.save(job);
		}
		return BulkImportStatusResponse.from(job);
	}

	private void markFailed(BulkImportItem item, String message) {
		item.setStatus(BulkImportItemStatus.FAILED);
		item.setErrorMessage(message);
		itemRepository.save(item);
	}

	public BulkImportStatusResponse status(Long jobId) {
		return BulkImportStatusResponse.from(findJob(jobId));
	}

	private void delayBetweenRequests() {
		long delay = properties.getDelayBetweenRequestsMs();
		if (delay == 0) {
			return;
		}
		try {
			Thread.sleep(delay);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Bulk import was interrupted.");
		}
	}

	private BulkImportJob findJob(Long jobId) {
		return jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("Bulk import job not found with id " + jobId));
	}
}
