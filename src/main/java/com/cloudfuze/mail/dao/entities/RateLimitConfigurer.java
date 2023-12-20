package com.cloudfuze.mail.dao.entities;

import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import com.cloudfuze.mail.repo.entities.Clouds.CLOUD_NAME;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Document(collection = "rateLimitConfig")
@TypeAlias(value ="rateLimitConfig")
public class RateLimitConfigurer {

	@Id
	private String id;
	private Map<String, Integer>rateLimits;
	private CLOUD_NAME cloudName;
	
}
