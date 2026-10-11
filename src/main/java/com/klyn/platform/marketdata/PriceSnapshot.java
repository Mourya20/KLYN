package com.klyn.platform.marketdata;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PriceSnapshot(LocalDate tradingDate, BigDecimal openPrice, BigDecimal highPrice, BigDecimal lowPrice,
		BigDecimal closePrice, long volume) {
}
