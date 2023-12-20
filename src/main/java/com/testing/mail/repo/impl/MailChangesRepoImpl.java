package com.testing.mail.repo.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.testing.mail.constants.DBConstants;
import com.testing.mail.repo.MailChangesRepository;
import com.testing.mail.repo.entities.MailChanges;

import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
public class MailChangesRepoImpl implements MailChangesRepository {

	@Autowired
	MongoTemplate mongoTemplate;
	
	@Override
	public void save(MailChanges changes) {
		mongoTemplate.save(changes);
	}

	@Override
	public void save(List<MailChanges> chanegs) {
		chanegs.forEach(this::save);
	}

	@Override
	public List<MailChanges> find(String mailChangeId) {
		Query query = new Query(Criteria.where(DBConstants.MAIL_CHANGE_ID).is(mailChangeId));
		return mongoTemplate.find(query, MailChanges.class);
	}

	@Override
	public List<MailChanges> findByWorkSpace(String emailWorkSpaceId) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId));
		return mongoTemplate.find(query, MailChanges.class);
	}

	@Override
	public List<MailChanges>findByWorkSpaceParent(String emailWorkSpaceId,String sourceParent,boolean folder,boolean events){
		int pageNo=0;
		int pageSize = 100;
		List<MailChanges>changeList = new ArrayList<>();
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and("sourceParent").is(sourceParent).and("folder").is(folder).and("events").is(events).and(DBConstants.PROCESS_STATUS).is(MailChanges.PROCESS.NOT_PROCESSED.name()));
		while(true) {
			log.warn("==Fetched changes==="+changeList.size()+"==workSpaceID=="+emailWorkSpaceId);
			List<MailChanges>changes = mongoTemplate.find(query.skip(pageNo).limit(pageSize), MailChanges.class);
			if(changes!=null && !changes.isEmpty()) {
				changeList.addAll(changes);
				pageNo = pageNo+pageSize;
			}else {
				break;
			}
		}
		return changeList;
	}
	
	@Override
	public List<MailChanges>findByWorkSpaceParent(String emailWorkSpaceId,String sourceParent,boolean events){
		int pageNo=0;
		int pageSize = 100;
		List<MailChanges>changeList = new ArrayList<>();
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and("sourceParent").is(sourceParent).and("events").is(events).and(DBConstants.PROCESS_STATUS).is(MailChanges.PROCESS.NOT_PROCESSED.name()));
		while(true) {
			log.warn("==Fetched changes==="+changeList.size()+"==workSpaceID=="+emailWorkSpaceId);
			List<MailChanges>changes = mongoTemplate.find(query.skip(pageNo).limit(pageSize), MailChanges.class);
			if(changes!=null && !changes.isEmpty()) {
				changeList.addAll(changes);
				pageNo = pageNo+pageSize;
			}else {
				break;
			}
		}
		return changeList;
	}
	
	
	@Override
	public List<MailChanges>findByWorkSpaceParent(String emailWorkSpaceId,boolean folder,boolean events){
		int pageNo=0;
		int pageSize = 100;
		List<MailChanges>changeList = new ArrayList<>();
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and("folder").is(folder).and("events").is(events).and(DBConstants.PROCESS_STATUS).is(MailChanges.PROCESS.NOT_PROCESSED.name()));
		while(true) {
			log.warn("==Fetched changes==="+changeList.size()+"==workSpaceID=="+emailWorkSpaceId);
			List<MailChanges>changes = mongoTemplate.find(query.skip(pageNo).limit(pageSize), MailChanges.class);
			if(changes!=null && !changes.isEmpty()) {
				changeList.addAll(changes);
				pageNo = pageNo+pageSize;
			}else {
				break;
			}
		}
		return changeList;
	}
	
	
	
}
