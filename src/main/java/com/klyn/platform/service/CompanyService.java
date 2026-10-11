package com.klyn.platform.service;

import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.klyn.platform.dto.CompanyCreateRequest;
import com.klyn.platform.dto.CompanyResponse;
import com.klyn.platform.entity.Company;
import com.klyn.platform.entity.Instrument;
import com.klyn.platform.exception.DuplicateResourceException;
import com.klyn.platform.exception.ResourceNotFoundException;
import com.klyn.platform.repository.CompanyRepository;
import com.klyn.platform.repository.InstrumentRepository;

@Service
public class CompanyService {

	private final CompanyRepository companyRepository;
	private final InstrumentRepository instrumentRepository;

	public CompanyService(CompanyRepository companyRepository, InstrumentRepository instrumentRepository) {
		this.companyRepository = companyRepository;
		this.instrumentRepository = instrumentRepository;
	}

	public Page<CompanyResponse> listCompanies(int page, int size) {
		Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by("name").ascending());
		return companyRepository.findByActiveTrue(pageable).map(this::toResponse);
	}

	public Page<CompanyResponse> searchCompanies(String query, int page, int size) {
		String normalized = query == null ? "" : query.trim();
		Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by("name").ascending());
		if (normalized.isBlank()) {
			return listCompanies(page, size);
		}
		return companyRepository.searchByNameOrSymbol(normalized, pageable).map(this::toResponse);
	}

	public CompanyResponse getCompany(Long companyId) {
		Company company = companyRepository.findById(companyId)
				.orElseThrow(() -> new ResourceNotFoundException("Company not found with id " + companyId));
		return toResponse(company);
	}

	@Transactional
	public CompanyResponse createCompany(CompanyCreateRequest request) {
		String symbol = normalizeSymbol(request.symbol());
		String exchangeCode = normalizeExchangeCode(request.exchangeCode());
		String currency = normalizeCurrency(request.currency());
		String name = request.name().trim();
		if (instrumentRepository.existsByExchangeCodeIgnoreCaseAndSymbolIgnoreCase(exchangeCode, symbol)) {
			throw new DuplicateResourceException("Instrument already exists for exchange " + exchangeCode + " and symbol " + symbol);
		}
		Company company = new Company();
		company.setName(name);
		company.setCurrency(currency);
		company.setSector(request.sector() == null ? null : request.sector().trim());
		company.setActive(request.active() == null || request.active());
		Company savedCompany = companyRepository.save(company);

		Instrument instrument = new Instrument();
		instrument.setCompany(savedCompany);
		instrument.setSymbol(symbol);
		instrument.setExchangeCode(exchangeCode);
		instrument.setTradingCurrency(currency);
		instrument.setProviderSymbol(request.providerSymbol() == null || request.providerSymbol().isBlank()
				? symbol : request.providerSymbol().trim());
		instrument.setActive(savedCompany.isActive());
		instrumentRepository.save(instrument);
		return toResponse(savedCompany);
	}

	public Instrument resolveInstrument(String exchangeCode, String symbol) {
		String normalizedExchange = normalizeExchangeCode(exchangeCode);
		String normalizedSymbol = normalizeSymbol(symbol);
		return instrumentRepository.findActiveByExchangeAndSymbol(normalizedExchange, normalizedSymbol)
				.orElseThrow(() -> new ResourceNotFoundException("Active instrument not found for exchange " + normalizedExchange + " and symbol " + normalizedSymbol));
	}

	private CompanyResponse toResponse(Company company) {
		return CompanyResponse.from(company, instrumentRepository.findByCompanyAndActiveTrueOrderByExchangeCodeAscSymbolAsc(company)
				.stream().map(com.klyn.platform.dto.InstrumentResponse::from).toList());
	}

	public String normalizeSymbol(String symbol) {
		if (symbol == null || symbol.isBlank()) {
			throw new IllegalArgumentException("Company symbol is required.");
		}
		return symbol.trim().replace(" ", "").toUpperCase(Locale.ROOT);
	}

	public String normalizeExchangeCode(String exchangeCode) {
		if (exchangeCode == null || exchangeCode.isBlank()) {
			throw new IllegalArgumentException("Exchange code is required.");
		}
		return exchangeCode.trim().toUpperCase(Locale.ROOT);
	}

	private String normalizeCurrency(String currency) {
		if (currency == null || currency.isBlank()) {
			throw new IllegalArgumentException("Currency code is required.");
		}
		return currency.trim().toUpperCase(Locale.ROOT);
	}
}
