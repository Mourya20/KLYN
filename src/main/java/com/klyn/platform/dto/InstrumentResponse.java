package com.klyn.platform.dto;

import com.klyn.platform.entity.Instrument;

public record InstrumentResponse(Long id, String symbol, String exchangeCode, String tradingCurrency,
		String providerSymbol, boolean active) {

	public static InstrumentResponse from(Instrument instrument) {
		return new InstrumentResponse(instrument.getId(), instrument.getSymbol(), instrument.getExchangeCode(),
				instrument.getTradingCurrency(), instrument.getProviderSymbol(), instrument.isActive());
	}
}