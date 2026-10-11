package com.klyn.platform.marketdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

class NseEquityListingParserTest {

	@Test
	void parsesOfficialEqRowsAndQuotedCompanyNamesWithoutAssumingProviderCoverage() throws Exception {
		String csv = "SYMBOL,NAME OF COMPANY,SERIES,PROVIDER SYMBOL\n"
				+ "RELIANCE,\"Reliance Industries, Ltd.\",EQ,RELIANCE.BSE\n"
				+ "TCS,Tata Consultancy Services,EQ,\n"
				+ "TEST,Non equity row,BE,TEST.BSE\n";

		List<NseEquityListing> listings = new NseEquityListingParser().parse(
				new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

		assertEquals(2, listings.size());
		assertEquals("RELIANCE", listings.get(0).symbol());
		assertEquals("Reliance Industries, Ltd.", listings.get(0).companyName());
		assertEquals("RELIANCE.BSE", listings.get(0).providerSymbol());
		assertNull(listings.get(1).providerSymbol());
	}
}
