package com.cloudfuze.mail.repo;

import java.util.UUID;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.cloudfuze.mail.repo.entities.PasswordResetToken;

public interface PasswordResetTokenRepository extends MongoRepository<PasswordResetToken, String> {
	
	public PasswordResetToken findByTokenId(UUID tokenId);

}
