package com.klyn.platform.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.klyn.platform.entity.MarketPrice;

public record MarketPriceResponse(Long id, Long companyId, Long instrumentId, String symbol, String exchangeCode,
		LocalDate tradingDate, BigDecimal openPrice, BigDecimal highPrice, BigDecimal lowPrice, BigDecimal closePrice,
		Long volume, String dataSource, String priceAdjustment, String providerTimeZone) {

	public static MarketPriceResponse from(MarketPrice marketPrice) {
		return new MarketPriceResponse(marketPrice.getId(), marketPrice.getInstrument().getCompany().getId(),
				marketPrice.getInstrument().getId(), marketPrice.getInstrument().getSymbol(),
				marketPrice.getInstrument().getExchangeCode(), marketPrice.getTradingDate(), marketPrice.getOpenPrice(),
				marketPrice.getHighPrice(), marketPrice.getLowPrice(), marketPrice.getClosePrice(), marketPrice.getVolume(),
				marketPrice.getDataSource(), marketPrice.getPriceAdjustment(), marketPrice.getProviderTimeZone());
	}
}
