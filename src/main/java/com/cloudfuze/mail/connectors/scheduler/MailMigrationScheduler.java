package com.cloudfuze.mail.connectors.scheduler; 

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.ObjectUtils;
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

import com.cloudfuze.mail.connectors.factory.MailServiceFactory;
import com.cloudfuze.mail.connectors.management.MailMigrationTask;
import com.cloudfuze.mail.constants.DBConstants;
import com.cloudfuze.mail.constants.ExceptionConstants;
import com.cloudfuze.mail.repo.entities.Clouds;
import com.cloudfuze.mail.repo.entities.EmailInfo;
import com.cloudfuze.mail.repo.entities.EmailInfo.PROCESS;
import com.cloudfuze.mail.repo.entities.EmailWorkSpace;
import com.cloudfuze.mail.repo.entities.ThreadControl;
import com.cloudfuze.mail.service.DBConnectorService;
import com.cloudfuze.mail.utils.HttpUtils;
import com.mongodb.DBObject;

import lombok.extern.slf4j.Slf4j;


@Slf4j
//@Component
public class MailMigrationScheduler {


	@Autowired
	DBConnectorService dbConnectorService;
	@Autowired
	MongoTemplate mongoTemplate;

	ThreadPoolTaskExecutor threadPoolTaskExecutor;

	public MailMigrationScheduler() {
	}
	public MailMigrationScheduler(ThreadPoolTaskExecutor threadPoolTaskExecutor) {
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
		List<String> emailWorkSpaeIds = getDistrinctMoveWorkspaceIds(mongoTemplate, threadControl);
		if(ObjectUtils.isNotEmpty(emailWorkSpaeIds) && !emailWorkSpaeIds.isEmpty()) {
			emailWorkSpaeIds.stream().forEach(emailWrksSpace-> processWorkSpaes(threadControl,emailWrksSpace));
		}
	}
	private void processWorkSpaes(final ThreadControl threadControl,String emailWorkSpaceId) {
		EmailWorkSpace emailWorkSpace = dbConnectorService.getWorkSpaceRepoImpl().findOne(emailWorkSpaceId);
			if(ObjectUtils.isEmpty(emailWorkSpace)) {
				log.info("===WORKSPACE is EMPTY UPDATING THE EMAILINFOS=="+emailWorkSpaceId);
				dbConnectorService.getEmailInfoRepoImpl().findAndUpdateByWorkSpace(emailWorkSpaceId, threadControl.getRetryCount(), PROCESS.CONFLICT.name(), false, ExceptionConstants.WORKSPACE_NOT_FOUND);
				return;
			}else {
				Clouds fromCloud = dbConnectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getFromCloudId());
				Clouds toCloud = dbConnectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getToCloudId());
				if(ObjectUtils.isEmpty(fromCloud) || ObjectUtils.isEmpty(toCloud)) {
					dbConnectorService.getEmailInfoRepoImpl().findAndUpdateByWorkSpace(emailWorkSpaceId, threadControl.getRetryCount(), PROCESS.CONFLICT.name(), false, ExceptionConstants.WORKSPACE_NOT_FOUND);
					return;
				}
			}
			
		log.info("===Thread Controller count======="+threadControl.getMoveCount());
		long count = threadControl.getMoveCount();
		count = (threadPoolTaskExecutor.getMaxPoolSize()-threadPoolTaskExecutor.getActiveCount())-count;
		if(count<=0) {
			log.info("===COUNT FOR migrating new Threads are "+count+"==for the workspace id==="+emailWorkSpaceId);
			return;
		}
		List<EmailInfo> emails = new ArrayList<>();
		extractMailsBasedOnPrior(threadControl, emailWorkSpaceId, count, emails);
		
		//Query query = new Query(Criteria.where("_id").is("63f488c64a799566ff597a06"));
		log.info("===COUNT FOR migrating new Threads are "+count+"==for the workspace id==="+emailWorkSpaceId);
		log.info("==NOT_PROCESSED emails found is  : =="+emails.size());
		for(EmailInfo emailInfo : emails) {
			if(!checkThreadPool()) {
				return;
			}
			emailInfo.setProcessStatus(EmailInfo.PROCESS.IN_PROGRESS);
			emailInfo.setThreadBy(Thread.currentThread().getName());
			emailInfo.setErrorDescription(null);
			dbConnectorService.getEmailInfoRepoImpl().save(emailInfo);
			threadPoolTaskExecutor.submit(new MailMigrationTask(mailServiceFactory, dbConnectorService, emailWorkSpace, emailInfo));	
		}
	}
	
	private void extractMailsBasedOnPrior(final ThreadControl threadControl, String emailWorkSpaceId, long count,
			List<EmailInfo> emails) {
		for(int prior=0;prior<7;prior++) {
			Order order = Sort.Order.asc(DBConstants.PRIORITY);
			long mails = dbConnectorService.getEmailInfoRepoImpl().getEmailInfosBasedOnProcessStatus(emailWorkSpaceId, prior,PROCESS.NOT_PROCESSED,false);
			if(mails>0) {
				Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and(DBConstants.PROCESS_STATUS).is(PROCESS.NOT_PROCESSED).and("preScan").is(false).and("threadBy").is(null).and("retryCount").lt(threadControl.getRetryCount()).and(DBConstants.PRIORITY).is(prior)).limit((int)count);
				List<EmailInfo> _emails = mongoTemplate.find(query.with(Sort.by(order)), EmailInfo.class);
				emails.addAll(_emails);
				break;
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
	
	
	private List<String> getDistrinctMoveWorkspaceIds(MongoTemplate mongoOps, ThreadControl threadControl) {
		checkThreadPool();
		List<String> moveWorkSpaceIdList = new ArrayList<>();
		Order order = new Order(Direction.ASC, "_id");
		log.info(" ----- I am Normal MailMigration job ----- ");

		Set<String> moveWorkSpaceIdSet = new LinkedHashSet<>();

		List<EmailInfo.PROCESS> processStatusList = new ArrayList<>();
		processStatusList.add(PROCESS.NOT_PROCESSED);
		Criteria matchCreteria = Criteria.where("processStatus").in(processStatusList);
		Aggregation aggregation = Aggregation.newAggregation(Aggregation.match(matchCreteria), Aggregation.sort(Sort.by(order)),
				Aggregation.group(DBConstants.EMAILWORKSPACEID), //
				Aggregation.limit(threadControl.getMoveCount()));

		AggregationResults<DBObject> result = mongoOps.aggregate(aggregation, EmailInfo.class,
				DBObject.class);

		if (result != null && result.getMappedResults() != null) {
			for (DBObject obj : result.getMappedResults()) {
				if (obj != null && obj.get("_id") != null) {
					moveWorkSpaceIdSet.add((String) obj.get("_id"));
				}
			}
		}
		moveWorkSpaceIdList.addAll(moveWorkSpaceIdSet);
		log.info(" After check of Workspaces -- moveWorkSpace object Ids : " + moveWorkSpaceIdList);
		return moveWorkSpaceIdList;
	}
}
