package com.klyn.platform.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.klyn.platform.entity.BulkImportJob;

public interface BulkImportJobRepository extends JpaRepository<BulkImportJob, Long> {
}
