package com.cloudfuze.mail.repo;

import java.util.UUID;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.cloudfuze.mail.repo.entities.PlatformUser;

public interface PlatformUserRepository extends MongoRepository<PlatformUser, String> {

	public PlatformUser findByEmail(String email);
	
	public PlatformUser findByPublicId(UUID publicId);
	
	 @Query(value = "{$and:[{'email' : ?0 }, { 'ent' : ?1 }]}")
	public PlatformUser findByEmailandEnt(String email,String ent);

	
}
