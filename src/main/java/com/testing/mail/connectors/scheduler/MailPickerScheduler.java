package com.testing.mail.connectors.scheduler;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.mongodb.DBObject;
import com.testing.mail.connectors.factory.MailServiceFactory;
import com.testing.mail.connectors.management.MailPickingV2;
import com.testing.mail.constants.DBConstants;
import com.testing.mail.constants.ExceptionConstants;
import com.testing.mail.repo.entities.Clouds;
import com.testing.mail.repo.entities.EmailFolderInfo;
import com.testing.mail.repo.entities.EmailInfo;
import com.testing.mail.repo.entities.EmailWorkSpace;
import com.testing.mail.repo.entities.ThreadControl;
import com.testing.mail.repo.entities.EmailInfo.PROCESS;
import com.testing.mail.service.DBConnectorService;
import com.testing.mail.utils.HttpUtils;

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
public class MailPickerScheduler {


	@Autowired
	DBConnectorService dbConnectorService;
	@Autowired
	MongoTemplate mongoTemplate;
	
	ThreadPoolTaskExecutor threadPoolTaskExecutor;
	
	public MailPickerScheduler(ThreadPoolTaskExecutor threadPoolTaskExecutor) {
		this.threadPoolTaskExecutor = threadPoolTaskExecutor;
	}
	
	@Autowired
	MailServiceFactory mailServiceFactory;

	@Scheduled(cron ="${email.picking.cron.expression}")	
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
		List<String> emailWorkSpaeIds = getDistrinctMoveWorkspaceIds(mongoTemplate, threadControl);
		if(ObjectUtils.isNotEmpty(emailWorkSpaeIds) && !emailWorkSpaeIds.isEmpty()) {
			emailWorkSpaeIds.stream().forEach(emailWrksSpace->
			{
					processWorkSpaces(threadControl,emailWrksSpace);
			});
		}
	}

	private void processWorkSpaces(ThreadControl threadControl,String emailWorkSpaceId) {
		EmailWorkSpace emailWorkSpace = dbConnectorService.getWorkSpaceRepoImpl().findOne(emailWorkSpaceId);
		if(ObjectUtils.isEmpty(emailWorkSpace)) {
			log.info("===WORKSPACE is EMPTY UPDATING THE EMAILINFOS=="+emailWorkSpaceId);
			dbConnectorService.getEmailInfoRepoImpl().findAndUpdateByWorkSpace(emailWorkSpaceId, threadControl.getRetryCount(), PROCESS.CONFLICT.name(),true,ExceptionConstants.WORKSPACE_NOT_FOUND);
			return;
		}
		
		Clouds fromCloud = dbConnectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getFromCloudId());
		Clouds toCloud = dbConnectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getToCloudId());
		if(ObjectUtils.isEmpty(fromCloud) || ObjectUtils.isEmpty(toCloud)) {
			dbConnectorService.getEmailInfoRepoImpl().findAndUpdateByWorkSpace(emailWorkSpaceId, threadControl.getRetryCount(), PROCESS.CONFLICT.name(),true,ExceptionConstants.REQUIRED_MISSING);
			return;
		}
		
		long count = threadControl.getPickCount();
		count = (threadPoolTaskExecutor.getMaxPoolSize()-threadPoolTaskExecutor.getActiveCount())-count;
		if(count<=0) {
			log.info("===COUNT FOR migrating new Threads are "+count+"==for the workspace id==="+emailWorkSpaceId);
			return;
		}
		List<EmailInfo> emails = new ArrayList<>();
		extractMailsBasedOnPrior(threadControl, emailWorkSpaceId, count, emails);
		log.warn("==Not_Started emails found is  : =="+emails.size());
		
		for(EmailInfo emailInfo : emails) {
			if(!checkThreadPool()) {
				return;
			}
			EmailFolderInfo target = new EmailFolderInfo();
			BeanUtils.copyProperties(emailInfo, target);
			emailInfo.setProcessStatus(EmailInfo.PROCESS.IN_PROGRESS);
			emailInfo.setThreadBy(Thread.currentThread().getName());
			dbConnectorService.getEmailInfoRepoImpl().save(emailInfo);
			threadPoolTaskExecutor.submit(new MailPickingV2(mailServiceFactory, dbConnectorService, emailWorkSpace, target));	
			//new MailPickerV2(mailServiceFactory, dbConnectorService, emailWorkSpace, emailInfo).call();
		}
	}

	private boolean checkThreadPool() {
		if(threadPoolTaskExecutor.getActiveCount()+5==threadPoolTaskExecutor.getCorePoolSize()) {
			log.info("===PLASE WAIT SOMETIME TO PROCESS OTHER THREADS");
			return false;
		}
		return true;
	}
	
	
	private void extractMailsBasedOnPrior(final ThreadControl threadControl, String emailWorkSpaceId, long count,
			List<EmailInfo> emails) {
		for(int prior=0;prior<7;prior++) {
			Order order = Sort.Order.asc(DBConstants.PRIORITY);
			long mails = dbConnectorService.getEmailInfoRepoImpl().getEmailInfosBasedOnProcessStatus(emailWorkSpaceId, prior,PROCESS.NOT_STARTED,true);
			if(mails>0) {
				Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and(DBConstants.PROCESS_STATUS).is(PROCESS.NOT_STARTED.name()).and("folder").is(true).and("threadBy").is(null).and("retryCount").lt(threadControl.getRetryCount()).and(DBConstants.PRIORITY).is(prior)).limit((int)count);
				List<EmailInfo> _emails = mongoTemplate.find(query.with(Sort.by(order)), EmailInfo.class);
				emails.addAll(_emails);
				if(emails.size()>=count) {
				break;
			}
		}
	}
	}
	private List<String> getDistrinctMoveWorkspaceIds(MongoTemplate mongoOps, ThreadControl threadControl) {
		checkThreadPool();
		List<String> moveWorkSpaceIdList = new ArrayList<>();
		Order order = new Order(Direction.ASC, "_id");
		log.info(" ----- I am Normal MailMigration job ----- ");

		Set<String> moveWorkSpaceIdSet = new LinkedHashSet<>();

		List<EmailInfo.PROCESS> processStatusList = new ArrayList<>();
		processStatusList.add(PROCESS.NOT_STARTED);
		Criteria matchCreteria = Criteria.where("processStatus").in(processStatusList).and("folder").is(true);
		Aggregation aggregation = Aggregation.newAggregation(Aggregation.match(matchCreteria), Aggregation.sort(Sort.by(order)),
				Aggregation.group(DBConstants.EMAILWORKSPACEID), //
				Aggregation.limit(threadControl.getMoveCount()));

		AggregationResults<DBObject> result = mongoOps.aggregate(aggregation, EmailInfo.class,
				DBObject.class);

		if (result.getMappedResults() != null) {
			for (DBObject obj : result.getMappedResults()) {
				if (obj != null && obj.get("_id") != null) {
					moveWorkSpaceIdSet.add((String) obj.get("_id"));
				}
			}
		}
		moveWorkSpaceIdList.addAll(moveWorkSpaceIdSet);
		log.warn(" After check of Workspaces -- moveWorkSpace object Ids : " + moveWorkSpaceIdList);
		return moveWorkSpaceIdList;
	}
	
	
//write a method to check the workspace if from clouds null update all the emailInfos in a single time will reduce time for next time
}
