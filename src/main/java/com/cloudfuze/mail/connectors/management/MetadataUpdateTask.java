package com.cloudfuze.mail.connectors.management;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.cloudfuze.mail.connectors.factory.MailServiceFactory;
import com.cloudfuze.mail.connectors.scheduler.MetadataUpdateScheduler;
import com.cloudfuze.mail.constants.Const;
import com.cloudfuze.mail.dao.entities.EmailFlagsInfo;
import com.cloudfuze.mail.repo.entities.Clouds;
import com.cloudfuze.mail.repo.entities.Clouds.CLOUD_NAME;
import com.cloudfuze.mail.repo.entities.EmailFolderInfo;
import com.cloudfuze.mail.repo.entities.EmailInfo;
import com.cloudfuze.mail.repo.entities.EmailWorkSpace;
import com.cloudfuze.mail.repo.entities.EmailWorkSpace.PROCESS;
import com.cloudfuze.mail.repo.entities.MemberDetails;
import com.cloudfuze.mail.service.DBConnectorService;
import com.cloudfuze.mail.utils.MappingUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Updating the Destination Metadata
 * <pre>
 * Example : Sender Mail,Sent Time, Received Time, CC, To , BCC
 * </pre>
 * Calling Scheduler : {@link MetadataUpdateScheduler}
 * @see MetadataUpdateScheduler
 */

@Slf4j
public class MetadataUpdateTask implements Runnable{

	private MailServiceFactory mailServiceFactory;
	private DBConnectorService connectorService; 
	EmailWorkSpace emailWorkSpace;
	EmailInfo emailInfo;

	com.cloudfuze.mail.repo.entities.EmailInfo.PROCESS processStatus = com.cloudfuze.mail.repo.entities.EmailInfo.PROCESS.METADATA_INPROGRESS;

	public MetadataUpdateTask(MailServiceFactory mailServiceFactory, DBConnectorService connectorService,
			EmailWorkSpace emailWorkSpace, EmailInfo emailInfo) {
		this.mailServiceFactory = mailServiceFactory;
		this.connectorService = connectorService;
		this.emailWorkSpace = emailWorkSpace;
		this.emailInfo = emailInfo;
	}


	@Override
	public void run() {
		log.info("==Entered for updating the metadata for the workspace==="+emailWorkSpace.getId());
		updateMetadata();
	}

