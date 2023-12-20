package com.testing.mail.connectors.scheduler;

import java.time.LocalDateTime;
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
import com.testing.mail.connectors.management.CalendarPickingTask;
import com.testing.mail.constants.DBConstants;
import com.testing.mail.repo.entities.CalendarMoveQueue;
import com.testing.mail.repo.entities.CalendarPickingQueue;
import com.testing.mail.repo.entities.CalenderInfo;
import com.testing.mail.repo.entities.EmailWorkSpace;
import com.testing.mail.repo.entities.ThreadControl;
import com.testing.mail.repo.entities.CalenderInfo.PROCESS;
import com.testing.mail.service.DBConnectorService;
import com.testing.mail.utils.HttpUtils;

import lombok.extern.slf4j.Slf4j;


/**
 *<p>
 *Scheduler for Picking the NOT_STARTED CalendarInfo's
 *</p>
 *<p>
 *CronExpression : application.properties
 *</p>
 *<p>
 *Implemented class CalendarPicker
 *</p>
 *@see  CalendarPicker
 */

@Slf4j
//@Component
public class CalendarPickerScheduler {


	@Autowired
	DBConnectorService dbConnectorService;
	@Autowired
	MongoTemplate mongoTemplate;

	ThreadPoolTaskExecutor threadPoolTaskExecutor;

	public CalendarPickerScheduler(ThreadPoolTaskExecutor threadPoolTaskExecutor) {
		this.threadPoolTaskExecutor = threadPoolTaskExecutor;
	}

	@Autowired
	MailServiceFactory mailServiceFactory;

	@Scheduled(cron ="${email.picking.cron.expression}")	
	public void processSchedule() {

		final ThreadControl threadControl = dbConnectorService.getEmailInfoRepoImpl().getThreadControl();
		if(HttpUtils.checkController(threadControl) || threadControl.isStopPicking()) {
			log.warn("**********Thread Control DISABLED PICKING *******");
			return;
		}
		log.warn("===ThreadPoolActivecount==="+threadPoolTaskExecutor.getActiveCount()+"==Thread Max=="+threadPoolTaskExecutor.getMaxPoolSize()+"==Thread core=="+threadPoolTaskExecutor.getCorePoolSize());
		if(!checkThreadPool()) {
			return;
		}
		List<CalendarPickingQueue> calendarPickingQueue = getDistrinctMoveWorkspaceIds(0);
		if(null!=calendarPickingQueue) {
			calendarPickingQueue.forEach(emailWrksSpace->
			{
				processWorkSpaces(threadControl,emailWrksSpace,calendarPickingQueue,0);
			});
		}
	}

	private void processWorkSpaces(ThreadControl threadControl,CalendarPickingQueue calendarPickingQueue,List<CalendarPickingQueue> emailWorkSpaeIds,int pageNo) {
		EmailWorkSpace emailWorkSpace = dbConnectorService.getWorkSpaceRepoImpl().findOne(calendarPickingQueue.getEmailWorkSpaceId());
		if(ObjectUtils.isEmpty(emailWorkSpace)) {
			log.warn("===WORKSPACE is EMPTY UPDATING THE EMAILINFOS=="+calendarPickingQueue.getEmailWorkSpaceId());
			dbConnectorService.getEmailInfoRepoImpl().findAndUpdateByWorkSpace(calendarPickingQueue.getEmailWorkSpaceId(), threadControl.getRetryCount(), PROCESS.CONFLICT.name(),true,"Workspace Not Found");
			return;
		}
		calendarPickingQueue.setProcessStatus(com.testing.mail.repo.entities.PROCESS.IN_PROGRESS);
		calendarPickingQueue.setModifiedTime(LocalDateTime.now());
		dbConnectorService.getEmailQueueRepoImpl().saveCalendarPickingQueue(calendarPickingQueue);
		long count = (threadPoolTaskExecutor.getMaxPoolSize()-threadPoolTaskExecutor.getActiveCount());
		if(count<=0) {
			log.warn("===COUNT FOR migrating new Threads are "+count+"==for the workspace id==="+calendarPickingQueue.getEmailWorkSpaceId());
			return;
		}
		Query query = new Query(Criteria.where("_id").is("657c697ab3ad7f605b4b58d8")); 
		//Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(calendarPickingQueue.getEmailWorkSpaceId()).and(DBConstants.PROCESS_STATUS).is(PROCESS.NOT_STARTED.name()).and("calender").is(true).and("threadBy").is(null).and("retryCount").lt(threadControl.getRetryCount())).limit((int)count);
		List<CalenderInfo> emails = mongoTemplate.find(query, CalenderInfo.class);
		log.warn("==Not_Started emails found is  : =="+emails.size());

		for(CalenderInfo emailInfo : emails) {
			if(!checkThreadPool()) {
				return;
			}
			emailInfo.setProcessStatus(PROCESS.IN_PROGRESS);
			emailInfo.setThreadBy(Thread.currentThread().getName());
			dbConnectorService.getCalendarInfoRepoImpl().save(emailInfo);
			//threadPoolTaskExecutor.submit(new CalendarPickingTask(mailServiceFactory, dbConnectorService, emailWorkSpace, emailInfo));	
			try {
				new CalendarPickingTask(mailServiceFactory, dbConnectorService, emailWorkSpace, emailInfo).call();
			} catch (Exception e) {
			}
		}
		pageNo = pageNo+20;
		emailWorkSpaeIds = getDistrinctMoveWorkspaceIds(pageNo);
		if(ObjectUtils.isNotEmpty(emailWorkSpaeIds) && !emailWorkSpaeIds.isEmpty()) {
			for(CalendarPickingQueue pickingQueue : emailWorkSpaeIds) {
				processWorkSpaces(threadControl, pickingQueue, emailWorkSpaeIds,pageNo);
			}
		}
	}

