package com.testing.mail.repo.impl;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.stereotype.Repository;

import com.testing.mail.repo.ForceExpiredTokenRepository;
import com.testing.mail.repo.PasswordResetTokenRepository;
import com.testing.mail.repo.PlatformUserRepository;
import com.testing.mail.repo.entities.ForceExpiredToken;
import com.testing.mail.repo.entities.PasswordResetToken;
import com.testing.mail.repo.entities.PlatformUser;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Setter@Getter
@Slf4j
@Repository
@EnableMongoRepositories(basePackages = "com.cloudfuze.mail.repo",mongoTemplateRef = "mongoTemplate")
public class MongoOpsManager {
	
	@Autowired
	private PlatformUserRepository platformUserRepository;
	
	@Autowired
	private ForceExpiredTokenRepository forceExpiredTokenRepository;
	
	@Autowired
	private PasswordResetTokenRepository passwordResetTokenRepository;
	
	public PlatformUser registerUser(PlatformUser user) {
		return platformUserRepository.insert(user);
	}
	
	public PlatformUser findUserByEmail(String email) {
		return platformUserRepository.findByEmail(email);
	}
	
	public PlatformUser findUserByEmailAndEnt(String email,String ent) {
		return platformUserRepository.findByEmailandEnt(email, ent);
	}
		
	public ForceExpiredToken saveForceExpiredToken(ForceExpiredToken token) {
		return forceExpiredTokenRepository.save(token);
	}
	
	public ForceExpiredToken fetchForceExpiredToken(String token) {
		return forceExpiredTokenRepository.findByToken(token);
	}
	
	public PlatformUser findUserById(String id) {
		return platformUserRepository.findById(id).orElse(null);
	}
	
	public PasswordResetToken fetchTokenbyTokenId(UUID uuid) {
		return passwordResetTokenRepository.findByTokenId(uuid);
	}
	
	public PlatformUser findUserByPublicId(UUID uuid) {
		return platformUserRepository.findByPublicId(uuid);
	}
	
	public PlatformUser updatePlatformUser(PlatformUser user) {
		return platformUserRepository.save(user);
	}
	
	public PlatformUser findPlatformUserByPasswordResetToken(UUID tokenId) {
		
		PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenId(tokenId);
		
		return platformUserRepository.findByPublicId(resetToken.getUserPublicId());
		
	}

	public PasswordResetToken savePasswordResetToken(PasswordResetToken passwordResetToken) {
		
		return passwordResetTokenRepository.insert(passwordResetToken);		
	}
	
}