package com.testing.mail.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.testing.mail.security.JWTAuthorizationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired
	private JWTAuthorizationFilter jWTAuthorizationFilter;
	
	//White listed Uri's from Authentication and Authorization
	private static String[] whiltelitedURIs = { "/app/register",
									 			"/app/users/check/{email}",
									 			"/app/login",
									 			"/app/forgot-password/{email}",
									 			"/app/pwd/verify/{token}/user/{email}",
									 			"/app/pwd/reset/{token}/{password}"
									 			
									 };
	//White listed Uri's from Authentication and Authorization
	private static  String[] swaggerURIs = { "/v2/api-docs",
											 "/v3/**",
								             "/configuration/**",
								             "/swagger-resources/**",                                   
								             "/swagger-ui.html",
								             "/swagger-ui/**",
								             "/webjars/**"};
	
	@Override
    protected void configure(HttpSecurity security) throws Exception
    {
	     security.httpBasic().disable()
			     .cors().disable()
			     .csrf().disable()     
			     .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
			     .authorizeRequests()
			     .antMatchers(whiltelitedURIs)	    		 	  			    		 	 
			     .permitAll()
			     .anyRequest().authenticated()
			     .and().addFilterBefore(jWTAuthorizationFilter,UsernamePasswordAuthenticationFilter.class);
    }	
	
	//Excluding Swagger from security checks 
    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers(swaggerURIs);
    }
}
