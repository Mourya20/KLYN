package com.klyn.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Validated
@ConfigurationProperties(prefix = "alpha.vantage")
public class AlphaVantageProperties {

	@NotBlank
	private String apiKey;

	@NotBlank
	private String baseUrl = "https://www.alphavantage.co/query";

	@Min(1)
	private int connectTimeoutMs = 5000;

	@Min(1)
	private int responseTimeoutMs = 10000;

	@NotBlank
	@Pattern(regexp = "compact|full", flags = Pattern.Flag.CASE_INSENSITIVE)
	private String outputSize = "compact";

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public int getConnectTimeoutMs() {
		return connectTimeoutMs;
	}

	public void setConnectTimeoutMs(int connectTimeoutMs) {
		this.connectTimeoutMs = connectTimeoutMs;
	}

	public int getResponseTimeoutMs() {
		return responseTimeoutMs;
	}

	public void setResponseTimeoutMs(int responseTimeoutMs) {
		this.responseTimeoutMs = responseTimeoutMs;
	}

	public String getOutputSize() {
		return outputSize;
	}

	public void setOutputSize(String outputSize) {
		this.outputSize = outputSize;
	}
}
