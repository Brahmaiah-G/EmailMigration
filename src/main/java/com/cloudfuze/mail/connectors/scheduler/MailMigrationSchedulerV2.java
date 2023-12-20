package com.cloudfuze.mail.connectors.scheduler; 

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.cloudfuze.mail.connectors.factory.MailServiceFactory;
import com.cloudfuze.mail.connectors.management.MailMigrationTask;
import com.cloudfuze.mail.constants.DBConstants;
import com.cloudfuze.mail.constants.ExceptionConstants;
import com.cloudfuze.mail.repo.entities.Clouds;
import com.cloudfuze.mail.repo.entities.Clouds.CLOUD_NAME;
import com.cloudfuze.mail.repo.entities.EmailInfo;
import com.cloudfuze.mail.repo.entities.EmailMoveQueue;
import com.cloudfuze.mail.repo.entities.EmailWorkSpace;
import com.cloudfuze.mail.repo.entities.PROCESS;
import com.cloudfuze.mail.repo.entities.ThreadControl;
import com.cloudfuze.mail.service.DBConnectorService;
import com.cloudfuze.mail.utils.HttpUtils;

import lombok.extern.slf4j.Slf4j;


@Slf4j
//@Component
public class MailMigrationSchedulerV2 {


	@Autowired
	DBConnectorService dbConnectorService;
	@Autowired
	MongoTemplate mongoTemplate;

	ThreadPoolTaskExecutor threadPoolTaskExecutor;

	public MailMigrationSchedulerV2() {
	}
	public MailMigrationSchedulerV2(ThreadPoolTaskExecutor threadPoolTaskExecutor) {
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
		//updateMoveQueue(emailMoveQueue.getId(), ExceptionConstants.STARTED, PROCESS.IN_PROGRESS);
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
			
		log.info("===Thread Controller count======="+threadControl.getMoveCount());
		long count = threadControl.getMoveCount();
		count = (threadPoolTaskExecutor.getMaxPoolSize()-threadPoolTaskExecutor.getActiveCount())-count;
		if(count<=0) {
			log.info("===COUNT FOR migrating new Threads are "+count+"==for the workspace id==="+emailMoveQueue);
			return;
		}
		List<EmailInfo> emails = new ArrayList<>();
		//extractMailsBasedOnPrior(threadControl, emailMoveQueue.getEmailWorkSpaceId(), count, emails);
		Query query = new Query(Criteria.where("_id").is("657b2954f2c8e5387a8024b2"));
		emails = mongoTemplate.find(query, EmailInfo.class);
		log.info("===COUNT FOR migrating new Threads are "+count+"==for the workspace id==="+emailMoveQueue);
		log.info("==NOT_PROCESSED emails found is  : =="+emails.size());
		for(EmailInfo emailInfo : emails) {
			if(!checkThreadPool()) {
				return;
			}
			emailInfo.setProcessStatus(EmailInfo.PROCESS.IN_PROGRESS);
			emailInfo.setThreadBy(Thread.currentThread().getName());
			emailInfo.setErrorDescription(null);
			dbConnectorService.getEmailInfoRepoImpl().save(emailInfo);
			//threadPoolTaskExecutor.submit(new MailMigrationTask(mailServiceFactory, dbConnectorService, emailWorkSpace, emailInfo));
			new MailMigrationTask(mailServiceFactory, dbConnectorService, emailWorkSpace, emailInfo).run();
		}
		pageNo = pageNo+20;
		emailWorkSpaeIds = getDistrinctMoveWorkspaceIds(mongoTemplate, threadControl,pageNo);
		for(EmailMoveQueue moveQueue : emailWorkSpaeIds) {
			processWorkSpaes(threadControl, moveQueue, pageNo, emailWorkSpaeIds);
		}
	}
	
	private void extractMailsBasedOnPrior(final ThreadControl threadControl, String emailWorkSpaceId, long count,
			List<EmailInfo> emails) {
		for(int prior=0;prior<7;prior++) {
			Order order = Sort.Order.asc(DBConstants.PRIORITY);
			long mails = dbConnectorService.getEmailInfoRepoImpl().getEmailInfosBasedOnProcessStatus(emailWorkSpaceId, prior,EmailInfo.PROCESS.NOT_PROCESSED,false);
			if(mails>0) {
				Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and(DBConstants.PROCESS_STATUS).is(PROCESS.NOT_PROCESSED).and(DBConstants.TO_CLOUD).is(Clouds.CLOUD_NAME.GMAIL.name()).and("preScan").is(false).and("threadBy").is(null).and("retryCount").lt(threadControl.getRetryCount()).and(DBConstants.PRIORITY).is(prior)).limit((int)count);
				List<EmailInfo> _emails = mongoTemplate.find(query.with(Sort.by(order)), EmailInfo.class);
				emails.addAll(_emails);
				break;
			}
		}
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
		EmailMoveQueue emailMoveQueue = new EmailMoveQueue();
		emailMoveQueue.setEmailWorkSpaceId("657b291af2c8e5387a802496");
		return Arrays.asList(emailMoveQueue);
		//return dbConnectorService.getEmailQueueRepoImpl().findMoveQueueByProcessStatusWithPazination(Arrays.asList(PROCESS.NOT_PROCESSED,PROCESS.IN_PROGRESS), 20, pageNo,CLOUD_NAME.GMAIL.name());
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
