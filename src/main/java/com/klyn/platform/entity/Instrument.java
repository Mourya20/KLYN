package com.klyn.platform.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "instruments",
		uniqueConstraints = @UniqueConstraint(name = "uk_instrument_exchange_symbol", columnNames = { "exchange_code", "symbol" }),
		indexes = {
				@Index(name = "idx_instrument_company", columnList = "company_id"),
				@Index(name = "idx_instrument_symbol", columnList = "symbol")
		})
public class Instrument {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "company_id", nullable = false, foreignKey = @ForeignKey(name = "fk_instrument_company"))
	private Company company;

	@Column(nullable = false, length = 32)
	@NotBlank
	@Size(max = 32)
	private String symbol;

	@Column(name = "exchange_code", nullable = false, length = 20)
	@NotBlank
	@Size(max = 20)
	private String exchangeCode;

	@Column(name = "trading_currency", nullable = false, length = 10)
	@NotBlank
	@Size(max = 10)
	private String tradingCurrency;

	@Column(name = "provider_symbol", length = 64)
	@Size(max = 64)
	private String providerSymbol;

	@Enumerated(EnumType.STRING)
	@Column(name = "provider_mapping_status", nullable = false, length = 20)
	private ProviderMappingStatus providerMappingStatus = ProviderMappingStatus.UNVERIFIED;

	@Column(name = "provider_mapping_note", length = 255)
	private String providerMappingNote;

	@Column(nullable = false)
	private boolean active = true;

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
	void onUpdate() { updatedAt = Instant.now(); }

	public Long getId() { return id; }
	public Company getCompany() { return company; }
	public void setCompany(Company company) { this.company = company; }
	public String getSymbol() { return symbol; }
	public void setSymbol(String symbol) { this.symbol = symbol; }
	public String getExchangeCode() { return exchangeCode; }
	public void setExchangeCode(String exchangeCode) { this.exchangeCode = exchangeCode; }
	public String getTradingCurrency() { return tradingCurrency; }
	public void setTradingCurrency(String tradingCurrency) { this.tradingCurrency = tradingCurrency; }
	public String getProviderSymbol() { return providerSymbol; }
	public void setProviderSymbol(String providerSymbol) { this.providerSymbol = providerSymbol; }
	public ProviderMappingStatus getProviderMappingStatus() { return providerMappingStatus; }
	public void setProviderMappingStatus(ProviderMappingStatus providerMappingStatus) { this.providerMappingStatus = providerMappingStatus; }
	public String getProviderMappingNote() { return providerMappingNote; }
	public void setProviderMappingNote(String providerMappingNote) { this.providerMappingNote = providerMappingNote; }
	public boolean isActive() { return active; }
	public void setActive(boolean active) { this.active = active; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}