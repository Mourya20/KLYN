package com.klyn.platform.security;

import java.io.IOException;
import java.time.Instant;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

import com.klyn.platform.exception.ApiError;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException, ServletException {
		write(response, new ApiError(Instant.now(), HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized",
				"Authentication is required", request.getRequestURI()));
	}

	private void write(HttpServletResponse response, ApiError error) throws IOException {
		response.setStatus(error.status());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), error);
	}
}