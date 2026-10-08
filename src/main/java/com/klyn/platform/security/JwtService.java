package com.klyn.platform.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.klyn.platform.config.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	private final SecretKey signingKey;
	private final long expiration;

	public JwtService(JwtProperties properties) {
		this.signingKey = createKey(properties.getSecret());
		this.expiration = properties.getExpiration();
	}

	public String generateToken(UserDetails userDetails) {
		Date issuedAt = new Date();
		return Jwts.builder()
				.subject(userDetails.getUsername())
				.issuedAt(issuedAt)
				.expiration(new Date(issuedAt.getTime() + expiration))
				.claim("roles", userDetails.getAuthorities().stream().map(Object::toString).toList())
				.signWith(signingKey)
				.compact();
	}

	public String extractUsername(String token) {
		return parseClaims(token).getSubject();
	}

	public boolean isTokenValid(String token, UserDetails userDetails) {
		Claims claims = parseClaims(token);
		return claims.getSubject().equals(userDetails.getUsername())
				&& claims.getExpiration().after(new Date());
	}

	private Claims parseClaims(String token) {
		return Jwts.parser().verifyWith((SecretKey) signingKey).build().parseSignedClaims(token).getPayload();
	}

	private SecretKey createKey(String secret) {
		return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}
}