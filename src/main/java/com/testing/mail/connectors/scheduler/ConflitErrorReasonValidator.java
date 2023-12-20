package com.testing.mail.connectors.scheduler;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.testing.mail.repo.entities.ThreadControl;
import com.testing.mail.repo.entities.EmailInfo.PROCESS;
import com.testing.mail.service.DBConnectorService;
import com.testing.mail.utils.HttpUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
//@Component
public class ConflitErrorReasonValidator {

	
	@Autowired
	DBConnectorService dbConnectorService;
	@Autowired
	MongoTemplate mongoTemplate;

	public ConflitErrorReasonValidator() {
	}
	
	@Scheduled(cron ="0/10 * * * * ?")		
	public void processSchedule() {

		final ThreadControl threadControl = dbConnectorService.getEmailInfoRepoImpl().getThreadControl();
		if(HttpUtils.checkController(threadControl)) {
			log.error("**********Thread Control PAUSE IS DISABLED*******");
			return;
		}
		
		long count = dbConnectorService.getEmailInfoRepoImpl().countByProcessStatusAndError(Arrays.asList(PROCESS.CONFLICT.name(),PROCESS.DRAFT_CREATION_CONFLICT.name(),PROCESS.METADATA_CONFLICT.name()),"limit");
		log.info("==Total Count for Updating the CONFLICT STATUS for OUTLOOK---"+count);
		long limit = 10000;
		if(count>0) {
			dbConnectorService.getEmailInfoRepoImpl().updateEmailInfosByProcessStatus(PROCESS.CONFLICT.name(), PROCESS.DRAFT_NOTPROCESSED.name(), (int)limit,"limit");
			dbConnectorService.getEmailInfoRepoImpl().updateEmailInfosByProcessStatus(PROCESS.DRAFT_CREATION_CONFLICT.name(), PROCESS.NOT_PROCESSED.name(), (int)limit,"limit");
			dbConnectorService.getEmailInfoRepoImpl().updateEmailInfosByProcessStatus(PROCESS.METADATA_CONFLICT.name(), PROCESS.METADATA_STARTED.name(), (int)limit,"limit");
		}
	}
}
