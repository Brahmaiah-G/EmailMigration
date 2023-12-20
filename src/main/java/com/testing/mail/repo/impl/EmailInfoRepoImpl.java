package com.testing.mail.repo.impl;
/**@author Brahmaiah_G*/

import java.util.ArrayList;
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
import com.mongodb.client.result.UpdateResult;
import com.testing.mail.constants.DBConstants;
import com.testing.mail.dao.entities.EMailRules;
import com.testing.mail.repo.EmailInfoRepository;
import com.testing.mail.repo.entities.CalenderInfo;
import com.testing.mail.repo.entities.ContactsInfo;
import com.testing.mail.repo.entities.EmailFolderInfo;
import com.testing.mail.repo.entities.EmailInfo;
import com.testing.mail.repo.entities.EmailWorkSpace;
import com.testing.mail.repo.entities.EventsInfo;
import com.testing.mail.repo.entities.GlobalReports;
import com.testing.mail.repo.entities.MailChangeIds;
import com.testing.mail.repo.entities.ThreadControl;
import com.testing.mail.repo.entities.EmailInfo.PROCESS;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Repository
public class EmailInfoRepoImpl implements EmailInfoRepository {

	private static final String SIZE = "size";
	private static final String ID = "_id";
	private static final String TOTAL = "total";
	private static final String PROCESS_STATUS = "$processStatus";
	private static final String GROUP = "$group";
	private static final String SORT = "$sort";
	private static final String MATCH = "$match";
	@Autowired
	MongoTemplate mongoTemplate;

	@Override
	public EmailInfo save(EmailInfo emailInfo) {
		return mongoTemplate.save(emailInfo);
	}

	@Override
	public void save(List<EmailInfo> emailInfos) {
		emailInfos.forEach(info->{
			try {
				save(info);
			}catch(Exception e) {
				mongoTemplate.save(info);
			}
		});
	}

	@Override
	public EmailInfo findOne(String id) {
		Query query= new Query(Criteria.where(DBConstants.ID).is(id)); 
		return mongoTemplate.findOne(query, EmailInfo.class);
	}

