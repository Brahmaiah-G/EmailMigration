package com.testing.mail.repo;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;

import com.testing.mail.repo.entities.ForceExpiredToken;

public interface ForceExpiredTokenRepository extends MongoRepository<ForceExpiredToken, String> {
	
	public ForceExpiredToken findByToken(String token);
	
	@Query("{'originalExpiry': {$lte: :currentDateTime}")
	public List<ForceExpiredToken> findByExpiry(@Param("currentDateTime") String timestamp);
	
//	public List<ForceExpiredToken> findRemoveTokens(org.springframework.data.mongodb.core.query.Query query,Class<ForceExpiredToken> entityClass);
	
}