	private boolean checkThreadPool() {
		if(threadPoolTaskExecutor.getActiveCount()+1>=threadPoolTaskExecutor.getMaxPoolSize()) {
			log.warn("===PLASE WAIT SOMETIME TO PROCESS OTHER THREADS");
			return false;
		}
		return true;
	}

	private List<CalendarPickingQueue> getDistrinctMoveWorkspaceIds(int pageNo) {
		log.info(" ----- I am Normal MailMigration job ----- ");
		CalendarPickingQueue calendarMoveQueue = new CalendarPickingQueue();
		calendarMoveQueue.setEmailWorkSpaceId("657c6973b3ad7f605b4b58c1");
		return Arrays.asList(calendarMoveQueue);
		//checkForPickingCompletion();
		//return dbConnectorService.getEmailQueueRepoImpl().findCalendarPickingByProcessStatusWithPazination(Arrays.asList(com.cloudfuze.mail.repo.entities.PROCESS.NOT_PROCESSED,com.cloudfuze.mail.repo.entities.PROCESS.IN_PROGRESS), 20, pageNo);
	}
	private void checkForPickingCompletion() {
		List<CalendarPickingQueue> inQueues = dbConnectorService.getEmailQueueRepoImpl().findCalendarPickingByProcessStatusWithPazination(Arrays.asList(com.testing.mail.repo.entities.PROCESS.IN_PROGRESS,com.testing.mail.repo.entities.PROCESS.CONFLICT), 100, 0);
		if(!inQueues.isEmpty()) {
			inQueues.forEach(pickingQueue->{
				List<CalenderInfo> infos = dbConnectorService.getCalendarInfoRepoImpl().findByProcessStatus(pickingQueue.getEmailWorkSpaceId(), Arrays.asList(com.testing.mail.repo.entities.PROCESS.IN_PROGRESS,com.testing.mail.repo.entities.PROCESS.NOT_STARTED,com.testing.mail.repo.entities.PROCESS.CONFLICT));
				if(infos.isEmpty()) {
					pickingQueue.setProcessStatus(com.testing.mail.repo.entities.PROCESS.PROCESSED);
					dbConnectorService.getEmailQueueRepoImpl().saveCalendarPickingQueue(pickingQueue);
				}else {
					List<CalenderInfo> _infos = dbConnectorService.getCalendarInfoRepoImpl().findByProcessStatus(pickingQueue.getEmailWorkSpaceId(), Arrays.asList(com.testing.mail.repo.entities.PROCESS.CONFLICT));
					if(_infos.size()==infos.size()) {
						pickingQueue.setProcessStatus(com.testing.mail.repo.entities.PROCESS.CONFLICT);
						dbConnectorService.getEmailQueueRepoImpl().saveCalendarPickingQueue(pickingQueue);
					}
				}
			});
		}
	}
}
