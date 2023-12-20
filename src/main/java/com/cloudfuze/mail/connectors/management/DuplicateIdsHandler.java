package com.cloudfuze.mail.connectors.management;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.cloudfuze.mail.constants.DBConstants;
import com.cloudfuze.mail.repo.entities.CalendarMoveQueue;
import com.cloudfuze.mail.repo.entities.CalendarPickingQueue;
import com.cloudfuze.mail.repo.entities.EmailJobDetails;
import com.cloudfuze.mail.repo.entities.EmailMoveQueue;
import com.cloudfuze.mail.repo.entities.EmailPickingQueue;
import com.cloudfuze.mail.repo.entities.PROCESS;
import com.cloudfuze.mail.service.DBConnectorService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DuplicateIdsHandler {


	@Autowired
	DBConnectorService dbService;

	@Autowired
	MongoTemplate mongoTemplate;
	int pageNo = 20;
	int skip = pageNo;
	Gson gson = new Gson();

	private void updateDuplcateEventRecords(String jobId,int skip) {

		List<Document> pipeline = Arrays.asList(
                new Document("$match", new Document(DBConstants.JOB_ID, jobId).append(DBConstants.PROCESS_STATUS, PROCESS.NOT_PROCESSED.name())),
                new Document("$group", new Document("_id", new Document("sourceId", "$sourceId"))
                        .append("uniqueIds", new Document("$addToSet", "$_id"))
                        .append("count", new Document("$sum", 1))),
                new Document("$match", new Document("count", new Document("$gt", 1))),
                new Document("$project", new Document("duplicate", new Document("$slice", Arrays.asList("$uniqueIds", 1)))
                        .append("_id", 0))
        );
		log.info("--Entered for duplicate events processing for JobId:"+jobId);
		MongoCollection<Document> mongoCollection = mongoTemplate.getCollection("eventsInfo");
		AggregateIterable<Document> output = mongoCollection.aggregate(pipeline).allowDiskUse(true);
		MongoCursor<?>result = output.cursor();
		List<String> dupIds = new ArrayList<>();
		while(result.hasNext()) {
			Document data = (Document)result.next();
			if(data.get("duplicate")!=null) {
				ArrayList<String> dups = new ArrayList<>();
				try {
					Type listType = new TypeToken<ArrayList<String>>(){}.getType();
					dups = gson.fromJson(data.get("duplicate").toString(), listType);
				} catch (Exception e) {
					log.error(ExceptionUtils.getStackTrace(e));
				}
				dupIds.addAll(dups);
				if(dupIds.size()>20) {
					dbService.getCalendarInfoRepoImpl().updateDuplicateEvents(dupIds);
					dupIds.clear();
				}
			}
		}
		if(!dupIds.isEmpty()) {
			dbService.getCalendarInfoRepoImpl().updateDuplicateEvents(dupIds);
			dupIds.clear();
		}
	}
	
	
	
	
	private void updateDuplcateMailRecords(String jobId,int skip) {

		List<Document> pipeline = Arrays.asList(
                new Document("$match", new Document(DBConstants.JOB_ID, jobId).append(DBConstants.PROCESS_STATUS, PROCESS.NOT_PROCESSED.name())),
                new Document("$group", new Document("_id", new Document("sourceId", "$sourceId"))
                        .append("uniqueIds", new Document("$addToSet", "$_id"))
                        .append("count", new Document("$sum", 1))),
                new Document("$match", new Document("count", new Document("$gt", 1))),
                new Document("$project", new Document("duplicate", new Document("$slice", Arrays.asList("$uniqueIds", 1)))
                        .append("_id", 0))
        );
		log.info("--Entered for duplicate Mails processing for JobId:"+jobId);
		MongoCollection<Document> mongoCollection = mongoTemplate.getCollection("emailInfo");
		AggregateIterable<Document> output = mongoCollection.aggregate(pipeline).allowDiskUse(true);
		MongoCursor<?>result = output.cursor();
		List<String> dupIds = new ArrayList<>();
		while(result.hasNext()) {
			Document data = (Document)result.next();
			if(data.get("duplicate")!=null) {
				ArrayList<String> dups = new ArrayList<>();
				try {
					Type listType = new TypeToken<ArrayList<String>>(){}.getType();
					dups = gson.fromJson(data.get("duplicate").toString(), listType);
				} catch (Exception e) {
					log.error(ExceptionUtils.getStackTrace(e));
				}
				dupIds.addAll(dups);
				if(dupIds.size()>20) {
					dbService.getEmailInfoRepoImpl().updateDuplicateMails(dupIds);
					dupIds.clear();
				}
			}
		}
		if(!dupIds.isEmpty()) {
			dbService.getCalendarInfoRepoImpl().updateDuplicateEvents(dupIds);
			dupIds.clear();
		}
	}

	@Scheduled(cron ="* */1 * * * ?")	
	private void processSchedule() {
		fetchAndUpdateJobDetails(0);
	}

	private void fetchAndUpdateJobDetails(int skip) {
		List<EmailJobDetails> emailJobDetails =	dbService.getEmailJobRepoImpl().getInprogressJobDetails(20, skip);

		if(emailJobDetails.isEmpty()) {
			return;
		}
		for(EmailJobDetails job : emailJobDetails){
			if(job.isCalendars() && !job.isCalFiltered()) {
				List<CalendarPickingQueue> listQueues = dbService.getEmailQueueRepoImpl().findCalendarsPickingQueueByJob(job.getId());
				if(listQueues.isEmpty() || !validatePickingQueue(listQueues)) {
					continue;
				}
				job.setCalFiltered(true);
				dbService.getEmailJobRepoImpl().save(job);
				updateDuplcateEventRecords(job.getId(), skip);
				listQueues.forEach(this::createCalMoveQueue);
			}else if(!job.isMailsFiltered()){
				List<EmailPickingQueue> listQueues = dbService.getEmailQueueRepoImpl().findMailsPickingQueueByJob(job.getId());
				if(listQueues.isEmpty() || !validateMailPickingQueue(listQueues)) {
					continue;
				}
				job.setMailsFiltered(true);
				dbService.getEmailJobRepoImpl().save(job);
				updateDuplcateMailRecords(job.getId(), skip);
				listQueues.forEach(this::createMoveQueue);
			}
		}
		skip = skip+20;
		fetchAndUpdateJobDetails(skip);
	}

	private boolean validatePickingQueue(List<CalendarPickingQueue> listQueues) {
		boolean allDone = false;
		if(listQueues.isEmpty()) {
			return allDone;
		}
		for(CalendarPickingQueue pickingQueue : listQueues) {
			if(PROCESS.IN_PROGRESS.name().equals(pickingQueue.getProcessStatus().name())) {
				break;
			}
			allDone = true;
		}
		return allDone;
	}

	private boolean validateMailPickingQueue(List<EmailPickingQueue> listQueues) {
		boolean allDone = false;
		if(listQueues.isEmpty()) {
			return allDone;
		}
		for(EmailPickingQueue pickingQueue : listQueues) {
			if(PROCESS.IN_PROGRESS.name().equals(pickingQueue.getProcessStatus().name())) {
				break;
			}
			allDone = true;
		}
		return allDone;
	}
	
	
	
	
	private void createCalMoveQueue(CalendarPickingQueue pickingQueue) {
		CalendarMoveQueue emailQueue = new CalendarMoveQueue();
		emailQueue.setUserId(pickingQueue.getUserId());
		emailQueue.setEmailWorkSpaceId(pickingQueue.getEmailWorkSpaceId());
		emailQueue.setJobId(pickingQueue.getJobId());
		emailQueue.setCreatedTime(LocalDateTime.now());
		emailQueue.setCloudName(pickingQueue.getCloudName());
		dbService.getEmailQueueRepoImpl().saveCalendarQueue(emailQueue); 
	}
	
	private void createMoveQueue(EmailPickingQueue pickingQueue) {
		EmailMoveQueue emailQueue  = dbService.getEmailQueueRepoImpl().findMoveQueue(pickingQueue.getEmailWorkSpaceId());
		if(null==emailQueue) {
			emailQueue = new EmailMoveQueue();
		}
		emailQueue.setUserId(pickingQueue.getUserId());
		emailQueue.setEmailWorkSpaceId(pickingQueue.getEmailWorkSpaceId());
		emailQueue.setJobId(pickingQueue.getJobId());
		emailQueue.setCreatedTime(LocalDateTime.now());
		emailQueue.setCloudName(pickingQueue.getCloudName());
		emailQueue.setContacts(pickingQueue.isContacts());
		emailQueue.setMailRules(pickingQueue.isMailRules());
		emailQueue.setSettings(pickingQueue.isSettings());
		dbService.getEmailQueueRepoImpl().saveQueue(emailQueue); 
	}
}
