package com.testing.mail.connectors.management;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.testing.mail.connectors.factory.MailServiceFactory;
import com.testing.mail.connectors.google.data.DriveAbout;
import com.testing.mail.connectors.impl.GMailConnector;
import com.testing.mail.connectors.impl.OutLookMailConnector;
import com.testing.mail.connectors.impl.helper.GmailHelper;
import com.testing.mail.constants.Const;
import com.testing.mail.constants.ExceptionConstants;
import com.testing.mail.dao.entities.ConnectFlags;
import com.testing.mail.dao.entities.EmailFlagsInfo;
import com.testing.mail.dao.impl.AppMongoOpsManager;
import com.testing.mail.exceptions.handler.ThreadExceptionHandler;
import com.testing.mail.repo.entities.Clouds;
import com.testing.mail.repo.entities.EmailInfo;
import com.testing.mail.repo.entities.MemberDetails;
import com.testing.mail.repo.entities.OAuthKey;
import com.testing.mail.repo.entities.VendorOAuthCredential;
import com.testing.mail.repo.entities.Clouds.CLOUD_NAME;
import com.testing.mail.repo.impl.CloudsRepoImpl;
import com.testing.mail.repo.impl.VendorOAuthCredentialImpl;
import com.testing.mail.utils.MappingUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * For Saving the Clouds in Clouds-DB From the CloudProviders
 * 
 */
@Slf4j
public class CloudSaveTask implements Callable<Object> {



	private Clouds vo;
	private String userId;
	private String accessToken;
	private CLOUD_NAME cloudName;
	private MailServiceFactory vendorServiceFactory;
	private CloudsRepoImpl cloudsRepo;
	AppMongoOpsManager connectorService;
	VendorOAuthCredentialImpl  authCredentialImpl;
	GmailHelper gmailHelper;

	public CloudSaveTask(Clouds vo, String userId, String accessToken, CLOUD_NAME cloudName,
			MailServiceFactory vendorServiceFactory, CloudsRepoImpl dbConnectorService,AppMongoOpsManager connectorService,VendorOAuthCredentialImpl  authCredentialImpl
			,GmailHelper gmailHelper) {
		this.vo = vo;
		this.userId = userId;
		this.accessToken = accessToken;
		this.cloudName = cloudName;
		this.vendorServiceFactory = vendorServiceFactory;
		this.cloudsRepo = dbConnectorService;
		this.connectorService = connectorService;
		this.authCredentialImpl = authCredentialImpl;
		this.gmailHelper = gmailHelper;
	}


