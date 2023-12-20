package com.cloudfuze.mail.repo.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.cloudfuze.mail.constants.DBConstants;
import com.cloudfuze.mail.repo.EmailJobRepository;
import com.cloudfuze.mail.repo.entities.EmailJobDetails;
import com.cloudfuze.mail.repo.entities.EmailWorkSpace;
import com.cloudfuze.mail.repo.entities.EmailInfo.PROCESS;

@Repository
public class EmailJobRepoImpl implements EmailJobRepository {

	@Autowired
	MongoTemplate mongoTemplate;

	@Override
	public EmailJobDetails save(EmailJobDetails emailWorkSpace) {
		return mongoTemplate.save(emailWorkSpace);
	}

	@Override
	public void save(List<EmailJobDetails> emailWorkSpace) {
		emailWorkSpace.stream().forEach(this::save);
	}

	@Override
	public EmailJobDetails findOne(String id) {
		Query query = new Query(Criteria.where(DBConstants.ID).is(id));
		return mongoTemplate.findOne(query, EmailJobDetails.class);
	}

	@Override
	public List<EmailJobDetails> getEmailJobDetails(String userId) {
		Order order = new Order(Direction.DESC, DBConstants.CREATEDTIME);
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId));
		return mongoTemplate.find(query.with(Sort.by(order)), EmailJobDetails.class);
	}
	
	@Override
	public List<EmailJobDetails> getEmailJobDEtails(String userId,int limit,int skip) {
		Order order = new Order(Direction.DESC, DBConstants.CREATEDTIME);
		Query query= new Query(Criteria.where(DBConstants.USERID).is(userId)).limit(limit).skip(skip); 
		return mongoTemplate.find(query.with(Sort.by(order)), EmailJobDetails.class);
	}
	
	@Override
	public List<EmailJobDetails> getInprogressJobDetails(int limit,int skip) {
		Order order = new Order(Direction.ASC, DBConstants.PRIORITY);
		Query query= new Query(Criteria.where(DBConstants.PROCESS_STATUS).is(PROCESS.IN_PROGRESS.name()).orOperator(Criteria.where(DBConstants.CAL_FILTERED).is(false),Criteria.where(DBConstants.MAILS_FILTERED).is(false))).limit(limit).skip(skip); 
		return mongoTemplate.find(query.with(Sort.by(order)), EmailJobDetails.class);
	}
}
