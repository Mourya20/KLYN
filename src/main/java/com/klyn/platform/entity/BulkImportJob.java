package com.klyn.platform.entity;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "bulk_import_jobs")
public class BulkImportJob {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "exchange_code", nullable = false, length = 20)
	private String exchangeCode;

	@Column(name = "source_file_name", nullable = false, length = 255)
	private String sourceFileName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private BulkImportJobStatus status = BulkImportJobStatus.PENDING;

	@Column(nullable = false)
	private int totalInstruments;

	@Column(nullable = false)
	private int completed;

	@Column(nullable = false)
	private int pending;

	@Column(nullable = false)
	private int unsupported;

	@Column(nullable = false)
	private int unverified;

	@Column(nullable = false)
	private int failed;

	@Column(nullable = false)
	private int recordsSaved;

	@Column(name = "latest_trading_date")
	private LocalDate latestTradingDate;

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
	public String getExchangeCode() { return exchangeCode; }
	public void setExchangeCode(String exchangeCode) { this.exchangeCode = exchangeCode; }
	public String getSourceFileName() { return sourceFileName; }
	public void setSourceFileName(String sourceFileName) { this.sourceFileName = sourceFileName; }
	public BulkImportJobStatus getStatus() { return status; }
	public void setStatus(BulkImportJobStatus status) { this.status = status; }
	public int getTotalInstruments() { return totalInstruments; }
	public void setTotalInstruments(int totalInstruments) { this.totalInstruments = totalInstruments; }
	public int getCompleted() { return completed; }
	public void setCompleted(int completed) { this.completed = completed; }
	public int getPending() { return pending; }
	public void setPending(int pending) { this.pending = pending; }
	public int getUnsupported() { return unsupported; }
	public void setUnsupported(int unsupported) { this.unsupported = unsupported; }
	public int getUnverified() { return unverified; }
	public void setUnverified(int unverified) { this.unverified = unverified; }
	public int getFailed() { return failed; }
	public void setFailed(int failed) { this.failed = failed; }
	public int getRecordsSaved() { return recordsSaved; }
	public void setRecordsSaved(int recordsSaved) { this.recordsSaved = recordsSaved; }
	public LocalDate getLatestTradingDate() { return latestTradingDate; }
	public void setLatestTradingDate(LocalDate latestTradingDate) { this.latestTradingDate = latestTradingDate; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
