package com.klyn.platform.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.klyn.platform.repository.UserRepository;
import com.klyn.platform.entity.User;
import com.klyn.platform.security.JwtService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AdminProbeController.class)
class AuthSecurityIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JwtService jwtService;

	@BeforeEach
	void cleanUsers() {
		userRepository.deleteAll();
	}

	@Test
	void registrationHashesPasswordAndLoginProtectsCurrentUser() throws Exception {
		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"mourya\",\"email\":\"MOURYA@example.com\",\"password\":\"StrongPassword123\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.password").doesNotExist());

		String storedPassword = userRepository.findByEmail("mourya@example.com").orElseThrow().getPassword();
		org.junit.jupiter.api.Assertions.assertNotEquals("StrongPassword123", storedPassword);
		org.junit.jupiter.api.Assertions.assertTrue(storedPassword.startsWith("$2"));

		String response = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"mourya@example.com\",\"password\":\"StrongPassword123\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andReturn().getResponse().getContentAsString();
		String token = response.substring(response.indexOf("\"accessToken\":\"") + 15);
		token = token.substring(0, token.indexOf('"'));

		mockMvc.perform(get("/api/users/me"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("mourya"))
				.andExpect(jsonPath("$.role").value("USER"))
				.andExpect(jsonPath("$.password").doesNotExist());
	}

	@Test
	void duplicateEmailAndInvalidTokenAreRejected() throws Exception {
		String request = "{\"username\":\"mourya\",\"email\":\"mourya@example.com\",\"password\":\"StrongPassword123\"}";
		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(request))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"other\",\"email\":\"mourya@example.com\",\"password\":\"StrongPassword123\"}"))
				.andExpect(status().isConflict());
		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"mourya\",\"email\":\"other@example.com\",\"password\":\"StrongPassword123\"}"))
				.andExpect(status().isConflict());
		mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer invalid-token"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void wrongPasswordUnknownEmailAndDisabledUserCannotLogin() throws Exception {
		register("mourya", "mourya@example.com", "StrongPassword123");
		login("mourya@example.com", "WrongPassword123").andExpect(status().isUnauthorized());
		login("unknown@example.com", "StrongPassword123").andExpect(status().isUnauthorized());

		User disabled = userRepository.findByEmail("mourya@example.com").orElseThrow();
		disabled.setEnabled(false);
		userRepository.save(disabled);
		login("mourya@example.com", "StrongPassword123").andExpect(status().isUnauthorized());
	}

	@Test
	void expiredAndTamperedTokensAreRejected() throws Exception {
		register("mourya", "mourya@example.com", "StrongPassword123");
		User user = userRepository.findByEmail("mourya@example.com").orElseThrow();
		String validToken = jwtService.generateToken(org.springframework.security.core.userdetails.User
				.withUsername(user.getEmail()).password(user.getPassword()).roles(user.getRole().name()).build());
		String tamperedToken = validToken.substring(0, validToken.length() - 2) + "xx";
		String expiredToken = Jwts.builder().subject(user.getEmail()).issuedAt(new Date(System.currentTimeMillis() - 2_000))
				.expiration(new Date(System.currentTimeMillis() - 1_000))
				.signWith(Keys.hmacShaKeyFor("test-secret-that-is-long-enough-for-hmac-signing".getBytes(StandardCharsets.UTF_8)))
				.compact();
		mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + tamperedToken))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + expiredToken))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void userIsForbiddenFromAdminOnlyEndpoint() throws Exception {
		register("mourya", "mourya@example.com", "StrongPassword123");
		String token = accessToken("mourya@example.com", "StrongPassword123");
		mockMvc.perform(get("/test/admin").header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.error").value("Forbidden"));
	}

	@Test
	void invalidRegistrationIsRejectedAtApiBoundary() throws Exception {
		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"x\",\"email\":\"not-an-email\",\"password\":\"short\"}"))
			.andExpect(status().isBadRequest());
		login("", "").andExpect(status().isBadRequest());
	}

	@Test
	void adminCanAccessAdminOnlyEndpoint() throws Exception {
		User admin = new User();
		admin.setUsername("admin");
		admin.setEmail("admin@example.com");
		admin.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
				.encode("StrongPassword123"));
		admin.setRole(com.klyn.platform.entity.UserRole.ADMIN);
		admin.setEnabled(true);
		userRepository.save(admin);

		String token = accessToken("admin@example.com", "StrongPassword123");
		mockMvc.perform(get("/test/admin").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
	}

	private void register(String username, String email, String password) throws Exception {
		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"" + username + "\",\"email\":\"" + email
						+ "\",\"password\":\"" + password + "\"}"))
				.andExpect(status().isCreated());
	}

	private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
		return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
	}

	private String accessToken(String email, String password) throws Exception {
		String response = login(email, password).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		String token = response.substring(response.indexOf("\"accessToken\":\"") + 15);
		return token.substring(0, token.indexOf('"'));
	}
}