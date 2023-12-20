package com.testing.mail.dao.persist;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.testing.mail.dao.entities.RateLimitConfigurer;
import com.testing.mail.repo.entities.Clouds.CLOUD_NAME;

@Repository
public interface RateLimitRepository extends MongoRepository<RateLimitConfigurer, String>{
	
	@Query(value = "{cloudName : ?0}")
	public RateLimitConfigurer findRateLimitConfig(CLOUD_NAME cloudName);
	
}
