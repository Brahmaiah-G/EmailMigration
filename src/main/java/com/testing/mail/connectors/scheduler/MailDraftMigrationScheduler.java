package com.testing.mail.connectors.scheduler; 

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.testing.mail.connectors.factory.MailServiceFactory;
import com.testing.mail.connectors.management.MailDraftsMigrationTask;
import com.testing.mail.constants.DBConstants;
import com.testing.mail.constants.ExceptionConstants;
import com.testing.mail.repo.entities.Clouds;
import com.testing.mail.repo.entities.EmailInfo;
import com.testing.mail.repo.entities.EmailMoveQueue;
import com.testing.mail.repo.entities.EmailWorkSpace;
import com.testing.mail.repo.entities.PROCESS;
import com.testing.mail.repo.entities.ThreadControl;
import com.testing.mail.repo.entities.Clouds.CLOUD_NAME;
import com.testing.mail.service.DBConnectorService;
import com.testing.mail.utils.HttpUtils;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class MailDraftMigrationScheduler {


	@Autowired
	DBConnectorService dbConnectorService;
	@Autowired
	MongoTemplate mongoTemplate;

	ThreadPoolTaskExecutor threadPoolTaskExecutor;

	public MailDraftMigrationScheduler() {
	}
	public MailDraftMigrationScheduler(ThreadPoolTaskExecutor threadPoolTaskExecutor) {
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
			//return;
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
		EmailWorkSpace emailWorkSpace = dbConnectorService.getWorkSpaceRepoImpl().findOne("6569fa74eb42674f7f88898d");
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
		long count = ((threadPoolTaskExecutor.getMaxPoolSize()+threadPoolTaskExecutor.getCorePoolSize())-threadPoolTaskExecutor.getActiveCount());
		log.info("===COUNT FOR migrating new Threads are "+count+"==for the workspace id==="+emailMoveQueue.getEmailWorkSpaceId());
		//Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailMoveQueue.getEmailWorkSpaceId()).and(DBConstants.TO_CLOUD).is(Clouds.CLOUD_NAME.OUTLOOK.name()).and(DBConstants.PROCESS_STATUS).is(PROCESS.DRAFT_NOTPROCESSED).and("preScan").is(false).and("threadBy").is(null).and("retryCount").lt(threadControl.getRetryCount())).limit((int)count);
		Query query = new Query(Criteria.where("_id").is("656ad821164e8a15d44a37bb"));
		List<EmailInfo> emails = mongoTemplate.find(query, EmailInfo.class);
		boolean processFolders = true;
		int pageSize = 15;
		int sizeOfFileFolderMetaData = 0;
		int sizeOfSubList = pageSize;
		while(processFolders) {
			String batchId = UUID.randomUUID().toString();
			List<EmailInfo> infos = null;
			List<EmailInfo> emailInfos = new ArrayList<>();
			if(emails.size()>sizeOfSubList) {
				infos = emails.subList(sizeOfFileFolderMetaData, sizeOfSubList);
			}else {
				processFolders = false;
				infos = emails.subList(sizeOfFileFolderMetaData, emails.size());
			}
			log.info("==NOT_PROCESSED emails found is  : =="+infos.size());
			if(!infos.isEmpty()) {
				infos.forEach(emailInfo->{
					if(checkThreadPool()) {
						emailInfo.setProcessStatus(EmailInfo.PROCESS.DRAFT_MIGRATION_IN_PROGRESS);
						emailInfo.setThreadBy(Thread.currentThread().getName());
						emailInfo.setErrorDescription(null);
						emailInfo.setMigBatchId(batchId);
						dbConnectorService.getEmailInfoRepoImpl().save(emailInfo);
						emailInfos.add(emailInfo);
					}
				});
				threadPoolTaskExecutor.submit(new MailDraftsMigrationTask(mailServiceFactory, dbConnectorService, emailWorkSpace, emailInfos));
				sizeOfFileFolderMetaData = sizeOfSubList;
				sizeOfSubList = sizeOfSubList+pageSize;
			}
		}
		pageNo = pageNo+20;
		List<EmailMoveQueue>_emailWorkSpaeIds = getDistrinctMoveWorkspaceIds(mongoTemplate, threadControl,pageNo);
		if(_emailWorkSpaeIds.isEmpty()) {
			return;
		}
		for(EmailMoveQueue moveQueue : _emailWorkSpaeIds) {
			processWorkSpaes(threadControl, moveQueue, pageNo, emailWorkSpaeIds);
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
		return dbConnectorService.getEmailQueueRepoImpl().findMoveQueueByProcessStatusWithPazination(Arrays.asList(PROCESS.NOT_PROCESSED,PROCESS.IN_PROGRESS), 20, pageNo,CLOUD_NAME.OUTLOOK.name());
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
