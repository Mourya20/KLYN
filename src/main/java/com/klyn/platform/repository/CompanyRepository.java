package com.klyn.platform.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.klyn.platform.entity.Company;

public interface CompanyRepository extends JpaRepository<Company, Long> {

	Page<Company> findByActiveTrue(Pageable pageable);

	@Query("SELECT DISTINCT c FROM Company c JOIN Instrument i ON i.company = c WHERE c.active = true AND i.active = true AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(i.symbol) LIKE LOWER(CONCAT('%', :query, '%')))")
	Page<Company> searchByNameOrSymbol(@Param("query") String query, Pageable pageable);
}
