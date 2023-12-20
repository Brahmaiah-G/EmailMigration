package com.testing.mail.connectors.management;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.testing.mail.connectors.factory.MailServiceFactory;
import com.testing.mail.connectors.management.utility.ConnectorUtility;
import com.testing.mail.dao.entities.EmailFlagsInfo;
import com.testing.mail.dao.entities.EmailUserSettings;
import com.testing.mail.dao.entities.PermissionCache;
import com.testing.mail.repo.entities.EmailWorkSpace;
import com.testing.mail.repo.entities.PROCESS;
import com.testing.mail.service.DBConnectorService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SettingMigrationTask implements Runnable{

	EmailWorkSpace emailWorkSpace ;
	MailServiceFactory  mailServiceFactory;
	DBConnectorService connectorService;
	EmailUserSettings setting;
	Map<String, String> mappedPairs = new HashMap<>();

	
	public SettingMigrationTask(EmailWorkSpace emailWorkSpace, MailServiceFactory mailServiceFactory,
			DBConnectorService connectorService, EmailUserSettings setting) {
		this.emailWorkSpace = emailWorkSpace;
		this.mailServiceFactory = mailServiceFactory;
		this.connectorService = connectorService;
		this.setting = setting;
	}

	@Override
	public void run() {
		log.info("==STARTING *** Migrating the Email settings in the user Workspace in --"+emailWorkSpace.getId());
		initiateMigration();
	}

	public void initiateMigration() {
		migrateEmailSettings();
	}
	void migrateEmailSettings(){
		EmailFlagsInfo emailFlagsInfo = new EmailFlagsInfo();
		mappedPermissions();
		if(emailWorkSpace.isSettings()) {
			try {
				setting.setThreadBy(Thread.currentThread().getName());
				emailFlagsInfo.setCloudId(emailWorkSpace.getToCloudId());
				setting.setProcessStatus(PROCESS.IN_PROGRESS);
				setting.setEmail(ConnectorUtility.mapMails(Arrays.asList(setting.getEmail()), mappedPairs).get(0));
				if(setting.getAutoForwardSettings()!=null) {
					setting.getAutoForwardSettings().setEmailAddress(ConnectorUtility.mapMails(Arrays.asList(setting.getAutoForwardSettings().getEmailAddress()), mappedPairs).get(0));
				}
				if(setting.getForwardingAddresses()!=null && !setting.getForwardingAddresses().isEmpty()) {
					setting.getForwardingAddresses().forEach(forward->
					forward.setForwardingEmail(ConnectorUtility.mapMails(Arrays.asList(forward.getForwardingEmail()), mappedPairs).get(0)));
				}
				connectorService.getMailUserSettingsRepoImpl().save(setting);
				EmailUserSettings rule = mailServiceFactory.getConnectorService(emailWorkSpace.getToCloud()).createUpdateSettings(setting, emailFlagsInfo);
				if(rule!=null) {
					setting.setUpdated(true);
					setting.setDestId(rule.getEmail());
					setting.setProcessStatus(PROCESS.PROCESSED);
				}
			} catch (Exception e) {
				log.error(ExceptionUtils.getStackTrace(e));
				setting.setErrorDescription(ExceptionUtils.getStackTrace(e));
				setting.setProcessStatus(PROCESS.CONFLICT);
			}
			finally {
				connectorService.getMailUserSettingsRepoImpl().save(setting);
			}
		}
	}
	/**
	 *Getting the Mapped Pairs from the PermissionCache based on FromAdminCloudId,ToAdminCloudId,UserId<br></br>
	 */
	private void mappedPermissions() {
		if(emailWorkSpace.isMapping()) {
			List<PermissionCache> mappedUsers = connectorService.getPermissionCacheRepoImpl().getPermissionsFromAdmin(emailWorkSpace.getFromAdminCloud(), emailWorkSpace.getToAdminCloud(), emailWorkSpace.getUserId());
			if(!mappedUsers.isEmpty()) {
				mappedUsers.forEach(mapped->{
					if(mapped.getFromMail()!=null && mapped.getToMail()!=null) {
						mappedPairs.put(mapped.getFromMail(), mapped.getToMail());
					}
				});
			}
		}
	}
}
