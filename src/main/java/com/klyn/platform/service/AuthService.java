package com.klyn.platform.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.klyn.platform.dto.AuthResponse;
import com.klyn.platform.dto.LoginRequest;
import com.klyn.platform.dto.RegisterRequest;
import com.klyn.platform.dto.UserResponse;
import com.klyn.platform.entity.User;
import com.klyn.platform.entity.UserRole;
import com.klyn.platform.exception.DuplicateResourceException;
import com.klyn.platform.security.JwtService;
import com.klyn.platform.repository.UserRepository;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
			AuthenticationManager authenticationManager, JwtService jwtService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
	}

	public UserResponse register(RegisterRequest request) {
		String username = request.username().trim();
		String email = normalizeEmail(request.email());
		if (userRepository.existsByUsername(username)) {
			throw new DuplicateResourceException("Username is already registered");
		}
		if (userRepository.existsByEmail(email)) {
			throw new DuplicateResourceException("Email is already registered");
		}

		User user = new User();
		user.setUsername(username);
		user.setEmail(email);
		user.setPassword(passwordEncoder.encode(request.password()));
		user.setRole(UserRole.USER);
		user.setEnabled(true);
		try {
			return UserResponse.from(userRepository.save(user));
		} catch (DataIntegrityViolationException exception) {
			throw new DuplicateResourceException("Username or email is already registered");
		}
	}

	public AuthResponse login(LoginRequest request) {
		String email = normalizeEmail(request.email());
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(email, request.password()));
		return new AuthResponse(jwtService.generateToken((UserDetails) authentication.getPrincipal()), "Bearer");
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase();
	}
}