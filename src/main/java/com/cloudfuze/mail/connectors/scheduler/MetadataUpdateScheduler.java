package com.cloudfuze.mail.connectors.scheduler; 

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
import com.cloudfuze.mail.connectors.management.MetadataUpdateTask;
import com.cloudfuze.mail.constants.DBConstants;
import com.cloudfuze.mail.constants.ExceptionConstants;
import com.cloudfuze.mail.repo.entities.Clouds;
import com.cloudfuze.mail.repo.entities.Clouds.CLOUD_NAME;
import com.cloudfuze.mail.repo.entities.EmailInfo;
import com.cloudfuze.mail.repo.entities.EmailInfo.PROCESS;
import com.cloudfuze.mail.repo.entities.EmailMoveQueue;
import com.cloudfuze.mail.repo.entities.EmailWorkSpace;
import com.cloudfuze.mail.repo.entities.ThreadControl;
import com.cloudfuze.mail.service.DBConnectorService;
import com.cloudfuze.mail.utils.HttpUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Fetching the METADATA_STARTED EmailInfo's for updating the Metadata
 * <p></p>
 * Implementation class : {@link MetadataUpdateTask}
*/
@Slf4j
//@Component
public class MetadataUpdateScheduler {


	@Autowired
	DBConnectorService dbConnectorService;
	@Autowired
	MongoTemplate mongoTemplate;

	ThreadPoolTaskExecutor threadPoolTaskExecutor;

	public MetadataUpdateScheduler() {
	}
	public MetadataUpdateScheduler(ThreadPoolTaskExecutor threadPoolTaskExecutor) {
		this.threadPoolTaskExecutor = threadPoolTaskExecutor;
	}


	@Autowired
	MailServiceFactory mailServiceFactory;

