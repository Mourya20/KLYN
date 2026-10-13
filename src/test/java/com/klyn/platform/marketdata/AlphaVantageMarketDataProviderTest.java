package com.klyn.platform.marketdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class AlphaVantageMarketDataProviderTest {

	@Test
	void parsesValidDailySeriesAndPreservesNumericValues() {
		String json = """
			{
			  "Meta Data": {
			    "1. Information": "Daily Prices",
			    "2. Symbol": "RELIANCE.BSE",
			    "3. Last Refreshed": "2026-10-09",
			    "4. Output Size": "Compact",
			    "5. Time Zone": "US/Eastern"
			  },
			  "Time Series (Daily)": {
			    "2026-10-09": {
			      "1. open": "1179.5000",
			      "2. high": "1189.0500",
			      "3. low": "1160.4000",
			      "4. close": "1170.8000",
			      "5. volume": "2089919"
			    }
			  }
			}
			""";

		MarketDataFetchResult result = AlphaVantageMarketDataProvider.parseResponse(json, "RELIANCE.BSE");

		assertEquals("RELIANCE.BSE", result.symbol());
		assertEquals(1, result.prices().size());
		assertEquals(new BigDecimal("1179.5000"), result.prices().get(0).openPrice());
		assertEquals(new BigDecimal("1189.0500"), result.prices().get(0).highPrice());
		assertEquals(2089919L, result.prices().get(0).volume());
		assertNull(result.exchangeCode());
	}

	@Test
	void rejectsProviderErrorAndMismatchedSymbols() {
		String providerError = "{\"Error Message\":\"The daily API call frequency is 5 calls per minute and 500 calls per day. Please try again later.\"}";
		assertThrows(MarketDataProviderException.class, () -> AlphaVantageMarketDataProvider.parseResponse(providerError, "RELIANCE.BSE"));

		String mismatched = """
			{
			  "Meta Data": {"2. Symbol": "TCS.BSE"},
			  "Time Series (Daily)": {"2026-10-09": {"1. open": "10", "2. high": "11", "3. low": "9", "4. close": "10.5", "5. volume": "1000"}}
			}
			""";
		assertThrows(MarketDataProviderException.class, () -> AlphaVantageMarketDataProvider.parseResponse(mismatched, "RELIANCE.BSE"));

		MarketDataProviderException rateLimit = assertThrows(MarketDataProviderException.class,
				() -> AlphaVantageMarketDataProvider.parseResponse("{\"Note\":\"frequency limit reached\"}", "RELIANCE.BSE"));
		assertTrue(rateLimit.isRateLimited());
	}

	@Test
	void rejectsEmptyOrMalformedSeries() {
		String emptySeries = "{\"Meta Data\": {\"2. Symbol\": \"RELIANCE.BSE\"}, \"Time Series (Daily)\": {}}";
		assertThrows(MarketDataProviderException.class, () -> AlphaVantageMarketDataProvider.parseResponse(emptySeries, "RELIANCE.BSE"));

		String malformed = "{\"Meta Data\": {\"2. Symbol\": \"RELIANCE.BSE\"}, \"Time Series (Daily)\": {\"2026-10-09\": {\"1. open\": \"bad\"}}}";
		assertThrows(MarketDataProviderException.class, () -> AlphaVantageMarketDataProvider.parseResponse(malformed, "RELIANCE.BSE"));
	}
}
