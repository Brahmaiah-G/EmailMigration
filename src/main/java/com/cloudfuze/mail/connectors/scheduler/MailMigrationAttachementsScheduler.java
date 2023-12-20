package com.cloudfuze.mail.connectors.scheduler; 
/**
 * @author BrahmaiahG
*/

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
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
import com.cloudfuze.mail.repo.entities.Clouds;
import com.cloudfuze.mail.repo.entities.EmailInfo;
import com.cloudfuze.mail.repo.entities.EmailWorkSpace;
import com.cloudfuze.mail.repo.entities.ThreadControl;
import com.cloudfuze.mail.repo.entities.EmailInfo.PROCESS;
import com.cloudfuze.mail.service.DBConnectorService;
import com.cloudfuze.mail.utils.HttpUtils;
import com.mongodb.DBObject;

import lombok.extern.slf4j.Slf4j;


@Slf4j
//@Component
public class MailMigrationAttachementsScheduler {


	@Autowired
	DBConnectorService dbConnectorService;
	@Autowired
	MongoTemplate mongoTemplate;

	ThreadPoolTaskExecutor threadPoolTaskExecutor;

	public MailMigrationAttachementsScheduler() {
	}
	public MailMigrationAttachementsScheduler(ThreadPoolTaskExecutor threadPoolTaskExecutor) {
		this.threadPoolTaskExecutor = threadPoolTaskExecutor;
	}


	@Autowired
	MailServiceFactory mailServiceFactory;

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
		List<String> emailWorkSpaeIds = getDistrinctMoveWorkspaceIds(mongoTemplate, threadControl);
		if(ObjectUtils.isNotEmpty(emailWorkSpaeIds) && !emailWorkSpaeIds.isEmpty()) {
			emailWorkSpaeIds.stream().forEach(emailWrksSpace-> processWorkSpaes(threadControl,emailWrksSpace));
		}
	}
	private void processWorkSpaes(final ThreadControl threadControl,String emailWorkSpaceId) {
		EmailWorkSpace emailWorkSpace = dbConnectorService.getWorkSpaceRepoImpl().findOne(emailWorkSpaceId);{
			if(ObjectUtils.isEmpty(emailWorkSpace)) {
				log.warn("===WORKSPACE is EMPTY UPDATING THE EMAILINFOS=="+emailWorkSpaceId);
				dbConnectorService.getEmailInfoRepoImpl().findAndUpdateByWorkSpace(emailWorkSpaceId, threadControl.getRetryCount(), PROCESS.CONFLICT.name(), false, "WorkSpace not Found");
				return;
			}
		}
		long count = dbConnectorService.getEmailInfoRepoImpl().getInprogressCount(emailWorkSpaceId, true);
		count = threadControl.getMoveCount()-count;
		if(count<=0) {
			log.warn("===COUNT FOR migrating new Threads are "+count+"==for the workspace id==="+emailWorkSpaceId);
			return;
		}
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and(DBConstants.PROCESS_STATUS).is(PROCESS.NOT_PROCESSED).and("preScan").is(false).and("attachMents").is(true).and("threadBy").is(null).and("retryCount").lt(threadControl.getRetryCount())).limit((int)count);
		List<EmailInfo> emails = mongoTemplate.find(query, EmailInfo.class);
		log.warn("==Not_Started emails found is  : =="+emails.size());
		if(emails!=null && !emails.isEmpty()) {
			List<EmailInfo> _emails = new ArrayList<EmailInfo>();
			emails.forEach(email->{
				email.setProcessStatus(EmailInfo.PROCESS.IN_PROGRESS);
				email.setThreadBy(Thread.currentThread().getName());
				_emails.add(email);
			});
		}
		dbConnectorService.getEmailInfoRepoImpl().save(emails);
		for(EmailInfo emailInfo : emails) {
			if(!checkThreadPool()) {
				return;
			}
			Clouds fromCloud = dbConnectorService.getCloudsRepoImpl().findOne(emailInfo.getFromCloudId());
			Clouds toCloud = dbConnectorService.getCloudsRepoImpl().findOne(emailInfo.getToCloudId());
			if(ObjectUtils.isEmpty(fromCloud) || ObjectUtils.isEmpty(toCloud) || StringUtils.isEmpty(emailInfo.getSourceId())) {
				emailInfo.setErrorDescription("required values for migration are null please check From and toClouds");
				emailInfo.setProcessStatus(EmailInfo.PROCESS.CONFLICT);
				emailInfo.setRetryCount(emailInfo.getRetryCount()+1);
				dbConnectorService.getEmailInfoRepoImpl().save(emailInfo);
				continue;
			}
			threadPoolTaskExecutor.submit(new MailMigrationTask(mailServiceFactory, dbConnectorService, emailWorkSpace, emailInfo));	
		}
	}

	private boolean checkThreadPool() {
		if(threadPoolTaskExecutor.getActiveCount()+1==threadPoolTaskExecutor.getCorePoolSize()) {
			log.warn("===PLASE WAIT SOMETIME TO PROCESS OTHER THREADS");
			return false;
		}
		return true;
	}
	
	
	private List<String> getDistrinctMoveWorkspaceIds(MongoTemplate mongoOps, ThreadControl threadControl) {
		checkThreadPool();
		List<String> moveWorkSpaceIdList = new ArrayList<String>();
		Order order = new Order(Direction.ASC, DBConstants.PRIORITY);
		log.warn(" ----- I am Normal MailMigration job ----- ");

		Set<String> moveWorkSpaceIdSet = new LinkedHashSet<String>();

		//long startTime = System.currentTimeMillis();
		List<EmailInfo.PROCESS> processStatusList = new ArrayList<>();
		processStatusList.add(PROCESS.NOT_PROCESSED);
		Criteria matchCreteria = Criteria.where("processStatus").in(processStatusList).and("attachMents").is(true);
		// log.warn("skip:"+skip);
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
		log.warn(" After check of Workspaces -- moveWorkSpace object Ids : " + moveWorkSpaceIdList);
		return moveWorkSpaceIdList;
	}
	
	
	
}
