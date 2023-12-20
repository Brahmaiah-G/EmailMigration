package com.testing.mail.repo.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.testing.mail.constants.DBConstants;
import com.testing.mail.repo.EmailQueueRepository;
import com.testing.mail.repo.entities.CalendarMoveQueue;
import com.testing.mail.repo.entities.CalendarPickingQueue;
import com.testing.mail.repo.entities.EmailMetadataQueue;
import com.testing.mail.repo.entities.EmailMoveQueue;
import com.testing.mail.repo.entities.EmailPickingQueue;
import com.testing.mail.repo.entities.EmailPurgeQueue;
import com.testing.mail.repo.entities.PROCESS;

@Repository
public class EmailQueueRepoImpl implements EmailQueueRepository {

	@Autowired
	MongoTemplate mongoTemplate;
	
	@Override
	public void save(EmailPickingQueue emailQueue) {
		mongoTemplate.save(emailQueue);
	}

	@Override
	public void saveAll(List<EmailPickingQueue> emailQueues) {
		mongoTemplate.save(emailQueues);
	}

	@Override
	public EmailPickingQueue findOne(String id) {
		Query query = new Query(Criteria.where(DBConstants.ID).is(id));
		return mongoTemplate.findOne(query, EmailPickingQueue.class);
	}

	@Override
	public EmailPickingQueue findByWorkSpace(String emailWorkSpaceId) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId));
		return mongoTemplate.findOne(query, EmailPickingQueue.class);
	}

	@Override
	public List<EmailPickingQueue> findPickingProcessStatus(PROCESS processStatus) {
		Query query = new Query(Criteria.where(DBConstants.PROCESS_STATUS).is(processStatus));
		return mongoTemplate.find(query, EmailPickingQueue.class);
	}
	
	@Override
	public List<EmailPickingQueue> findPickingByProcessStatusWithPazination(List<PROCESS> processStatus,int pageSize,int pageNum) {
		Query query = new Query(Criteria.where(DBConstants.PROCESS_STATUS).in(processStatus));
		return mongoTemplate.find(query.limit(pageSize).skip(pageNum), EmailPickingQueue.class);
	}
	
	@Override
	public List<CalendarPickingQueue> findCalendarPickingByProcessStatusWithPazination(List<PROCESS> processStatus,int pageSize,int pageNum) {
		Query query = new Query(Criteria.where(DBConstants.PROCESS_STATUS).in(processStatus));
		return mongoTemplate.find(query.limit(pageSize).skip(pageNum), CalendarPickingQueue.class);
	}
	
	
	@Override
	public long findPickingByProcessStatusWithPazination(List<PROCESS> processStatus,String userId) {
		Query query = new Query(Criteria.where(DBConstants.PROCESS_STATUS).in(processStatus).and(DBConstants.USERID).is(userId));
		return mongoTemplate.count(query, EmailPickingQueue.class);
	}
	
	@Override
	public void saveQueue(EmailMoveQueue emailQueue) {
		mongoTemplate.save(emailQueue);
	}
	
	@Override
	public void savePurgeQueue(EmailPurgeQueue emailQueue) {
		mongoTemplate.save(emailQueue);
	}
	
	@Override
	public void saveMetadataQueue(EmailMetadataQueue emailQueue) {
		mongoTemplate.save(emailQueue);
	}
	
	@Override
	public void saveCalendarPickingQueue(CalendarPickingQueue emailQueue) {
		mongoTemplate.save(emailQueue);
	}
	
	@Override
	public void saveCalendarQueue(CalendarMoveQueue emailQueue) {
		mongoTemplate.save(emailQueue);
	}


	@Override
	public void saveMovequeues(List<EmailMoveQueue> emailQueues) {
		mongoTemplate.save(emailQueues);
	}

	@Override
	public EmailMoveQueue findMoveQueue(String id) {
		Query query = new Query(Criteria.where(DBConstants.ID).is(id));
		return mongoTemplate.findOne(query, EmailMoveQueue.class);
	}

	@Override
	public EmailMoveQueue findMoveQueueByWorkSpace(String emailWorkSpaceId) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId));
		return mongoTemplate.findOne(query, EmailMoveQueue.class);
	}
	@Override
	public CalendarMoveQueue findCalendarMoveQueueByWorkSpace(String emailWorkSpaceId) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId));
		return mongoTemplate.findOne(query, CalendarMoveQueue.class);
	}
	@Override
	public CalendarPickingQueue findCalendarPickingQueueByWorkSpace(String emailWorkSpaceId) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId));
		return mongoTemplate.findOne(query, CalendarPickingQueue.class);
	}
	
	
	@Override
	public List<CalendarPickingQueue> findCalendarsPickingQueueByJob(String jobId) {
		Query query = new Query(Criteria.where(DBConstants.JOB_ID).is(jobId));
		return mongoTemplate.find(query, CalendarPickingQueue.class);
	}
	
	@Override
	public CalendarPickingQueue findCalendarPickingQueueByJobId(String jobId) {
		Query query = new Query(Criteria.where(DBConstants.JOB_ID).is(jobId));
		return mongoTemplate.findOne(query, CalendarPickingQueue.class);
	}

	@Override
	public List<EmailMoveQueue> findMoveQueueByProcessStatus(PROCESS processStatus) {
		Query query = new Query(Criteria.where(DBConstants.PROCESS_STATUS).is(processStatus));
		return mongoTemplate.find(query, EmailMoveQueue.class);
	}
	
	@Override
	public List<EmailMoveQueue> findMoveQueueByProcessStatusWithPazination(List<PROCESS> processStatus,int pageSize,int pageNum,String cloudName) {
		Query query = new Query(Criteria.where(DBConstants.PROCESS_STATUS).in(processStatus).and(DBConstants.CLOUDNAME).is(cloudName));
		return mongoTemplate.find(query.limit(pageSize).skip(pageNum), EmailMoveQueue.class);
	}

	public List<EmailMoveQueue> findMoveQueueByforRules(List<PROCESS> processStatus,int pageSize,int pageNum) {
		Query query = new Query(Criteria.where(DBConstants.PROCESS_STATUS).in(processStatus).and(DBConstants.MAIL_RULES).is(true));
		return mongoTemplate.find(query.limit(pageSize).skip(pageNum), EmailMoveQueue.class);
	}
	
	public List<EmailMoveQueue> findMoveQueueByforContacts(List<PROCESS> processStatus,int pageSize,int pageNum) {
		Query query = new Query(Criteria.where(DBConstants.PROCESS_STATUS).in(processStatus).and(DBConstants.CONTACTS).is(true));
		return mongoTemplate.find(query.limit(pageSize).skip(pageNum), EmailMoveQueue.class);
	}
	
	public List<EmailMoveQueue> findMoveQueueByforSettings(List<PROCESS> processStatus,int pageSize,int pageNum) {
		Query query = new Query(Criteria.where(DBConstants.PROCESS_STATUS).in(processStatus).and(DBConstants.SETTINGS).is(true));
		return mongoTemplate.find(query.limit(pageSize).skip(pageNum), EmailMoveQueue.class);
	}
	
	@Override
	public List<CalendarMoveQueue> findCalendarMoveQueueByProcessStatusWithPazination(List<PROCESS> processStatus,int pageSize,int pageNum) {
		Query query = new Query(Criteria.where(DBConstants.PROCESS_STATUS).in(processStatus));
		return mongoTemplate.find(query.limit(pageSize).skip(pageNum), CalendarMoveQueue.class);
	}
	
	public List<CalendarMoveQueue> findCalendarMoveQueueByProcessStatusWithPazination(List<PROCESS> processStatus,int pageSize,int pageNum,String cloudName) {
		Query query = new Query(Criteria.where(DBConstants.PROCESS_STATUS).in(processStatus).and(DBConstants.CLOUDNAME).is(cloudName));
		return mongoTemplate.find(query.limit(pageSize).skip(pageNum), CalendarMoveQueue.class);
	}
	
	
	@Override
	public List<EmailMoveQueue> findMoveQueueByProcessStatusWithPazination(List<PROCESS> processStatus, int pageSize,
			int pageNum) {
		Query query = new Query(Criteria.where(DBConstants.PROCESS_STATUS).in(processStatus));
		return mongoTemplate.find(query.limit(pageSize).skip(pageNum), EmailMoveQueue.class);
	}
	@Override
	public List<EmailPurgeQueue> findPurgeQueueByProcessStatusWithPazination(List<PROCESS> processStatus, int pageSize,
			int pageNum) {
		Query query = new Query(Criteria.where(DBConstants.PROCESS_STATUS).in(processStatus));
		return mongoTemplate.find(query.limit(pageSize).skip(pageNum), EmailPurgeQueue.class);
	}
	
	public List<EmailPickingQueue> findMailsPickingQueueByJob(String jobId) {
		Query query = new Query(Criteria.where(DBConstants.JOB_ID).is(jobId));
		return mongoTemplate.find(query, EmailPickingQueue.class);
	}

}
