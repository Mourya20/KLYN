package com.klyn.platform.dto;

import java.util.List;

import com.klyn.platform.entity.Company;

public record CompanyResponse(Long id, String name, String currency, String sector, boolean active,
		List<InstrumentResponse> instruments) {

	public static CompanyResponse from(Company company, List<InstrumentResponse> instruments) {
		return new CompanyResponse(company.getId(), company.getName(), company.getCurrency(), company.getSector(),
				company.isActive(), instruments);
	}
}