	@Override
	public Boolean call() {
		long startTime = System.nanoTime();
		boolean success = false;
		long activeUsers = 0;
		long inActiveUsers = 0;
		Thread.currentThread().setName("SaveVendorTask " + userId + "time : " + new Date(System.nanoTime()));
		Thread.currentThread().setUncaughtExceptionHandler(new ThreadExceptionHandler());
		ConnectFlags connectFlags = new ConnectFlags();
		connectFlags.setAccessToken(accessToken);
		connectFlags.setEmailId(vo.getEmail());
		connectFlags.setUserId(userId);
		connectFlags.setCloud(cloudName.name());
		connectFlags.setAdminMemberId(vo.getMemberId());
		List<String>mapepdDetails = new ArrayList<>();
		long total = 1;
		OAuthKey oauthKey = null;
		if(cloudName.equals(CLOUD_NAME.GMAIL)) {
			oauthKey = connectorService.getOAuthKeyRepository().findByCloud(cloudName);	
		}
		long totalUsersQuota = 0;
		try {
			do {
				List<Clouds> msftUsers = vendorServiceFactory.getConnectorService(cloudName).getUsersList(connectFlags);
				log.info("--total users found for saving in DB---"+(msftUsers!=null?msftUsers.size():0));
				List<Clouds> cloudUsers = new ArrayList<>();
				for (Clouds user : msftUsers) {
					if (user != null) {
						try {
							total= total+1;
							user.setAdminMemberId(vo.getMemberId());
							user.setUserId(userId);
							user.setAdminCloudId(vo.getId());
							user.setCloudName(cloudName);
							user.setCreatedTime(LocalDateTime.now());
							user.setAdminEmailId(vo.getEmail());
							if(cloudName.equals(CLOUD_NAME.GMAIL) && oauthKey!=null) {
								log.info("==Getting the accesstoken for the user=="+user.getEmail());
								VendorOAuthCredential credential =  gmailHelper.getAccessTokenForUser(user.getEmail(), oauthKey.getCilentEmail());
								if(credential!=null) {
									user.setCredential(credential);
									authCredentialImpl.save(credential);
									user.setMailBoxStatus(validateUserMailStatus(user.getMemberId(),credential.getAccessToken()));
									try {
										DriveAbout driveAbout = ((GMailConnector) vendorServiceFactory.getConnectorService(cloudName)).getDriveDetails(credential.getAccessToken(), credential.getId());
										if(driveAbout!=null) {
										    user.setQuotaBytesUsed(Long.valueOf(driveAbout.getQuotaBytesByService().stream().filter(dabout->dabout.getServiceName().equals("GMAIL")).findAny().get().getBytesUsed()));
										    totalUsersQuota = totalUsersQuota+user.getQuotaBytesUsed();
										    user.setQuotaUsed(MappingUtils.formatFileSize(user.getQuotaBytesUsed()));
										}
									} catch (Exception e) {
									}
								}else {
									user.setErrorDescription("OAuth credentials not created");
									user.setActive(false);
								}
							}else if(cloudName.equals(CLOUD_NAME.OUTLOOK)){
								user.setMailBoxStatus(validateUserMailStatus(user.getMemberId(),accessToken));
							}
							if(user.isActive()) {
								activeUsers= activeUsers+1;
							}else {
								inActiveUsers= inActiveUsers+1;
							}
							mapepdDetails.add(user.getEmail()+Const.HASHTAG+ user.getMemberId());
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
				log.info("Saving users completed in " + (System.nanoTime() - startTime));
				success = true;
			}while(connectFlags.getNextPageToken()!=null);
		} catch (Exception e) {
			log.error("Error while saving user list" + ExceptionUtils.getStackTrace(e));
			e.printStackTrace();
			return success;
		}
		if(vo!=null) {
			List<Clouds> clouds = null;
			List<Clouds> totalClouds = new ArrayList<>();
			int pageNo = 0;
			int pageSize = 100;
			while(true) {
				clouds = cloudsRepo.findCloudsByAdminWithPazination(vo.getUserId(), vo.getAdminMemberId(), pageSize, pageNo);
				if(clouds.isEmpty()) {
					break;
				}
				totalClouds.addAll(clouds);
				pageNo =pageNo+pageSize;
			}
			if(cloudName.equals(CLOUD_NAME.OUTLOOK)) {
				List<Clouds>cloudUsers = new ArrayList<>();
				for(Clouds cloud : totalClouds) {
					try {
						if(!cloud.isActive()) {
							cloud.setQuotaUsed("0 Bytes");
							cloudUsers.add(cloud);
							continue;
						}
						EmailFlagsInfo emailFlagsInfo = new EmailFlagsInfo();
						emailFlagsInfo.setCloudId(cloud.getId());
						List<EmailFlagsInfo> infos = ((OutLookMailConnector) vendorServiceFactory.getConnectorService(cloudName)).getListOfMailFolders(emailFlagsInfo);
						if(!infos.isEmpty()) {
							cloud.setQuotaBytesUsed(infos.stream().mapToLong(EmailFlagsInfo::getSizeInBytes).sum());
							cloud.setQuotaUsed(MappingUtils.formatFileSize(cloud.getQuotaBytesUsed()));
							totalUsersQuota = totalUsersQuota+cloud.getQuotaBytesUsed();
							cloudUsers.add(cloud);
						}
					}catch(Exception e) {
					}
				}
				cloudsRepo.save(cloudUsers);
				cloudUsers.clear();
			}
			if(!mapepdDetails.contains(vo.getEmail()+Const.HASHTAG+ vo.getMemberId())) {
				mapepdDetails.add(vo.getEmail()+Const.HASHTAG+ vo.getMemberId());
			}
			saveMemberDetails(mapepdDetails);
			mapepdDetails.clear();
			vo.setTotal(total);
			vo.setProvisioned(activeUsers+1L);
			if(cloudName.equals(CLOUD_NAME.GMAIL)) {
				try {
					DriveAbout driveAbout = ((GMailConnector) vendorServiceFactory.getConnectorService(cloudName)).getDriveDetails(vo.getCredential().getAccessToken(), vo.getCredential().getId());
					if(driveAbout!=null) {
					    vo.setQuotaBytesUsed(Long.valueOf(driveAbout.getQuotaBytesByService().stream().filter(dabout->dabout.getServiceName().equals("GMAIL")).findAny().get().getBytesUsed()));
					    totalUsersQuota = totalUsersQuota+vo.getQuotaBytesUsed();
					    vo.setQuotaUsed(MappingUtils.formatFileSize(vo.getQuotaBytesUsed()));
					}
				} catch (Exception e) {
				}
			}else {
				EmailFlagsInfo emailFlagsInfo = new EmailFlagsInfo();
				emailFlagsInfo.setCloudId(vo.getId());
				List<EmailFlagsInfo> infos = ((OutLookMailConnector) vendorServiceFactory.getConnectorService(cloudName)).getListOfMailFolders(emailFlagsInfo);
				if(!infos.isEmpty()) {
					vo.setQuotaBytesUsed(infos.stream().mapToLong(EmailFlagsInfo::getSizeInBytes).sum());
					vo.setQuotaUsed(MappingUtils.formatFileSize(vo.getQuotaBytesUsed()));
					totalUsersQuota = totalUsersQuota+vo.getQuotaBytesUsed();
				}
			}
			vo.setTotalUsersQuotaUsed(MappingUtils.formatFileSize(totalUsersQuota));
			vo.setPicking(false);
			if(oauthKey!=null) {
				vo.getCredential().setRefreshToken(oauthKey.getCilentEmail());
				authCredentialImpl.save(vo.getCredential());
			}
			try {
				vo.setDomains(vendorServiceFactory.getConnectorService(cloudName).getDomains(connectFlags));
			} catch (Exception e) {
				log.warn(ExceptionUtils.getStackTrace(e));
				vo.setDomains(Arrays.asList(vo.getEmail().split(Const.ATTHERATE)[1]));
			}
			vo.setAdminCloudId(vo.getId());
			vo.setNonProvisioned(vo.getTotal()-vo.getProvisioned());
			cloudsRepo.save(vo);
		}
		log.info("CloudSaveTask complete in " + (System.nanoTime()-startTime));
		return success;
	}

	/**For Validating the Member MailBox Status  
	 */
	private String validateUserMailStatus(String memberId,String accessToken) {
		try {
			EmailInfo info = null;
			if(cloudName.equals(Clouds.CLOUD_NAME.OUTLOOK)) {
				info = ((OutLookMailConnector)vendorServiceFactory.getConnectorService(cloudName)).checkUserMailBoxStatus(accessToken, memberId);
			}else {
				info = ((GMailConnector)vendorServiceFactory.getConnectorService(cloudName)).checkUserMailBoxStatus(accessToken, memberId);
			}
			if(info == null) {
				return ExceptionConstants.MAILBOXNOT_FOUND;
			}
		} catch (Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
		}
		return "Sucess";
	}

	public void saveMemberDetails(List<String>mapepdDetails){
		try {
			MemberDetails memberDetails =  new MemberDetails();
			mapepdDetails.add(vo.getEmail()+Const.HASHTAG+vo.getMemberId());
			memberDetails.setUserId(userId);
			memberDetails.setAdminCloudId(vo.getAdminCloudId());
			memberDetails.setMembers(mapepdDetails);
			memberDetails.setCloudName(cloudName);
			cloudsRepo.saveMemberDetails(memberDetails);
		} catch (Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
			vo.setErrorDescription(ExceptionUtils.getStackTrace(e));
		}
	}
	
}




