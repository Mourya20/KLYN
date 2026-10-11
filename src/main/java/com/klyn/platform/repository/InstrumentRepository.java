package com.klyn.platform.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.klyn.platform.entity.Company;
import com.klyn.platform.entity.Instrument;

public interface InstrumentRepository extends JpaRepository<Instrument, Long> {

	@org.springframework.data.jpa.repository.Query("SELECT i FROM Instrument i WHERE i.exchangeCode = :exchangeCode AND i.symbol = :symbol AND i.active = true AND i.company.active = true")
	Optional<Instrument> findActiveByExchangeAndSymbol(@org.springframework.data.repository.query.Param("exchangeCode") String exchangeCode,
			@org.springframework.data.repository.query.Param("symbol") String symbol);

	Optional<Instrument> findByCompanyAndActiveTrue(Company company);

	Optional<Instrument> findByExchangeCodeAndSymbolIgnoreCase(String exchangeCode, String symbol);

	List<Instrument> findByCompanyAndActiveTrueOrderByExchangeCodeAscSymbolAsc(Company company);

	boolean existsByExchangeCodeIgnoreCaseAndSymbolIgnoreCase(String exchangeCode, String symbol);
}