package com.cloudfuze.mail.connectors.management;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import com.cloudfuze.mail.repo.entities.EmailInfo.PROCESS;
import com.cloudfuze.mail.repo.entities.ThreadControl;
import com.cloudfuze.mail.service.DBConnectorService;
import com.cloudfuze.mail.utils.HttpUtils;

import lombok.extern.slf4j.Slf4j;




@Slf4j
public class LimitController {

	@Autowired
	DBConnectorService dbConnectorService;
	@Autowired
	MongoTemplate mongoTemplate;

	public LimitController() {
	}
	
	@Scheduled(cron ="${email.limit.cron.expression}")	
	public void processSchedule() {

		final ThreadControl threadControl = dbConnectorService.getEmailInfoRepoImpl().getThreadControl();
		if(HttpUtils.checkController(threadControl) || threadControl.isStopPause()) {
			log.error("**********Thread Control PAUSE IS DISABLED*******");
			return;
		}
		
		long count = dbConnectorService.getEmailInfoRepoImpl().countByProcessStatus(PROCESS.NOT_PROCESSED.name());
		long pausingCount = threadControl.getPauseMoreThanThis()*10000;
		if(count>pausingCount) {
			dbConnectorService.getEmailInfoRepoImpl().updateEmailInfosByProcessStatus(PROCESS.NOT_PROCESSED.name(), PROCESS.PAUSE.name(), (int)(count-pausingCount));
		}
		long psCount = dbConnectorService.getEmailInfoRepoImpl().countByProcessStatus(PROCESS.PAUSE.name());
		if(count<pausingCount && psCount>0) {
			long limitCount = pausingCount-count;
			if(limitCount>0) {
				dbConnectorService.getEmailInfoRepoImpl().updateEmailInfosByProcessStatus(PROCESS.PAUSE.name(), PROCESS.NOT_PROCESSED.name(), (int)(limitCount));
			}else {
				dbConnectorService.getEmailInfoRepoImpl().updateEmailInfosByProcessStatus(PROCESS.PAUSE.name(), PROCESS.NOT_PROCESSED.name(), (int)(psCount));
			}
		}
	}

}
