package com.klyn.platform.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.klyn.platform.dto.MarketDataImportRequest;
import com.klyn.platform.dto.MarketDataImportResponse;
import com.klyn.platform.dto.MarketPriceResponse;
import com.klyn.platform.entity.Company;
import com.klyn.platform.entity.Instrument;
import com.klyn.platform.entity.MarketPrice;
import com.klyn.platform.exception.ResourceNotFoundException;
import com.klyn.platform.marketdata.MarketDataFetchResult;
import com.klyn.platform.marketdata.MarketDataProvider;
import com.klyn.platform.marketdata.PriceSnapshot;
import com.klyn.platform.repository.CompanyRepository;
import com.klyn.platform.repository.InstrumentRepository;
import com.klyn.platform.repository.MarketPriceRepository;

@Service
public class MarketPriceService {

	private final CompanyRepository companyRepository;
	private final MarketPriceRepository marketPriceRepository;
	private final InstrumentRepository instrumentRepository;
	private final MarketDataProvider marketDataProvider;

	public MarketPriceService(CompanyRepository companyRepository, MarketPriceRepository marketPriceRepository,
			InstrumentRepository instrumentRepository, MarketDataProvider marketDataProvider) {
		this.companyRepository = companyRepository;
		this.marketPriceRepository = marketPriceRepository;
		this.instrumentRepository = instrumentRepository;
		this.marketDataProvider = marketDataProvider;
	}

