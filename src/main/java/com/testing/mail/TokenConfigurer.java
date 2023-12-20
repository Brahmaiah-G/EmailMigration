package com.testing.mail;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
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


@Slf4j
@Setter
@Getter
@Component
public class TokenConfigurer {

	@Value("${jwt.secret-phrase}")
	private String keyphrase;

	@Value("${jwt.token-expiry-time}")
	private Integer tokenExpiry;

	private Key key;
	
	//Token Builder reusable Instance
	private JwtBuilder tokenBuilder;
	
	//Token Parser reusable Instance
	private JwtParser tokenParser;

	@Autowired
	private PasswordEncoder encoder;

	@PostConstruct
	private void generateKey() {
		String privateKey =	Base64.getEncoder().encodeToString(keyphrase.getBytes());		
		this.key = Keys.hmacShaKeyFor(encoder.encode(privateKey).getBytes());
		this.tokenBuilder= Jwts.builder().setIssuer("CLOUDFUZE LLC").signWith(key, SignatureAlgorithm.HS256);
		this.tokenParser= Jwts.parserBuilder().setSigningKey(key).build();
		
	}

	public JwtBuilder jwtBuilder() {
		return tokenBuilder.setIssuedAt(new Date(System.currentTimeMillis()))
						   .setExpiration(new Date(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(tokenExpiry)))	;
				
	}

	public Claims getTokenClaims(String token) {
		Claims claims = null;
		try {
			claims = tokenParser.parseClaimsJws(token).getBody();
		} catch (ExpiredJwtException ex) {
			log.error("Exception while getting token claims: " + token + " " + ex);
		} catch (UnsupportedJwtException ex) {
			log.error("Exception while getting token claims: " + token + " " + ex);
		} catch (MalformedJwtException ex) {
			log.error("Exception while getting token claims: " + token + " " + ex);
		} catch (SignatureException ex) {
			log.error("Exception while getting token claims: " + token + " " + ex);
		} catch (IllegalArgumentException ex) {
			log.error("Exception while getting token claims: " + token + " " + ex);
		}
		return claims;
	}
	
	public Jws<Claims> parseJws(String token) {
		
		return tokenParser.parseClaimsJws(token);
	}
}