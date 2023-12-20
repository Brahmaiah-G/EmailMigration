package com.testing.mail.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.testing.mail.dao.impl.AppMongoOpsManager;
import com.testing.mail.dao.persist.OAuthKeyRepository;
import com.testing.mail.repo.PlatformUserRepository;
import com.testing.mail.repo.impl.CalendarInfoRepoImpl;
import com.testing.mail.repo.impl.CloudsRepoImpl;
import com.testing.mail.repo.impl.EmailFolderInfoRepoImpl;
import com.testing.mail.repo.impl.EmailInfoRepoImpl;
import com.testing.mail.repo.impl.EmailJobRepoImpl;
import com.testing.mail.repo.impl.EmailQueueRepoImpl;
import com.testing.mail.repo.impl.MailChangeIdRepoImpl;
import com.testing.mail.repo.impl.MailChangesRepoImpl;
import com.testing.mail.repo.impl.MailUserSettingsRepoImpl;
import com.testing.mail.repo.impl.PermissionCacheRepoImpl;
import com.testing.mail.repo.impl.PremigrationRepoImpl;
import com.testing.mail.repo.impl.VendorOAuthCredentialImpl;
import com.testing.mail.repo.impl.WorkSpaceRepoImpl;

import lombok.Getter;
import lombok.Setter;

@Service
@Setter
@Getter
//Added for getting the all repository in db instead of getting single one in every constructor
public class DBConnectorService {

	@Autowired
	private AppMongoOpsManager appMongoOpsManager;

	@Autowired 
	private VendorOAuthCredentialImpl credentialsRepo;

	@Autowired
	private CloudsRepoImpl cloudsRepoImpl;

	@Autowired
	private WorkSpaceRepoImpl workSpaceRepoImpl;
	
	@Autowired
	private EmailInfoRepoImpl emailInfoRepoImpl;
	
	@Autowired
	private EmailJobRepoImpl emailJobRepoImpl;
	
	@Autowired
	private MailChangeIdRepoImpl mailChangeIdRepoImpl;
	
	@Autowired 
	private MailChangesRepoImpl mailChangesRepoImpl;
	
	@Autowired
	private CalendarInfoRepoImpl calendarInfoRepoImpl;
	
	@Autowired
	private OAuthKeyRepository oAuthKeyRepository;
	
	@Autowired
	private PlatformUserRepository platformUserRepository;
	
	@Autowired
	private MailUserSettingsRepoImpl mailUserSettingsRepoImpl;
	
	@Autowired
	private PermissionCacheRepoImpl permissionCacheRepoImpl;
	
	@Autowired
	private EmailFolderInfoRepoImpl emailFolderInfoRepoImpl;
	
	@Autowired
	private EmailQueueRepoImpl emailQueueRepoImpl;
	
	@Autowired
	private PremigrationRepoImpl premigrationRepoImpl;

}
