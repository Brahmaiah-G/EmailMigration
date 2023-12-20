package com.cloudfuze.mail.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@Configuration
@EnableWebMvc
public class SwaggerConfig{
	
	@Value("${server.servlet.context-path}")
	private String contextPath;
	

	  @Bean
	  public OpenAPI springShopOpenAPI() {
	    return (new OpenAPI())
	      .info((new Info()).title("CloudFuze Connect API")
	        .description("CloudFuze Connect - A SaaS Management Platform")
	        .version("v1.0")
	        .license((new License()).name("CloudFuze LLC").url("https://www.cloudfuze.com")))
	      	.externalDocs((new ExternalDocumentation())
	        .description("CloudFuze Connect Documentation")
	        .url("https://www.cloudfuze.com/blog/"));
	  }
}