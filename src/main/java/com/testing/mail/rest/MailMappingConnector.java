package com.testing.mail.rest;

import java.time.LocalDateTime;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Controller;

import com.testing.mail.connectors.factory.MailServiceFactory;
import com.testing.mail.connectors.impl.GMailConnector;
import com.testing.mail.connectors.impl.OutLookMailConnector;
import com.testing.mail.connectors.impl.helper.GmailHelper;
import com.testing.mail.connectors.management.CloudSaveTask;
import com.testing.mail.connectors.management.DeltaUsersSaveTask;
import com.testing.mail.connectors.management.GroupsSaveTask;
import com.testing.mail.dao.entities.ConnectFlags;
import com.testing.mail.dao.entities.Enums.Status;
import com.testing.mail.dao.impl.AppMongoOpsManager;
import com.testing.mail.repo.entities.Clouds;
import com.testing.mail.repo.entities.OAuthKey;
import com.testing.mail.repo.entities.VendorOAuthCredential;
import com.testing.mail.repo.entities.Clouds.CLOUD_NAME;
import com.testing.mail.repo.impl.CloudsRepoImpl;
import com.testing.mail.repo.impl.PermissionCacheRepoImpl;
import com.testing.mail.repo.impl.VendorOAuthCredentialImpl;
import com.testing.mail.utils.ConnectUtils;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class MailMappingConnector {

	@Autowired
	CloudsRepoImpl cloudsRepoImpl;
	@Autowired
	OutLookMailConnector outLookMailConnector;
	@Autowired
	GmailHelper gmailHelper;
	@Autowired
	GMailConnector gMailConnector;
	@Autowired
	VendorOAuthCredentialImpl vendorOAuthCredentialRepository;
	@Autowired
	ThreadPoolTaskExecutor taskExecutor;
	@Autowired
	MailServiceFactory mailServiceFactory;
	@Autowired
	AppMongoOpsManager appMongoOpsManager;
	@Autowired
	VendorOAuthCredentialImpl oAuthCredentialImpl;
	@Autowired
	PermissionCacheRepoImpl permissionCacheRepoImpl;


	public Boolean getAccessToken(CLOUD_NAME label, String userId, String authCode) {

		Clouds saaSUser = null;
		VendorOAuthCredential oauthcreds =null;

		// Fetch Access Token
		switch(label){
		case OUTLOOK :{
			oauthcreds = outLookMailConnector.getAccessTokenByAuthorizationCode(authCode);
			break;
		}
		case GMAIL:{
			oauthcreds = gmailHelper.getAccesstoken(authCode);
			break;
		}
		default:
			break;
		}

		if(oauthcreds == null)
			throw new RuntimeException("OAuth Credential is null in MailMappingService");
		// Link to user
		oauthcreds.setUserId(userId);
		// Persist in db 

		boolean isAdmin = false;

		//Verify is mapped account is admin or not
		switch(label){

		case OUTLOOK :{
			// Get Admin info, need to handle getting 
			saaSUser  = outLookMailConnector.getAdminDetails(oauthcreds.getAccessToken(),null);
			break;
		}
		case GMAIL:{
			log.warn(oauthcreds.getAccessToken());
			String emailId = gMailConnector.getUserInfo(oauthcreds.getAccessToken());
			if(emailId!=null) {
				saaSUser  = gMailConnector.getAdminDetails(new ConnectFlags(oauthcreds.getAccessToken(),label.name(),emailId,null,null,null, null, null));
				if(saaSUser!=null) {
					saaSUser.setEmail(emailId);
				}
			}
			break;
		}				
		default:
			break; 
		}
		if(saaSUser!=null) {
			saaSUser.setAdmin(true);
			saaSUser.setCloudName(label);
			saaSUser.setAdminMemberId(saaSUser.getMemberId());
			saaSUser.setActive(true);
			saaSUser.setUserId(userId);
			saaSUser.setCreatedTime(LocalDateTime.now());
			saaSUser.setAdminEmailId(saaSUser.getEmail());
			if(oauthcreds.getId()==null) {
				oauthcreds.setId(saaSUser.getEmail()+":"+label.name());
			}
			vendorOAuthCredentialRepository.save(oauthcreds);
			if(checkUserVendorMappingExists(saaSUser.getMemberId(), userId)) {
				throw new DuplicateKeyException("cloud alredy registered");
			}
			saaSUser.setPicking(true);
			cloudsRepoImpl.save(saaSUser);
			//taskExecutor.submit(new CloudSaveTask(saaSUser, userId, oauthcreds.getAccessToken(), label, mailServiceFactory, cloudsRepoImpl,appMongoOpsManager, oAuthCredentialImpl, gmailHelper));
			new CloudSaveTask(saaSUser, userId, oauthcreds.getAccessToken(), label, mailServiceFactory, cloudsRepoImpl,appMongoOpsManager, oAuthCredentialImpl, gmailHelper).call();
			isAdmin = true;
			log.info("exiting  executor!!!");
		}

		//On Success start SaaS Users Synchronization

		return isAdmin;
	}

	
	
	public Status saveVendorFromXchange(CLOUD_NAME label, String userId, VendorOAuthCredential credential) {
		log.warn("--Adding vendor From Exchange ---"+label.name()+"---for the user---"+userId);
		Clouds saaSUser = null;
		if(credential == null)
			throw new RuntimeException("OAuth Credential is null in VendorMappingService");
		// Link to user
		credential.setUserId(userId);

		switch(label){
			case OUTLOOK :{
				// Get Admin info, need to handle getting
				saaSUser  = outLookMailConnector.getAdminDetails(credential.getAccessToken(),credential.getAdminEmail());
				break;
			}
			case GMAIL:{
				log.debug(credential.getAccessToken());
				String emailId = gMailConnector.getUserInfo(credential.getAccessToken());
				if(emailId!=null) {
					saaSUser  = gMailConnector.getAdminDetails(new ConnectFlags(credential.getAccessToken(),label.name(),emailId,null,null,null, null, null));
					if(saaSUser!=null) {
						saaSUser.setEmail(emailId);
					}
				}
				break;
			}
			default:
				break;
		}
		if(saaSUser!=null) {
			saaSUser.setAdmin(true);
			saaSUser.setCloudName(label);
			saaSUser.setAdminMemberId(saaSUser.getMemberId());
			saaSUser.setActive(true);
			saaSUser.setUserId(userId);
			saaSUser.setExchange(true);
			saaSUser.setAdminCloudId(saaSUser.getId());
			saaSUser.setCreatedTime(LocalDateTime.now());
			if(credential.getId()==null) {
				credential.setId(saaSUser.getEmail()+":"+label.name());
			}
			credential.setCloudName(label);
			saaSUser.setCredential(credential);
			vendorOAuthCredentialRepository.save(credential);
			if(checkUserVendorMappingExists(saaSUser.getMemberId(), userId)) {
				//throw new DuplicateKeyException("Cloud alredy registered");
			}
			saaSUser.setPicking(true);
			cloudsRepoImpl.save(saaSUser);
			saaSUser.setAdminCloudId(saaSUser.getId());
			//taskExecutor.submit(new CloudSaveTask(saaSUser, userId, credential.getAccessToken(), label,
				//	mailServiceFactory, cloudsRepoImpl, appMongoOpsManager, oAuthCredentialImpl, gmailHelper));
			new CloudSaveTask(saaSUser, userId, credential.getAccessToken(), label, mailServiceFactory, cloudsRepoImpl,appMongoOpsManager, oAuthCredentialImpl, gmailHelper).call();
			taskExecutor.submit(new GroupsSaveTask(saaSUser, cloudsRepoImpl, mailServiceFactory));
			log.info("exiting  executor!!!");
		}else {
			return Status.FAILURE;
		}

		return Status.SUCCESS;
	}

	public boolean checkUserVendorMappingExists(String memberId, String userId) {
		Clouds mappings = cloudsRepoImpl.findAdmin(memberId, userId);
		return mappings != null;
	}
	
	public OAuthKey getOAuthKeys(CLOUD_NAME label) {
		log.info("From getOAuthKeys : %s",label);
		return appMongoOpsManager.findOAuthKeyByCloud(label);
	}
	
	
	public boolean removeCloudAccess(String adminEmail,String userId,CLOUD_NAME name) {
		Clouds clouds = cloudsRepoImpl.findCloudsByEmailId(userId, adminEmail,name.name());
		log.warn("==cloud found==="+clouds);
		if(clouds!=null) {
			try {
				cloudsRepoImpl.removeCloudsByAdmin(userId, clouds.getAdminMemberId());
				vendorOAuthCredentialRepository.removeOne(clouds.getAdminEmailId()+":"+clouds.getCloudName().name());
				cloudsRepoImpl.deleteGroups(clouds.getAdminCloudId());
				cloudsRepoImpl.deleteMemberDetails(clouds.getAdminCloudId());
				permissionCacheRepoImpl.deletePermissionCacheBasedOnAdmin(clouds.getAdminCloudId());
				return true;
			} catch (Exception e) {
				log.warn(ExceptionUtils.getStackTrace(e));
			}
		}
		return false;
	}
	
	public boolean refreshUsers(String cloudId,String cloudName) {
		Clouds cloud = cloudsRepoImpl.findCloudsByEmailId(cloudId,cloudName);
		ConnectUtils.checkClouds(cloud);
		cloud.setReAuthenticate(true);
		cloud.setPicking(true);
		cloudsRepoImpl.save(cloud);
		taskExecutor.submit(new DeltaUsersSaveTask(cloud, mailServiceFactory, cloudsRepoImpl, appMongoOpsManager, oAuthCredentialImpl, gmailHelper));
		return true;
	}

}
