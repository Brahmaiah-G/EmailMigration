package com.testing.mail.service;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.testing.mail.exceptions.InvalidTokenException;
import com.testing.mail.exceptions.TokenExpiredException;
import com.testing.mail.repo.entities.PasswordResetToken;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


@Setter@Getter@Slf4j
@Service
public class PlatformUserPasswordModificationService {
	
	//token expiry duration in minutes
	@Value("${platform.password-reset.expiry}")
	private Integer tokenExpiry;
	
	//token secret phase
	@Value("${platform.password-reset.secret}")
	private String secret;
	
	/*
	 * Token Signing key
	 * Generated at runtime
	 */	
	private Key key;
	
	//Token Builder reusable Instance
	private JwtBuilder tokenBuilder;
	
	//Token Parser reusable Instance
	private JwtParser tokenParser; 
	

	@PostConstruct
	public void handleKey() {
		this.key= Keys.hmacShaKeyFor(Base64.getEncoder().encode(secret.getBytes() ) );
		this.tokenBuilder= Jwts.builder().setIssuer("CLOUDFUZE LLC").signWith(key, SignatureAlgorithm.HS256);
		this.tokenParser= Jwts.parserBuilder().setSigningKey(key).build();
	}
	
	public String generateToken(String tokenId,String name) {
		
		return tokenBuilder.setIssuedAt(new Date(System.currentTimeMillis()))
						.setExpiration(new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(tokenExpiry)))
						.setId(tokenId)
						.setSubject(name)						
						.compact();
	}
	
	
	public String uncompressToken(String token) {
		
		Claims claims =  parseToken(token);
		
		return claims.getId();
	}
	
	/**Checks token expired or not, valid or not, authentic or not
	 *
	 */	
	public Boolean verifyToken(String token) {
				
		try {			

			//Validate Authenticity of token
			Boolean valid = validateToken(token);
			if(!valid)
				throw new InvalidTokenException("Untrusted Token");
			//Validate Expiry of token
			Boolean expired = hasTokenExpired(token);
			if(expired)
				throw new TokenExpiredException("Link Expired!. Try Again in sometime");

		} 
		catch (SignatureException e) {
			log.error("Inavlid Token - {}",token);
			return false;
		} 
		catch (ExpiredJwtException e) {
			log.error("Token Expired - {}",token);
			return false;
		} 
		catch (UnsupportedJwtException e) {
			log.error("Token Parse Exception - {}",token);
			return false;
		} 
		catch (MalformedJwtException e) {
			log.error("Token Corrupted - {}",token);
			return false;
		} 
		catch (IllegalArgumentException e) {
			log.error("Unable to validate Token - {}",token);
			return false;
		}
		return true;
	}
	
	//Get Claims For a valid token
	private Claims parseToken(String token) {	
		
		return tokenParser.parseClaimsJws(token).getBody();
	}
	
	//validating Token Authenticity only 
	public Boolean validateToken(String token) {
		
		Boolean isValid = false;
	
		try {			
			tokenParser.parseClaimsJws(token).getBody();
			isValid = true;					
		} 
		catch (SignatureException e) {
			log.error("Inavlid Token - {}",token);
			isValid = false;
		} 
		catch (UnsupportedJwtException e) {
			log.error("Token Parse Exception - {} ->" + "{}",token,e.getMessage());
			isValid = false;
		} 
		catch (MalformedJwtException e) {			
			log.error("Token Corrupted - {} ->" + "{}",token,e.getMessage());
			isValid = false;
		} 
		catch (IllegalArgumentException e) {			
			log.error("Unable to validate Token - {} ->" + "{}",token,e.getMessage());
			isValid = false;
		}
		log.info("Token {} "+ (isValid?"Valid":"InValid"),isValid);	
		return isValid;
	}
	
	//validating Token Expiry only 
	public Boolean hasTokenExpired(String token) {
		
		boolean expired = false;

		try {			
			Claims claims = tokenParser.parseClaimsJws(token).getBody();
			expired = claims.getExpiration().before(new Date(System.currentTimeMillis()));			
		} 
		catch (ExpiredJwtException ex) {			
			expired = true;
		}
		log.info("Token - {} " + (expired?"Expired":"Valid") ,token);
		return expired;
	}
	
	public PasswordResetToken getPasswordResetToken(UUID tokenId, String token, UUID userPublicId ) {
		
		PasswordResetToken passwordResetToken = new PasswordResetToken();
		
		passwordResetToken.setTokenId(tokenId);
		passwordResetToken.setToken(token);
		passwordResetToken.setUserPublicId(userPublicId);
		passwordResetToken.setOriginalExpiry(new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(tokenExpiry)));
		
		return passwordResetToken;
	}

}