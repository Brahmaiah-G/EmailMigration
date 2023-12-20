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
import com.testing.mail.connectors.management.SettingMigrationTask;
import com.testing.mail.constants.DBConstants;
import com.testing.mail.constants.ExceptionConstants;
import com.testing.mail.dao.entities.EmailUserSettings;
import com.testing.mail.repo.entities.Clouds;
import com.testing.mail.repo.entities.EmailMoveQueue;
import com.testing.mail.repo.entities.EmailWorkSpace;
import com.testing.mail.repo.entities.PROCESS;
import com.testing.mail.repo.entities.ThreadControl;
import com.testing.mail.service.DBConnectorService;
import com.testing.mail.utils.HttpUtils;

import lombok.extern.slf4j.Slf4j;


@Slf4j
//@Component
public class MailSettingsScheduler {


	@Autowired
	DBConnectorService dbConnectorService;
	@Autowired
	MongoTemplate mongoTemplate;

	ThreadPoolTaskExecutor threadPoolTaskExecutor;

	public MailSettingsScheduler() {
	}
	public MailSettingsScheduler(ThreadPoolTaskExecutor threadPoolTaskExecutor) {
		this.threadPoolTaskExecutor = threadPoolTaskExecutor;
	}

	@Autowired
	MailServiceFactory mailServiceFactory;

