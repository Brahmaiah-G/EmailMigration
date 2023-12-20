package com.testing.mail.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import com.github.f4b6a3.uuid.UuidCreator;
import com.testing.mail.JwtUtil;
import com.testing.mail.repo.entities.ForceExpiredToken;
import com.testing.mail.repo.entities.PlatformUser;
import com.testing.mail.repo.entities.vo.PlatformUserVO;
import com.testing.mail.repo.impl.MongoOpsManager;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Service
@Setter@Getter@Slf4j
public class JWTAuthService {
		
	@Autowired 
	private JwtUtil jwtUtil;	
	
	@Autowired 
	private MongoOpsManager mongoOpsManager;
	
	public HttpHeaders  generateSessionToken(PlatformUserVO uservo, String host, String userAgent) {
		
		String token = jwtUtil.generateToken(uservo.getId(), uservo.getName(),host,userAgent);		

		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setBearerAuth(token);
		
		return responseHeaders; 
	}

	public Boolean forceExpireToken(String authorization) {
		
		Claims claims = jwtUtil.validateToken(authorization);
		ForceExpiredToken token = new ForceExpiredToken();
		token.setToken(authorization);
		token.setOriginalExpiry(claims.getExpiration());
		ForceExpiredToken newToken = mongoOpsManager.saveForceExpiredToken(token);
		if(newToken!=null)
			return true;
		else
			return false;
	}
	
	public Boolean isTokenValid(String token) {
		Boolean valid = Boolean.FALSE;
		Claims claims = jwtUtil.validateToken(token);
		if(claims == null) {
			log.error("Claims returned null.");
			return valid;
		}
		String id = claims.getId();
		UUID publicId = UuidCreator.fromString(id);
		PlatformUser dbUser = mongoOpsManager.findUserByPublicId(publicId);				
		if(dbUser != null) {
			valid = dbUser.getName().equalsIgnoreCase(claims.getSubject());
			log.info("Valid Set to true for " + id + " and UserName: " + dbUser.getName());
		}
		return valid;
	}
	
	public Jws<Claims> extractJws(String token) {
		
		return jwtUtil.parseJws(token);
	}
}