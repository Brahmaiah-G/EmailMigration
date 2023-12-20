package com.testing.mail.connectors.scheduler; 

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.testing.mail.connectors.factory.MailServiceFactory;
import com.testing.mail.connectors.management.CalendarMigrationTask;
import com.testing.mail.constants.DBConstants;
import com.testing.mail.repo.entities.CalendarMoveQueue;
import com.testing.mail.repo.entities.CalenderInfo;
import com.testing.mail.repo.entities.Clouds;
import com.testing.mail.repo.entities.EmailWorkSpace;
import com.testing.mail.repo.entities.EventsInfo;
import com.testing.mail.repo.entities.ThreadControl;
import com.testing.mail.repo.entities.CalenderInfo.PROCESS;
import com.testing.mail.service.DBConnectorService;
import com.testing.mail.utils.HttpUtils;

import lombok.extern.slf4j.Slf4j;


@Slf4j
//@Component
public class CalendarMigrationScheduler {


	@Autowired
	DBConnectorService dbConnectorService;
	@Autowired
	MongoTemplate mongoTemplate;

	ThreadPoolTaskExecutor threadPoolTaskExecutor;

	public CalendarMigrationScheduler() {
	}
	public CalendarMigrationScheduler(ThreadPoolTaskExecutor threadPoolTaskExecutor) {
		this.threadPoolTaskExecutor = threadPoolTaskExecutor;
	}


	@Autowired
	MailServiceFactory mailServiceFactory;

	//Check the application.properties for the cronExpresion
	@Scheduled(cron ="${email.move.cron.expression}")	
	public void processSchedule() throws Exception {
		log.warn("--Enterd for the--"+getClass().getSimpleName());
		final ThreadControl threadControl = dbConnectorService.getEmailInfoRepoImpl().getThreadControl();
		if(HttpUtils.checkController(threadControl) || threadControl.isStopMoving()) {
			log.warn("**********Thread Control MOVING IS DIABLED*******");
			return;
		}
		log.warn("===ThreadPoolActivecount==="+threadPoolTaskExecutor.getActiveCount()+"==Thread Max=="+threadPoolTaskExecutor.getMaxPoolSize()+"==Thread core=="+threadPoolTaskExecutor.getCorePoolSize());
		if(!checkThreadPool()) {
			return;
		}
		List<CalendarMoveQueue> emailWorkSpaeIds = getDistrinctMoveWorkspaceIds(0);
		if(ObjectUtils.isNotEmpty(emailWorkSpaeIds) && !emailWorkSpaeIds.isEmpty()) {
			// processing Each workspace based on the calendarItems
			emailWorkSpaeIds.forEach(emailWrksSpace-> processWorkSpaes(threadControl,emailWrksSpace, 0, emailWorkSpaeIds));
		}
	}
	private void processWorkSpaes(final ThreadControl threadControl,CalendarMoveQueue calendarMoveQueue,int pageNo,List<CalendarMoveQueue> emailWorkSpaeIds ) {
		EmailWorkSpace emailWorkSpace = dbConnectorService.getWorkSpaceRepoImpl().findOne(calendarMoveQueue.getEmailWorkSpaceId());
		calendarMoveQueue.setProcessStatus(com.testing.mail.repo.entities.PROCESS.IN_PROGRESS);
		dbConnectorService.getEmailQueueRepoImpl().saveCalendarQueue(calendarMoveQueue);
		if(ObjectUtils.isEmpty(emailWorkSpace)) {
			log.warn("===WORKSPACE is EMPTY UPDATING THE CALENDERINFOS=="+calendarMoveQueue.getEmailWorkSpaceId());
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

		long count = (threadPoolTaskExecutor.getMaxPoolSize()-threadPoolTaskExecutor.getActiveCount());
		log.info("===COUNT FOR migrating new Threads are "+count+"==for the workspace id==="+calendarMoveQueue.getEmailWorkSpaceId());
		if(count<=0) {
			return;
		}
		Query query = new Query(Criteria.where("_id").is("657c6983b3ad7f605b4b58e4"));//For Debug Enable this and comment the below line
		//Query query = new Query(Criteria.where(DBConstants.USERID).is("657c4fe23bc94c52ad6a78a1").and(DBConstants.PROCESS_STATUS).is(PROCESS.CONFLICT).and("preScan").is(false)).limit((int)count);
		List<EventsInfo> _emails = mongoTemplate.find(query, EventsInfo.class);
		//List<EventsInfo>emails = new ArrayList<>();
//		for(CalenderInfo calenderInfo : _emails) {
//			EventsInfo  eventsInfo = new EventsInfo();
//			BeanUtils.copyProperties(calenderInfo, eventsInfo);
//			emails.add(eventsInfo);
//		}
		//log.warn("==NOT_PROCESSED emails found is  : =="+emails.size());
		// for handling the duplicate processing them first later iterating
		
		for(EventsInfo emailInfo : _emails) {
			if(!checkThreadPool()) {
				return;
			}	
			emailInfo.setProcessStatus(EventsInfo.PROCESS.IN_PROGRESS);
			emailInfo.setThreadBy(Thread.currentThread().getName());
			dbConnectorService.getCalendarInfoRepoImpl().save(emailInfo);
			//threadPoolTaskExecutor.submit(new CalendarMigrationTask(mailServiceFactory, dbConnectorService, emailWorkSpace, emailInfo));
			try {
				new CalendarMigrationTask(mailServiceFactory, dbConnectorService, emailWorkSpace, emailInfo).call();
			} catch (Exception e) {
			}
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
			log.warn("===PLASE WAIT SOMETIME TO PROCESS OTHER THREADS");
			return false;
		}
		return true;
	}


	private List<CalendarMoveQueue> getDistrinctMoveWorkspaceIds(int pageNo) {
		log.info(" ----- I am Normal MailMigration job ----- ");
		CalendarMoveQueue calendarMoveQueue = new CalendarMoveQueue();
		calendarMoveQueue.setEmailWorkSpaceId("657c6973b3ad7f605b4b58c1");
		return Arrays.asList(calendarMoveQueue);
		//return dbConnectorService.getEmailQueueRepoImpl().findCalendarMoveQueueByProcessStatusWithPazination(Arrays.asList(com.cloudfuze.mail.repo.entities.PROCESS.NOT_PROCESSED,com.cloudfuze.mail.repo.entities.PROCESS.IN_PROGRESS), 20, pageNo);
	}

}
