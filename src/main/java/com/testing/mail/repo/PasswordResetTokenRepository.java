package com.testing.mail.repo;

import java.util.UUID;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.testing.mail.repo.entities.PasswordResetToken;

public interface PasswordResetTokenRepository extends MongoRepository<PasswordResetToken, String> {
	
	public PasswordResetToken findByTokenId(UUID tokenId);

}