	@Scheduled(cron ="${email.move.cron.expression}")	
	public void processSchedule() throws Exception {
		log.info("--Enterd for the--"+getClass().getSimpleName());
		final ThreadControl threadControl = dbConnectorService.getEmailInfoRepoImpl().getThreadControl();
		if(HttpUtils.checkController(threadControl) || threadControl.isStopMoving()) {
			log.error("**********Thread Control MOVING IS DIABLED*******");
			return;
		}
		log.info("===ThreadPoolActivecount==="+threadPoolTaskExecutor.getActiveCount()+"==Thread Max=="+threadPoolTaskExecutor.getMaxPoolSize()+"==Thread core=="+threadPoolTaskExecutor.getCorePoolSize());
		if(!checkThreadPool()) {
			return;
		}
		List<EmailMoveQueue> emailWorkSpaeIds = getDistrinctMoveWorkspaceIds(mongoTemplate, threadControl,0);
		if(ObjectUtils.isNotEmpty(emailWorkSpaeIds) && !emailWorkSpaeIds.isEmpty()) {
			emailWorkSpaeIds.forEach(emailWrksSpace-> processWorkSpaes(threadControl,emailWrksSpace,0,emailWorkSpaeIds));
		}
	}
	private void processWorkSpaes(final ThreadControl threadControl,EmailMoveQueue emailMoveQueue,int pageNo,List<EmailMoveQueue> emailWorkSpaeIds) {
		EmailWorkSpace emailWorkSpace = dbConnectorService.getWorkSpaceRepoImpl().findOne(emailMoveQueue.getEmailWorkSpaceId());
		updateMoveQueue(emailMoveQueue.getId(), ExceptionConstants.STARTED, PROCESS.IN_PROGRESS);
			if(ObjectUtils.isEmpty(emailWorkSpace)) {
				log.info("===WORKSPACE is EMPTY UPDATING THE EMAILINFOS=="+emailMoveQueue);
				dbConnectorService.getEmailInfoRepoImpl().findAndUpdateByWorkSpace(emailMoveQueue.getEmailWorkSpaceId(), threadControl.getRetryCount(),EmailWorkSpace.PROCESS.CONFLICT.name(), false, ExceptionConstants.WORKSPACE_NOT_FOUND);
				updateMoveQueue(emailMoveQueue.getId(), ExceptionConstants.WORKSPACE_NOT_FOUND, PROCESS.CONFLICT);
				return;
			}else {
				Clouds fromCloud = dbConnectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getFromCloudId());
				Clouds toCloud = dbConnectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getToCloudId());
				if(ObjectUtils.isEmpty(fromCloud) || ObjectUtils.isEmpty(toCloud)) {
					dbConnectorService.getEmailInfoRepoImpl().findAndUpdateByWorkSpace(emailMoveQueue.getEmailWorkSpaceId(), threadControl.getRetryCount(), EmailWorkSpace.PROCESS.CONFLICT.name(), false, ExceptionConstants.CLOUD_NOT_FOUND);
					updateMoveQueue(emailMoveQueue.getId(), ExceptionConstants.CLOUD_NOT_FOUND, PROCESS.CONFLICT);
					return;
				}
			}
			
		long count = (threadPoolTaskExecutor.getMaxPoolSize()-threadPoolTaskExecutor.getActiveCount());
		log.info("===Thread Controller count======="+count+"--"+emailMoveQueue.getEmailWorkSpaceId());
		if(count<=0) {
			log.info("===COUNT FOR migrating new Threads are "+count+"==for the workspace id==="+emailMoveQueue.getEmailWorkSpaceId());
			return;
		}
		List<EmailUserSettings> emails = extractRulesBasedOnPrior(threadControl, emailMoveQueue.getEmailWorkSpaceId(), count);
		//Query query = new Query(Criteria.where("_id").is("657b2954f2c8e5387a8024b2"));
		//emails = mongoTemplate.find(query, EmailInfo.class);
		log.info("==NOT_PROCESSED contact found is  : =="+emails.size()+"--"+emailWorkSpace.getId());
		for(EmailUserSettings settings : emails) {
			if(!checkThreadPool()) {
				return;
			}
			settings.setProcessStatus(PROCESS.IN_PROGRESS);
			settings.setThreadBy(Thread.currentThread().getName());
			settings.setErrorDescription(null);
			dbConnectorService.getMailUserSettingsRepoImpl().save(settings);
			threadPoolTaskExecutor.submit(new SettingMigrationTask(emailWorkSpace, mailServiceFactory, dbConnectorService, settings));
		}
		pageNo = pageNo+20;
		emailWorkSpaeIds = getDistrinctMoveWorkspaceIds(mongoTemplate, threadControl,pageNo);
		for(EmailMoveQueue moveQueue : emailWorkSpaeIds) {
			processWorkSpaes(threadControl, moveQueue, pageNo, emailWorkSpaeIds);
		}
	}
	
	private List<EmailUserSettings> extractRulesBasedOnPrior(final ThreadControl threadControl, String emailWorkSpaceId, long count) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and(DBConstants.PROCESS_STATUS).is(PROCESS.NOT_PROCESSED).and("threadBy").is(null).and("retryCount").lt(threadControl.getRetryCount()));
		return mongoTemplate.find(query.limit((int) count), EmailUserSettings.class);
	}

	private boolean checkThreadPool() {
		if(threadPoolTaskExecutor.getActiveCount()+1>=threadPoolTaskExecutor.getMaxPoolSize()) {
			log.info("===PLASE WAIT SOMETIME TO PROCESS OTHER THREADS");
			return false;
		}
		return true;
	}
	
	
	private List<EmailMoveQueue> getDistrinctMoveWorkspaceIds(MongoTemplate mongoOps, ThreadControl threadControl,int pageNo) {
		checkThreadPool();
		log.info(" ----- I am Normal MailMigration job ----- ");
		return dbConnectorService.getEmailQueueRepoImpl().findMoveQueueByforSettings(Arrays.asList(PROCESS.NOT_PROCESSED,PROCESS.IN_PROGRESS), 20, pageNo);
	}
	
	private void updateMoveQueue(String moveQueueId,String errorDescription,PROCESS processStatus) {
		EmailMoveQueue queue = dbConnectorService.getEmailQueueRepoImpl().findMoveQueue(moveQueueId);
		if(queue!=null) {
			queue.setProcessStatus(processStatus);
			queue.setErrorDescription(errorDescription);
			dbConnectorService.getEmailQueueRepoImpl().saveQueue(queue);
		}
	
	}
}
