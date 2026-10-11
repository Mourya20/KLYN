package com.klyn.platform.marketdata;

public class MarketDataProviderException extends RuntimeException {

	private final boolean rateLimited;

	public MarketDataProviderException(String message) {
		this(message, null, false);
	}

	public MarketDataProviderException(String message, Throwable cause) {
		this(message, cause, false);
	}

	public MarketDataProviderException(String message, boolean rateLimited) {
		this(message, null, rateLimited);
	}

	public MarketDataProviderException(String message, Throwable cause, boolean rateLimited) {
		super(message, cause);
		this.rateLimited = rateLimited;
	}

	public boolean isRateLimited() { return rateLimited; }
}
