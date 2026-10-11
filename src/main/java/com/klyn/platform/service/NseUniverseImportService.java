package com.klyn.platform.service;

import java.io.IOException;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.klyn.platform.dto.BulkImportStatusResponse;
import com.klyn.platform.entity.BulkImportItem;
import com.klyn.platform.entity.BulkImportItemStatus;
import com.klyn.platform.entity.BulkImportJob;
import com.klyn.platform.entity.BulkImportJobStatus;
import com.klyn.platform.entity.Company;
import com.klyn.platform.entity.Instrument;
import com.klyn.platform.entity.ProviderMappingStatus;
import com.klyn.platform.exception.ResourceNotFoundException;
import com.klyn.platform.marketdata.NseEquityListing;
import com.klyn.platform.marketdata.NseEquityListingParser;
import com.klyn.platform.repository.BulkImportItemRepository;
import com.klyn.platform.repository.BulkImportJobRepository;
import com.klyn.platform.repository.CompanyRepository;
import com.klyn.platform.repository.InstrumentRepository;

@Service
public class NseUniverseImportService {

	private final NseEquityListingParser parser;
	private final CompanyRepository companyRepository;
	private final InstrumentRepository instrumentRepository;
	private final BulkImportJobRepository jobRepository;
	private final BulkImportItemRepository itemRepository;

	public NseUniverseImportService(NseEquityListingParser parser, CompanyRepository companyRepository,
			InstrumentRepository instrumentRepository, BulkImportJobRepository jobRepository,
			BulkImportItemRepository itemRepository) {
		this.parser = parser;
		this.companyRepository = companyRepository;
		this.instrumentRepository = instrumentRepository;
		this.jobRepository = jobRepository;
		this.itemRepository = itemRepository;
	}

	@Transactional
	public BulkImportStatusResponse createNseJob(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("An official NSE equity listing file is required.");
		}
		List<NseEquityListing> listings;
		try {
			listings = parser.parse(file.getInputStream());
		} catch (IOException exception) {
			throw new IllegalArgumentException("The NSE equity listing file could not be read.");
		}
		if (listings.isEmpty()) {
			throw new IllegalArgumentException("The NSE equity listing file contained no EQ listings.");
		}

		BulkImportJob job = new BulkImportJob();
		job.setExchangeCode("NSE");
		job.setSourceFileName(safeFileName(file.getOriginalFilename()));
		job.setStatus(BulkImportJobStatus.PENDING);
		BulkImportJob savedJob = jobRepository.save(job);

		Set<String> seenSymbols = new HashSet<>();
		for (NseEquityListing listing : listings) {
			if (!seenSymbols.add(listing.symbol())) {
				continue;
			}
			Instrument instrument = instrumentRepository.findByExchangeCodeAndSymbolIgnoreCase("NSE", listing.symbol())
					.orElseGet(() -> createInstrument(listing));
			updateInstrumentMapping(instrument, listing);
			instrument = instrumentRepository.save(instrument);

			BulkImportItem item = new BulkImportItem();
			item.setJob(savedJob);
			item.setInstrument(instrument);
			item.setStatus(itemStatus(instrument));
			item.setErrorMessage(instrument.getProviderMappingNote());
			itemRepository.save(item);
		}
		refreshCounts(savedJob);
		return BulkImportStatusResponse.from(savedJob);
	}

	public BulkImportStatusResponse getStatus(Long jobId) {
		return BulkImportStatusResponse.from(findJob(jobId));
	}

	private Instrument createInstrument(NseEquityListing listing) {
		Company company = new Company();
		company.setName(listing.companyName());
		company.setCurrency("INR");
		company.setActive(true);
		Company savedCompany = companyRepository.save(company);
		Instrument instrument = new Instrument();
		instrument.setCompany(savedCompany);
		instrument.setSymbol(listing.symbol());
		instrument.setExchangeCode("NSE");
		instrument.setTradingCurrency("INR");
		return instrument;
	}

	private void updateInstrumentMapping(Instrument instrument, NseEquityListing listing) {
		if (listing.providerSymbol() != null && !listing.providerSymbol().isBlank()) {
			instrument.setProviderSymbol(listing.providerSymbol().trim());
			instrument.setProviderMappingStatus(ProviderMappingStatus.UNVERIFIED);
			instrument.setProviderMappingNote("Provider symbol supplied by mapping file; awaiting response verification.");
		} else if ("UNSUPPORTED".equalsIgnoreCase(listing.providerStatus())) {
			instrument.setProviderSymbol(null);
			instrument.setProviderMappingStatus(ProviderMappingStatus.UNSUPPORTED);
			instrument.setProviderMappingNote("No supported Alpha Vantage mapping was supplied.");
		} else if (instrument.getProviderSymbol() == null || instrument.getProviderSymbol().isBlank()) {
			instrument.setProviderSymbol(null);
			instrument.setProviderMappingStatus(ProviderMappingStatus.UNVERIFIED);
			instrument.setProviderMappingNote("No provider symbol mapping was supplied by the NSE listing file.");
		}
	}

	private BulkImportItemStatus itemStatus(Instrument instrument) {
		if (instrument.getProviderMappingStatus() == ProviderMappingStatus.UNSUPPORTED) {
			return BulkImportItemStatus.UNSUPPORTED;
		}
		if (instrument.getProviderSymbol() == null || instrument.getProviderSymbol().isBlank()) {
			return BulkImportItemStatus.UNVERIFIED;
		}
		return BulkImportItemStatus.PENDING;
	}

	void refreshCounts(BulkImportJob job) {
		List<BulkImportItem> items = itemRepository.findByJobOrderByIdAsc(job);
		job.setTotalInstruments(items.size());
		job.setCompleted(count(items, BulkImportItemStatus.COMPLETED));
		job.setPending(count(items, BulkImportItemStatus.PENDING));
		job.setUnsupported(count(items, BulkImportItemStatus.UNSUPPORTED));
		job.setUnverified(count(items, BulkImportItemStatus.UNVERIFIED));
		job.setFailed(count(items, BulkImportItemStatus.FAILED));
		job.setRecordsSaved(items.stream().mapToInt(BulkImportItem::getRecordsSaved).sum());
		job.setLatestTradingDate(items.stream().map(BulkImportItem::getLatestTradingDate).filter(java.util.Objects::nonNull)
				.max(LocalDateComparator.INSTANCE).orElse(null));
		jobRepository.save(job);
	}

	private int count(List<BulkImportItem> items, BulkImportItemStatus status) {
		return (int) items.stream().filter(item -> item.getStatus() == status).count();
	}

	private BulkImportJob findJob(Long jobId) {
		return jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("Bulk import job not found with id " + jobId));
	}

	private String safeFileName(String fileName) {
		if (fileName == null || fileName.isBlank()) {
			return "nse-equity-listing.csv";
		}
		return fileName.replaceAll("[^A-Za-z0-9._-]", "_");
	}

	private static final class LocalDateComparator implements java.util.Comparator<java.time.LocalDate> {
		private static final LocalDateComparator INSTANCE = new LocalDateComparator();
		@Override public int compare(java.time.LocalDate left, java.time.LocalDate right) { return left.compareTo(right); }
	}
}
