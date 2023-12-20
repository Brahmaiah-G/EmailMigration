package com.testing.mail.connectors.management;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.testing.mail.connectors.factory.MailServiceFactory;
import com.testing.mail.connectors.impl.OutLookMailConnector;
import com.testing.mail.connectors.microsoft.data.Value;
import com.testing.mail.dao.entities.EmailFlagsInfo;
import com.testing.mail.repo.entities.EmailInfo;
import com.testing.mail.repo.entities.EmailWorkSpace;
import com.testing.mail.repo.entities.CalenderInfo.PROCESS;
import com.testing.mail.service.DBConnectorService;
import com.testing.mail.utils.MappingUtils.MAIL_FOLDERS;

import lombok.extern.slf4j.Slf4j;


/**
 * For Deleting the Mails in OutLook Migrating User<p></p>
 * As We are Migrating from the Admin in Outlook Need to delete those in SENT(Admin User) Outlook 
 */

//TODO : Need to use batch call instead of single call it will impact the large migration//for that need to find the mails in batch
@Slf4j
public class EmailPurger  implements Runnable{

	private MailServiceFactory mailServiceFactory;
	private DBConnectorService connectorService; 
	EmailWorkSpace emailWorkSpace;
	EmailInfo emailInfo;


	public EmailPurger(MailServiceFactory mailServiceFactory, DBConnectorService connectorService,
			EmailWorkSpace emailWorkSpace, EmailInfo emailInfo) {
		this.mailServiceFactory = mailServiceFactory;
		this.connectorService = connectorService;
		this.emailWorkSpace = emailWorkSpace;
		this.emailInfo = emailInfo;
	}

	@Override
	public void run() {
		log.info("Entered for PURGE in the admin directory==="+emailWorkSpace.getId()+"==ID:=="+emailInfo.getId());
		deleteMails();

	}

	/**
	 * For deleting Sent Email<br></br>
	 * ONLY for <b>OUTLOOK</b>
	 */
	private void deleteMails() {
		try {
			EmailFlagsInfo emailFlagsInfo = new EmailFlagsInfo();
			emailFlagsInfo.setCloudId(emailWorkSpace.getToAdminCloud());
			emailFlagsInfo.setId(emailInfo.getDestThreadId());
			emailFlagsInfo.setConvIndex(emailInfo.getConvIndex());
			emailFlagsInfo.setThread(emailInfo.getOrder()>0);
			emailFlagsInfo.setFrom(emailInfo.getFromMail());
			emailFlagsInfo.setCopy(emailWorkSpace.isCopy());
			boolean status = false;
			emailFlagsInfo.setOrder(emailInfo.getOrder());
			if(!emailWorkSpace.getToCloudId().equals(emailWorkSpace.getToAdminCloud())) {
				if(!emailInfo.getMailFolder().equalsIgnoreCase(MAIL_FOLDERS.SENTITEMS.name().toLowerCase())) {
					emailFlagsInfo.setFolder(MAIL_FOLDERS.SENTITEMS.name().toLowerCase());
				}else {
					emailFlagsInfo.setFolder(MAIL_FOLDERS.INBOX.name().toLowerCase());
				}
				emailInfo.setAdminStatus("AdminDeleted Folder "+emailFlagsInfo.getFolder());
				status = mailServiceFactory.getConnectorService(emailWorkSpace.getToCloud()).deleteEmails(emailFlagsInfo, false);
				emailInfo.setAdminDeleted(status);
			}else if(!emailInfo.getProcessStatus().name().equals(PROCESS.METADATA_STARTED.name()) && emailWorkSpace.getToCloudId().equals(emailWorkSpace.getToAdminCloud())){
				log.info("admin  Entered for deleting the sent admin mails in the admin directory==="+emailWorkSpace.getId()+"==ID:=="+emailInfo.getId());
				if(!emailInfo.getMailFolder().equalsIgnoreCase(MAIL_FOLDERS.SENTITEMS.name().toLowerCase())) {
					emailFlagsInfo.setFolder(MAIL_FOLDERS.SENTITEMS.name().toLowerCase());
				}else {
					emailFlagsInfo.setFolder(MAIL_FOLDERS.INBOX.name().toLowerCase());
				}
				emailInfo.setAdminStatus("AdminDeleted Folder "+emailFlagsInfo.getFolder());
				status = mailServiceFactory.getConnectorService(emailWorkSpace.getToCloud()).deleteEmails(emailFlagsInfo, false);
				emailInfo.setAdminDeleted(status);
				log.info("==Deleted Status=="+status+"==ID:=="+emailInfo.getId());
				if(status) {
					emailFlagsInfo.setFolder("deleteditems");
					status = mailServiceFactory.getConnectorService(emailWorkSpace.getToCloud()).deleteEmails(emailFlagsInfo, false);
					emailInfo.setAdminDeleted(status);
				}else {
					emailInfo.setAdminDeleted(false);
				}
				if(status) {
					if(!emailInfo.getMailFolder().equalsIgnoreCase(MAIL_FOLDERS.SENTITEMS.name().toLowerCase())) {
						emailFlagsInfo.setFolder(MAIL_FOLDERS.SENTITEMS.name().toLowerCase());
					}else {
						emailFlagsInfo.setFolder(MAIL_FOLDERS.INBOX.name().toLowerCase());
					}
					Value value= ((OutLookMailConnector)mailServiceFactory.getConnectorService(emailWorkSpace.getToCloud())).getSingleMailByConversationId(emailFlagsInfo);
					if(value!=null) {
						emailInfo.setAdminDeleted(false);
					}
				}
				Thread.sleep(1000);
			}
			log.warn("==Status after deleting email---"+status+"==for the Email workspace id=== "+emailWorkSpace.getId());
		} catch (Exception e) {
			emailInfo.setErrorDescription(ExceptionUtils.getStackTrace(e));
			log.error(ExceptionUtils.getStackTrace(e));
			emailInfo.setAdminDeleted(true);
		}finally {
			if(emailInfo.getAdminStatus()==null || StringUtils.isBlank(emailInfo.getAdminStatus())) {
				emailInfo.setAdminDeleted(false);
			}
			connectorService.getEmailInfoRepoImpl().save(emailInfo);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
		}
	}


}
