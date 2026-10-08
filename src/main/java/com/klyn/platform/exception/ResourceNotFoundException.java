package com.klyn.platform.exception;

public class ResourceNotFoundException extends RuntimeException {
	public ResourceNotFoundException(String message) { super(message); }
}