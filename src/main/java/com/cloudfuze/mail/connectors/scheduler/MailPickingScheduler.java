package com.cloudfuze.mail.connectors.scheduler;

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

import com.cloudfuze.mail.connectors.factory.MailServiceFactory;
import com.cloudfuze.mail.connectors.management.MailPickingV2;
import com.cloudfuze.mail.connectors.management.MailPickingV3;
import com.cloudfuze.mail.constants.DBConstants;
import com.cloudfuze.mail.constants.ExceptionConstants;
import com.cloudfuze.mail.repo.entities.Clouds;
import com.cloudfuze.mail.repo.entities.EmailFolderInfo;
import com.cloudfuze.mail.repo.entities.EmailInfo;
import com.cloudfuze.mail.repo.entities.EmailMoveQueue;
import com.cloudfuze.mail.repo.entities.EmailPickingQueue;
import com.cloudfuze.mail.repo.entities.EmailWorkSpace;
import com.cloudfuze.mail.repo.entities.PROCESS;
import com.cloudfuze.mail.repo.entities.ThreadControl;
import com.cloudfuze.mail.service.DBConnectorService;
import com.cloudfuze.mail.utils.HttpUtils;

import lombok.extern.slf4j.Slf4j;

/**
*<p>
*Scheduler for Picking the <b>NOT_STARTED</b> EmailInfo's<p></p>
*CronExpression : check application.properties
*<p>
*Implemented class MailPickerV2
*</p>
*@see  MailPickingV2
*/


@Slf4j
//@Component
public class MailPickingScheduler {


	@Autowired
	DBConnectorService dbConnectorService;
	@Autowired
	MongoTemplate mongoTemplate;
	
	ThreadPoolTaskExecutor threadPoolTaskExecutor;
	
	public MailPickingScheduler(ThreadPoolTaskExecutor threadPoolTaskExecutor) {
		this.threadPoolTaskExecutor = threadPoolTaskExecutor;
	}
	
	@Autowired
	MailServiceFactory mailServiceFactory;

	@Scheduled(cron ="${email.picking.cron.expression.v1}")	
	public void processSchedule() {

		final ThreadControl threadControl = dbConnectorService.getEmailInfoRepoImpl().getThreadControl();
		if(HttpUtils.checkController(threadControl) || threadControl.isStopPicking()) {
			log.info("**********Thread Control DISABLED PICKING *******");
			return;
		}
		log.info("===ThreadPoolActivecount==="+threadPoolTaskExecutor.getActiveCount()+"==Thread Max=="+threadPoolTaskExecutor.getMaxPoolSize()+"==Thread core=="+threadPoolTaskExecutor.getCorePoolSize());
		if(!checkThreadPool()) {
			return;
		}
		List<EmailPickingQueue> emailWorkSpaeIds = getDistrinctMoveWorkspaceIds(mongoTemplate, threadControl,0);
		if(ObjectUtils.isNotEmpty(emailWorkSpaeIds) && !emailWorkSpaeIds.isEmpty()) {
			emailWorkSpaeIds.forEach(emailWrksSpace->
			{
					processWorkSpaces(threadControl,emailWrksSpace,emailWorkSpaeIds,0);
			});
		}
	}

	private void processWorkSpaces(ThreadControl threadControl,EmailPickingQueue emailPickingQueue,List<EmailPickingQueue> emailWorkSpaeIds,int pageNo) {
		EmailWorkSpace emailWorkSpace = dbConnectorService.getWorkSpaceRepoImpl().findOne("657b151932410f723f76c37c");
		if(ObjectUtils.isEmpty(emailWorkSpace)) {
			log.info("===WORKSPACE is EMPTY UPDATING THE EMAILINFOS=="+emailPickingQueue.getEmailWorkSpaceId());
			dbConnectorService.getEmailInfoRepoImpl().findAndUpdateByWorkSpace(emailPickingQueue.getEmailWorkSpaceId(), threadControl.getRetryCount(), PROCESS.CONFLICT.name(),true,ExceptionConstants.WORKSPACE_NOT_FOUND);
			return;
		}
		
		Clouds fromCloud = dbConnectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getFromCloudId());
		Clouds toCloud = dbConnectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getToCloudId());
		if(ObjectUtils.isEmpty(fromCloud) || ObjectUtils.isEmpty(toCloud)) {
			dbConnectorService.getEmailInfoRepoImpl().findAndUpdateByWorkSpace(emailPickingQueue.getEmailWorkSpaceId(), threadControl.getRetryCount(), PROCESS.CONFLICT.name(),true,ExceptionConstants.REQUIRED_MISSING);
			emailPickingQueue.setProcessStatus(PROCESS.CONFLICT);
			emailPickingQueue.setErrorDescription(ExceptionConstants.CLOUD_NOT_FOUND);
			dbConnectorService.getEmailQueueRepoImpl().save(emailPickingQueue);	
			return;
		}
		
