package com.klyn.platform.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

class JwtPropertiesTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void acceptsConfiguredStrongSecretAndExpiration() {
		JwtProperties properties = new JwtProperties();
		properties.setSecret("test-secret-that-is-long-enough-for-hmac-signing");
		properties.setExpiration(3_600_000);

		assertTrue(validator.validate(properties).isEmpty());
	}

	@Test
	void rejectsShortSecretAndNonPositiveExpiration() {
		JwtProperties properties = new JwtProperties();
		properties.setSecret("short");
		properties.setExpiration(0);

		assertFalse(validator.validate(properties).isEmpty());
	}
}