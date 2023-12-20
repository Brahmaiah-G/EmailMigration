package com.testing.mail.repo.impl;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.testing.mail.repo.VendorOAuthCredentialRepository;
import com.testing.mail.repo.entities.VendorOAuthCredential;
import com.testing.mail.repo.entities.Clouds.CLOUD_NAME;

@Repository
public class VendorOAuthCredentialImpl implements VendorOAuthCredentialRepository {

	@Autowired
	MongoTemplate mongoTemplate;
	@Override
	public VendorOAuthCredential findByUserId(String userId) {
		Query query = new Query(Criteria.where("userId").is(userId));
		return mongoTemplate.findOne(query, VendorOAuthCredential.class);	
	}

	@Override
	public VendorOAuthCredential findByUserIdAndDomainNameAndVendor(String userId, String domain, CLOUD_NAME label,
			String vendorId) {
		Query query = new Query(
				Criteria.where("userId").is(userId)
				.and("domainName").is(domain)
				.and("cloudName").is(label)
				.and("vendorId").is(vendorId))
				.with(Sort.by(Sort.Direction.DESC, "lastRefreshed"));
			return mongoTemplate.findOne(query, VendorOAuthCredential.class);
	}

	@Override
	public VendorOAuthCredential save(VendorOAuthCredential credentials) {
		if(credentials.getId() == null) {
			credentials.setId(ObjectId.get().toString());			
		}
		return mongoTemplate.save(credentials);
	}

	@Override
	public void delete(VendorOAuthCredential credential) {
		mongoTemplate.remove(credential);
	}

	@Override
	public VendorOAuthCredential findById(String id) {
		Query query = new Query(Criteria.where("_id").is(id));
		return mongoTemplate.findOne(query, VendorOAuthCredential.class);
	}

	@Override
	public VendorOAuthCredential removeOne(String id) {
		Query query = new Query(Criteria.where("_id").is(id));
		return mongoTemplate.findAndRemove(query, VendorOAuthCredential.class);
	}

}
