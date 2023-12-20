package com.cloudfuze.mail.connectors.scheduler; 
/**
 * @author BrahmaiahG
 */

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.cloudfuze.mail.connectors.factory.MailServiceFactory;
import com.cloudfuze.mail.connectors.management.CalendarMetadataTask;
import com.cloudfuze.mail.constants.DBConstants;
import com.cloudfuze.mail.repo.entities.CalendarMoveQueue;
import com.cloudfuze.mail.repo.entities.CalenderInfo.PROCESS;
import com.cloudfuze.mail.repo.entities.EmailWorkSpace;
import com.cloudfuze.mail.repo.entities.EventsInfo;
import com.cloudfuze.mail.repo.entities.ThreadControl;
import com.cloudfuze.mail.service.DBConnectorService;
import com.cloudfuze.mail.utils.HttpUtils;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class CalendarMetadataScheduler {


	@Autowired
	DBConnectorService dbConnectorService;
	@Autowired
	MongoTemplate mongoTemplate;

	ThreadPoolTaskExecutor threadPoolTaskExecutor;

	public CalendarMetadataScheduler() {
	}
	public CalendarMetadataScheduler(ThreadPoolTaskExecutor threadPoolTaskExecutor) {
		this.threadPoolTaskExecutor = threadPoolTaskExecutor;
	}


	@Autowired
	MailServiceFactory mailServiceFactory;

	@Scheduled(cron ="0/30 * * * * ?")	
	public void processSchedule() throws Exception {
		log.warn("--Enterd for the--"+getClass().getSimpleName());
		final ThreadControl threadControl = dbConnectorService.getEmailInfoRepoImpl().getThreadControl();
		if(HttpUtils.checkController(threadControl) || threadControl.isMetadata()) {
			log.warn("**********Thread Control MOVING IS DIABLED*******");
			return;
		}
		log.warn("===ThreadPoolActivecount==="+threadPoolTaskExecutor.getActiveCount()+"==Thread Max=="+threadPoolTaskExecutor.getMaxPoolSize()+"==Thread core=="+threadPoolTaskExecutor.getCorePoolSize());
		if(!checkThreadPool()) {
			return;
		}
		List<CalendarMoveQueue> emailWorkSpaeIds = getDistrinctMoveWorkspaceIds(0);
		if(ObjectUtils.isNotEmpty(emailWorkSpaeIds) && !emailWorkSpaeIds.isEmpty()) {
			emailWorkSpaeIds.stream().forEach(emailWrksSpace-> processWorkSpaes(threadControl,emailWrksSpace));
		}
	}
	private void processWorkSpaes(final ThreadControl threadControl,CalendarMoveQueue calMoveQueue) {
		EmailWorkSpace emailWorkSpace = dbConnectorService.getWorkSpaceRepoImpl().findOne(calMoveQueue.getEmailWorkSpaceId());
		calMoveQueue.setProcessStatus(com.cloudfuze.mail.repo.entities.PROCESS.IN_PROGRESS);
		dbConnectorService.getEmailQueueRepoImpl().saveCalendarQueue(calMoveQueue);
		if(ObjectUtils.isEmpty(emailWorkSpace)) {
			log.warn("===WORKSPACE is EMPTY UPDATING THE CALENDARINFOS=="+calMoveQueue.getEmailWorkSpaceId());
			dbConnectorService.getCalendarInfoRepoImpl().findAndUpdateByWorkSpace(calMoveQueue.getEmailWorkSpaceId(), threadControl.getRetryCount(), PROCESS.CONFLICT.name(), false, "WorkSpace not Found");
			return;
		}

		log.warn("===Thread Controller count======="+threadControl.getMoveCount());
		long count = threadControl.getMetadataCount();
		count = (threadPoolTaskExecutor.getMaxPoolSize()-threadPoolTaskExecutor.getActiveCount())-count;
		if(count<=0) {
			log.warn("===COUNT FOR migrating new Threads are "+count+"==for the workspace id==="+calMoveQueue);
			return;
		}
		//Query query = new Query(Criteria.where("_id").is("63ee49ff3bb7520cbd9c2f12"));
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(calMoveQueue.getEmailWorkSpaceId()).and(DBConstants.PROCESS_STATUS).is(PROCESS.METADATA_STARTED).and("preScan").is(false)).limit(threadControl.getMetadataCount());
		List<EventsInfo> emails = mongoTemplate.find(query, EventsInfo.class);
		log.warn("==METADATA Calendars found is  : =="+emails.size());
		for(EventsInfo emailInfo : emails) {
			if(!checkThreadPool()) {
				return;
			}
			emailInfo.setThreadBy(Thread.currentThread().getName());
			emailInfo.setProcessStatus(EventsInfo.PROCESS.METADATA_INPROGRESS);
			dbConnectorService.getCalendarInfoRepoImpl().save(emailInfo);
			threadPoolTaskExecutor.submit(new CalendarMetadataTask(mailServiceFactory, dbConnectorService, emailWorkSpace, emailInfo));	
		}
	}

	private boolean checkThreadPool() {
		if(threadPoolTaskExecutor.getActiveCount()+1>=threadPoolTaskExecutor.getMaxPoolSize()) {
			log.warn("===PLASE WAIT SOMETIME TO PROCESS OTHER THREADS");
			return false;
		}
		return true;
	}

	private List<CalendarMoveQueue> getDistrinctMoveWorkspaceIds(int pageNo) {
		log.info(" ----- I am Normal MailMigration job ----- ");
		return dbConnectorService.getEmailQueueRepoImpl().findCalendarMoveQueueByProcessStatusWithPazination(Arrays.asList(com.cloudfuze.mail.repo.entities.PROCESS.NOT_PROCESSED,com.cloudfuze.mail.repo.entities.PROCESS.IN_PROGRESS), 20, pageNo);
	}
}
