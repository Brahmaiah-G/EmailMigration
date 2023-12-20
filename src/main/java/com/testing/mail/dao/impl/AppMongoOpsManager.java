package com.testing.mail.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.stereotype.Repository;

import com.testing.mail.dao.entities.RateLimitConfigurer;
import com.testing.mail.dao.persist.OAuthKeyRepository;
import com.testing.mail.dao.persist.RateLimitRepository;
import com.testing.mail.repo.entities.OAuthKey;
import com.testing.mail.repo.entities.Clouds.CLOUD_NAME;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Setter@Getter@Slf4j
@Repository
@EnableMongoRepositories(basePackages = "com.cloudfuze.mail.dao", mongoTemplateRef = "appMongoTemplate")
public class AppMongoOpsManager {
	
	@Autowired
	private OAuthKeyRepository oAuthKeyRepository;
	
	@Autowired
	private RateLimitRepository rateLimitRepository;
		
	public String fetchSendGridAPIKey() {
		log.info("Fetching SendGrid API Key");
		return oAuthKeyRepository.findSendGridAPIKey().getClientSecret();
	}

	public OAuthKey findOAuthKeyByCloud(CLOUD_NAME cloud) {
		return oAuthKeyRepository.findByCloud(cloud);
	}
	
	public RateLimitConfigurer findRateLimitConfig(CLOUD_NAME cloudName) {
		return rateLimitRepository.findRateLimitConfig(cloudName);
	}
	
	public void saveRateLimiter(RateLimitConfigurer connfigurer) {
		rateLimitRepository.save(connfigurer);
	}
		

}
