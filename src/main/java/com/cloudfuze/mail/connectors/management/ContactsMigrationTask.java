package com.cloudfuze.mail.connectors.management;

import com.cloudfuze.mail.connectors.factory.MailServiceFactory;
import com.cloudfuze.mail.repo.entities.ContactsInfo;
import com.cloudfuze.mail.repo.entities.EmailWorkSpace;
import com.cloudfuze.mail.repo.entities.PROCESS;
import com.cloudfuze.mail.service.DBConnectorService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ContactsMigrationTask implements Runnable{

	EmailWorkSpace emailWorkSpace ;
	MailServiceFactory  mailServiceFactory;
	DBConnectorService connectorService;
	ContactsInfo contactsInfo;
	
	public ContactsMigrationTask(EmailWorkSpace emailWorkSpace, MailServiceFactory mailServiceFactory,
			DBConnectorService connectorService, ContactsInfo contactsInfo) {
		this.emailWorkSpace = emailWorkSpace;
		this.mailServiceFactory = mailServiceFactory;
		this.connectorService = connectorService;
		this.contactsInfo = contactsInfo;
	}

	@Override
	public void run() {
		initiateMigration();
	}
	
	private void initiateMigration() {
		contactsInfo.setProcessStatus(PROCESS.IN_PROGRESS);
		contactsInfo.setThreadBy(Thread.currentThread().getName());
		connectorService.getEmailInfoRepoImpl().saveContact(contactsInfo);
		emailWorkSpace.setProcessStatus(EmailWorkSpace.PROCESS.IN_PROGRESS);
		connectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
	}
	
	
	
}
