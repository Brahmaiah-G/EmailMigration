package com.testing.mail.repo.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.testing.mail.constants.DBConstants;
import com.testing.mail.repo.PremigrationRepository;
import com.testing.mail.repo.entities.PremigrationDetails;

@Repository
public class PremigrationRepoImpl implements PremigrationRepository {

	@Autowired
	MongoTemplate mongoTemplate;
	
	
	@Override
	public void save(PremigrationDetails premigrationDetails) {
		mongoTemplate.save(premigrationDetails);
	}

	@Override
	public void save(List<PremigrationDetails> premigrationDetails) {
		premigrationDetails.forEach(this::save);
	}

	@Override
	public PremigrationDetails findById(String id) {
		Query query = new Query(Criteria.where("_id").is(id));
		return mongoTemplate.findOne(query, PremigrationDetails.class);
	}

	@Override
	public PremigrationDetails findByEmailOrCloud(String cloudId) {
		Query query = new Query(Criteria.where(DBConstants.CLOUDID).is(cloudId));
		return mongoTemplate.findOne(query, PremigrationDetails.class);
	}

	@Override
	public List<PremigrationDetails> findByUserId(String userId) {
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId));
		return mongoTemplate.find(query, PremigrationDetails.class);
	}

	@Override
	public List<PremigrationDetails> findByProcessStatus(List<String> processStauts) {
		Query query = new Query(Criteria.where(DBConstants.PROCESS_STATUS).in(processStauts));
		return mongoTemplate.find(query, PremigrationDetails.class);
	}

	@Override
	public PremigrationDetails findByWorkSpace(String workSpaceId) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(workSpaceId));
		return mongoTemplate.findOne(query, PremigrationDetails.class);
	}

}
