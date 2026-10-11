package com.klyn.platform.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "market_prices",
		uniqueConstraints = @UniqueConstraint(name = "uk_instrument_trading_date", columnNames = { "instrument_id", "trading_date" }),
		indexes = {
				@Index(name = "idx_market_price_instrument_date", columnList = "instrument_id, trading_date"),
				@Index(name = "idx_market_price_trading_date", columnList = "trading_date")
		})
public class MarketPrice {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "instrument_id", nullable = false, foreignKey = @ForeignKey(name = "fk_market_price_instrument"))
	private Instrument instrument;

	@NotNull
	@Column(name = "trading_date", nullable = false)
	private LocalDate tradingDate;

	@NotNull
	@DecimalMin(value = "0.00000001", inclusive = true)
	@Column(nullable = false, precision = 19, scale = 8)
	private BigDecimal openPrice;

	@NotNull
	@DecimalMin(value = "0.00000001", inclusive = true)
	@Column(nullable = false, precision = 19, scale = 8)
	private BigDecimal highPrice;

	@NotNull
	@DecimalMin(value = "0.00000001", inclusive = true)
	@Column(nullable = false, precision = 19, scale = 8)
	private BigDecimal lowPrice;

	@NotNull
	@DecimalMin(value = "0.00000001", inclusive = true)
	@Column(nullable = false, precision = 19, scale = 8)
	private BigDecimal closePrice;

	@NotNull
	@Min(0)
	@Column(nullable = false)
	private Long volume;

	@Column(name = "data_source", nullable = false, length = 40)
	private String dataSource = "ALPHA_VANTAGE";

	@Column(name = "price_adjustment", nullable = false, length = 20)
	private String priceAdjustment = "UNADJUSTED";

	@Column(name = "provider_time_zone", length = 64)
	private String providerTimeZone;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	public boolean hasValidPriceStructure() {
		return highPrice.compareTo(openPrice) >= 0
				&& highPrice.compareTo(lowPrice) >= 0
				&& highPrice.compareTo(closePrice) >= 0
				&& lowPrice.compareTo(openPrice) <= 0
				&& lowPrice.compareTo(closePrice) <= 0;
	}

	public Long getId() { return id; }
	public Instrument getInstrument() { return instrument; }
	public void setInstrument(Instrument instrument) { this.instrument = instrument; }
	public LocalDate getTradingDate() { return tradingDate; }
	public void setTradingDate(LocalDate tradingDate) { this.tradingDate = tradingDate; }
	public BigDecimal getOpenPrice() { return openPrice; }
	public void setOpenPrice(BigDecimal openPrice) { this.openPrice = openPrice; }
	public BigDecimal getHighPrice() { return highPrice; }
	public void setHighPrice(BigDecimal highPrice) { this.highPrice = highPrice; }
	public BigDecimal getLowPrice() { return lowPrice; }
	public void setLowPrice(BigDecimal lowPrice) { this.lowPrice = lowPrice; }
	public BigDecimal getClosePrice() { return closePrice; }
	public void setClosePrice(BigDecimal closePrice) { this.closePrice = closePrice; }
	public Long getVolume() { return volume; }
	public void setVolume(Long volume) { this.volume = volume; }
	public String getDataSource() { return dataSource; }
	public void setDataSource(String dataSource) { this.dataSource = dataSource; }
	public String getPriceAdjustment() { return priceAdjustment; }
	public void setPriceAdjustment(String priceAdjustment) { this.priceAdjustment = priceAdjustment; }
	public String getProviderTimeZone() { return providerTimeZone; }
	public void setProviderTimeZone(String providerTimeZone) { this.providerTimeZone = providerTimeZone; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