	public Page<MarketPriceResponse> getHistoricalPrices(Long companyId, LocalDate from, LocalDate to, int page, int size) {
		Company company = companyRepository.findById(companyId)
				.orElseThrow(() -> new ResourceNotFoundException("Company not found with id " + companyId));
		LocalDate start = from != null ? from : LocalDate.now().minusYears(1);
		LocalDate end = to != null ? to : LocalDate.now();
		if (start.isAfter(end)) {
			throw new IllegalArgumentException("The from date must be before or equal to the to date.");
		}
		Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by("tradingDate").ascending());
		Instrument instrument = activeInstrumentFor(company);
		return marketPriceRepository.findHistoricalByInstrument(instrument, start, end, pageable).map(MarketPriceResponse::from);
	}

	public MarketPriceResponse getLatestPrice(Long companyId) {
		Company company = companyRepository.findById(companyId)
				.orElseThrow(() -> new ResourceNotFoundException("Company not found with id " + companyId));
		Instrument instrument = activeInstrumentFor(company);
		MarketPrice marketPrice = marketPriceRepository.findTopByInstrumentOrderByTradingDateDesc(instrument)
				.orElseThrow(() -> new ResourceNotFoundException("No price data found for company " + company.getName()));
		return MarketPriceResponse.from(marketPrice);
	}

	@Transactional
	public MarketDataImportResponse importDailyData(MarketDataImportRequest request) {
		String symbol = request.symbol().trim();
		String exchangeCode = request.exchangeCode().trim();
		if (symbol.isBlank() || exchangeCode.isBlank()) {
			throw new IllegalArgumentException("Company symbol and exchange code are required.");
		}
		Instrument instrument = instrumentRepository.findActiveByExchangeAndSymbol(exchangeCode, symbol)
				.orElseThrow(() -> new ResourceNotFoundException("Active instrument not found for exchange " + exchangeCode + " and symbol " + symbol));

		MarketDataFetchResult fetchResult = marketDataProvider.fetchDailySeries(instrument.getProviderSymbol(), instrument.getExchangeCode());
		if (!instrument.getProviderSymbol().equalsIgnoreCase(fetchResult.symbol())) {
			throw new IllegalArgumentException("Provider symbol did not match the configured instrument mapping.");
		}
		if (fetchResult.prices() == null || fetchResult.prices().isEmpty()) {
			throw new IllegalArgumentException("No daily records were returned for symbol " + instrument.getSymbol());
		}

		List<MarketPrice> toSave = new ArrayList<>();
		int inserted = 0;
		int updated = 0;
		int skipped = 0;
		for (PriceSnapshot snapshot : fetchResult.prices()) {
			validateSnapshot(snapshot);
			Optional<MarketPrice> existing = marketPriceRepository.findByInstrumentAndTradingDate(instrument, snapshot.tradingDate());
			if (existing.isPresent()) {
				MarketPrice persisted = existing.get();
				if (sameValues(persisted, snapshot)) {
					persisted.setProviderTimeZone(fetchResult.timeZone());
					skipped++;
					continue;
				}
				persisted.setProviderTimeZone(fetchResult.timeZone());
				applySnapshot(persisted, snapshot);
				updated++;
				toSave.add(persisted);
			} else {
				MarketPrice price = new MarketPrice();
				price.setInstrument(instrument);
				price.setProviderTimeZone(fetchResult.timeZone());
				applySnapshot(price, snapshot);
				toSave.add(price);
				inserted++;
			}
		}
		if (!toSave.isEmpty()) {
			marketPriceRepository.saveAll(toSave);
		}
		return new MarketDataImportResponse(instrument.getSymbol(), instrument.getExchangeCode(), fetchResult.prices().size(),
				inserted, updated, skipped, "success", "Imported " + fetchResult.prices().size() + " daily records for " + instrument.getSymbol());
	}

	private Instrument activeInstrumentFor(Company company) {
		return instrumentRepository.findByCompanyAndActiveTrue(company)
				.orElseThrow(() -> new ResourceNotFoundException("No active instrument found for company " + company.getId()));
	}

	private void validateSnapshot(PriceSnapshot snapshot) {
		if (snapshot.tradingDate() == null) {
			throw new IllegalArgumentException("Trading date is required");
		}
		if (snapshot.openPrice() == null || snapshot.highPrice() == null || snapshot.lowPrice() == null || snapshot.closePrice() == null) {
			throw new IllegalArgumentException("All OHLC values are required for " + snapshot.tradingDate());
		}
		if (snapshot.openPrice().compareTo(BigDecimal.ZERO) <= 0 || snapshot.highPrice().compareTo(BigDecimal.ZERO) <= 0
				|| snapshot.lowPrice().compareTo(BigDecimal.ZERO) <= 0 || snapshot.closePrice().compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("OHLC values must be positive for " + snapshot.tradingDate());
		}
		if (snapshot.volume() < 0) {
			throw new IllegalArgumentException("Volume cannot be negative for " + snapshot.tradingDate());
		}
		if (snapshot.highPrice().compareTo(snapshot.openPrice()) < 0 || snapshot.highPrice().compareTo(snapshot.closePrice()) < 0
				|| snapshot.highPrice().compareTo(snapshot.lowPrice()) < 0 || snapshot.lowPrice().compareTo(snapshot.openPrice()) > 0
				|| snapshot.lowPrice().compareTo(snapshot.closePrice()) > 0) {
			throw new IllegalArgumentException("OHLC values are inconsistent for " + snapshot.tradingDate());
		}
	}

	private boolean sameValues(MarketPrice marketPrice, PriceSnapshot snapshot) {
		return marketPrice.getTradingDate().equals(snapshot.tradingDate())
				&& marketPrice.getOpenPrice().compareTo(snapshot.openPrice()) == 0
				&& marketPrice.getHighPrice().compareTo(snapshot.highPrice()) == 0
				&& marketPrice.getLowPrice().compareTo(snapshot.lowPrice()) == 0
				&& marketPrice.getClosePrice().compareTo(snapshot.closePrice()) == 0
				&& marketPrice.getVolume().equals(snapshot.volume());
	}

	private void applySnapshot(MarketPrice price, PriceSnapshot snapshot) {
		price.setTradingDate(snapshot.tradingDate());
		price.setOpenPrice(snapshot.openPrice());
		price.setHighPrice(snapshot.highPrice());
		price.setLowPrice(snapshot.lowPrice());
		price.setClosePrice(snapshot.closePrice());
		price.setVolume(snapshot.volume());
	}
}