	private void updateMetadata() {

		log.info("===Entered for updating the mails/Mail Folders for=="+emailWorkSpace.getId()+"==from mail=="+emailWorkSpace.getFromMailId()+"==toMail ID=="+emailWorkSpace.getToMailId()+"==fromFolder:="+emailWorkSpace.getFromFolderId()+"==ID:=="+emailInfo.getId());
		emailWorkSpace.setProcessStatus(PROCESS.IN_PROGRESS);
		emailInfo.setThreadBy(Thread.currentThread().getName());
		connectorService.getEmailInfoRepoImpl().save(emailInfo);
		emailWorkSpace = connectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
		Clouds fromCloud = connectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getFromCloudId());
		Clouds toCloud = connectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getToCloudId());
		Map<String, String> destMembers = getMemberDetails(emailWorkSpace.getUserId(), emailWorkSpace.getToAdminCloud());

		if(ObjectUtils.isEmpty(fromCloud) || ObjectUtils.isEmpty(toCloud) || StringUtils.isEmpty(emailInfo.getSourceId())) {
			emailInfo.setErrorDescription("required values for migration are null please check");
			emailInfo.setProcessStatus(EmailInfo.PROCESS.CONFLICT);
			return;
		}
		try {
			List<String>emails = new ArrayList<>();

			if(emailWorkSpace.isCopy()) {
				emails.add(toCloud.getEmail());
			}else {
				emails.addAll(emailInfo.getToMail());
				emails.addAll(emailInfo.getCc());
				emails.addAll(emailInfo.getBcc());
				emails.add(emailInfo.getFromMail());
			}
			if(!emails.contains(emailWorkSpace.getToMailId())) {
				emails.add(emailWorkSpace.getToMailId());
			}
			List<String>metadataUpdates = new ArrayList<>();
			for(String email: emails) {
				try {
					EmailFlagsInfo emailFlagsInfo = new EmailFlagsInfo();
					emailFlagsInfo.setAdminMemberId(fromCloud.getAdminMemberId());
					emailFlagsInfo.setCloudId(emailWorkSpace.getToCloudId());
					emailFlagsInfo.setUserId(fromCloud.getUserId());
					emailFlagsInfo.setEmailId(emailInfo.getFromMail());
					emailFlagsInfo.setTo(emailInfo.getToMail());
					emailFlagsInfo.setId(emailInfo.getDestThreadId());
					emailFlagsInfo.setCc(emailInfo.getCc());
					emailFlagsInfo.setBcc(emailInfo.getBcc());
					emailFlagsInfo.setReplyTo(emailInfo.getReplyTo());
					emailFlagsInfo.setDeltaThread(emailInfo.isDeltaThread());
					emailFlagsInfo.setThreadId(emailInfo.getThreadId());
					emailFlagsInfo.setFrom(emailInfo.getFromMail());
					emailFlagsInfo.setMetaData(emailInfo.isMetadata());
					emailFlagsInfo.setMessage(emailInfo.getHtmlBodyContent());
					emailFlagsInfo.setFolder(emailInfo.getDestFolderPath()==null?emailInfo.getMailFolder():emailInfo.getDestFolderPath());
					emailFlagsInfo.setCreatedTime(emailInfo.getMailCreatedTime());
					emailFlagsInfo.setSentTime(emailInfo.getMailModifiedTime());
					if(emailWorkSpace.isDeltaMigration()) {
						emailFlagsInfo.setDestId(emailInfo.getDestId());
					}
					emailFlagsInfo.setFromName(emailInfo.getFromName());
					emailFlagsInfo.setRead(emailInfo.isRead());
					emailFlagsInfo.setLabels(Arrays.asList(emailInfo.getMailFolder()));
					emailFlagsInfo.setConvIndex(emailInfo.getConvIndex());
					mailFolderMapping(emailFlagsInfo);
					emailFlagsInfo.setOrder(emailInfo.getOrder());
					emailFlagsInfo.setDraft(emailInfo.isDraft());
					emailFlagsInfo.setThread(emailInfo.getOrder()>0);
					emailFlagsInfo.setSubject(emailInfo.getSubject());
					if(!emailWorkSpace.getToFolderId().equals("/")) {
						emailFlagsInfo.setFolder(emailInfo.getDestFolderPath());
					}
					Clouds cloud = connectorService.getCloudsRepoImpl().findCloudsByEmailIdUsingAdmin(emailInfo.getUserId(), email, toCloud.getAdminCloudId());
					if(cloud==null && email.endsWith(toCloud.getEmail().split("@")[1])) {
						cloud = connectorService.getCloudsRepoImpl().findCloudsByEmailIdUsingAdmin(emailInfo.getUserId(), toCloud.getEmail(), toCloud.getAdminCloudId());
					}
					if(cloud==null) {
						log.info("--For External User Skipping the Metadata Update---"+emailInfo.getId()+"--"+emailInfo.getSourceId()+"--"+email);
						emailInfo.setExtraField(emailInfo.getErrorDescription()+"-"+email);
						continue;	
					}
					if(emailWorkSpace.isCopy()) {
						emailFlagsInfo.setRead(emailInfo.isRead());
						emailFlagsInfo.setImportance(emailInfo.getImportance());
						emailFlagsInfo.setFlagged(emailInfo.isFlagged());
					}else {
						if(emailInfo.getFromMail().equals(email) || (!destMembers.containsKey(emailInfo.getFromMail()) && email.equals(toCloud.getEmail()))) {
							emailFlagsInfo.setRead(emailInfo.isRead());
							emailFlagsInfo.setImportance(emailInfo.getImportance());
							emailFlagsInfo.setFlagged(emailInfo.isFlagged());
						}
					}
					emailFlagsInfo.setCloudId(cloud.getId());
					List<String>mails = new ArrayList<>();
					mails.addAll(emailInfo.getCc());
					mails.addAll(emailInfo.getBcc());
					mails.addAll(emailInfo.getToMail());
					EmailInfo metadata = mailServiceFactory.getConnectorService(toCloud.getCloudName()).updateMetadata(emailFlagsInfo);
					if(metadata!=null) {
						if(!com.cloudfuze.mail.repo.entities.EmailInfo.PROCESS.METADATA_CONFLICT.equals(processStatus)) {
							processStatus = com.cloudfuze.mail.repo.entities.EmailInfo.PROCESS.PROCESSED;
						}
						emailInfo.setUpdatedMetadata(metadata.getUpdatedMetadata()+"==metadata==");
						emailInfo.setDestId(metadata.getId());
						boolean moved = false;
						if(toCloud.getEmail().equals(email)) {
							connectorService.getEmailInfoRepoImpl().updateEmailInfoForDestThreadId(emailWorkSpace.getId(), emailInfo.getThreadId(), metadata.getId(),emailInfo.getOrder(),emailInfo.getConvIndex(),mails);
							connectorService.getEmailInfoRepoImpl().updateEmailInfoForDestThreadId(emailWorkSpace.getId(), emailInfo.getThreadId(), metadata.getId(),emailInfo.getOrder(),emailInfo.getConvIndex());
							if(!destMembers.containsKey(emailInfo.getFromMail())) {
								connectorService.getEmailInfoRepoImpl().updateEmailInfoForDestThreadIdConvex(emailWorkSpace.getId(), emailInfo.getThreadId(), metadata.getId(),emailInfo.getOrder(),emailInfo.getConvIndex());
							}
							if(!metadata.getDestParent().equals(emailInfo.getDestFolderPath())) {
								moved = true;
							}
						}
						if(metadata!=null) {
							metadataUpdates.add(email);
						}
						if(null!=emailInfo.getMovedFolder() && toCloud.getEmail().equals(email)) {
							EmailFolderInfo info = connectorService.getEmailFolderInfoRepoImpl().getParentFolderInfo(emailWorkSpace.getId(), emailInfo.getMovedFolder());
							
							if(null!=info) {
								if(info.getSourceId().equalsIgnoreCase(info.getMailFolder())) {
									info = connectorService.getEmailFolderInfoRepoImpl().getParentFolderInfoBySourceId(emailWorkSpace.getId(), emailInfo.getMovedFolder());
								}
								if(info.getDestId().equals(metadata.getDestParent())) {
									moved = false;
								}
								emailInfo.setDestFolderPath(info.getDestId());
							}else {
								String folder = MappingUtils.mapMailFolder(emailInfo.getMovedFolder(), emailWorkSpace.getFromCloud(), emailWorkSpace.getToCloud());
								emailInfo.setDestFolderPath(null!=folder?folder:emailInfo.getMovedFolder());
							}
							if(!metadata.getDestParent().equals(emailInfo.getDestFolderPath())) {
								moved = true;
							}
							if(moved) {
								emailFlagsInfo.setFolder(emailInfo.getDestFolderPath());
								emailFlagsInfo.setCloudId(emailWorkSpace.getToCloudId());
								metadata = mailServiceFactory.getConnectorService(toCloud.getCloudName()).moveEmails(emailFlagsInfo, metadata);
								if(metadata!=null) {
									metadata.setMailFolder(checkAndReturnMailFolder(metadata.getMailFolder()));
									String mailFolder = checkAndReturnMailFolder(emailInfo.getMailFolder());
									if(mailFolder!=null) {
										emailInfo.setMailFolder(mailFolder);
									}
									emailInfo.setDestId(metadata.getId());
									emailInfo.setDestParent(metadata.getDestParent());
									emailInfo.setErrorDescription("Moved");
								}
							}
						}
					}
				} catch (Exception e) {
					emailInfo.setErrorDescription(ExceptionUtils.getStackTrace(e));
					processStatus = com.cloudfuze.mail.repo.entities.EmailInfo.PROCESS.METADATA_CONFLICT;
					emailInfo.setRetryCount(emailInfo.getRetryCount()+1);
				}
			}
			emailInfo.setUpdatedMetadata("updated to="+metadataUpdates.toString());
		}catch(Exception e) {
			emailInfo.setRetryCount(emailInfo.getRetryCount()+1);
			emailInfo.setErrorDescription(ExceptionUtils.getStackTrace(e));
			if(e.getMessage().contains("ErrorInvalidMailboxItemId")) {
				emailInfo.setRetryCount(100);
			}
			processStatus = com.cloudfuze.mail.repo.entities.EmailInfo.PROCESS.METADATA_CONFLICT;
		}finally{
			emailInfo.setProcessStatus(processStatus);
			connectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
			connectorService.getEmailInfoRepoImpl().save(emailInfo);
		}
	}


	private void mailFolderMapping(EmailFlagsInfo emailFlagsInfo) {
		emailFlagsInfo.setFolder(checkAndReturnMailFolder(emailFlagsInfo.getFolder()));
	}


	private String checkAndReturnMailFolder(String mailFolder) {

		String retunFolder = mailFolder;
		if(emailWorkSpace.getFromCloud().equals(CLOUD_NAME.OUTLOOK) && emailWorkSpace.getToCloud().equals(CLOUD_NAME.GMAIL)) {
			if( mailFolder.equalsIgnoreCase("junkemail")) {
				retunFolder ="SPAM";
			}else if(mailFolder.equalsIgnoreCase("sentitems")) {
				retunFolder ="SENT";
			}else if(mailFolder.equalsIgnoreCase("drafts")) {
				retunFolder ="DRAFT";
			}else if(mailFolder.equalsIgnoreCase("outbox")) {
				retunFolder ="SENT";
			}else if(mailFolder.equalsIgnoreCase("deleteditems")) {
				retunFolder ="TRASH";
			}
		}else if(emailWorkSpace.getFromCloud().equals(CLOUD_NAME.GMAIL) && emailWorkSpace.getToCloud().equals(CLOUD_NAME.OUTLOOK)){
			if(mailFolder.equalsIgnoreCase("SPAM")) {
				retunFolder ="junkemail";
			}else if(mailFolder.equalsIgnoreCase("SENT")) {
				retunFolder = "sentitems";
			}else if(mailFolder.equalsIgnoreCase("DRAFT")) {
				retunFolder = "drafts";
			}else if(mailFolder.equalsIgnoreCase("TRASH")) {
				retunFolder = "deleteditems";
			}
		}else if(emailWorkSpace.getFromCloud().equals(emailWorkSpace.getToCloud())) {
			retunFolder = retunFolder.replace(" ", "").trim();
		}
		return retunFolder ;

	}
	public Map<String,String>getMemberDetails(String userId,String adminCloudId){
		MemberDetails memberDetails = connectorService.getCloudsRepoImpl().findMemberDetails(userId, adminCloudId);
		Map<String, String> resultMap = new HashMap<>();
		if(memberDetails!=null) {
			for(String member : memberDetails.getMembers()) {
				if(!resultMap.containsKey(member.split(Const.HASHTAG)[0])) {
					resultMap.put(member.split(Const.HASHTAG)[0], member.split(Const.HASHTAG)[1]);
				}
			}
		}
		return resultMap;
	}

}
