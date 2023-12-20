package com.testing.mail.connectors.scheduler; 

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.testing.mail.connectors.factory.MailServiceFactory;
import com.testing.mail.connectors.management.EventInstanceUpdationTask;
import com.testing.mail.constants.DBConstants;
import com.testing.mail.repo.entities.CalendarMoveQueue;
import com.testing.mail.repo.entities.Clouds;
import com.testing.mail.repo.entities.EmailWorkSpace;
import com.testing.mail.repo.entities.EventsInfo;
import com.testing.mail.repo.entities.ThreadControl;
import com.testing.mail.repo.entities.CalenderInfo.PROCESS;
import com.testing.mail.repo.entities.Clouds.CLOUD_NAME;
import com.testing.mail.service.DBConnectorService;
import com.testing.mail.utils.HttpUtils;

import lombok.extern.slf4j.Slf4j;


@Slf4j
//@Component
public class EventInstanceUpdateScheduler {


	@Autowired
	DBConnectorService dbConnectorService;
	@Autowired
	MongoTemplate mongoTemplate;

	ThreadPoolTaskExecutor threadPoolTaskExecutor;

	public EventInstanceUpdateScheduler() {
	}
	public EventInstanceUpdateScheduler(ThreadPoolTaskExecutor threadPoolTaskExecutor) {
		this.threadPoolTaskExecutor = threadPoolTaskExecutor;
	}


	@Autowired
	MailServiceFactory mailServiceFactory;

	/**Check the <b>application.properties</b> for the cronExpresion*/
	@Scheduled(cron ="${email.events.move.cron.expression}")	
	public void processSchedule() throws Exception {
		log.info("--Enterd for the--"+getClass().getSimpleName());
		final ThreadControl threadControl = dbConnectorService.getEmailInfoRepoImpl().getThreadControl();
		if(HttpUtils.checkController(threadControl) || threadControl.isStopMoving()) {
			log.warn("**********Thread Control MOVING IS DIABLED*******");
			return;
		}
		log.info("===ThreadPoolActivecount==="+threadPoolTaskExecutor.getActiveCount()+"==Thread Max=="+threadPoolTaskExecutor.getMaxPoolSize()+"==Thread core=="+threadPoolTaskExecutor.getCorePoolSize());
		if(!checkThreadPool()) {
			return;
		}
		List<CalendarMoveQueue> emailWorkSpaeIds = getDistrinctMoveWorkspaceIds(0);
		if(ObjectUtils.isNotEmpty(emailWorkSpaeIds) && !emailWorkSpaeIds.isEmpty()) {
			emailWorkSpaeIds.forEach(emailWrksSpace-> processWorkSpaes(threadControl,emailWrksSpace, 0, emailWorkSpaeIds));
		}
	}
	/**processing Each workspace based on the calendarItems*/
	private void processWorkSpaes(final ThreadControl threadControl,CalendarMoveQueue calendarMoveQueue,int pageNo,List<CalendarMoveQueue> emailWorkSpaeIds ) {
		EmailWorkSpace emailWorkSpace = dbConnectorService.getWorkSpaceRepoImpl().findOne(calendarMoveQueue.getEmailWorkSpaceId());
		calendarMoveQueue.setProcessStatus(com.testing.mail.repo.entities.PROCESS.IN_PROGRESS);
		dbConnectorService.getEmailQueueRepoImpl().saveCalendarQueue(calendarMoveQueue);
		if(ObjectUtils.isEmpty(emailWorkSpace)) {
			log.info("===WORKSPACE is EMPTY UPDATING THE CALENDERINFOS=="+calendarMoveQueue.getEmailWorkSpaceId());
			dbConnectorService.getCalendarInfoRepoImpl().findAndUpdateByWorkSpace(calendarMoveQueue.getEmailWorkSpaceId(), threadControl.getRetryCount(), PROCESS.CONFLICT.name(), false, "WorkSpace not Found");
			return;
		}
		Clouds fromCloud = dbConnectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getFromCloudId());
		Clouds toCloud = dbConnectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getToCloudId());
		if(ObjectUtils.isEmpty(fromCloud) || ObjectUtils.isEmpty(toCloud)) {
			log.warn("===CloudsNot Available is EMPTY UPDATING THE EMAILINFOS=="+calendarMoveQueue.getEmailWorkSpaceId());
			dbConnectorService.getCalendarInfoRepoImpl().findAndUpdateByWorkSpace(calendarMoveQueue.getEmailWorkSpaceId(), threadControl.getRetryCount(), PROCESS.CONFLICT.name(), false, "WorkSpace not Found");
			return;
		}

		log.info("===Thread Controller count======="+threadControl.getMoveCount());
		long count = (threadPoolTaskExecutor.getMaxPoolSize()-threadPoolTaskExecutor.getActiveCount());
		if(count<=0) {
			log.info("===COUNT FOR migrating new Threads are "+count+"==for the workspace id==="+calendarMoveQueue.getEmailWorkSpaceId());
			return;
		}
		//Query query = new Query(Criteria.where("_id").is("63ef707c5f831a09de82fb11"));//For Debug Enable this and comment the below line
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(calendarMoveQueue.getEmailWorkSpaceId()).and(DBConstants.PROCESS_STATUS).is(com.testing.mail.repo.entities.PROCESS.INSTANCE_NOTPROCESSED).and("preScan").is(false).and(DBConstants.RETRY).lte(threadControl.getRetryCount())).limit((int)count);
		List<EventsInfo> emails = mongoTemplate.find(query, EventsInfo.class);
		log.info("==INSTANCE_NOT_PROCESSED found is  : =="+emails.size());
		
		for(EventsInfo emailInfo : emails) {
			if(!checkThreadPool()) {
				return;
			}	
			emailInfo.setProcessStatus(EventsInfo.PROCESS.IN_PROGRESS);
			emailInfo.setThreadBy(Thread.currentThread().getName());
			dbConnectorService.getCalendarInfoRepoImpl().save(emailInfo);
			threadPoolTaskExecutor.submit(new EventInstanceUpdationTask(mailServiceFactory, dbConnectorService, emailWorkSpace, emailInfo));	
		}
		pageNo = pageNo+20;
		emailWorkSpaeIds = getDistrinctMoveWorkspaceIds(pageNo);
		if(ObjectUtils.isNotEmpty(emailWorkSpaeIds) && !emailWorkSpaeIds.isEmpty()) {
			for(CalendarMoveQueue pickingQueue : emailWorkSpaeIds) {
				processWorkSpaes(threadControl, pickingQueue, pageNo,emailWorkSpaeIds);
			}
		}
	}

	private boolean checkThreadPool() {
		if(threadPoolTaskExecutor.getActiveCount()+5>=threadPoolTaskExecutor.getMaxPoolSize()) {
			log.info("===PLASE WAIT SOMETIME TO PROCESS OTHER THREADS");
			return false;
		}
		return true;
	}


	private List<CalendarMoveQueue> getDistrinctMoveWorkspaceIds(int pageNo) {
		return dbConnectorService.getEmailQueueRepoImpl().findCalendarMoveQueueByProcessStatusWithPazination(Arrays.asList(com.testing.mail.repo.entities.PROCESS.NOT_PROCESSED,com.testing.mail.repo.entities.PROCESS.IN_PROGRESS), 20, pageNo,CLOUD_NAME.OUTLOOK.name());
	}

}
