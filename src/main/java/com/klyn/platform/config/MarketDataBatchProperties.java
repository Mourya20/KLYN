package com.klyn.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

@Validated
@ConfigurationProperties(prefix = "market-data.batch")
public class MarketDataBatchProperties {

	@Min(1)
	private int maxRequestsPerRun = 5;

	@Min(0)
	private long delayBetweenRequestsMs = 12000;

	public int getMaxRequestsPerRun() { return maxRequestsPerRun; }
	public void setMaxRequestsPerRun(int maxRequestsPerRun) { this.maxRequestsPerRun = maxRequestsPerRun; }
	public long getDelayBetweenRequestsMs() { return delayBetweenRequestsMs; }
	public void setDelayBetweenRequestsMs(long delayBetweenRequestsMs) { this.delayBetweenRequestsMs = delayBetweenRequestsMs; }
}
