package com.testing.mail.connectors.management;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.testing.mail.connectors.factory.MailServiceFactory;
import com.testing.mail.connectors.impl.GMailConnector;
import com.testing.mail.connectors.impl.OutLookMailConnector;
import com.testing.mail.connectors.impl.helper.GmailHelper;
import com.testing.mail.constants.ExceptionConstants;
import com.testing.mail.dao.entities.ConnectFlags;
import com.testing.mail.dao.impl.AppMongoOpsManager;
import com.testing.mail.exceptions.handler.ThreadExceptionHandler;
import com.testing.mail.repo.entities.Clouds;
import com.testing.mail.repo.entities.EmailInfo;
import com.testing.mail.repo.entities.OAuthKey;
import com.testing.mail.repo.entities.VendorOAuthCredential;
import com.testing.mail.repo.entities.Clouds.CLOUD_NAME;
import com.testing.mail.repo.impl.CloudsRepoImpl;
import com.testing.mail.repo.impl.VendorOAuthCredentialImpl;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class DeltaUsersSaveTask implements Callable<Boolean>{


	private Clouds vo;
	private MailServiceFactory vendorServiceFactory;
	private CloudsRepoImpl cloudsRepo;
	AppMongoOpsManager connectorService;
	VendorOAuthCredentialImpl  authCredentialImpl;
	GmailHelper gmailHelper;

	
	

	public DeltaUsersSaveTask(Clouds vo, MailServiceFactory vendorServiceFactory, CloudsRepoImpl cloudsRepo,
			AppMongoOpsManager connectorService, VendorOAuthCredentialImpl authCredentialImpl,
			GmailHelper gmailHelper) {
		this.vo = vo;
		this.vendorServiceFactory = vendorServiceFactory;
		this.cloudsRepo = cloudsRepo;
		this.connectorService = connectorService;
		this.authCredentialImpl = authCredentialImpl;
		this.gmailHelper = gmailHelper;
	}

	@Override
	public Boolean call() throws Exception {
		return getDeltaUsers();
	}

	private boolean getDeltaUsers() {
		long startTime = System.nanoTime();
		boolean success = false;
		long activeUsers = 0;
		long inActiveUsers = 0;
		Thread.currentThread().setName("DeltaUsers:-" + vo.getId() + "time : " + new Date(System.nanoTime()));
		Thread.currentThread().setUncaughtExceptionHandler(new ThreadExceptionHandler());
		ConnectFlags connectFlags = new ConnectFlags();
		connectFlags.setAccessToken(vo.getCredential().getAccessToken());
		connectFlags.setEmailId(vo.getEmail());
		connectFlags.setUserId(vo.getUserId());
		connectFlags.setCloud(vo.getCloudName().name());
		connectFlags.setNextPageToken(vo.getDeltaChangeId());
		connectFlags.setAdminMemberId(vo.getMemberId());
		Map<String,Boolean>usersMap = new HashMap<>();
		long total = 1;
		if(null==vo.getDeltaChangeId()) {
			List<Clouds> exisitngUsers = cloudsRepo.findAllCloudsByAdmin(vo.getUserId(), vo.getAdminCloudId());
			if(!exisitngUsers.isEmpty()) {
				exisitngUsers.forEach(user->{
					usersMap.put(user.getMemberId(), user.isActive());
				});
			}
		}



		OAuthKey oauthKey = null;
		if(vo.getCloudName().equals(CLOUD_NAME.GMAIL)) {
			oauthKey = connectorService.getOAuthKeyRepository().findByCloud(vo.getCloudName());	
		}
		try {
			do {
				List<Clouds> msftUsers = vendorServiceFactory.getConnectorService(vo.getCloudName()).getDeltaUsersList(connectFlags);
				log.info("--total users found for saving in DB---"+(msftUsers!=null?msftUsers.size():0));
				List<Clouds> cloudUsers = new ArrayList<>();
				for (Clouds user : msftUsers) {
					if (user != null) {
						try {
							if(usersMap.containsKey(user.getMemberId())) {
								if(user.isActive()==usersMap.get(user.getMemberId())) {
									continue;
								}
							}
							total= total+1;
							user.setAdminMemberId(vo.getMemberId());
							user.setUserId(vo.getUserId());
							user.setAdminCloudId(vo.getId());
							user.setCloudName(vo.getCloudName());
							user.setCreatedTime(LocalDateTime.now());
							user.setAdminEmailId(vo.getEmail());
							if(vo.getCloudName().equals(CLOUD_NAME.GMAIL) && oauthKey!=null) {
								log.info("==Getting the accesstoken for the user=="+user.getEmail());
								VendorOAuthCredential credential =  gmailHelper.getAccessTokenForUser(user.getEmail(), oauthKey.getCilentEmail());
								if(credential!=null) {
									user.setCredential(credential);
									authCredentialImpl.save(credential);
								}else {
									user.setErrorDescription("OAuth credentials not created");
									user.setActive(false);
								}
							}else if(vo.getCloudName().equals(CLOUD_NAME.OUTLOOK)){
								user.setMailBoxStatus(validateUserMailStatus(user.getId(),vo.getCredential().getAccessToken()));
							}
							if(user.isActive()) {
								activeUsers= activeUsers+1;
							}else {
								inActiveUsers= inActiveUsers+1;
							}
							cloudUsers.add(user);

						} catch (Exception e) {
							log.warn(ExceptionUtils.getStackTrace(e));
						}
						if(cloudUsers.size()>20) {
							cloudsRepo.save(cloudUsers);
							cloudUsers.clear();
						}
					}
				}
				cloudsRepo.save(cloudUsers);
				cloudUsers.clear();
				log.info("Saving Deltausers completed in " + (System.nanoTime() - startTime));
				success = true;
			}while(connectFlags.getNextPageToken()!=null);
		} catch (Exception e) {
			log.error("Error while saving user list" + ExceptionUtils.getStackTrace(e));
			e.printStackTrace();
			return success;
		}
		if(vo!=null) {
			vo.setTotal(vo.getTotal()+total);
			vo.setProvisioned(vo.getProvisioned()+activeUsers);
			vo.setPicking(false);
			vo.setReAuthenticate(false);
			if(oauthKey!=null) {
				vo.getCredential().setRefreshToken(oauthKey.getCilentEmail());
				authCredentialImpl.save(vo.getCredential());
			}
			vo.setAdminCloudId(vo.getId());
			vo.setNonProvisioned(vo.getTotal()-vo.getProvisioned());
			cloudsRepo.save(vo);
		}
		log.info("DeltaUsers SaveTask complete in " + (System.nanoTime()-startTime));
		return success;
	}

	/**
	 * For Validating the Member MailBox Status  
	 */
	private String validateUserMailStatus(String memberId,String accessToken) {
		EmailInfo info = null;
		if(vo.getCloudName().equals(Clouds.CLOUD_NAME.OUTLOOK)) {
			info = ((OutLookMailConnector)vendorServiceFactory.getConnectorService(vo.getCloudName())).checkUserMailBoxStatus(accessToken, memberId);
		}else {
			info = ((GMailConnector)vendorServiceFactory.getConnectorService(vo.getCloudName())).checkUserMailBoxStatus(accessToken, memberId);
		}
		if(info == null) {
			return ExceptionConstants.MAILBOXNOT_FOUND;
		}
		return "Sucess";
	}

}
