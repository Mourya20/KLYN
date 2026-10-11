package com.klyn.platform.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.klyn.platform.entity.Instrument;
import com.klyn.platform.entity.MarketPrice;

public interface MarketPriceRepository extends JpaRepository<MarketPrice, Long> {

	Optional<MarketPrice> findByInstrumentAndTradingDate(Instrument instrument, LocalDate tradingDate);

	List<MarketPrice> findByInstrumentAndTradingDateBetweenOrderByTradingDateAsc(Instrument instrument, LocalDate from, LocalDate to);

	Page<MarketPrice> findByInstrumentOrderByTradingDateAsc(Instrument instrument, Pageable pageable);

	@Query("SELECT mp FROM MarketPrice mp WHERE mp.instrument = :instrument AND mp.tradingDate BETWEEN :from AND :to ORDER BY mp.tradingDate ASC")
	Page<MarketPrice> findHistoricalByInstrument(@Param("instrument") Instrument instrument, @Param("from") LocalDate from,
			@Param("to") LocalDate to, Pageable pageable);

	Optional<MarketPrice> findTopByInstrumentOrderByTradingDateDesc(Instrument instrument);
}