	@Override
	public List<EmailInfo> findByWorkSpace(String workSpaceId) {
		Query query= new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(workSpaceId)); 
		return mongoTemplate.find(query, EmailInfo.class);
	}

	@Override
	public List<EmailInfo> findByWorkSpaceWithPagination(String workSpaceId) {
		int pageNo =0;
		int pageSize = 100;
		List<EmailInfo> lists = new ArrayList<>();
		Query query= new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(workSpaceId)); 
		while(true) {
			List<EmailInfo> emails =  mongoTemplate.find(query.limit(pageSize).skip(pageNo), EmailInfo.class);
			if(emails.isEmpty()) {
				break;
			}
			pageNo = pageNo+pageSize;
			lists.addAll(emails);
		}
		return lists;
	}


	@Override
	public List<EmailInfo> findByWorkSpace(String workSpaceId,int limit,int skip,boolean folder) {
		Query query= new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(workSpaceId).and(DBConstants.FOLDER).is(folder).and(DBConstants.DELETED).is(false)); 
		return mongoTemplate.find(query.limit(limit).skip(skip), EmailInfo.class);
	}

	@Override
	public List<EmailInfo> findByWorkSpace(String workSpaceId,int limit,int skip) {
		Query query= new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(workSpaceId)); 
		return mongoTemplate.find(query.limit(limit).skip(skip), EmailInfo.class);
	}


	@Override
	public List<EmailInfo> findByWorkSpaceAndProcessStatus(String workSpaceId,int limit,int skip,String processStatus,boolean folder) {
		Query query= new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(workSpaceId).and(DBConstants.PROCESS_STATUS).is(processStatus).and(DBConstants.FOLDER).is(folder)); 
		return mongoTemplate.find(query.limit(limit).skip(skip), EmailInfo.class);
	}

	@Override
	public ThreadControl getThreadControl() {
		Query query= new Query(); 
		return mongoTemplate.findOne(query, ThreadControl.class);
	}

	@Override
	public GlobalReports getGlobalReportConfigs() {
		Query query= new Query(); 
		return mongoTemplate.findOne(query, GlobalReports.class);
	}

	@Override
	public long countByWorkSpace(String workSpaceId) {
		Query query= new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(workSpaceId)); 
		return mongoTemplate.count(query, EmailInfo.class);
	}

	@Override
	public EmailWorkSpace getAggregartedResult(String workSpaceId) {

		EmailWorkSpace emailWorkSpace = new EmailWorkSpace();
		MongoCollection<Document> mongoCollection = mongoTemplate.getCollection("emailInfo");
		long count =0;
		try {
			MongoCursor<?>result =	mongoCollection.aggregate(Arrays.asList(
					new Document(MATCH, new Document(DBConstants.EMAILWORKSPACEID, workSpaceId).append(DBConstants.SOURCE_ID,  new Document("$nin", Arrays.asList("/")))),
					new Document(SORT, new Document(DBConstants.PROCESS_STATUS, 1)),
					new Document(GROUP, new Document(ID, PROCESS_STATUS).append(TOTAL, new Document("$sum", 1)).append(SIZE, new Document("$sum", "$attachmentsSize"))
							))).cursor();
			while(result!=null && result.hasNext()) {
				Document data = (Document)result.next();
				if(data!=null && data.get(ID)!=null && (data.get(ID).equals(PROCESS.IN_PROGRESS.name())|| data.get(ID).equals(PROCESS.METADATA_INPROGRESS.name()) || data.get(ID).equals(PROCESS.DRAFT_CREATION_INPROGRESS.name()) || data.get(ID).equals(PROCESS.DRAFT_MIGRATION_IN_PROGRESS.name()))) {
					emailWorkSpace.setInprogressCount(data.getInteger(TOTAL));
					count = count+emailWorkSpace.getInprogressCount();
					emailWorkSpace.setTotalAttachmentsSize(emailWorkSpace.getTotalAttachmentsSize() + data.getLong(SIZE));
				}else if(data!=null && data.get(ID)!=null && (data.get(ID).equals(PROCESS.CONFLICT.name()) || data.get(ID).equals(PROCESS.DRAFT_CREATION_CONFLICT.name()))) {
					emailWorkSpace.setConflictCount(data.getInteger(TOTAL));
					count = count+emailWorkSpace.getConflictCount();
					emailWorkSpace.setTotalAttachmentsSize(emailWorkSpace.getTotalAttachmentsSize() + data.getLong(SIZE));
				}else if(data!=null && data.get(ID)!=null && data.get(ID).equals(PROCESS.PROCESSED.name())) {
					emailWorkSpace.setProcessedCount(data.getInteger(TOTAL));
					count = count+emailWorkSpace.getProcessedCount();
					emailWorkSpace.setTotalAttachmentsSize(emailWorkSpace.getTotalAttachmentsSize() + data.getLong(SIZE));
				}else if(data!=null && data.get(ID)!=null && data.get(ID).equals(PROCESS.NOT_PROCESSED.name())) {
					emailWorkSpace.setNotProcessedCount(data.getInteger(TOTAL));
					count = count+emailWorkSpace.getNotProcessedCount();
					emailWorkSpace.setTotalAttachmentsSize(emailWorkSpace.getTotalAttachmentsSize() + data.getLong(SIZE));
				}else if(data!=null && data.get(ID)!=null && (data.get(ID).equals(PROCESS.NOT_STARTED.name()) || data.get(ID).equals(PROCESS.METADATA_STARTED.name())||data.get(ID).equals(PROCESS.DRAFT_NOTPROCESSED.name()))) {
					emailWorkSpace.setNotProcessedCount(emailWorkSpace.getNotProcessedCount()+ data.getInteger(TOTAL));
					count = count+emailWorkSpace.getNotProcessedCount();
					emailWorkSpace.setTotalAttachmentsSize(emailWorkSpace.getTotalAttachmentsSize() + data.getLong(SIZE));
				}else if(data!=null && data.get(ID)!=null && data.get(ID).equals(PROCESS.RETRY.name())) {
					emailWorkSpace.setRetryCount(data.getInteger(TOTAL));
					count = count+emailWorkSpace.getRetryCount();
					emailWorkSpace.setTotalAttachmentsSize(emailWorkSpace.getTotalAttachmentsSize() + data.getLong(SIZE));
				}else if(data!=null && data.get(ID)!=null && data.get(ID).equals(PROCESS.PAUSE.name())) {
					emailWorkSpace.setPauseCount(data.getInteger(TOTAL));
					count = count+emailWorkSpace.getPauseCount();
					emailWorkSpace.setTotalAttachmentsSize(emailWorkSpace.getTotalAttachmentsSize() + data.getLong(SIZE));
				}
			}
		} catch (Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
		}
		emailWorkSpace.setTotalCount(count);
		return emailWorkSpace;

	}


	@Override
	public EmailInfo getAggregartedResultForPremigration(String userId,String fromCloudId) {

		EmailInfo emailWorkSpace = new EmailInfo();
		MongoCollection<Document> mongoCollection = mongoTemplate.getCollection("emailInfo");
		try {
			MongoCursor<?>result =	mongoCollection.aggregate(Arrays.asList(new Document(MATCH,new Document("userId", userId).append("preScan", true).append("fromAdminCloudId", fromCloudId)), 
					new Document(GROUP,  new Document(ID, "$folder").append(TOTAL,new Document("$sum", 1L)).append(SIZE,new Document("$sum", "$totalSizeInBytes"))), 
					new Document(SORT, new Document("folder", 1L)))).cursor();
			while( result.hasNext()) {
				Document data = (Document)result.next();
				if(data.get(ID)!=null && data.get(ID).equals(true)) {
					emailWorkSpace.setTotalCount(data.getLong(TOTAL));
					emailWorkSpace.setAttachmentsSize(emailWorkSpace.getAttachmentsSize()+data.getLong(SIZE));
				}else {
					emailWorkSpace.setUnreadCount(data.getLong(TOTAL));
					emailWorkSpace.setAttachmentsSize(emailWorkSpace.getAttachmentsSize()+data.getLong(SIZE));
				}
			}	
		} catch (Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
		}
		return emailWorkSpace;

	}



	@Override
	public void findAndUpdateConflictsByWorkSpace(String workSpaceId,long retryCount) {
		Query query= new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(workSpaceId).and(DBConstants.PROCESS_STATUS).is(PROCESS.CONFLICT.name()).and(DBConstants.FOLDER).is(false).and(DBConstants.RETRY).lt(retryCount)); 
		Update update = new Update();
		update.set(DBConstants.PROCESS_STATUS, PROCESS.NOT_PROCESSED);
		update.set(DBConstants.THREADBY, null);
		update.set(DBConstants.ERRORDESCRIPTION, null);
		try {
			mongoTemplate.updateMulti(query, update, EmailInfo.class);
		}catch(Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
			List<EmailInfo> infos = mongoTemplate.find(query, EmailInfo.class);
			if(!infos.isEmpty()) {
				infos.forEach(info->{
					info.setProcessStatus( EmailInfo.PROCESS.NOT_PROCESSED);
					info.setThreadBy(null);
					info.setErrorDescription(null);
					mongoTemplate.save(info);
				});

			}
		}
	}

	@Override
	public void findAndUpdateConflictsByWorkSpaceOutlookDraftCreation(String workSpaceId,long retryCount) {
		Query query= new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(workSpaceId).and(DBConstants.PROCESS_STATUS).is(PROCESS.DRAFT_CREATION_CONFLICT.name()).and(DBConstants.FOLDER).is(false).and(DBConstants.RETRY).lt(retryCount)); 
		Update update = new Update();
		update.set(DBConstants.PROCESS_STATUS, PROCESS.NOT_PROCESSED);
		update.set(DBConstants.THREADBY, null);
		update.set(DBConstants.ERRORDESCRIPTION, null);
		try {
			mongoTemplate.updateMulti(query, update, EmailInfo.class);
		}catch(Exception e) {

			log.error(ExceptionUtils.getStackTrace(e));
			List<EmailInfo> infos = mongoTemplate.find(query, EmailInfo.class);
			if(!infos.isEmpty()) {
				infos.forEach(info->{
					info.setProcessStatus( EmailInfo.PROCESS.NOT_PROCESSED);
					info.setThreadBy(null);
					info.setErrorDescription(null);
					mongoTemplate.save(info);
				});
			}
		}
	}

	@Override
	public void findAndUpdateConflictsByWorkSpaceOutlookDraftMigration(String workSpaceId,long retryCount) {
		Query query= new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(workSpaceId).and(DBConstants.PROCESS_STATUS).is(PROCESS.CONFLICT.name()).and(DBConstants.FOLDER).is(false).and(DBConstants.RETRY).lt(retryCount)); 
		Update update = new Update();
		update.set(DBConstants.PROCESS_STATUS, PROCESS.DRAFT_NOTPROCESSED);
		update.set(DBConstants.THREADBY, null);
		update.set(DBConstants.ERRORDESCRIPTION, null);
		mongoTemplate.updateMulti(query, update, EmailInfo.class);
	}

	@Override
	public void findAndUpdateConflictFoldersByWorkSpace(String workSpaceId,long retryCount) {
		Query query= new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(workSpaceId).and(DBConstants.PROCESS_STATUS).is(PROCESS.CONFLICT.name()).and(DBConstants.FOLDER).is(true).and("picking").is(true).and(DBConstants.RETRY).lte(retryCount)); 
		Update update = new Update();
		update.set(DBConstants.PROCESS_STATUS, PROCESS.NOT_PROCESSED);
		update.set(DBConstants.THREADBY, null);
		update.set(DBConstants.ERRORDESCRIPTION, null);
		update.set(DBConstants.RETRY,0);
		mongoTemplate.updateMulti(query, update, EmailInfo.class);
	}

	public void findAndUpdateMetadataConflictsByWorkSpace(String workSpaceId,long retryCount) {
		Query query= new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(workSpaceId).and(DBConstants.PROCESS_STATUS).is(PROCESS.METADATA_CONFLICT.name()).and(DBConstants.FOLDER).is(false).and(DBConstants.RETRY).lt(retryCount)); 
		Update update = new Update();
		update.set(DBConstants.PROCESS_STATUS, PROCESS.METADATA_STARTED);
		update.set(DBConstants.THREADBY, null);
		update.set(DBConstants.ERRORDESCRIPTION, null);
		mongoTemplate.updateMulti(query, update, EmailInfo.class);
	}

	@Override
	public void findAndUpdateByWorkSpace(String workSpaceId,String processStatus,String errorDescription,List<String>ids,long retryCount) {
		Query query= new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(workSpaceId).and(ID).in(ids)); 
		Update update = new Update();
		update.set(DBConstants.PROCESS_STATUS, processStatus);
		update.set(DBConstants.ERRORDESCRIPTION, errorDescription);
		update.set(DBConstants.RETRY,retryCount+1);
		UpdateResult result= mongoTemplate.updateMulti(query, update, EmailInfo.class);
		log.info(result.toString());
	}




	@Override
	public void findAndUpdateByWorkSpace(String workSpaceId,long retryCount,String processStatus,boolean folder,String errorDescription) {
		Query query= new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(workSpaceId).and(DBConstants.PROCESS_STATUS).in(Arrays.asList(PROCESS.NOT_PROCESSED.name()),PROCESS.NOT_STARTED.name()).and(DBConstants.FOLDER).is(folder).and(DBConstants.RETRY).lt(retryCount)); 
		Update update = new Update();
		update.set(DBConstants.PROCESS_STATUS, processStatus);
		update.set(DBConstants.ERRORDESCRIPTION, errorDescription);
		mongoTemplate.updateMulti(query, update, EmailInfo.class);
	}


	@Override
	public void findAndUpdateByWorkSpaceForMailChangeIds(String workSpaceId,long retryCount,String processStatus,boolean folder,String errorDescription) {
		Query query= new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(workSpaceId).and(DBConstants.PROCESS_STATUS).in(Arrays.asList(PROCESS.NOT_PROCESSED.name()),PROCESS.NOT_STARTED.name()).and(DBConstants.FOLDER).is(folder).and(DBConstants.RETRY).lt(retryCount)); 
		Update update = new Update();
		update.set(DBConstants.PROCESS_STATUS, processStatus);
		update.set("errorDescription", errorDescription);
		mongoTemplate.updateMulti(query, update, MailChangeIds.class);
	}


	@Override
	public List<EmailInfo> findConflictFolders(String workSpaceId,long retryCount) {
		Query query= new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(workSpaceId).and(DBConstants.PROCESS_STATUS).is(PROCESS.CONFLICT.name()).and(DBConstants.FOLDER).is(true).and(DBConstants.RETRY).lt(retryCount)); 
		return mongoTemplate.find(query, EmailInfo.class);
	}

	@Override
	public long getInprogressFolders(String emailWorkSpaceId) {
		Query query= new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and(DBConstants.PROCESS_STATUS).is(PROCESS.IN_PROGRESS.name()).and(DBConstants.FOLDER).is(true)); 
		return mongoTemplate.count(query, EmailInfo.class);
	}
	@Override
	public long getInprogressCount(String emailWorkSpaceId,boolean attachments) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and(DBConstants.PROCESS_STATUS).is(PROCESS.IN_PROGRESS.name()).and(DBConstants.ATTACHMENTS).is(attachments).and(DBConstants.FOLDER).is(false));
		return mongoTemplate.count(query, EmailInfo.class);
	}

	public long getMetaDataInprogressCount(String emailWorkSpaceId,boolean attachments) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and(DBConstants.PROCESS_STATUS).is(PROCESS.METADATA_INPROGRESS.name()).and(DBConstants.ATTACHMENTS).is(attachments).and(DBConstants.FOLDER).is(false));
		return mongoTemplate.count(query, EmailInfo.class);
	}


	@Override
	public long countConlfictsByFolder(String moveWorkSpaceId,String folder) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(moveWorkSpaceId).and(DBConstants.MAIL_FOLDER).is(folder).and(DBConstants.PROCESS_STATUS).is(EmailInfo.PROCESS.CONFLICT.name()));
		return mongoTemplate.count(query, EmailInfo.class);
	}


	@Override
	public long countTotalByFolder(String moveWorkSpaceId,String folder) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(moveWorkSpaceId).and(DBConstants.MAIL_FOLDER).is(folder));
		return mongoTemplate.count(query, EmailInfo.class);
	}


	@Override
	public EmailInfo findBySourceId(String jobId,String userId,String sourceId) {
		Sort sort = Sort.by(Order.desc(DBConstants.CREATEDTIME));
		Query query = new Query(Criteria.where(DBConstants.JOB_ID).is(jobId).and(DBConstants.USERID).is(userId).and(DBConstants.SOURCE_ID).is(sourceId).and(DBConstants.PROCESS_STATUS).is(PROCESS.PROCESSED));
		return mongoTemplate.findOne(query.with(sort), EmailInfo.class);
	}

	@Override
	public EmailInfo findByThreadId(String jobId,String userId,String threadId) {
		Sort sort = Sort.by(Order.desc(DBConstants.CREATEDTIME));
		Query query = new Query(Criteria.where(DBConstants.JOB_ID).is(jobId).and(DBConstants.USERID).is(userId).and(DBConstants.THREADID).is(threadId).and(DBConstants.PROCESS_STATUS).is(PROCESS.PROCESSED));
		return mongoTemplate.findOne(query.with(sort), EmailInfo.class);
	}

	public EmailInfo findByThreadId(String jobId,String threadId) {
		Sort sort = Sort.by(Order.desc(DBConstants.CREATEDTIME));
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(jobId).and(DBConstants.THREADID).is(threadId).and(DBConstants.PROCESS_STATUS).is(PROCESS.NOT_PROCESSED));
		return mongoTemplate.findOne(query.with(sort), EmailInfo.class);
	}

	/**
	 * For Fetching the Aggregated Results based on mailFolders (ex:inbox)
	 */

	@Override
	public EmailWorkSpace getAggregartedResultForFolder(String workSpaceId,String folder) {

		EmailWorkSpace emailWorkSpace = new EmailWorkSpace();
		MongoCollection<Document> mongoCollection = mongoTemplate.getCollection("emailInfo");
		long count =0;
		try {
			MongoCursor<?>result =	mongoCollection.aggregate(Arrays.asList(new Document(MATCH, 
					new Document(DBConstants.EMAILWORKSPACEID, workSpaceId).append("originalMailFolder", folder).append("folder", false)), 
					new Document(SORT, new Document(DBConstants.PROCESS_STATUS, 1L)), 
					new Document(GROUP,new Document(ID, PROCESS_STATUS).append(TOTAL, 
							new Document("$sum", 1)).append(SIZE,new Document("$sum", "$totalSizeInBytes"))))).cursor();
			while(result.hasNext()) {
				Document data = (Document)result.next();
				if(data.get(ID)!=null) {
					count = getProcessStatusCount(emailWorkSpace, count, data);
				}
			}
		} catch (Exception e) {
			log.warn(ExceptionUtils.getStackTrace(e));
		}
		emailWorkSpace.setTotalCount(count);
		return emailWorkSpace;
	}

	private long getProcessStatusCount(EmailWorkSpace emailWorkSpace, long count, Document data) {
		if(data.get(ID).equals(PROCESS.IN_PROGRESS.name()) || data.get(ID).equals(PROCESS.METADATA_INPROGRESS.name())) {
			emailWorkSpace.setInprogressCount(emailWorkSpace.getInprogressCount()+data.getInteger(TOTAL));
			count = count+emailWorkSpace.getInprogressCount();
			emailWorkSpace.setTotalAttachmentsSize(emailWorkSpace.getTotalAttachmentsSize() + data.getLong(SIZE));
		}else if(data.get(ID).equals(PROCESS.CONFLICT.name())) {
			emailWorkSpace.setConflictCount(data.getInteger(TOTAL));
			count = count+emailWorkSpace.getConflictCount();
			emailWorkSpace.setTotalAttachmentsSize(emailWorkSpace.getTotalAttachmentsSize() + data.getLong(SIZE));
		}else if(data.get(ID).equals(PROCESS.PROCESSED.name())) {
			emailWorkSpace.setProcessedCount(data.getInteger(TOTAL));
			count = count+emailWorkSpace.getProcessedCount();
			emailWorkSpace.setTotalAttachmentsSize(emailWorkSpace.getTotalAttachmentsSize() + data.getLong(SIZE));
		}else if(data.get(ID).equals(PROCESS.NOT_PROCESSED.name())) {
			emailWorkSpace.setNotProcessedCount(data.getInteger(TOTAL));
			count = count+emailWorkSpace.getNotProcessedCount();
			emailWorkSpace.setTotalAttachmentsSize(emailWorkSpace.getTotalAttachmentsSize() + data.getLong(SIZE));
		}else if(data.get(ID).equals(PROCESS.NOT_STARTED.name()) || data.get(ID).equals(PROCESS.METADATA_STARTED.name())) {
			emailWorkSpace.setNotProcessedCount(emailWorkSpace.getNotProcessedCount()+ data.getInteger(TOTAL));
			count = count+emailWorkSpace.getNotProcessedCount();
			emailWorkSpace.setTotalAttachmentsSize(emailWorkSpace.getTotalAttachmentsSize() + data.getLong(SIZE));
		}else if(data.get(ID).equals(PROCESS.RETRY.name())) {
			emailWorkSpace.setRetryCount(data.getInteger(TOTAL));
			count = count+emailWorkSpace.getRetryCount();
			emailWorkSpace.setTotalAttachmentsSize(emailWorkSpace.getTotalAttachmentsSize() + data.getLong(SIZE));
		}else if(data.get(ID).equals(PROCESS.PAUSE.name())) {
			emailWorkSpace.setPauseCount(data.getInteger(TOTAL));
			count = count+emailWorkSpace.getPauseCount();
			emailWorkSpace.setTotalAttachmentsSize(emailWorkSpace.getTotalAttachmentsSize() + data.getLong(SIZE));
		}
		return count;
	}

	@Override
	public EmailWorkSpace getAggregartedResultBasedOnJob(String workSpaceId) {

		EmailWorkSpace emailWorkSpace = new EmailWorkSpace();
		MongoCollection<Document> mongoCollection = mongoTemplate.getCollection("emailInfo");
		long count =0;
		try {
			MongoCursor<?>result =	mongoCollection.aggregate(Arrays.asList(
					new Document(MATCH, new Document(DBConstants.JOB_ID, workSpaceId)),
					new Document(SORT, new Document(DBConstants.PROCESS_STATUS, 1)),
					new Document(GROUP, new Document(ID, PROCESS_STATUS).append(TOTAL, new Document("$sum", 1)).append(SIZE, new Document("$sum", "$attachmentsSize"))
							))).cursor();
			while(result.hasNext()) {
				Document data = (Document)result.next();
				if(data.get(ID)!=null) {
					count = getProcessStatusCount(emailWorkSpace, count, data);
				}
			}
		} catch (Exception e) {
			log.warn(ExceptionUtils.getStackTrace(e));
		}
		emailWorkSpace.setTotalCount(count);
		return emailWorkSpace;

	}

	@Override
	public void saveMailBoxRules(List<EMailRules> rules) {
		rules.forEach(rule->{
			try {
				mongoTemplate.save(rule);
			}catch(Exception e) {
				mongoTemplate.save(rule);
			}
		});
	}

	@Override
	public void removeEmails(String workSpaceId) {
		mongoTemplate.remove(new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(workSpaceId)), EmailInfo.class);
	}

	/**
	 * MailBox rules configs
	 */
	@Override
	public EmailInfo getFolderBasedOnSourceId(String emailWorkSpaceID,String sourceId) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceID).and(DBConstants.SOURCE_ID).is(sourceId).and(DBConstants.FOLDER).is(true).and(DBConstants.PROCESS_STATUS).is(EmailInfo.PROCESS.PROCESSED));
		return mongoTemplate.findOne(query, EmailInfo.class);
	}

	@Override
	public EmailInfo getFolderBasedOnMailFolder(String emailWorkSpaceID,String sourceId) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceID).and(DBConstants.MAIL_FOLDER).is(sourceId).and(DBConstants.FOLDER).is(true).and(DBConstants.PROCESS_STATUS).is(EmailInfo.PROCESS.PROCESSED));
		return mongoTemplate.findOne(query, EmailInfo.class);
	}

	@Override
	public void updateEmailInfoForDestThreadId(String emailWorkSpaceId,String sourceThreadId,String destThreadId,long order,String destConvIndex,List<String> env) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and(DBConstants.THREADID).is(sourceThreadId).and(DBConstants.FOLDER).is(false).and(DBConstants.PROCESS_STATUS).is(EmailInfo.PROCESS.THREAD_NOT_PROCESSED).and("fromMail").in(env));
		Update update = new Update();
		update.set(DBConstants.DEST_THREADID, destThreadId);
		update.set(DBConstants.CONV_INDEX, destConvIndex);
		try {
			mongoTemplate.updateMulti(query, update, EmailInfo.class);
		}catch(Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
			List<EmailInfo> infos = mongoTemplate.find(query, EmailInfo.class);
			if(!infos.isEmpty()) {
				infos.forEach(info->{
					info.setDestThreadId(destThreadId);
					info.setConvIndex(destConvIndex);
					mongoTemplate.save(info);
				});
			}
		}
	}

	@Override
	public void updateEmailInfoForDestThreadId(String emailWorkSpaceId,String sourceThreadId,String destThreadId,long order,String destConvIndex) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and(DBConstants.THREADID).is(sourceThreadId).and(DBConstants.FOLDER).is(false).and(DBConstants.PROCESS_STATUS).is(EmailInfo.PROCESS.THREAD_NOT_PROCESSED).and(DBConstants.ORDER).is(order+1));
		Update update = new Update();
		update.set(DBConstants.DEST_THREADID, destThreadId);
		update.set(DBConstants.PROCESS_STATUS, EmailInfo.PROCESS.NOT_PROCESSED);
		//update.set(DBConstants.CONV_INDEX, destConvIndex);
		try {
			mongoTemplate.updateMulti(query, update, EmailInfo.class);
		}catch(Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
			EmailInfo info = mongoTemplate.findOne(query, EmailInfo.class);
			if(null!=info) {
				info.setDestThreadId(destThreadId);
				info.setProcessStatus(EmailInfo.PROCESS.NOT_PROCESSED);
				mongoTemplate.save(info);
			}
		}
	}

	public void updateEmailInfoForDestThreadIdForGmailDest(String emailWorkSpaceId,String sourceThreadId,String destThreadId,long order,String destConvIndex) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and(DBConstants.THREADID).is(sourceThreadId).and(DBConstants.FOLDER).is(false).and(DBConstants.PROCESS_STATUS).is(EmailInfo.PROCESS.THREAD_NOT_PROCESSED));
		Update update = new Update();
		update.set(DBConstants.DEST_THREADID, destThreadId);
		update.set(DBConstants.PROCESS_STATUS, EmailInfo.PROCESS.NOT_PROCESSED);
		//update.set(DBConstants.CONV_INDEX, destConvIndex);
		try {
			mongoTemplate.updateMulti(query, update, EmailInfo.class);
		}catch(Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
			EmailInfo info = mongoTemplate.findOne(query, EmailInfo.class);
			if(null!=info) {
				info.setDestThreadId(destThreadId);
				info.setProcessStatus(EmailInfo.PROCESS.NOT_PROCESSED);
				mongoTemplate.save(info);
			}
		}
	}
	public void updateEmailInfoForDestThreadIdConvex(String emailWorkSpaceId,String sourceThreadId,String destThreadId,long order,String destConvIndex) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and(DBConstants.THREADID).is(sourceThreadId).and(DBConstants.FOLDER).is(false));
		Update update = new Update();
		update.set(DBConstants.DEST_THREADID, destThreadId);
		update.set(DBConstants.CONV_INDEX, destConvIndex);
		try {
			mongoTemplate.updateMulti(query, update, EmailInfo.class);
		}catch(Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
			EmailInfo info = mongoTemplate.findOne(query, EmailInfo.class);
			if(null!=info) {
				info.setDestThreadId(destThreadId);
				info.setProcessStatus(EmailInfo.PROCESS.NOT_PROCESSED);
				info.setConvIndex(destConvIndex);
				mongoTemplate.save(info);
			}
		}
	}




	@Override
	public void updateEmailInfoForDestChildFolder(String emailWorkSpaceId,String sourceThreadId,String destParent) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and(DBConstants.SOURCE_PARENT).is(sourceThreadId).and(DBConstants.FOLDER).is(true).and(DBConstants.PROCESS_STATUS).is(EmailInfo.PROCESS.PARENT_NOT_PROCESSED));
		Update update = new Update();
		update.set(DBConstants.PROCESS_STATUS, EmailInfo.PROCESS.NOT_STARTED);
		update.set(DBConstants.DEST_PARENT, destParent);
		try {
			mongoTemplate.updateMulti(query, update, EmailInfo.class);
		}catch(Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
			List<EmailInfo> infos = mongoTemplate.find(query, EmailInfo.class);
			if(!infos.isEmpty()) {
				infos.forEach(info->{
					info.setProcessStatus( EmailInfo.PROCESS.NOT_STARTED);
					info.setDestParent(destParent);
					mongoTemplate.save(info);
				});
			}
		}
	}

	@Override
	public void updateEmailInfoForDestThreadId(String emailWorkSpaceId,String sourceThreadId,String destThreadId) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and(DBConstants.THREADID).is(sourceThreadId).and(DBConstants.FOLDER).is(false).and(DBConstants.PROCESS_STATUS).is(EmailInfo.PROCESS.THREAD_NOT_PROCESSED));
		Update update = new Update();
		update.set(DBConstants.DEST_THREADID, destThreadId);
		mongoTemplate.updateFirst(query, update, EmailInfo.class);
	}

	@Override
	public EmailInfo checkEmailInfosBasedOnIds(String emailWorkSpaceId,String sourceId,boolean copy) {
		String id = DBConstants.USERID;
		if(copy) {
			id =  DBConstants.EMAILWORKSPACEID;
		}
		Query query = new Query(Criteria.where(id).is(emailWorkSpaceId).and(DBConstants.SOURCE_ID).is(sourceId).and(DBConstants.FOLDER).is(false));
		return mongoTemplate.findOne(query, EmailInfo.class);
	}

	public EmailFolderInfo checkEmailInfosBasedOnIds(String emailWorkSpaceId,String sourceId) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and(DBConstants.SOURCE_ID).is(sourceId));
		return mongoTemplate.findOne(query, EmailFolderInfo.class);
	}

	public EmailInfo checkEmailInfosBasedOnIds(String emailWorkSpaceId,String sourceId,String id) {
		Query query = new Query(Criteria.where(DBConstants.USERID).is(emailWorkSpaceId).and(DBConstants.SOURCE_ID).is(sourceId).and(ID).nin(Arrays.asList(id)).and(DBConstants.PROCESS_STATUS).in(Arrays.asList(PROCESS.PROCESSED,PROCESS.IN_PROGRESS,PROCESS.CONFLICT)).and(DBConstants.FOLDER).is(false));
		return mongoTemplate.findOne(query, EmailInfo.class);
	}

	@Override
	public void saveMailBoxRule(EMailRules rule) {
		mongoTemplate.save(rule);
	}

	public void saveContact(ContactsInfo rule) {
		mongoTemplate.save(rule);
	}
	
	public void saveContacts(List<ContactsInfo> rules) {
		rules.forEach(rule->{
			try {
				saveContact(rule);
			}catch(Exception e) {
				saveContact(rule);
			}
		});
	}

	@Override
	public List<EMailRules> findMailBoxRuleBySourceId(String userId,String emailWorkSpaceId,String sourceId) {
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId).and(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and("customFolder").is(sourceId).and(DBConstants.PROCESS_STATUS).is(PROCESS.NOT_PROCESSED.name()));
		return mongoTemplate.find(query, EMailRules.class);
	}


	@Override
	public List<EmailInfo> getUnDeletedMails(String emailWorkSpaceId,int limit,int skip){
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and(DBConstants.FOLDER).is(false).and("destId").exists(true).and("deleted").is(false)).limit(limit);
		return mongoTemplate.find(query.skip(skip), EmailInfo.class);
	}

	@Override
	public EmailInfo getDestThreadIdBasedOnSourceId(String moveWorkSpaceId,String sourceThreadId) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(moveWorkSpaceId).and(DBConstants.THREADID).is(sourceThreadId).and(DBConstants.PROCESS_STATUS).is(PROCESS.PROCESSED).and("destId").exists(true).and("deleted").is(false));
		return mongoTemplate.findOne(query, EmailInfo.class);
	}


	@Override
	public void getNotProcessedEmailsByDuplicateSourceID(String workSpaceId){
		int limit = 100;
		int skip =0;
		int count = 0;
		while(true) {
			MongoCollection<Document> collection = mongoTemplate.getCollection("emailInfo");
			List<Document> pipeline = Arrays.asList(
					new Document(MATCH, new Document("emailWorkSpaceId", workSpaceId)),
					new Document(GROUP, new Document(ID, new Document("sourceId", "$sourceId"))
							.append("uniqueIds", new Document("$addToSet", "$_id"))
							.append("count", new Document("$sum", 1))),
					new Document(MATCH, new Document("count", new Document("$gt", 1))),
					new Document("$project", new Document("idsToRemove", new Document("$slice", Arrays.asList("$uniqueIds", 1)))
							.append(ID, 0)),
					new Document("$skip", skip), // Skip the first 10 results
					new Document("$limit", limit) // Limit the output to 20 results
					);

			// Execute aggregation
			List<Document> result = collection.aggregate(pipeline).allowDiskUse(true).into(new ArrayList<>());
			log.info("==total result==="+result.size());
			count = count+result.size();
			if(result.isEmpty()) {
				break;
			}else {
				skip = skip+limit;
			}
			for (Document doc : result) {
				List<Object> idsToRemove = doc.getList("idsToRemove", Object.class);
				for (Object id : idsToRemove) {
					collection.deleteOne(new Document(ID, id));
					log.info("==remaining==="+count--);
				}
			}
		}
	}

	@Override
	public EmailInfo getParentFolderInfo(String workSpaceId,String sourceParent) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(workSpaceId).and(DBConstants.FOLDER).is(true).and(DBConstants.SOURCE_ID).is(sourceParent).and(DBConstants.PROCESS_STATUS).is(CalenderInfo.PROCESS.PROCESSED));
		return mongoTemplate.findOne(query, EmailInfo.class);
	}

	@Override
	public long countByProcessStatus(String processStatus) {
		Query query = new Query(Criteria.where(DBConstants.PROCESS_STATUS).is(processStatus));
		return mongoTemplate.count(query, EmailInfo.class);
	}
	@Override
	public long countByProcessStatusAndError(List<String> processStatus,String errorDescription) {
		Query query = new Query(Criteria.where(DBConstants.PROCESS_STATUS).in(processStatus).and(DBConstants.ERRORDESCRIPTION).regex(errorDescription, "i"));
		return mongoTemplate.count(query, EmailInfo.class);
	}

	@Override
	public void updateEmailInfosByProcessStatus(String sourceProcessStatus,String destProcessStatus, int count) {
		Query query = new Query(Criteria.where(DBConstants.PROCESS_STATUS).is(sourceProcessStatus));
		Update update = new Update();
		update.set(DBConstants.PROCESS_STATUS, destProcessStatus);
		try {
			mongoTemplate.updateFirst(query.limit(count), update, EmailInfo.class);
		}catch(Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
			EmailInfo info = mongoTemplate.findOne(query, EmailInfo.class);
			if(null!=info) {
				info.setProcessStatus(PROCESS.valueOf(destProcessStatus));
				mongoTemplate.save(info);
			}
		}
	}


	public void updateEmailInfosByProcessStatus(String sourceProcessStatus,String destProcessStatus, int count,String errorDescription) {
		Query query = new Query(Criteria.where(DBConstants.PROCESS_STATUS).is(sourceProcessStatus).orOperator(Criteria.where(DBConstants.ERRORDESCRIPTION).regex(errorDescription),Criteria.where("extraField").regex(errorDescription)));
		Update update = new Update();
		update.set(DBConstants.PROCESS_STATUS, destProcessStatus);
		update.set(DBConstants.THREADBY, null);
		update.set(DBConstants.ERRORDESCRIPTION, null);
		update.set(DBConstants.RETRY, 0);
		try {
			mongoTemplate.updateMulti(query.limit(count), update, EmailInfo.class);
		}catch(Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
			List<EmailInfo> infos = mongoTemplate.find(query, EmailInfo.class);
			if(!infos.isEmpty()) {
				infos.forEach(info->{
					info.setProcessStatus(PROCESS.valueOf(destProcessStatus));
					info.setThreadBy(null);
					info.setErrorDescription(null);
					info.setRetryCount(0);
					mongoTemplate.save(info);
				});
			}
		}
	}

	@Override
	public long getEmailInfosBasedOnProcessStatus(String emailWorkSpaceId,long mailFolder,PROCESS processStatus,boolean folder) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and(DBConstants.PROCESS_STATUS).is(processStatus).and(DBConstants.FOLDER).is(folder).and(DBConstants.PRIORITY).is(mailFolder));
		return mongoTemplate.count(query, EmailInfo.class);
	}

	public long getlistOfMails(String emailWorkSpaceId,String mailFolder){
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and(DBConstants.MAIL_FOLDER).is(mailFolder));
		return mongoTemplate.count(query, EmailInfo.class);
	}

	public void updateDuplicateMails(List<String> ids) {
		Query query = new Query(Criteria.where("_id").in(ids));
		Update update = new Update();
		update.set(DBConstants.PROCESS_STATUS, PROCESS.DUPLICATE_PROCESSED);
		try {
			mongoTemplate.updateMulti(query, update, EventsInfo.class);
		}catch(Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
			for(String id : ids) {
				query = new Query(Criteria.where("_id").is(id));
				EmailInfo info = mongoTemplate.findOne(query, EmailInfo.class);
				if(null!=info) {
					info.setProcessStatus(EmailInfo.PROCESS.DUPLICATE_PROCESSED);
					save(info);
				}
			}
		}
	}

}

