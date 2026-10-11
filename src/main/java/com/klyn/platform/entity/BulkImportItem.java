package com.klyn.platform.entity;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "bulk_import_items",
		uniqueConstraints = @UniqueConstraint(name = "uk_bulk_import_job_instrument", columnNames = { "job_id", "instrument_id" }))
public class BulkImportItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "job_id", nullable = false, foreignKey = @ForeignKey(name = "fk_bulk_item_job"))
	private BulkImportJob job;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "instrument_id", nullable = false, foreignKey = @ForeignKey(name = "fk_bulk_item_instrument"))
	private Instrument instrument;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private BulkImportItemStatus status;

	@Column(nullable = false)
	private int recordsSaved;

	@Column(name = "latest_trading_date")
	private LocalDate latestTradingDate;

	@Column(length = 500)
	private String errorMessage;

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
	public BulkImportJob getJob() { return job; }
	public void setJob(BulkImportJob job) { this.job = job; }
	public Instrument getInstrument() { return instrument; }
	public void setInstrument(Instrument instrument) { this.instrument = instrument; }
	public BulkImportItemStatus getStatus() { return status; }
	public void setStatus(BulkImportItemStatus status) { this.status = status; }
	public int getRecordsSaved() { return recordsSaved; }
	public void setRecordsSaved(int recordsSaved) { this.recordsSaved = recordsSaved; }
	public LocalDate getLatestTradingDate() { return latestTradingDate; }
	public void setLatestTradingDate(LocalDate latestTradingDate) { this.latestTradingDate = latestTradingDate; }
	public String getErrorMessage() { return errorMessage; }
	public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
