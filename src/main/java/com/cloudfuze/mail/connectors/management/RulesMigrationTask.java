package com.testing.mail.connectors.management;

import java.util.Arrays;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.BeanUtils;

import com.testing.mail.connectors.factory.MailServiceFactory;
import com.testing.mail.dao.entities.EMailRules;
import com.testing.mail.dao.entities.EmailFlagsInfo;
import com.testing.mail.repo.entities.EmailFolderInfo;
import com.testing.mail.repo.entities.EmailWorkSpace;
import com.testing.mail.repo.entities.PROCESS;
import com.testing.mail.service.DBConnectorService;

import lombok.extern.slf4j.Slf4j;
@Slf4j
public class RulesMigrationTask implements Runnable {

	EmailWorkSpace emailWorkSpace ;
	MailServiceFactory  mailServiceFactory;
	DBConnectorService connectorService;
	EMailRules rule;


	public RulesMigrationTask(EmailWorkSpace emailWorkSpace, MailServiceFactory mailServiceFactory,
			DBConnectorService connectorService, EMailRules eMailRules) {
		this.emailWorkSpace = emailWorkSpace;
		this.mailServiceFactory = mailServiceFactory;
		this.connectorService = connectorService;
		this.rule = eMailRules;
	}

	private void migrateMailBoxRules() {
		rule.setThreadBy(Thread.currentThread().getName());
		rule.setProcessStatus(PROCESS.IN_PROGRESS);
		connectorService.getEmailInfoRepoImpl().saveMailBoxRule(rule);
		EmailFlagsInfo emailFlagsInfo = new EmailFlagsInfo();
		try {
			EmailFolderInfo folder = connectorService.getEmailFolderInfoRepoImpl().getParentFolderInfoBySourceId(emailWorkSpace.getId(), rule.getSourceId());
			log.info("--Rule Found for the Workspace -:"+emailWorkSpace.getId()+"-:"+emailFlagsInfo.getFolder());
			if(folder!=null ) {
				rule.setUserId(emailWorkSpace.getUserId());
				rule.setEmailWorkSpaceId(emailWorkSpace.getId());
				rule.setProcessStatus(PROCESS.IN_PROGRESS);
				try {
					EMailRules _eMailRules = new EMailRules();
					BeanUtils.copyProperties(rule, _eMailRules);
					_eMailRules.setMailFolder(Arrays.asList(folder.getDestId()));
					emailFlagsInfo.setCloudId(emailWorkSpace.getToCloudId());
					EMailRules _rule = mailServiceFactory.getConnectorService(emailWorkSpace.getToCloud()).createMailBoxRule(_eMailRules, emailFlagsInfo);
					if(_rule!=null) {
						rule.setDestId(_rule.getId());
						rule.setProcessStatus(PROCESS.PROCESSED);
					}else {
						rule.setProcessStatus(PROCESS.CONFLICT);
					}
				} catch (Exception e) {
					log.error(ExceptionUtils.getStackTrace(e));
					rule.setProcessStatus(PROCESS.CONFLICT);
					rule.setErrorDescription(ExceptionUtils.getStackTrace(e));
				}finally {
					connectorService.getEmailInfoRepoImpl().saveMailBoxRule(rule);
				}
			}
		} catch (Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
			rule.setProcessStatus(PROCESS.CONFLICT);
			rule.setErrorDescription(ExceptionUtils.getStackTrace(e));
		}
		finally {
			connectorService.getEmailInfoRepoImpl().saveMailBoxRule(rule);
		}
	}

	@Override
	public void run() {
		initiateMigration();
	}
	private void initiateMigration() {
		migrateMailBoxRules();
	}
}
