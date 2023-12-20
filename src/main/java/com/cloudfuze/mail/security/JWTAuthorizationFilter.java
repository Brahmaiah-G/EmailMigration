package com.cloudfuze.mail.security;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.cloudfuze.mail.config.PlatformUserDetailSerice;
import com.cloudfuze.mail.constants.Const;
import com.cloudfuze.mail.service.JWTAuthService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Setter@Getter@Slf4j
@Component
public class JWTAuthorizationFilter extends OncePerRequestFilter {	
	
	@Autowired
	private JWTAuthService authService;

	@Autowired
	private PlatformUserDetailSerice userDetailsService;	 

	@SuppressWarnings("rawtypes")
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
			
		//Extract All Request Headers
		HttpHeaders headers = resolveHeaders(request);
		log.warn(request.toString());
		// Get Authorization
		String authorization = headers.getFirst(HttpHeaders.AUTHORIZATION);
		// Get Host
		String reqHost = headers.getFirst(HttpHeaders.HOST);
		// Get Authorization
		String reqUserAgent = headers.getFirst(HttpHeaders.USER_AGENT);
		
		// Check for valid Authorization Header
		if (authorization == null || org.apache.commons.lang3.StringUtils.isBlank(authorization) || !authorization.startsWith(Const.BEARER)) {
			response.reset();
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setHeader("Error", "Mismatch U");
			filterChain.doFilter(request, response);
			
            return;
        }
		//Extract JWT from authorization
		String token = StringUtils.delete(authorization, Const.BEARER);
		// Check for appropriate JWT
		if (org.apache.commons.lang3.StringUtils.isBlank(token)) {
			response.reset();
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setHeader("Error", "Mismatch U");
			filterChain.doFilter(request, response);
			return;
		}
		// check token validity
		Boolean isTokenValid = authService.isTokenValid(token);	
		
		// invalidate request if token is invalid
		if(!isTokenValid) {
			response.reset();
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setHeader("Error", "Mismatch U");
			return;
//			filterChain.doFilter(request, response);
//			return;
		}		
		
		Jws<Claims> jws = authService.extractJws(token);	
		
		JwsHeader tokenHeader = jws.getHeader();
		
//		String host = tokenHeader.get(HttpHeaders.HOST).toString();
//		String userAgent = tokenHeader.get(HttpHeaders.USER_AGENT).toString();
//		
//		if (!reqHost.equalsIgnoreCase(host) || !reqUserAgent.equalsIgnoreCase(userAgent)) {
//			response.reset();
//			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//			response.setHeader("Error", "Mismatch H");
//			return;
//		}
			
		
		String userId = jws.getBody().getId();		
		
		// Fetch User Details
		UserDetails user = userDetailsService.loadUserByUsername(userId);
		if (user == null) {	
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setHeader("Error", "Invalid User");
			filterChain.doFilter(request, response);
			return;
		} 
		else {
			/* 
			 * Construct Authentication Credentials and add to request scope 
			 */ 
			UsernamePasswordAuthenticationToken authToken = 
			new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword(), user.getAuthorities());
			
			authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			
			SecurityContextHolder.getContext().setAuthentication(authToken);
			request.setAttribute("userId", user.getUsername());
		}
		
		filterChain.doFilter(request, response);	

	}

	public HttpHeaders resolveHeaders(HttpServletRequest request) {
		
		HttpHeaders headers = new HttpHeaders(); 
		
		Enumeration<String> headerNames = request.getHeaderNames();
	    if (headerNames != null) {
	        while (headerNames.hasMoreElements()) {
	           String name = headerNames.nextElement();	           
	           headers.add(name, request.getHeader(name));
	        }
	    }
	    return headers;
	}

}


