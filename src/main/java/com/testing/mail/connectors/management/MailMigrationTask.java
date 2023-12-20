package com.testing.mail.connectors.management;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.testing.mail.connectors.factory.MailServiceFactory;
import com.testing.mail.connectors.microsoft.data.AttachmentsData;
import com.testing.mail.dao.entities.ConnectFlags;
import com.testing.mail.dao.entities.EmailFlagsInfo;
import com.testing.mail.exceptions.MailMigrationException;
import com.testing.mail.repo.entities.Clouds;
import com.testing.mail.repo.entities.EmailInfo;
import com.testing.mail.repo.entities.EmailWorkSpace;
import com.testing.mail.repo.entities.Clouds.CLOUD_NAME;
import com.testing.mail.repo.entities.EmailWorkSpace.PROCESS;
import com.testing.mail.service.DBConnectorService;
import com.testing.mail.utils.MappingUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MailMigrationTask implements Runnable {

	private MailServiceFactory mailServiceFactory;
	private DBConnectorService connectorService; 
	EmailWorkSpace emailWorkSpace;
	EmailInfo emailInfo;


	public MailMigrationTask(MailServiceFactory mailServiceFactory, DBConnectorService connectorService,
			EmailWorkSpace emailWorkSpace, EmailInfo emailInfo) {
		this.mailServiceFactory = mailServiceFactory;
		this.connectorService = connectorService;
		this.emailWorkSpace = emailWorkSpace;
		this.emailInfo = emailInfo;
	}

	@Override
	public void run() {
		initiateMigration();
		log.info("***LEAVING THREAD***"+Thread.currentThread().getName());
		return ;
	}

	private void initiateMigration() {
		log.info("===Entered for fetching the mails/Mail Folders for=="+emailWorkSpace.getId()+"==fromFolder:="+emailInfo.getMailFolder()+"==Id:=="+emailInfo.getId()+"==workSpaceId=="+emailWorkSpace.getId()+"==Started ON=="+new Date(System.currentTimeMillis()));
		emailWorkSpace.setProcessStatus(PROCESS.IN_PROGRESS);
		emailInfo.setProcessStatus(EmailInfo.PROCESS.IN_PROGRESS);
		emailInfo.setThreadBy(Thread.currentThread().getName());
		connectorService.getEmailInfoRepoImpl().save(emailInfo);
		emailWorkSpace = connectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
		Clouds fromCloud = connectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getFromCloudId());
		Clouds toCloud = connectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getToCloudId());
		if(ObjectUtils.isEmpty(fromCloud) || ObjectUtils.isEmpty(toCloud) || StringUtils.isEmpty(emailInfo.getSourceId())) {
			emailInfo.setErrorDescription("required values for migration are null please check");
			emailInfo.setProcessStatus(EmailInfo.PROCESS.CONFLICT);
			return;
		}
		emailInfo.setJobId(emailWorkSpace.getJobId());
		EmailFlagsInfo emailFlagsInfo = new EmailFlagsInfo();
		emailFlagsInfo.setAdminMemberId(fromCloud.getAdminMemberId());
		emailFlagsInfo.setCloudId(emailWorkSpace.getToCloudId());
		emailFlagsInfo.setUserId(fromCloud.getUserId());
		emailFlagsInfo.setEmailId(emailInfo.getFromMail());
		emailFlagsInfo.setTo(emailInfo.getToMail());
		emailFlagsInfo.setCc(emailInfo.getCc());
		emailFlagsInfo.setReplyTo(emailInfo.getReplyTo());
		emailFlagsInfo.setBcc(emailInfo.getBcc());
		if(emailWorkSpace.isDeltaMigration()) {
			emailFlagsInfo.setThreadId(emailInfo.getThreadId());
		}
		emailFlagsInfo.setRead(emailInfo.isRead());
		emailFlagsInfo.setFrom(emailInfo.getFromMail());
		emailFlagsInfo.setSubject(emailInfo.getSubject());
		emailFlagsInfo.setHadAttachments(emailInfo.isAttachMents());
		emailFlagsInfo.setMetaData(emailInfo.isMetadata());
		emailFlagsInfo.setImportance(emailInfo.getImportance());
		emailFlagsInfo.setFlagged(emailInfo.isFlagged());
		emailFlagsInfo.setHtmlContent(emailInfo.isHtmlContent());
		emailFlagsInfo.setHtmlMessage(emailInfo.getHtmlBodyContent());
		emailFlagsInfo.setBodyPreview(emailInfo.getBodyPreview());
		emailFlagsInfo.setDraft(emailInfo.isDraft());
		emailFlagsInfo.setMessage(emailInfo.getHtmlBodyContent());
		emailFlagsInfo.setSubFolder(emailInfo.isSubFolder());
		emailFlagsInfo.setThreadId(CLOUD_NAME.OUTLOOK.equals(emailWorkSpace.getToCloud())?setDestConvIdForOutlook():emailInfo.getDestThreadId());

		emailFlagsInfo.setThread(emailInfo.getOrder()>0);
		if(emailInfo.getDestFolderPath()!=null && !emailInfo.getDestFolderPath().equals("/")) {
			emailFlagsInfo.setFolder(emailInfo.getDestFolderPath().contains("CATEGORY")?emailInfo.getMailFolder():emailInfo.getDestFolderPath());
		}else {
			emailFlagsInfo.setFolder(emailInfo.getMailFolder());
			emailInfo.setDestFolderPath(emailInfo.getMailFolder());
		}
		
		mailFolderMapping(emailFlagsInfo);
		emailFlagsInfo.setCreatedTime(emailInfo.getMailCreatedTime());
		emailFlagsInfo.setSentTime(emailInfo.getMailModifiedTime());
		emailFlagsInfo.setDestId(emailInfo.getDestId());
		emailFlagsInfo.setFromName(emailInfo.getFromName());
		emailFlagsInfo.setFlagged(emailInfo.isFlagged());
		emailFlagsInfo.setRead(emailInfo.isRead());
		emailFlagsInfo.setLabels(Arrays.asList(emailInfo.getMovedFolder()));
		emailFlagsInfo.setParentFolderId(emailInfo.getDestParent());
		//added to Stop fetching source calendar notification emails
		emailFlagsInfo.setStopCalendarSource(emailWorkSpace.isCalendar());
		emailFlagsInfo.setOrder(emailInfo.getOrder());
		String message = "COMPLETED";
		try {
			EmailInfo created = null;
			emailFlagsInfo.setCustomLabel(MappingUtils.isCustomFolder(emailFlagsInfo.getFolder()));
			emailFlagsInfo.setGCombo(isGoogleCombo());
			
			 if(!emailInfo.isDeleted()){

				if(emailInfo.isAttachMents() || (emailInfo.getAttachmentIds()!=null && !emailInfo.getAttachmentIds().isEmpty())) {
					try {
						emailFlagsInfo.setFolder(emailInfo.getDestFolderPath());
						emailFlagsInfo.setCloudId(emailWorkSpace.getFromCloudId());
						emailFlagsInfo.setId(emailInfo.getSourceId());

						if(emailWorkSpace.getFromCloud().equals(CLOUD_NAME.GMAIL) && emailInfo.getAttachmentIds()!=null && !emailInfo.getAttachmentIds().isEmpty()) {
							List<AttachmentsData> attacs = new ArrayList<>();
							for(String data : emailInfo.getAttachmentIds()) {
								AttachmentsData attach = new AttachmentsData();
								attach.setId(data.split(":")[0]);
								attach.setName(data.split(":")[1]);
								attach.setContentType(data.split(":")[2]);
								attach.setSize(Long.valueOf(data.split(":")[3]));
								try {
									attach.setInline(Boolean.valueOf(data.split(":")[4]));
									attach.setCompleted(Boolean.valueOf(data.split(":")[5]));
								} catch (Exception e) {
								}
								attacs.add(attach);
							}
							emailFlagsInfo.setAttachments(attacs);
						}
					} catch (Exception e) {
						emailInfo.setErrorDescription(ExceptionUtils.getStackTrace(e));
					}
					try {
						List<AttachmentsData> attachments = mailServiceFactory.getConnectorService(fromCloud.getCloudName()).getAttachments(emailFlagsInfo);
						emailFlagsInfo.setAttachments(attachments);
					} catch (Exception e) {
						log.error(ExceptionUtils.getStackTrace(e));
						emailFlagsInfo.setAttachments(null);
					}
				}
				// added for custom folders as we can't fetch by name so going by id // issue for the drafts in this scenario for custom folders need to check in diff way
				mailFolderMapping(emailFlagsInfo);
				emailFlagsInfo.setFolder(emailInfo.getDestFolderPath());
				emailFlagsInfo.setCloudId(emailWorkSpace.getToCloudId());
				if(MappingUtils.isGoogleCombo(emailWorkSpace.getToCloud()) && !MappingUtils.isCustomFolder(emailFlagsInfo.getFolder())) {
					emailFlagsInfo.setFolder(emailFlagsInfo.getFolder().toUpperCase());
				}
				created = mailServiceFactory.getConnectorService(toCloud.getCloudName()).sendEmail(emailFlagsInfo);
				emailInfo.setLargeFile(emailFlagsInfo.isLargeFile());
				if(emailFlagsInfo.getAttachments()!=null && !emailFlagsInfo.getAttachments().isEmpty()) {
					try {
						List<String> attachs = new ArrayList<>();
						for(AttachmentsData data : emailFlagsInfo.getAttachments()) {
							attachs.add(data.getId()+":"+data.getName()+":"+data.getContentType()+":"+data.getSize()+":"+data.isInline()+":"+data.isCompleted());
						}
						emailInfo.setAttachmentIds(attachs);
					} catch (Exception e) {
						log.info(ExceptionUtils.getStackTrace(e));
						emailInfo.setErrorDescription(ExceptionUtils.getStackTrace(e));
					}
				}
				emailInfo.setCreated(true);
			}else {
				//For Deleting the mail from inbox directly moving to deletedItems
				emailFlagsInfo.setCloudId(emailWorkSpace.getToCloudId());
				String folder= emailInfo.getMovedFolder();
				if(emailInfo.getMovedFolder()!=null) {
					folder = mailFolderMapping(folder);
				}
				emailFlagsInfo.setThreadId(emailInfo.getDestThreadId());
				emailFlagsInfo.setFolder(folder);
				emailFlagsInfo.setDestId(emailInfo.getDestId());
				created = new EmailInfo();
				created.setId(emailInfo.getDestId());
				if(emailInfo.getMailFolder()!=null) {
					created.setMailFolder(mailFolderMapping(emailInfo.getMailFolder()));
				}
				created = mailServiceFactory.getConnectorService(toCloud.getCloudName()).moveEmails(emailFlagsInfo, created);
				created.setDeleted(true);
			}
			if(created!=null) {
				emailInfo.setConvIndex(created.getConvIndex());
				//For drafts we are not updating the metadata so the largeFileUpload won't happen 
				if(emailInfo.isDraft() && emailInfo.isLargeFile()) {
					uploadLargeFile(created,emailFlagsInfo.getFolder(),emailFlagsInfo.getAttachments());
				}
				emailInfo.setDestThreadId(created.getThreadId());
				if(!emailInfo.isFolder() && !emailInfo.isDraft()&&!emailWorkSpace.getToCloud().equals(CLOUD_NAME.GMAIL)) {
					emailInfo.setProcessStatus(EmailInfo.PROCESS.METADATA_STARTED);
				}else {
					emailInfo.setProcessStatus(EmailInfo.PROCESS.PROCESSED);
				}
				emailInfo.setErrorDescription(message);
				emailInfo.setDestId(created.getId());
				//Added for the updating the destThreadId based on the sourceThreadId
				if(CLOUD_NAME.GMAIL.equals(emailWorkSpace.getToCloud())) {
					connectorService.getEmailInfoRepoImpl().updateEmailInfoForDestThreadIdForGmailDest(emailWorkSpace.getId(), emailInfo.getThreadId(), created.getThreadId(),emailInfo.getOrder(),emailInfo.getConvIndex());
				}
				if(emailInfo.isFolder()) {
					connectorService.getEmailInfoRepoImpl().updateEmailInfoForDestChildFolder(emailWorkSpace.getId(), emailInfo.getMailFolder(),emailInfo.getDestId());
				}
				emailInfo.setDeleted(created.isDeleted());
			}else {
				emailInfo.setProcessStatus(EmailInfo.PROCESS.CONFLICT);
				emailInfo.setRetryCount(emailInfo.getRetryCount()+1);
				emailInfo.setErrorDescription("NOT_CREATED");
			}
		}catch(Exception e) {
			emailInfo.setProcessStatus(EmailInfo.PROCESS.CONFLICT);
			emailInfo.setRetryCount(emailInfo.getRetryCount()+1);
			emailInfo.setErrorDescription(ExceptionUtils.getStackTrace(e));
		}
		finally {
			connectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
			if(emailInfo.getProcessStatus().equals(EmailInfo.PROCESS.IN_PROGRESS)) {
				emailInfo.setProcessStatus(EmailInfo.PROCESS.CONFLICT);
				emailInfo.setErrorDescription("ExceptionOccured");
			}
			if(emailInfo.isDraft()) {
				emailInfo.setAdminDeleted(true);
			}
			connectorService.getEmailInfoRepoImpl().save(emailInfo);
		}
	}


	private void mailFolderMapping(EmailFlagsInfo emailFlagsInfo) {
		if(CLOUD_NAME.OUTLOOK.equals(emailWorkSpace.getFromCloud())) {
			return;
		}
		String folder = MappingUtils.mapMailFolder(emailFlagsInfo.getFolder(), emailWorkSpace.getFromCloud(), emailWorkSpace.getToCloud());
		emailFlagsInfo.setFolder(folder);
		emailInfo.setDestFolderPath(folder);
	}



	private String mailFolderMapping(String folder) {
		String mappedFolder = folder;
		mappedFolder = MappingUtils.mapMailFolder(folder, emailWorkSpace.getFromCloud(), emailWorkSpace.getToCloud());
		return mappedFolder;
	}

	ConnectFlags createFlagsFromAdmin(Clouds adminCloud) {
		ConnectFlags connectFlags = new ConnectFlags();
		connectFlags.setAccessToken(adminCloud.getCredential().getAccessToken());
		connectFlags.setAdminMemberId(adminCloud.getAdminMemberId());
		connectFlags.setEmailId(adminCloud.getEmail());
		connectFlags.setUserId(adminCloud.getUserId());
		return connectFlags;
	}

	/**
	 * Uploading largeFile More than <b>3 MB </b>
	 * @throws IOException 
	 */
	private void uploadLargeFile(EmailInfo metadata,String mailFolder,List<AttachmentsData>datas) throws IOException {
		try {
			EmailFlagsInfo emailFlagsInfo = new EmailFlagsInfo();
			emailFlagsInfo.setCloudId(emailWorkSpace.getFromCloudId());
			emailFlagsInfo.setId(emailInfo.getSourceId());
			emailFlagsInfo.setLargeFile(true);
			if(emailInfo.getAttachmentIds()!=null && !emailInfo.getAttachmentIds().isEmpty() ) {
				List<AttachmentsData> attacs = new ArrayList<>();
				for(String data : emailInfo.getAttachmentIds()) {
					try {
						AttachmentsData attach = new AttachmentsData();
						attach.setId(data.split(":")[0]);
						attach.setName(data.split(":")[1]);
						attach.setContentType(data.split(":")[2]);
						attacs.add(attach);
					} catch (Exception e) {
						log.info(ExceptionUtils.getStackTrace(e));
					}
				}
				emailFlagsInfo.setAttachments(attacs);
			}
			if(datas==null || datas.isEmpty())
			{
				List<AttachmentsData> attachments = mailServiceFactory.getConnectorService(emailWorkSpace.getFromCloud()).getAttachments(emailFlagsInfo);
				emailFlagsInfo.setAttachments(attachments);
			}else {
				emailFlagsInfo.setAttachments(datas);
			}
			emailFlagsInfo.setId(metadata.getId());
			emailFlagsInfo.setFolder(mailFolder);
			emailFlagsInfo.setCloudId(emailWorkSpace.getToCloudId());
			emailFlagsInfo.setFolder(emailInfo.getMailFolder());
			List<String> attaches = mailServiceFactory.getConnectorService(emailWorkSpace.getToCloud()).addAttachment(emailFlagsInfo, false);
			if(attaches!=null) {
				emailInfo.getAttachmentIds().addAll(attaches);
				log.info("===== Large File upload is succeessFull==For=="+emailWorkSpace.getToCloud()+"==Cloud=="+emailWorkSpace.getId());
			}
		} catch (MailMigrationException e) {
			log.error(ExceptionUtils.getStackTrace(e));
		}
	}

	private boolean isGoogleCombo() {
		return emailWorkSpace.getFromCloud().equals(CLOUD_NAME.GMAIL) && emailWorkSpace.getToCloud().equals(CLOUD_NAME.GMAIL);
	}

	private String setDestConvIdForOutlook() {
		if(CLOUD_NAME.OUTLOOK.equals(emailWorkSpace.getToCloud()) && emailInfo.getOrder()>0) {
			EmailInfo info = connectorService.getEmailInfoRepoImpl().getDestThreadIdBasedOnSourceId(emailInfo.getEmailWorkSpaceId(), emailInfo.getThreadId());
			if(info!=null) {
				return info.getDestId();
			}
		}
		return null;
	}	
}


