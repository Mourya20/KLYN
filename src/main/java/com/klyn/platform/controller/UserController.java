package com.klyn.platform.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import com.klyn.platform.dto.UserResponse;
import com.klyn.platform.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/me")
	@SecurityRequirement(name = "bearerAuth")
	public UserResponse currentUser(Authentication authentication) {
		return userService.currentUser(authentication.getName());
	}
}