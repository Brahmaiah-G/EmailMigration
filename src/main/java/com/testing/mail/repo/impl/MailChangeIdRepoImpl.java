package com.testing.mail.repo.impl;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.testing.mail.constants.DBConstants;
import com.testing.mail.repo.MailChangeIdRepository;
import com.testing.mail.repo.entities.MailChangeIds;
import com.testing.mail.repo.entities.EmailInfo.PROCESS;

@Repository
public class MailChangeIdRepoImpl implements MailChangeIdRepository{

	@Autowired
	MongoTemplate mongoTemplate;
	
	
	@Override
	public MailChangeIds save(MailChangeIds changeIds) {
		return mongoTemplate.save(changeIds);
	}

	@Override
	public void save(List<MailChangeIds> changeIds) {
		changeIds.stream().forEach(this::save);
	}

	@Override
	public MailChangeIds findOne(String id) {
		Query query = new Query(Criteria.where(DBConstants.ID).is(id));
		return mongoTemplate.findOne(query, MailChangeIds.class);
	}

	@Override
	public void delete(String id) {
		Query query = new Query(Criteria.where(DBConstants.ID).is(id));
		mongoTemplate.remove(query, MailChangeIds.class);
		
	}

	@Override
	public List<MailChangeIds> find(String emailWorkSpaceId) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId));
		return mongoTemplate.find(query, MailChangeIds.class);
	}
	
	@Override
	public long getInprogressCount(String emailWorkSpaceId) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and(DBConstants.PROCESS_STATUS).is("IN_PROGRESS"));
		return mongoTemplate.count(query, MailChangeIds.class);
	}
	
	@Override
	public long getNot_StartedCount(String emailWorkSpaceId) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and(DBConstants.PROCESS_STATUS).in(Arrays.asList("IN_PROGRESS" ,"NOT_STARTED","NOT_PROCESSED")));
		return mongoTemplate.count(query, MailChangeIds.class);
	}
	@Override
	public void findAndUpdateMailChangeId(String workSpaceId,String newEmailWorkSpaceId) {
		Query query= new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(workSpaceId).and(DBConstants.PROCESS_STATUS).is(PROCESS.NOT_PROCESSED.name())); 
		Update update = new Update();
		update.set(DBConstants.EMAILWORKSPACEID, newEmailWorkSpaceId);
		//update.set(DBConstants.THREADBY, null);
		mongoTemplate.updateMulti(query, update, MailChangeIds.class);
	}
	
	public MailChangeIds findBySourceId(String emailWorkSpaceId,String sourceId) {
		Query query= new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and(DBConstants.SOURCE_ID).is(sourceId)); 
		return mongoTemplate.findOne(query, MailChangeIds.class);

	}

}
