package com.klyn.platform.service;

import org.springframework.stereotype.Service;

import com.klyn.platform.dto.UserResponse;
import com.klyn.platform.exception.ResourceNotFoundException;
import com.klyn.platform.repository.UserRepository;

@Service
public class UserService {

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public UserResponse currentUser(String email) {
		return UserResponse.from(userRepository.findByEmail(email.trim().toLowerCase())
				.orElseThrow(() -> new ResourceNotFoundException("User not found")));
	}
}