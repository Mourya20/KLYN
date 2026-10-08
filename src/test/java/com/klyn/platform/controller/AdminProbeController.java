package com.klyn.platform.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AdminProbeController {

	@GetMapping("/test/admin")
	@PreAuthorize("hasRole('ADMIN')")
	String adminOnly() {
		return "allowed";
	}
}