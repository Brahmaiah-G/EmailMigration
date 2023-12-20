package com.testing.mail.repo.impl;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.testing.mail.constants.DBConstants;
import com.testing.mail.repo.CalendarInfoRepo;
import com.testing.mail.repo.entities.CalenderInfo;
import com.testing.mail.repo.entities.EmailWorkSpace;
import com.testing.mail.repo.entities.EventInstacesInfo;
import com.testing.mail.repo.entities.EventsInfo;
import com.testing.mail.repo.entities.PROCESS;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class CalendarInfoRepoImpl implements CalendarInfoRepo{

	@Autowired
	MongoTemplate mongoTemplate;

	@Override
	public void save(EventsInfo calenderInfo) {
		mongoTemplate.save(calenderInfo);
	}

	public void save(CalenderInfo calenderInfo) {
		mongoTemplate.save(calenderInfo);
	}

	public void save(EventInstacesInfo calenderInfo) {
		mongoTemplate.save(calenderInfo);
	}

	public void saveCalendars(List<CalenderInfo> calenderInfos) {
		calenderInfos.forEach(this::save);
	}

	@Override
	public void save(List<EventsInfo> calenderInfos) {
		calenderInfos.forEach(this::save);
	}

	@Override
	public long getInprogressFolders(String emailWorkSpaceId) {
		Query query= new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and(DBConstants.PROCESS_STATUS).is(PROCESS.IN_PROGRESS.name())); 
		return mongoTemplate.count(query, CalenderInfo.class);
	}
	@Override
	public long getInprogressCount(String emailWorkSpaceId,boolean attachments) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and(DBConstants.PROCESS_STATUS).is(PROCESS.IN_PROGRESS.name()).and("attachMents").is(attachments));
		return mongoTemplate.count(query, EventsInfo.class);
	}

	@Override
	public void findAndUpdateConflictsByWorkSpace(String workSpaceId,long retryCount) {
		Query query= new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(workSpaceId).and(DBConstants.PROCESS_STATUS).is(PROCESS.CONFLICT.name()).and(DBConstants.RETRY).lt(retryCount)); 
		Update update = new Update();
		update.set(DBConstants.PROCESS_STATUS, PROCESS.NOT_PROCESSED);
		update.set(DBConstants.THREADBY, null);
		mongoTemplate.updateMulti(query, update, EventsInfo.class);
	}

	public void findAndUpdateMetadataConflictsByWorkSpace(String workSpaceId,long retryCount) {
		Query query= new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(workSpaceId).and(DBConstants.PROCESS_STATUS).is(PROCESS.METADATA_CONFLICT.name())); 
		Update update = new Update();
		update.set(DBConstants.PROCESS_STATUS, PROCESS.METADATA_STARTED);
		update.set(DBConstants.THREADBY, null);
		mongoTemplate.updateMulti(query, update, EventsInfo.class);
	}

	@Override
	public void findAndUpdateByWorkSpace(String workSpaceId,long retryCount,String processStatus,boolean folder,String errorDescription) {
		Query query= new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(workSpaceId).and(DBConstants.PROCESS_STATUS).in(Arrays.asList(PROCESS.NOT_PROCESSED.name()),PROCESS.NOT_STARTED.name()).and(DBConstants.RETRY).lt(retryCount)); 
		Update update = new Update();
		update.set(DBConstants.PROCESS_STATUS, processStatus);
		update.set("errorDescription", errorDescription);
		//update.set(DBConstants.THREADBY, null);
		mongoTemplate.updateMulti(query, update, EventsInfo.class);
	}

	@Override
	public EmailWorkSpace getAggregartedResult(String workSpaceId) {

		EmailWorkSpace emailWorkSpace = new EmailWorkSpace();
		MongoCollection<Document> mongoCollection = mongoTemplate.getCollection("eventsInfo");
		long count =0;
		long processedCount = 0;
		long notProcessedCount = 0;
		long inprogressCount = 0;
		long conflictCount = 0;

		try {
			MongoCursor<?>result =	mongoCollection.aggregate(Arrays.asList(
					new Document("$match", new Document(DBConstants.EMAILWORKSPACEID, workSpaceId).append(DBConstants.SOURCE_ID,  new Document("$nin", Arrays.asList("/")))),
					new Document("$sort", new Document(DBConstants.PROCESS_STATUS, 1)),
					new Document("$group", new Document("_id", "$processStatus").append("total", new Document("$sum", 1)).append("size", new Document("$sum", "$attachmentsSize"))
							))).cursor();
			while(result!=null && result.hasNext()) {
				Document data = (Document)result.next();
				count = count+data.getInteger("total");
				if(data!=null && data.get("_id")!=null && (data.get("_id").equals(PROCESS.IN_PROGRESS.name()) || data.get("_id").equals(PROCESS.METADATA_INPROGRESS.name()) || data.get("_id").equals(PROCESS.INSTANCE_INPROGRESS.name()))) {
					inprogressCount = inprogressCount+data.getInteger("total");
					emailWorkSpace.setTotalAttachmentsSize(emailWorkSpace.getTotalAttachmentsSize() + data.getLong("size"));
				}else if(data!=null && data.get("_id")!=null && (data.get("_id").equals(PROCESS.INSTANCE_CONFLICT.name())||data.get("_id").equals(PROCESS.CONFLICT.name()))) {
					conflictCount = conflictCount+data.getInteger("total");
					emailWorkSpace.setTotalAttachmentsSize(emailWorkSpace.getTotalAttachmentsSize() + data.getLong("size"));
				}else if(data!=null && data.get("_id")!=null && (data.get("_id").equals(PROCESS.PROCESSED.name()) || data.get("_id").equals(PROCESS.INSTANCE_NOTMODIFIED.name()) || data.get("_id").equals(PROCESS.INSTANCE_PROCESSED.name()) || data.get("_id").equals(PROCESS.DUPLICATE_PROCESSED.name()))) {
					processedCount =processedCount+data.getInteger("total");
					emailWorkSpace.setTotalAttachmentsSize(emailWorkSpace.getTotalAttachmentsSize() + data.getLong("size"));
				}else if(data!=null && data.get("_id")!=null && (data.get("_id").equals(PROCESS.NOT_STARTED.name()) || data.get("_id").equals(PROCESS.METADATA_STARTED.name()) ||data.get("_id").equals(PROCESS.INSTANCE_NOTPROCESSED.name()) ||data.get("_id").equals(PROCESS.NOT_PROCESSED.name()))) {
					notProcessedCount = notProcessedCount+data.getInteger("total");
					emailWorkSpace.setTotalAttachmentsSize(emailWorkSpace.getTotalAttachmentsSize() + data.getLong("size"));
				}else if(data!=null && data.get("_id")!=null && data.get("_id").equals(PROCESS.RETRY.name())) {
					emailWorkSpace.setRetryCount(data.getInteger("total"));
					emailWorkSpace.setTotalAttachmentsSize(emailWorkSpace.getTotalAttachmentsSize() + data.getLong("size"));
				}else if(data!=null && data.get("_id")!=null && data.get("_id").equals(PROCESS.PAUSE.name())) {
					emailWorkSpace.setPauseCount(data.getInteger("total"));
					emailWorkSpace.setTotalAttachmentsSize(emailWorkSpace.getTotalAttachmentsSize() + data.getLong("size"));
				}
			}
			emailWorkSpace.setNotProcessedCount(notProcessedCount);
			emailWorkSpace.setProcessedCount(processedCount);
			emailWorkSpace.setInprogressCount(inprogressCount);
			emailWorkSpace.setConflictCount(conflictCount);
		} catch (Exception e) {
			log.warn(ExceptionUtils.getStackTrace(e));
		}
		emailWorkSpace.setTotalCount(count);
		return emailWorkSpace;

	}

	@Override
	public long getMetaDataInprogressCount(String emailWorkSpaceId) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and(DBConstants.PROCESS_STATUS).is(PROCESS.METADATA_INPROGRESS.name()).and("calender").is(false));
		return mongoTemplate.count(query, EventsInfo.class);
	}

	@Override
	public EventsInfo findBySourceId(String jobId,String userId,String sourceId) {
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId).and(DBConstants.SOURCE_ID).is(sourceId).and(DBConstants.PROCESS_STATUS).is(PROCESS.PROCESSED));
		return mongoTemplate.findOne(query, EventsInfo.class);
	}

	public CalenderInfo findBySourceId(String jobId,String sourceId) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(jobId).and(DBConstants.SOURCE_ID).is(sourceId).and(DBConstants.PROCESS_STATUS).is(PROCESS.PROCESSED));
		return mongoTemplate.findOne(query, CalenderInfo.class);
	}
	
	
	@Override
	public List<EventsInfo> findByWorkSpace(String workSpaceId,int limit,int skip,boolean folder) {
		Query query= new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(workSpaceId).and("deleted").is(folder).and(DBConstants.PROCESS_STATUS).nin(Arrays.asList(PROCESS.DUPLICATE_PROCESSED.name()))); 
		return mongoTemplate.find(query.limit(limit).skip(skip), EventsInfo.class);
	}

	@Override
	public List<CalenderInfo> findByWorkSpaceCalendars(String workSpaceId,int limit,int skip,boolean folder) {
		Query query= new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(workSpaceId).and("calender").is(folder)); 
		return mongoTemplate.find(query.limit(limit).skip(skip), CalenderInfo.class);
	}


	@Override
	public void removeCalendars(String workSpaceId) {
		mongoTemplate.remove(new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(workSpaceId)),CalenderInfo.class);
	}

	@Override
	public long checkExistingCalendar(String calendarId,String userId,String jobId) {
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId).and(DBConstants.SOURCE_ID).is(calendarId));
		return mongoTemplate.count(query, EventsInfo.class);
	}

	public EventsInfo checkExistingEvent(String calendarId,String userId,String jobId) {
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId).and(DBConstants.SOURCE_ID).is(calendarId));
		return mongoTemplate.findOne(query.with(Sort.by("_id")), EventsInfo.class);
	}


	@Override
	public List<EventsInfo> getUnDeletedMails(String emailWorkSpaceId,int limit,int skip){
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and(DBConstants.CALENDER).is(false).and("destId").exists(true).and("deleted").is(false)).limit(limit);
		return mongoTemplate.find(query.skip(skip), EventsInfo.class);
	}

	@Override
	public CalenderInfo getParentCalendarInfo(String workSpaceId,String sourceParent) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(workSpaceId).and(DBConstants.CALENDER).is(true).and(DBConstants.SOURCE_ID).is(sourceParent).and(DBConstants.PROCESS_STATUS).is(CalenderInfo.PROCESS.PROCESSED));
		return mongoTemplate.findOne(query, CalenderInfo.class);
	}

	@Override
	public List<EventsInfo>getCalendarEvents(String userId){
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId).and(DBConstants.CALENDER).is(false).and(DBConstants.PROCESS_STATUS).is(CalenderInfo.PROCESS.PROCESSED));
		return mongoTemplate.find(query, EventsInfo.class);
	}

	public void updateDuplicateEvents(List<String> ids) {
		Query query = new Query(Criteria.where("_id").in(ids));
		Update update = new Update();
		update.set(DBConstants.PROCESS_STATUS, PROCESS.DUPLICATE_PROCESSED);
		try {
			mongoTemplate.updateMulti(query, update, EventsInfo.class);
		}catch(Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
			for(String id : ids) {
				query = new Query(Criteria.where("_id").is(id));
				EventsInfo info = mongoTemplate.findOne(query, EventsInfo.class);
				if(null!=info) {
					info.setProcessStatus(EventsInfo.PROCESS.DUPLICATE_PROCESSED);
					save(info);
				}
			}
		}
	}

	@Override
	public List<CalenderInfo> findByProcessStatus(String emailWorkSpaceId, List<PROCESS> processStatus) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and(DBConstants.PROCESS_STATUS).in(processStatus));
		return mongoTemplate.find(query, CalenderInfo.class);
	}

	@Override
	public void saveInstances(List<EventInstacesInfo> instances){
		instances.forEach(this::save);
	}
	
	@Override
	public List<EventInstacesInfo>getInstances(String emailWorkSpaceId,String recurenceId){
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and(DBConstants.RECURENCE_ID).is(recurenceId).and(DBConstants.PROCESS_STATUS).nin(PROCESS.PROCESSED.name()));
		return mongoTemplate.find(query, EventInstacesInfo.class);
	}
	
	public EventInstacesInfo getStartingInstance(String emailWorkSpaceId,String recurenceId){
		Sort sort = Sort.by(Order.desc("_id"));
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and(DBConstants.RECURENCE_ID).is(recurenceId));
		return mongoTemplate.findOne(query.with(sort), EventInstacesInfo.class);
	}
	
	public EventInstacesInfo getSingleInstance(String emailWorkSpaceId,String recurenceId){
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and(DBConstants.SOURCE_ID).is(recurenceId));
		return mongoTemplate.findOne(query, EventInstacesInfo.class);
	}
	
	public EventInstacesInfo getSingleInstanceByRec(String emailWorkSpaceId,String recurenceId){
		Sort sort = Sort.by(Order.desc("_id"));
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and(DBConstants.RECURENCE_ID).is(recurenceId));
		return mongoTemplate.findOne(query.with(sort), EventInstacesInfo.class);
	}
	
	@Override
	public EventInstacesInfo getParentInstances(String emailWorkSpaceId,String recurenceId){
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and(DBConstants.RECURENCE_ID).is(recurenceId).and(DBConstants.PARENT).is(true));
		return mongoTemplate.findOne(query, EventInstacesInfo.class);
	}

	public void findAndUpdateEventInstanceConflictsByWorkSpace(String workSpaceId,long retryCount) {
		Query query= new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(workSpaceId).and(DBConstants.PROCESS_STATUS).is(PROCESS.INSTANCE_CONFLICT.name())); 
		Update update = new Update();
		update.set(DBConstants.PROCESS_STATUS, PROCESS.INSTANCE_NOTPROCESSED);
		update.set(DBConstants.THREADBY, null);
		mongoTemplate.updateMulti(query, update, EventsInfo.class);
	}
	
	public EventInstacesInfo getEventInstanceInfoById(String emailWorkSpaceId,String sourceId) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and(DBConstants.SOURCE_ID).is(sourceId).and("destId").exists(true));
		return mongoTemplate.findOne(query, EventInstacesInfo.class);
	}
	
}