	@Scheduled(cron ="0/10 * * * * ?")	
	public void processSchedule() throws Exception {
		log.info("--Enterd for the--"+getClass().getSimpleName());
		final ThreadControl threadControl = dbConnectorService.getEmailInfoRepoImpl().getThreadControl();
		if(HttpUtils.checkController(threadControl) || threadControl.isMetadata()) {
			log.warn("**********Thread Control MOVING IS DIABLED*******");
			return;
		}
		log.info("===ThreadPoolActivecount==="+threadPoolTaskExecutor.getActiveCount()+"==Thread Max=="+threadPoolTaskExecutor.getMaxPoolSize()+"==Thread core=="+threadPoolTaskExecutor.getCorePoolSize());
		if(!checkThreadPool()) {
			return;
		}
		List<EmailMoveQueue> emailWorkSpaeIds = getDistrinctMoveWorkspaceIds(mongoTemplate, threadControl,0);
		if(ObjectUtils.isNotEmpty(emailWorkSpaeIds) && !emailWorkSpaeIds.isEmpty()) {
			emailWorkSpaeIds.forEach(emailWrksSpace-> processWorkSpaes(threadControl,emailWrksSpace,0));
		}
	}
	private void processWorkSpaes(final ThreadControl threadControl,EmailMoveQueue emailMoveQueue,int pageNo) {
		EmailWorkSpace emailWorkSpace = dbConnectorService.getWorkSpaceRepoImpl().findOne(emailMoveQueue.getEmailWorkSpaceId());
		updateMoveQueue(emailMoveQueue.getId(), ExceptionConstants.STARTED,com.cloudfuze.mail.repo.entities.PROCESS.IN_PROGRESS);
		if(ObjectUtils.isEmpty(emailWorkSpace)) {
			log.info("===WORKSPACE is EMPTY UPDATING THE EMAILINFOS=="+emailMoveQueue.getEmailWorkSpaceId());
			dbConnectorService.getEmailInfoRepoImpl().findAndUpdateByWorkSpace(emailMoveQueue.getEmailWorkSpaceId(), threadControl.getRetryCount(), PROCESS.CONFLICT.name(), false, "WorkSpace not Found");
			return;
		}
		Clouds fromCloud = dbConnectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getFromCloudId());
		Clouds toCloud = dbConnectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getToCloudId());
		if(ObjectUtils.isEmpty(fromCloud) || ObjectUtils.isEmpty(toCloud)) {
			dbConnectorService.getEmailInfoRepoImpl().findAndUpdateByWorkSpace(emailMoveQueue.getEmailWorkSpaceId(), threadControl.getRetryCount(), PROCESS.CONFLICT.name(), false, "WorkSpace not Found");
			return;
		}
		long count = ((threadPoolTaskExecutor.getMaxPoolSize())-threadPoolTaskExecutor.getActiveCount());
		log.info("===COUNT FOR migrating new Threads are "+count+"==for the workspace id==="+emailMoveQueue.getEmailWorkSpaceId());
		if(count<=0) {
			return;
		}
		//Query query = new Query(Criteria.where("_id").is("654261833f4ef358438da42c"));
		List<EmailInfo> emails = new ArrayList<>();
		extractMailsBasedOnPrior(threadControl, emailMoveQueue.getEmailWorkSpaceId(), count, emails);
		//emails = mongoTemplate.find(query, EmailInfo.class);
		log.info("==METADATA_STARTED emails found is  : =="+emails.size());
		for(EmailInfo emailInfo : emails) {
			if(!checkThreadPool()) {
				return;
			}
			emailInfo.setErrorDescription(null);
			emailInfo.setThreadBy(Thread.currentThread().getName());
			emailInfo.setProcessStatus(EmailInfo.PROCESS.METADATA_INPROGRESS);
			dbConnectorService.getEmailInfoRepoImpl().save(emailInfo);
			threadPoolTaskExecutor.execute(new MetadataUpdateTask(mailServiceFactory, dbConnectorService, emailWorkSpace, emailInfo));	
		}
		pageNo = pageNo+20;
		List<EmailMoveQueue>_emailWorkSpaeIds = getDistrinctMoveWorkspaceIds(mongoTemplate, threadControl,pageNo);
		if(_emailWorkSpaeIds.isEmpty()) {
			return;
		}
		for(EmailMoveQueue moveQueue : _emailWorkSpaeIds) {
			processWorkSpaes(threadControl, moveQueue, pageNo);
		}
	}

	private boolean checkThreadPool() {
		if(threadPoolTaskExecutor.getActiveCount()+1>=threadPoolTaskExecutor.getMaxPoolSize()) {
			log.warn("===PLASE WAIT SOMETIME TO PROCESS OTHER THREADS");
			return false;
		}
		return true;
	}
	
	
	private List<EmailMoveQueue> getDistrinctMoveWorkspaceIds(MongoTemplate mongoOps, ThreadControl threadControl,int pageNo) {
		checkThreadPool();
		log.info(" ----- I am Normal MailMigration job ----- ");
		return dbConnectorService.getEmailQueueRepoImpl().findMoveQueueByProcessStatusWithPazination(Arrays.asList(com.cloudfuze.mail.repo.entities.PROCESS.IN_PROGRESS), 20, pageNo,CLOUD_NAME.OUTLOOK.name());
	}
	
	private void extractMailsBasedOnPrior(final ThreadControl threadControl, String emailWorkSpaceId, long count,
			List<EmailInfo> emails) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and(DBConstants.PROCESS_STATUS).is(PROCESS.METADATA_STARTED).and("preScan").is(false).and("retryCount").lt(threadControl.getRetryCount())).limit((int)count);
		List<EmailInfo> _emails = mongoTemplate.find(query, EmailInfo.class);
		emails.addAll(_emails);
	}
	
	private void updateMoveQueue(String moveQueueId,String errorDescription,com.cloudfuze.mail.repo.entities.PROCESS processStatus) {
		EmailMoveQueue queue = dbConnectorService.getEmailQueueRepoImpl().findMoveQueue(moveQueueId);
		if(queue!=null) {
			queue.setProcessStatus(processStatus);
			queue.setErrorDescription(errorDescription);
			dbConnectorService.getEmailQueueRepoImpl().saveQueue(queue);
		}
	}
	
}
