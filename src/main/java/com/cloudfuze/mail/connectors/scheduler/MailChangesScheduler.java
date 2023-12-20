package com.cloudfuze.mail.connectors.scheduler;

import java.util.ArrayList;
import java.util.Date;
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
import com.cloudfuze.mail.connectors.management.CalendarChangesPickerTask;
import com.cloudfuze.mail.connectors.management.MailChangesPickerTask;
import com.cloudfuze.mail.constants.DBConstants;
import com.cloudfuze.mail.constants.ExceptionConstants;
import com.cloudfuze.mail.repo.entities.Clouds;
import com.cloudfuze.mail.repo.entities.EmailWorkSpace;
import com.cloudfuze.mail.repo.entities.MailChangeIds;
import com.cloudfuze.mail.repo.entities.MailChangeIds.PROCESS;
import com.cloudfuze.mail.repo.entities.ThreadControl;
import com.cloudfuze.mail.service.DBConnectorService;
import com.cloudfuze.mail.utils.HttpUtils;
import com.mongodb.DBObject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MailChangesScheduler {


	@Autowired
	DBConnectorService dbConnectorService;
	@Autowired
	MongoTemplate mongoTemplate;

	ThreadPoolTaskExecutor threadPoolTaskExecutor;

	public MailChangesScheduler() {
	}

	public MailChangesScheduler(ThreadPoolTaskExecutor threadPoolTaskExecutor) {
		this.threadPoolTaskExecutor = threadPoolTaskExecutor;
	}

	@Autowired
	MailServiceFactory mailServiceFactory;

	@Scheduled(cron ="${email.updates.cron.expression}")	
	public void processSchedule() throws Exception {
		log.info("--Enterd for the--"+getClass().getSimpleName());
		final ThreadControl threadControl = dbConnectorService.getEmailInfoRepoImpl().getThreadControl();
		if(HttpUtils.checkController(threadControl) || threadControl.isStopChanges()) {
			log.warn("**********Thread Control Changes IS DISABLED*******");
			//return;
		}
		log.info("===ThreadPoolActivecount==="+threadPoolTaskExecutor.getActiveCount()+"==Thread Max=="+threadPoolTaskExecutor.getMaxPoolSize()+"==Thread core=="+threadPoolTaskExecutor.getCorePoolSize());
		if(!checkThreadPool()) {
			return;
		}
		List<String> emailWorkSpaeIds = getDistrinctMoveWorkspaceIds(mongoTemplate, threadControl);
		if(ObjectUtils.isNotEmpty(emailWorkSpaeIds) && !emailWorkSpaeIds.isEmpty()) {
			emailWorkSpaeIds.stream().forEach(emailWrksSpace-> processWorkSpaces(threadControl,emailWrksSpace));
		}
	}

	private void processWorkSpaces(ThreadControl threadControl,String emailWorkSpaceId) {
		EmailWorkSpace emailWorkSpace = dbConnectorService.getWorkSpaceRepoImpl().findOne(emailWorkSpaceId);
		if(ObjectUtils.isEmpty(emailWorkSpace)) {
			log.info("===WORKSPACE is EMPTY UPDATING THE EMAILINFOS=="+emailWorkSpaceId);
			dbConnectorService.getEmailInfoRepoImpl().findAndUpdateByWorkSpaceForMailChangeIds(emailWorkSpaceId, threadControl.getRetryCount(), PROCESS.CONFLICT.name(),true,ExceptionConstants.WORKSPACE_NOT_FOUND);
			return;
		}
		Clouds fromCloud = dbConnectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getFromCloudId());
		Clouds toCloud = dbConnectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getToCloudId());
		if(ObjectUtils.isEmpty(fromCloud) || ObjectUtils.isEmpty(toCloud)) {
			dbConnectorService.getEmailInfoRepoImpl().findAndUpdateByWorkSpaceForMailChangeIds(emailWorkSpaceId, threadControl.getRetryCount(), PROCESS.CONFLICT.name(),true,ExceptionConstants.WORKSPACE_NOT_FOUND);
			return;
		}
		long count = threadControl.getChangesCount();
		count = (threadPoolTaskExecutor.getMaxPoolSize()-threadPoolTaskExecutor.getActiveCount())-count;
		log.info("====Total  MailchangeIds for the delta are==="+count+"=== for WorkSpaceID=="+emailWorkSpaceId);
		Query query = new Query(Criteria.where(DBConstants.PROCESS_STATUS).is(PROCESS.NOT_PROCESSED.name()).and("nextPickTime").lte(new Date()).and(DBConstants.THREADBY).is(null).and(DBConstants.RETRY).lt(threadControl.getRetryCount())).limit((int)count);
		List<MailChangeIds> emails = mongoTemplate.find(query, MailChangeIds.class);
		log.info("==Not_Started MailchangeIds found is  : =="+emails.size());
		for(MailChangeIds emailInfo : emails) {
			if(!checkThreadPool()) {
				return;
			}
			
			emailInfo.setThreadBy(Thread.currentThread().getName());
			emailInfo.setProcessStatus(PROCESS.IN_PROGRESS);
			dbConnectorService.getMailChangeIdRepoImpl().save(emailInfo);
			if(emailInfo.isCalendar()) {
				threadPoolTaskExecutor.submit(new CalendarChangesPickerTask(mailServiceFactory, dbConnectorService, emailInfo, emailWorkSpace));	
			}else {
				threadPoolTaskExecutor.submit(new MailChangesPickerTask(mailServiceFactory, dbConnectorService, emailInfo, emailWorkSpace));	
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
	
	private List<String> getDistrinctMoveWorkspaceIds(MongoTemplate mongoOps, ThreadControl threadControl) {
		checkThreadPool();
		List<String> moveWorkSpaceIdList = new ArrayList<>();
		Order order = new Order(Direction.ASC, "_id");
		log.info(" ----- I am Normal MailMigration job ----- ");

		Set<String> moveWorkSpaceIdSet = new LinkedHashSet<>();

		List<MailChangeIds.PROCESS> processStatusList = new ArrayList<>();
		processStatusList.add(PROCESS.NOT_PROCESSED);
		Criteria matchCreteria = Criteria.where("processStatus").in(processStatusList);
		Aggregation aggregation = Aggregation.newAggregation(Aggregation.match(matchCreteria), Aggregation.sort(Sort.by(order)),
				Aggregation.group(DBConstants.EMAILWORKSPACEID), //
				Aggregation.limit(threadControl.getChangesCount()));

		AggregationResults<DBObject> result = mongoOps.aggregate(aggregation, MailChangeIds.class,
				DBObject.class);

		for (DBObject obj : result.getMappedResults()) {
			if (obj != null && obj.get("_id") != null) {
				moveWorkSpaceIdSet.add((String) obj.get("_id"));
			}
		}
		moveWorkSpaceIdList.addAll(moveWorkSpaceIdSet);
		log.info(" After check of Workspaces -- moveWorkSpace object Ids : " + moveWorkSpaceIdList);
		return moveWorkSpaceIdList;
	}
	
}



