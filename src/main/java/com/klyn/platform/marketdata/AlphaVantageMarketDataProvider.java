package com.klyn.platform.marketdata;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.klyn.platform.config.AlphaVantageProperties;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class AlphaVantageMarketDataProvider implements MarketDataProvider {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private final RestClient restClient;
	private final AlphaVantageProperties properties;

	public AlphaVantageMarketDataProvider(AlphaVantageProperties properties) {
		this.properties = properties;
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(properties.getConnectTimeoutMs());
		requestFactory.setReadTimeout(properties.getResponseTimeoutMs());
		this.restClient = RestClient.builder().baseUrl(properties.getBaseUrl()).requestFactory(requestFactory).build();
	}

	@Override
	public MarketDataFetchResult fetchDailySeries(String symbol, String exchangeCode) {
		String normalized = normalizeSymbol(symbol);
		String response;
		try {
			response = restClient.get()
					.uri(uriBuilder -> uriBuilder
							.queryParam("function", "TIME_SERIES_DAILY")
							.queryParam("symbol", normalized)
							.queryParam("outputsize", properties.getOutputSize())
							.queryParam("apikey", properties.getApiKey())
							.build())
					.retrieve()
					.onStatus(HttpStatusCode::is4xxClientError, (request, responseEntity) -> {
						boolean rateLimited = responseEntity.getStatusCode().value() == 429;
						throw new MarketDataProviderException("Alpha Vantage rejected the symbol request. Verify the configured symbol and API key.", rateLimited);
					})
					.onStatus(HttpStatusCode::is5xxServerError, (request, responseEntity) -> {
						throw new MarketDataProviderException("Alpha Vantage is currently unavailable. Please retry later.");
					})
					.body(String.class);
		} catch (RestClientException exception) {
			throw new MarketDataProviderException("Alpha Vantage request failed. Please retry later.");
		}
		return parseResponse(response, normalized);
	}

	public static MarketDataFetchResult parseResponse(String json, String expectedSymbol) {
		if (json == null || json.isBlank()) {
			throw new MarketDataProviderException("Empty Alpha Vantage response received.");
		}
		JsonNode root = MAPPER.readTree(json);
		if (root == null || root.isMissingNode()) {
			throw new MarketDataProviderException("Alpha Vantage response was empty or malformed.");
		}

		String errorMessage = readText(root, "Error Message");
		if (errorMessage != null && !errorMessage.isBlank()) {
			throw new MarketDataProviderException(errorMessage);
		}

		String note = readText(root, "Note");
		if (note != null && !note.isBlank()) {
			throw new MarketDataProviderException(note, isRateLimitMessage(note));
		}

		String information = readText(root, "Information");
		if (information != null && !information.isBlank()) {
			throw new MarketDataProviderException(information, isRateLimitMessage(information));
		}

		JsonNode meta = root.get("Meta Data");
		if (meta == null || meta.isNull()) {
			throw new MarketDataProviderException("Alpha Vantage response did not include Meta Data.");
		}

		String symbol = readText(meta, "2. Symbol");
		if (symbol == null || symbol.isBlank()) {
			throw new MarketDataProviderException("Alpha Vantage response did not include a symbol in metadata.");
		}
		if (!symbol.equalsIgnoreCase(expectedSymbol)) {
			throw new MarketDataProviderException("Requested symbol " + expectedSymbol + " did not match provider metadata symbol " + symbol + ".");
		}

		JsonNode seriesNode = root.get("Time Series (Daily)");
		if (seriesNode == null || seriesNode.isNull() || seriesNode.isEmpty()) {
			throw new MarketDataProviderException("Alpha Vantage response did not include any daily price records for " + expectedSymbol + ".");
		}

		List<PriceSnapshot> prices = new ArrayList<>();
		for (Map.Entry<String, JsonNode> entry : seriesNode.properties()) {
			String dateText = entry.getKey();
			JsonNode values = entry.getValue();
			if (values == null || values.isNull()) {
				throw new MarketDataProviderException("Invalid time-series payload on date " + dateText + ".");
			}
			LocalDate tradingDate;
			try {
				tradingDate = LocalDate.parse(dateText);
			} catch (DateTimeParseException exception) {
				throw new MarketDataProviderException("Invalid date value returned by Alpha Vantage: " + dateText, exception);
			}
			BigDecimal openPrice = parseDecimal(values.get("1. open"), "open");
			BigDecimal highPrice = parseDecimal(values.get("2. high"), "high");
			BigDecimal lowPrice = parseDecimal(values.get("3. low"), "low");
			BigDecimal closePrice = parseDecimal(values.get("4. close"), "close");
			long volume = parseLong(values.get("5. volume"), "volume");
			prices.add(new PriceSnapshot(tradingDate, openPrice, highPrice, lowPrice, closePrice, volume));
		}
		if (prices.isEmpty()) {
			throw new MarketDataProviderException("Alpha Vantage returned an empty daily series for " + expectedSymbol + ".");
		}
		String timeZone = readText(meta, "5. Time Zone");
		return new MarketDataFetchResult(symbol, null, timeZone, prices);
	}

	private static String readText(JsonNode parent, String fieldName) {
		JsonNode node = parent.get(fieldName);
		if (node == null || node.isNull() || node.isMissingNode()) {
			return null;
		}
		return node.asString(null);
	}

	private static BigDecimal parseDecimal(JsonNode node, String fieldName) {
		String value = node == null || node.isNull() || node.isMissingNode() ? null : node.asString(null);
		if (value == null) {
			throw new MarketDataProviderException("Missing numeric value for " + fieldName + ".");
		}
		try {
			return new BigDecimal(value);
		} catch (NumberFormatException exception) {
			throw new MarketDataProviderException("Invalid numeric value for " + fieldName + ": " + value, exception);
		}
	}

	private static long parseLong(JsonNode node, String fieldName) {
		String value = node == null || node.isNull() || node.isMissingNode() ? null : node.asString(null);
		if (value == null) {
			throw new MarketDataProviderException("Missing numeric value for " + fieldName + ".");
		}
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException exception) {
			throw new MarketDataProviderException("Invalid numeric value for " + fieldName + ": " + value, exception);
		}
	}

	private static String normalizeSymbol(String symbol) {
		if (symbol == null || symbol.isBlank()) {
			throw new IllegalArgumentException("Symbol must not be empty.");
		}
		String trimmed = symbol.trim();
		if (trimmed.contains(" ")) {
			trimmed = trimmed.replace(" ", "");
		}
		return trimmed.toUpperCase();
	}

	private static boolean isRateLimitMessage(String message) {
		String normalized = message.toLowerCase();
		return normalized.contains("frequency") || normalized.contains("rate limit")
				|| normalized.contains("call volume") || normalized.contains("requests per minute");
	}
}
