package com.testing.mail.repo.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.testing.mail.constants.DBConstants;
import com.testing.mail.repo.WorkSpaceRepository;
import com.testing.mail.repo.entities.EmailWorkSpace;
import com.testing.mail.repo.entities.EmailInfo.PROCESS;

@Repository
public class WorkSpaceRepoImpl implements WorkSpaceRepository {

	@Autowired
	private MongoTemplate mongoTemplate;
	
	
	@Override
	public EmailWorkSpace save(EmailWorkSpace emailWorkSpace) {
		return mongoTemplate.save(emailWorkSpace);
	}

	@Override
	public void save(List<EmailWorkSpace> emailWorkSpace) {
		mongoTemplate.save(emailWorkSpace);
	}

	@Override
	public EmailWorkSpace findOne(String id) {
		Query query= new Query(Criteria.where(DBConstants.ID).is(id)); 
		return mongoTemplate.findOne(query, EmailWorkSpace.class);
	}

	@Override
	public List<EmailWorkSpace> getWorkspaces(String userId) {
		Query query= new Query(Criteria.where(DBConstants.USERID).is(userId)); 
		return mongoTemplate.find(query, EmailWorkSpace.class);
	}

	@Override
	public List<EmailWorkSpace> getWorkspaces(int limit,int skip) {
		Order order = new Order(Direction.ASC, DBConstants.PRIORITY);
		Query query= new Query(Criteria.where(DBConstants.PROCESS_STATUS).is(PROCESS.IN_PROGRESS.name())).limit(limit).skip(skip); 
		return mongoTemplate.find(query.with(Sort.by(order)), EmailWorkSpace.class);
	}
	
	@Override
	public List<EmailWorkSpace> getWorkspacesByJob(String jobId,int limit,int skip) {
		Order order = new Order(Direction.ASC, DBConstants.PRIORITY);
		Query query= new Query(Criteria.where(DBConstants.JOB_ID).is(jobId));
		int pageNo =0;
		int pageSize = 100;
		List<EmailWorkSpace> lists = new ArrayList<>();
		while(true) {
			List<EmailWorkSpace> emails = mongoTemplate.find(query.limit(pageSize).skip(pageNo).with(Sort.by(order)), EmailWorkSpace.class);
			if(emails.isEmpty()) {
				break;
			}
			pageNo = pageNo+pageSize;
			lists.addAll(emails);
		}
		return lists;
	}
	
	@Override
	public EmailWorkSpace getWorkSpaceBasedOnPaths(String userId,String fromMailId,String toMailId,String fromCloudName,String toCloudName,String ownerEmailId) {
		Order order = new Order(Direction.DESC, DBConstants.CREATEDTIME);
		Query query= new Query(Criteria.where(DBConstants.USERID).is(userId).and(DBConstants.OWNER_EMAIL_ID).is(ownerEmailId).and(DBConstants.FROM_CLOUD).is(fromCloudName).and(DBConstants.TO_CLOUD)
				.is(toCloudName).and(DBConstants.FROM_MAIL_ID).is(fromMailId).and(DBConstants.TO_MAIL_ID).is(toMailId));
		return mongoTemplate.findOne(query.with(Sort.by(order)), EmailWorkSpace.class);
	}
	
	@Override
	public EmailWorkSpace getPremigrationWorkSpace(String fromEmail,String cloud) {
		Order order = new Order(Direction.DESC, DBConstants.CREATEDTIME);
		Query query= new Query(Criteria.where("fromMailId").is(fromEmail).and("fromCloud").is(cloud).and("preScan").is(true)); 
		return mongoTemplate.findOne(query.with(Sort.by(order)), EmailWorkSpace.class);
	}
	
	@Override
	public void removeOne(String workSpaceId) {
		mongoTemplate.remove(new Query(Criteria.where("_id").is(workSpaceId)), EmailWorkSpace.class);
	}
	

}
