package com.klyn.platform.marketdata;

public interface MarketDataProvider {

	MarketDataFetchResult fetchDailySeries(String symbol, String exchangeCode);
}
