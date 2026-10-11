package com.klyn.platform.marketdata;

import java.util.List;

public record MarketDataFetchResult(String symbol, String exchangeCode, String timeZone, List<PriceSnapshot> prices) {
}
