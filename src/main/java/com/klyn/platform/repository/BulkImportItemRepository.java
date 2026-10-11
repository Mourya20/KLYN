package com.klyn.platform.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.klyn.platform.entity.BulkImportItem;
import com.klyn.platform.entity.BulkImportItemStatus;
import com.klyn.platform.entity.BulkImportJob;
import com.klyn.platform.entity.Instrument;

public interface BulkImportItemRepository extends JpaRepository<BulkImportItem, Long> {

	List<BulkImportItem> findByJobOrderByIdAsc(BulkImportJob job);

	Optional<BulkImportItem> findByJobAndInstrument(BulkImportJob job, Instrument instrument);

	long countByJobAndStatus(BulkImportJob job, BulkImportItemStatus status);
}
