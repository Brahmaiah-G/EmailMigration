package com.cloudfuze.mail.connectors.management;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;

import com.cloudfuze.mail.connectors.factory.MailServiceFactory;
import com.cloudfuze.mail.connectors.impl.GMailConnector;
import com.cloudfuze.mail.dao.entities.EmailFlagsInfo;
import com.cloudfuze.mail.repo.entities.CalenderInfo;
import com.cloudfuze.mail.repo.entities.Clouds;
import com.cloudfuze.mail.repo.entities.EmailInfo;
import com.cloudfuze.mail.repo.entities.Clouds.CLOUD_NAME;
import com.cloudfuze.mail.repo.entities.EmailWorkSpace;
import com.cloudfuze.mail.service.DBConnectorService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EmailDestroyer implements Runnable{

	MailServiceFactory mailServiceFactory;
	List<String>ids ;
	DBConnectorService dbConnectorService;
	boolean events;
	
	public EmailDestroyer(MailServiceFactory mailServiceFactory, List<String> ids,
			DBConnectorService dbConnectorService,boolean events) {
		this.mailServiceFactory = mailServiceFactory;
		this.ids = ids;
		this.dbConnectorService = dbConnectorService;
		this.events = events;
	}



	@Override
	public void run() {
		ids.forEach(id->{
			EmailWorkSpace emailWorkSpace = dbConnectorService.getWorkSpaceRepoImpl().findOne(id);
			Clouds fromCloud = dbConnectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getFromCloudId());
			Clouds toCloud = dbConnectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getToCloudId());
			if(ObjectUtils.isEmpty(fromCloud) || ObjectUtils.isEmpty(toCloud)) {
				return;
			}


			int skip =0;
			int limit = 50;
			while(true) {
				List<?>emails ;
				if(events) {
					emails = dbConnectorService.getCalendarInfoRepoImpl().getUnDeletedMails(id, limit, skip);
				}else {
					emails = dbConnectorService.getEmailInfoRepoImpl().getUnDeletedMails(id, limit, skip);

				}
				if(emails.isEmpty()) {
					break;
				}
				log.warn("==Email Destroyer Not deleted emails found is  : =="+emails.size());
				List<String>_ids = new ArrayList<>();
				if(!emails.isEmpty()) {
					emails.forEach(email->{
						if(email instanceof EmailInfo) {
							((EmailInfo)email).setDeleted(true);
							_ids.add(((EmailInfo)email).getDestId());
							dbConnectorService.getEmailInfoRepoImpl().save((EmailInfo)email);
							
						}else {
							((CalenderInfo)email).setDeleted(true);
							_ids.add(((CalenderInfo)email).getDestId());
							dbConnectorService.getCalendarInfoRepoImpl().save((CalenderInfo)email);
						}
					});
				}
				if(!_ids.isEmpty()) {
					EmailFlagsInfo emailFlagsInfo = new EmailFlagsInfo();
					emailFlagsInfo.setCloudId(toCloud.getId());
					boolean event =((GMailConnector) mailServiceFactory.getConnectorService(CLOUD_NAME.GMAIL)).deleteBatchEmails(toCloud,emailFlagsInfo, false, _ids);
					log.info("==EmailDestroyer==="+_ids.size()+"==event"+event);
				}
				skip = skip+limit;
			}
		});
	}

}