//		long inProgressQueue = dbConnectorService.getEmailQueueRepoImpl().findPickingByProcessStatusWithPazination(Arrays.asList(PROCESS.IN_PROGRESS), emailWorkSpace.getUserId());
//		if(inProgressQueue>1) {
//			return;
//		}
		emailPickingQueue.setProcessStatus(PROCESS.IN_PROGRESS);
		emailPickingQueue.setModifiedTime(LocalDateTime.now());
		dbConnectorService.getEmailQueueRepoImpl().save(emailPickingQueue);
		long count = threadControl.getPickCount();
		count = (threadPoolTaskExecutor.getMaxPoolSize()-threadPoolTaskExecutor.getActiveCount())-count;
		if(count<=0) {
			log.info("===COUNT FOR migrating new Threads are "+count+"==for the workspace id==="+emailPickingQueue);
			return;
		}
		//List<EmailFolderInfo> emails = extractMailsBasedOnPrior(threadControl, emailPickingQueue.getEmailWorkSpaceId(), count);
		Query query = new Query(Criteria.where("_id").is("657b152832410f723f76c37f"));
		List<EmailFolderInfo> emails = mongoTemplate.find(query, EmailFolderInfo.class);
		log.warn("==Not_Started emails found is  : =="+emails.size());
		
		for(EmailFolderInfo emailInfo : emails) {
			if(!checkThreadPool()) {
				return;
			}
			emailInfo.setProcessStatus(PROCESS.IN_PROGRESS);
			emailInfo.setThreadBy(Thread.currentThread().getName());
			dbConnectorService.getEmailFolderInfoRepoImpl().save(emailInfo);
			//threadPoolTaskExecutor.submit(new MailPickingV3(mailServiceFactory, dbConnectorService, emailWorkSpace, emailInfo));	
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
			}
			new MailPickingV3(mailServiceFactory, dbConnectorService, emailWorkSpace, emailInfo).call();
		}
		pageNo = pageNo+20;
		emailWorkSpaeIds = getDistrinctMoveWorkspaceIds(mongoTemplate, threadControl,pageNo);
		if(ObjectUtils.isNotEmpty(emailWorkSpaeIds) && !emailWorkSpaeIds.isEmpty()) {
			for(EmailPickingQueue pickingQueue : emailWorkSpaeIds) {
				processWorkSpaces(threadControl, pickingQueue, emailWorkSpaeIds,pageNo);
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
	
	private List<EmailFolderInfo> extractMailsBasedOnPrior(final ThreadControl threadControl, String emailWorkSpaceId, long count) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and(DBConstants.PROCESS_STATUS).is(PROCESS.NOT_STARTED.name()).and("threadBy").is(null).and("retryCount").lt(threadControl.getRetryCount())).limit((int)count);
		return mongoTemplate.find(query, EmailFolderInfo.class);
	}
	private List<EmailPickingQueue> getDistrinctMoveWorkspaceIds(MongoTemplate mongoOps, ThreadControl threadControl,int pageNo) {
		checkForPickingCompletion(threadControl);
		return dbConnectorService.getEmailQueueRepoImpl().findPickingByProcessStatusWithPazination(Arrays.asList(PROCESS.PROCESSED,PROCESS.IN_PROGRESS), 20, pageNo);
	}
	
	private void checkForPickingCompletion(ThreadControl threadControl) {
		List<EmailPickingQueue> inQueues = dbConnectorService.getEmailQueueRepoImpl().findPickingByProcessStatusWithPazination(Arrays.asList(PROCESS.IN_PROGRESS,PROCESS.CONFLICT), 100, 0);
		if(!inQueues.isEmpty()) {
			inQueues.forEach(pickingQueue->{
				List<EmailFolderInfo> infos = dbConnectorService.getEmailFolderInfoRepoImpl().findByProcessStatus(pickingQueue.getEmailWorkSpaceId(), Arrays.asList(PROCESS.IN_PROGRESS,PROCESS.NOT_STARTED,PROCESS.PICKING,PROCESS.NOT_PROCESSED,PROCESS.PARENT_NOT_PROCESSED));
				if(infos.isEmpty()) {
					pickingQueue.setProcessStatus(PROCESS.PROCESSED);
					dbConnectorService.getEmailQueueRepoImpl().save(pickingQueue);
					//createMoveQueue(pickingQueue);
				}else {
					List<EmailFolderInfo> _infos = dbConnectorService.getEmailFolderInfoRepoImpl().findByProcessStatus(pickingQueue.getEmailWorkSpaceId(), Arrays.asList(PROCESS.CONFLICT));
					if(_infos.size()==infos.size()) {
						for(EmailFolderInfo emailFolderInfo:_infos) {
							if(emailFolderInfo.getRetryCount()<=threadControl.getRetryCount()) {
								break;
							}
						}
						pickingQueue.setProcessStatus(PROCESS.CONFLICT);
						dbConnectorService.getEmailQueueRepoImpl().save(pickingQueue);
						//createMoveQueue(pickingQueue);
					}
				}
			});
		}
	}
}
