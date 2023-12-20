package com.cloudfuze.mail.connectors.scheduler;

import java.time.LocalDateTime;
/**
 * @author BrahmaiahG
*/
import java.util.ArrayList;
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
import com.cloudfuze.mail.connectors.management.EmailPurger;
import com.cloudfuze.mail.constants.DBConstants;
import com.cloudfuze.mail.repo.entities.EmailInfo;
import com.cloudfuze.mail.repo.entities.EmailInfo.PROCESS;
import com.cloudfuze.mail.repo.entities.EmailPurgeQueue;
import com.cloudfuze.mail.repo.entities.EmailWorkSpace;
import com.cloudfuze.mail.repo.entities.ThreadControl;
import com.cloudfuze.mail.service.DBConnectorService;
import com.cloudfuze.mail.utils.HttpUtils;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class MailPurgerScheduler {


	@Autowired
	DBConnectorService dbConnectorService;
	@Autowired
	MongoTemplate mongoTemplate;
	
	ThreadPoolTaskExecutor threadPoolTaskExecutor;
	
	public MailPurgerScheduler(ThreadPoolTaskExecutor threadPoolTaskExecutor) {
		this.threadPoolTaskExecutor = threadPoolTaskExecutor;
	}
	
	@Autowired
	MailServiceFactory mailServiceFactory;

	@Scheduled(cron ="0/45 * * * * ?")	
	public void processSchedule() {

		final ThreadControl threadControl = dbConnectorService.getEmailInfoRepoImpl().getThreadControl();
		if(HttpUtils.checkController(threadControl) || threadControl.isStopPurge()) {
			log.info("**********Thread Control DISABLED PICKING *******");
			return;
		}
		log.info("===ThreadPoolActivecount==="+threadPoolTaskExecutor.getActiveCount()+"==Thread Max=="+threadPoolTaskExecutor.getMaxPoolSize()+"==Thread core=="+threadPoolTaskExecutor.getCorePoolSize());
		if(!checkThreadPool()) {
			return;
		}
		List<EmailPurgeQueue> emailWorkSpaeIds = getDistrinctMoveWorkspaceIds(threadControl);
		if(ObjectUtils.isNotEmpty(emailWorkSpaeIds) && !emailWorkSpaeIds.isEmpty()) {
			emailWorkSpaeIds.stream().forEach(emailWrksSpace-> processWorkSpaces(threadControl,emailWrksSpace));
		}
	}

	private void processWorkSpaces(ThreadControl threadControl,EmailPurgeQueue emailPurgeQueue) {
		EmailWorkSpace emailWorkSpace = dbConnectorService.getWorkSpaceRepoImpl().findOne(emailPurgeQueue.getEmailWorkSpaceId());
		if(ObjectUtils.isEmpty(emailWorkSpace)) {
			log.info("===WORKSPACE is EMPTY UPDATING THE EMAILINFOS=="+emailPurgeQueue);
			dbConnectorService.getEmailInfoRepoImpl().findAndUpdateByWorkSpace(emailPurgeQueue.getEmailWorkSpaceId(), threadControl.getRetryCount(), PROCESS.CONFLICT.name(),true,"Workspace Not Found");
			return;

		}
		long count = 0;
		count = (threadPoolTaskExecutor.getMaxPoolSize()-threadPoolTaskExecutor.getActiveCount());
		if(count<=0) {
			log.info("===COUNT FOR migrating new Threads are "+count+"==for the workspace id==="+emailPurgeQueue.getEmailWorkSpaceId());
			return;
		}
		emailPurgeQueue.setProcessStatus(com.cloudfuze.mail.repo.entities.PROCESS.IN_PROGRESS);
		emailPurgeQueue.setModifiedTime(LocalDateTime.now());
		dbConnectorService.getEmailQueueRepoImpl().savePurgeQueue(emailPurgeQueue);
		List<EmailInfo.PROCESS> processStatusList = new ArrayList<>();
		processStatusList.add(PROCESS.PROCESSED);
		processStatusList.add(PROCESS.METADATA_STARTED);
		processStatusList.add(PROCESS.METADATA_CONFLICT);
		processStatusList.add(PROCESS.CONFLICT);
		//Query query = new Query(Criteria.where("_id").is("654758a30c466e0c1de2cb27"));
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailPurgeQueue.getEmailWorkSpaceId()).and(DBConstants.PROCESS_STATUS).in(processStatusList).and("folder").is(false).and("adminDeleted").is(false).and("preScan").is(false)).limit(threadControl.getPickCount());
		List<EmailInfo> emails = mongoTemplate.find(query, EmailInfo.class);
		log.info("==EMAIL PURGER  emails found is  : =="+emails.size());
		for(EmailInfo emailInfo : emails) {
			if(!checkThreadPool()) {
				return;
			}
			emailInfo.setThreadBy(Thread.currentThread().getName());
			emailInfo.setAdminDeleted(true);
			//emailInfo.setProcessStatus(EmailInfo.PROCESS.IN_PROGRESS);
			dbConnectorService.getEmailInfoRepoImpl().save(emailInfo);
			threadPoolTaskExecutor.submit(new EmailPurger(mailServiceFactory, dbConnectorService, emailWorkSpace, emailInfo));	
		}
	}

	private boolean checkThreadPool() {
		if(threadPoolTaskExecutor.getActiveCount()+1==threadPoolTaskExecutor.getCorePoolSize()) {
			log.info("===PLASE WAIT SOMETIME TO PROCESS OTHER THREADS");
			return false;
		}
		return true;
	}
	
	private List<EmailPurgeQueue> getDistrinctMoveWorkspaceIds(ThreadControl threadControl) {
		checkThreadPool();
		return dbConnectorService.getEmailQueueRepoImpl().findPurgeQueueByProcessStatusWithPazination(Arrays.asList(com.cloudfuze.mail.repo.entities.PROCESS.NOT_PROCESSED,com.cloudfuze.mail.repo.entities.PROCESS.IN_PROGRESS), 20, 0);
	
	}

}
