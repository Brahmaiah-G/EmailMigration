package com.cloudfuze.mail.dao.persist;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.cloudfuze.mail.repo.entities.Clouds.CLOUD_NAME;
import com.cloudfuze.mail.repo.entities.OAuthKey;

@Repository
public interface OAuthKeyRepository extends MongoRepository<OAuthKey, String> {
	
	@Query(value = "{cloudName : SENDGRID}",fields = "{ clientSecret : 1 }")
	public OAuthKey findSendGridAPIKey();
	@Query(value = "{cloudName : ?0}")
	public OAuthKey findByCloud(CLOUD_NAME cloudName);

}
