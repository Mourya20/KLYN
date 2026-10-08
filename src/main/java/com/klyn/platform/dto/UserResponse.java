package com.klyn.platform.dto;

import com.klyn.platform.entity.User;

public record UserResponse(Long id, String username, String email, String role) {
	public static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole().name());
	}
}