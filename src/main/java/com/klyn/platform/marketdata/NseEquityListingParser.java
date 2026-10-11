package com.klyn.platform.marketdata;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class NseEquityListingParser {

	public List<NseEquityListing> parse(InputStream inputStream) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
			String headerLine = reader.readLine();
			if (headerLine == null) {
				throw new IllegalArgumentException("NSE equity listing file is empty.");
			}
			Map<String, Integer> headers = indexHeaders(parseLine(headerLine));
			Integer symbolIndex = findHeader(headers, "SYMBOL");
			Integer nameIndex = findHeader(headers, "NAME OF COMPANY", "COMPANY NAME");
			if (symbolIndex == null || nameIndex == null) {
				throw new IllegalArgumentException("NSE file must contain SYMBOL and NAME OF COMPANY columns.");
			}
			Integer seriesIndex = findHeader(headers, "SERIES");
			Integer providerSymbolIndex = findHeader(headers, "PROVIDER SYMBOL", "ALPHA VANTAGE SYMBOL");
			Integer providerStatusIndex = findHeader(headers, "PROVIDER STATUS", "MAPPING STATUS");

			List<NseEquityListing> listings = new ArrayList<>();
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank()) {
					continue;
				}
				List<String> values = parseLine(line);
				String series = value(values, seriesIndex);
				if (seriesIndex != null && !"EQ".equalsIgnoreCase(series)) {
					continue;
				}
				String symbol = value(values, symbolIndex).trim().toUpperCase(Locale.ROOT);
				String companyName = value(values, nameIndex).trim();
				if (symbol.isBlank() || companyName.isBlank()) {
					continue;
				}
				listings.add(new NseEquityListing(symbol, companyName, blankToNull(value(values, providerSymbolIndex)),
						blankToNull(value(values, providerStatusIndex))));
			}
			return listings;
		}
	}

	private Map<String, Integer> indexHeaders(List<String> values) {
		Map<String, Integer> headers = new HashMap<>();
		for (int index = 0; index < values.size(); index++) {
			headers.put(normalizeHeader(values.get(index)), index);
		}
		return headers;
	}

	private Integer findHeader(Map<String, Integer> headers, String... names) {
		for (String name : names) {
			Integer index = headers.get(normalizeHeader(name));
			if (index != null) {
				return index;
			}
		}
		return null;
	}

	private String normalizeHeader(String value) {
		return value.replace("\uFEFF", "").trim().toUpperCase(Locale.ROOT);
	}

	private String value(List<String> values, Integer index) {
		return index == null || index >= values.size() ? "" : values.get(index);
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private List<String> parseLine(String line) {
		List<String> values = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean quoted = false;
		for (int index = 0; index < line.length(); index++) {
			char character = line.charAt(index);
			if (character == '"') {
				if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
					current.append('"');
					index++;
				} else {
					quoted = !quoted;
				}
			} else if (character == ',' && !quoted) {
				values.add(current.toString().trim());
				current.setLength(0);
			} else {
				current.append(character);
			}
		}
		values.add(current.toString().trim());
		return values;
	}
}
