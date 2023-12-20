package com.cloudfuze.mail;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import com.cloudfuze.mail.repo.entities.ForceExpiredToken;
import com.cloudfuze.mail.repo.impl.MongoOpsManager;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

@Component
public class JwtUtil {

	@Autowired
	TokenConfigurer config;

	@Autowired
	private MongoOpsManager mongoOpsManager;

	public String generateToken(String id, String subject, String host, String userAgent) {
		return config.jwtBuilder()			
					.setId(id)
					.setSubject(subject)
					.setHeaderParam(HttpHeaders.HOST, host)
					.setHeaderParam(HttpHeaders.USER_AGENT, userAgent)
					.compact();
	}

	public Claims validateToken(String token) {
		Claims tokenClaims = config.getTokenClaims(token);
		if (tokenClaims != null) {
			Boolean isExpired = tokenClaims.getExpiration().after(new Date(System.currentTimeMillis()));
			if (!isExpired)
				return null;
		}
		Boolean isForceExpired = verifyForceExpiryOfToken(token);
		if (isForceExpired)
			return null;
		return tokenClaims;
	}

	private boolean verifyForceExpiryOfToken(String token) {

		ForceExpiredToken expiredToken = mongoOpsManager.fetchForceExpiredToken(token);
		if (expiredToken != null)
			return true;
		return false;
	}
	
	public Jws<Claims> parseJws(String token) {
		
		return config.parseJws(token);
	}
}