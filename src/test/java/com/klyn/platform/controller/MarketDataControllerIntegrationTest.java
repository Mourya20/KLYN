package com.klyn.platform.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.klyn.platform.entity.Company;
import com.klyn.platform.entity.Instrument;
import com.klyn.platform.entity.User;
import com.klyn.platform.entity.UserRole;
import com.klyn.platform.marketdata.MarketDataFetchResult;
import com.klyn.platform.marketdata.MarketDataProvider;
import com.klyn.platform.marketdata.PriceSnapshot;
import com.klyn.platform.repository.CompanyRepository;
import com.klyn.platform.repository.BulkImportItemRepository;
import com.klyn.platform.repository.BulkImportJobRepository;
import com.klyn.platform.repository.InstrumentRepository;
import com.klyn.platform.repository.MarketPriceRepository;
import com.klyn.platform.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AdminProbeController.class)
class MarketDataControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CompanyRepository companyRepository;

	@Autowired
	private InstrumentRepository instrumentRepository;

	@Autowired
	private MarketPriceRepository marketPriceRepository;
	@Autowired
	private BulkImportItemRepository bulkImportItemRepository;

	@Autowired
	private BulkImportJobRepository bulkImportJobRepository;

	@org.springframework.test.context.bean.override.mockito.MockitoBean
	private MarketDataProvider marketDataProvider;

	@BeforeEach
	void reset() {
		bulkImportItemRepository.deleteAll();
		bulkImportJobRepository.deleteAll();
		marketPriceRepository.deleteAll();
		instrumentRepository.deleteAll();
		companyRepository.deleteAll();
		userRepository.deleteAll();
	}

    @Test
    void adminCanCreateAndRunNseBulkJobWithUnverifiedSymbolsTracked() throws Exception {
	saveAdmin();
	when(marketDataProvider.fetchDailySeries(anyString(), anyString())).thenReturn(new MarketDataFetchResult("RELIANCE.BSE", null,
		"US/Eastern", List.of(new PriceSnapshot(LocalDate.of(2026, 10, 9), new BigDecimal("100"), new BigDecimal("105"),
			new BigDecimal("99"), new BigDecimal("104"), 1000L))));

	String token = accessToken("admin@example.com", "StrongPassword123");
	MockMultipartFile file = new MockMultipartFile("file", "nse-equity.csv", "text/csv",
		("SYMBOL,NAME OF COMPANY,SERIES,PROVIDER SYMBOL,PROVIDER STATUS\n"
			+ "RELIANCE,\"Reliance Industries, Ltd.\",EQ,RELIANCE.BSE,\n"
			+ "TCS,\"Tata Consultancy Services\",EQ,,\n"
			+ "TEST,Test Non Equity,BE,TEST.BSE,UNSUPPORTED\n").getBytes());

	String createResponse = mockMvc.perform(multipart("/api/market-data/bulk-import/nse")
		.file(file).header("Authorization", "Bearer " + token))
	    .andExpect(status().isOk())
	    .andExpect(jsonPath("$.totalInstruments").value(2))
	    .andExpect(jsonPath("$.pending").value(1))
	    .andExpect(jsonPath("$.unverified").value(1))
	    .andReturn().getResponse().getContentAsString();
	long jobId = Long.parseLong(createResponse.substring(createResponse.indexOf("jobId") + 7,
		createResponse.indexOf(',', createResponse.indexOf("jobId"))));

	mockMvc.perform(post("/api/market-data/bulk-import/" + jobId + "/run").param("limit", "1")
		.header("Authorization", "Bearer " + token))
	    .andExpect(status().isOk())
	    .andExpect(jsonPath("$.status").value("COMPLETED_WITH_SKIPPED"))
	    .andExpect(jsonPath("$.completed").value(1))
	    .andExpect(jsonPath("$.recordsSaved").value(1))
	    .andExpect(jsonPath("$.mostRecentImportedTradingDate").value("2026-10-09"));

	mockMvc.perform(get("/api/market-data/bulk-import/" + jobId).header("Authorization", "Bearer " + token))
	    .andExpect(status().isOk())
	    .andExpect(jsonPath("$.unverified").value(1));
    }

	@Test
	void adminCanImportCompanyPricesAndUserCanReadThem() throws Exception {
		Company company = new Company();
		company.setName("Reliance Industries");
		company.setCurrency("INR");
		company.setActive(true);
		companyRepository.save(company);
		Instrument instrument = new Instrument();
		instrument.setCompany(company);
		instrument.setSymbol("RELIANCE.BSE");
		instrument.setExchangeCode("BSE");
		instrument.setTradingCurrency("INR");
		instrument.setProviderSymbol("RELIANCE.BSE");
		instrumentRepository.save(instrument);

		MarketDataFetchResult initial = new MarketDataFetchResult("RELIANCE.BSE", null, "US/Eastern",
				List.of(new PriceSnapshot(LocalDate.of(2026, 10, 9), new BigDecimal("1179.5000"),
						new BigDecimal("1189.0500"), new BigDecimal("1160.4000"), new BigDecimal("1170.8000"), 2089919L)));
		MarketDataFetchResult corrected = new MarketDataFetchResult("RELIANCE.BSE", null, "US/Eastern",
				List.of(new PriceSnapshot(LocalDate.of(2026, 10, 9), new BigDecimal("1179.5000"),
						new BigDecimal("1189.0500"), new BigDecimal("1160.4000"), new BigDecimal("1171.8000"), 2089919L)));
		when(marketDataProvider.fetchDailySeries(anyString(), anyString())).thenReturn(initial, initial, corrected);

		User admin = new User();
		admin.setUsername("admin");
		admin.setEmail("admin@example.com");
		admin.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("StrongPassword123"));
		admin.setRole(UserRole.ADMIN);
		admin.setEnabled(true);
		userRepository.save(admin);

		String token = accessToken("admin@example.com", "StrongPassword123");
		mockMvc.perform(post("/api/market-data/import")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"symbol\":\"RELIANCE.BSE\",\"exchangeCode\":\"BSE\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.symbol").value("RELIANCE.BSE"))
			.andExpect(jsonPath("$.recordsProcessed").value(1));

		mockMvc.perform(post("/api/market-data/import")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"symbol\":\"RELIANCE.BSE\",\"exchangeCode\":\"BSE\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.recordsSkipped").value(1));

		mockMvc.perform(post("/api/market-data/import")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"symbol\":\"RELIANCE.BSE\",\"exchangeCode\":\"BSE\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.recordsUpdated").value(1));

		mockMvc.perform(get("/api/companies/" + company.getId() + "/prices")
				.header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].closePrice").value(1171.8000))
				.andExpect(jsonPath("$.content[0].dataSource").value("ALPHA_VANTAGE"))
				.andExpect(jsonPath("$.content[0].priceAdjustment").value("UNADJUSTED"))
				.andExpect(jsonPath("$.content[0].providerTimeZone").value("US/Eastern"));

		mockMvc.perform(get("/api/companies/search").param("q", "Reliance").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].name").value("Reliance Industries"));
	}

	@Test
	void rateLimitedBulkItemRemainsPendingAndCanResume() throws Exception {
		saveAdmin();
		MarketDataFetchResult result = new MarketDataFetchResult("INFY.BSE", null, "US/Eastern",
				List.of(new PriceSnapshot(LocalDate.of(2026, 10, 9), new BigDecimal("100"), new BigDecimal("105"),
						new BigDecimal("99"), new BigDecimal("104"), 1000L)));
		when(marketDataProvider.fetchDailySeries(anyString(), anyString()))
				.thenThrow(new com.klyn.platform.marketdata.MarketDataProviderException("rate limit", true))
				.thenReturn(result);
		String token = accessToken("admin@example.com", "StrongPassword123");
		MockMultipartFile file = new MockMultipartFile("file", "nse.csv", "text/csv",
				"SYMBOL,NAME OF COMPANY,SERIES,PROVIDER SYMBOL\nINFY,Infosys,EQ,INFY.BSE\n".getBytes());
		String createResponse = mockMvc.perform(multipart("/api/market-data/bulk-import/nse").file(file)
				.header("Authorization", "Bearer " + token)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		long jobId = Long.parseLong(createResponse.substring(createResponse.indexOf("jobId") + 7,
				createResponse.indexOf(',', createResponse.indexOf("jobId"))));

		mockMvc.perform(post("/api/market-data/bulk-import/" + jobId + "/run").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAUSED"))
				.andExpect(jsonPath("$.pending").value(1)).andExpect(jsonPath("$.failed").value(0));
		mockMvc.perform(post("/api/market-data/bulk-import/" + jobId + "/run").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"))
				.andExpect(jsonPath("$.completed").value(1));
	}

	@Test
	void rejectsInvalidOhlcvAndDoesNotPersistIt() throws Exception {
		createInstrument("TCS.BSE", "BSE", "TCS.BSE", true);
		saveAdmin();
		when(marketDataProvider.fetchDailySeries(anyString(), anyString())).thenReturn(new MarketDataFetchResult("TCS.BSE", null,
				"US/Eastern", List.of(new PriceSnapshot(LocalDate.of(2026, 10, 9), new BigDecimal("12"), new BigDecimal("10"),
						new BigDecimal("9"), new BigDecimal("11"), 100L))));

		String token = accessToken("admin@example.com", "StrongPassword123");
		mockMvc.perform(post("/api/market-data/import")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"symbol\":\"TCS.BSE\",\"exchangeCode\":\"BSE\"}"))
			.andExpect(status().isBadRequest());
		assertEquals(0, marketPriceRepository.count());
	}

	@Test
	void mapsProviderFailuresToGenericGatewayErrors() throws Exception {
		createInstrument("INFY.BSE", "BSE", "INFY.BSE", true);
		saveAdmin();
		when(marketDataProvider.fetchDailySeries(anyString(), anyString()))
				.thenThrow(new com.klyn.platform.marketdata.MarketDataProviderException("secret api key or provider URL"));

		String token = accessToken("admin@example.com", "StrongPassword123");
		mockMvc.perform(post("/api/market-data/import")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"symbol\":\"INFY.BSE\",\"exchangeCode\":\"BSE\"}"))
			.andExpect(status().isBadGateway())
			.andExpect(jsonPath("$.message").value("Market data provider request failed"));
	}

	@Test
	void inactiveCompaniesAreExcludedFromListing() throws Exception {
		createInstrument("OLD.BSE", "BSE", "OLD.BSE", false);
		saveAdmin();

		String token = accessToken("admin@example.com", "StrongPassword123");
		mockMvc.perform(get("/api/companies").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isEmpty());
	}

	@Test
	void nonAdminCannotImportAndUnauthenticatedRequestsAreRejected() throws Exception {
		User user = new User();
		user.setUsername("user");
		user.setEmail("user@example.com");
		user.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("StrongPassword123"));
		user.setRole(UserRole.USER);
		user.setEnabled(true);
		userRepository.save(user);

		String token = accessToken("user@example.com", "StrongPassword123");
		mockMvc.perform(post("/api/market-data/import")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"symbol\":\"RELIANCE.BSE\",\"exchangeCode\":\"BSE\"}"))
			.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/companies"))
			.andExpect(status().isUnauthorized());
	}

	private String accessToken(String email, String password) throws Exception {
		String response = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		return response.substring(response.indexOf("\"accessToken\":\"") + 15, response.indexOf("\"", response.indexOf("\"accessToken\":\"") + 15));
	}

	private void createInstrument(String symbol, String exchangeCode, String providerSymbol, boolean active) {
		Company company = new Company();
		company.setName(symbol + " issuer");
		company.setCurrency("INR");
		company.setActive(active);
		companyRepository.save(company);
		Instrument instrument = new Instrument();
		instrument.setCompany(company);
		instrument.setSymbol(symbol);
		instrument.setExchangeCode(exchangeCode);
		instrument.setTradingCurrency("INR");
		instrument.setProviderSymbol(providerSymbol);
		instrument.setActive(active);
		instrumentRepository.save(instrument);
	}

	private void saveAdmin() {
		User admin = new User();
		admin.setUsername("admin");
		admin.setEmail("admin@example.com");
		admin.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("StrongPassword123"));
		admin.setRole(UserRole.ADMIN);
		admin.setEnabled(true);
		userRepository.save(admin);
	}
}
