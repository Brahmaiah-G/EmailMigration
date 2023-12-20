package com.cloudfuze.mail.connectors.management;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.cloudfuze.mail.connectors.factory.MailServiceFactory;
import com.cloudfuze.mail.connectors.impl.OutLookMailConnector;
import com.cloudfuze.mail.connectors.microsoft.data.AttachmentsData;
import com.cloudfuze.mail.dao.entities.ConnectFlags;
import com.cloudfuze.mail.dao.entities.EmailFlagsInfo;
import com.cloudfuze.mail.exceptions.MailMigrationException;
import com.cloudfuze.mail.repo.entities.Clouds;
import com.cloudfuze.mail.repo.entities.Clouds.CLOUD_NAME;
import com.cloudfuze.mail.repo.entities.EmailInfo;
import com.cloudfuze.mail.repo.entities.EmailWorkSpace;
import com.cloudfuze.mail.repo.entities.EmailWorkSpace.PROCESS;
import com.cloudfuze.mail.service.DBConnectorService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MailDraftsCreationTask implements Runnable {

	private MailServiceFactory mailServiceFactory;
	private DBConnectorService connectorService; 
	EmailWorkSpace emailWorkSpace;
	List<EmailInfo> emailInfos;


	public MailDraftsCreationTask(MailServiceFactory mailServiceFactory, DBConnectorService connectorService,
			EmailWorkSpace emailWorkSpace, List<EmailInfo> emailInfo) {
		this.mailServiceFactory = mailServiceFactory;
		this.connectorService = connectorService;
		this.emailWorkSpace = emailWorkSpace;
		this.emailInfos = emailInfo;
	}

	@Override
	public void run() {
		initiateMigration();
		log.info("***LEAVING THREAD***"+Thread.currentThread().getName());
		return ;
	}

	private void initiateMigration() {
		emailWorkSpace.setProcessStatus(PROCESS.IN_PROGRESS);
		emailWorkSpace = connectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
		long count = 0;
		List<EmailInfo> infosList = new ArrayList<>();
		try {
			List<EmailFlagsInfo> emailFlagsInfos = new ArrayList<>();
			Map<String,EmailInfo> infoMap = new HashMap<>();
			List<String>ids = new ArrayList<>();
			for(EmailInfo emailInfo : emailInfos) {
				count = emailInfo.getRetryCount();
				ids.add(emailInfo.getId());
				log.info("===Entered for Draft creation for=="+emailWorkSpace.getId()+"==fromFolder:="+emailInfo.getMailFolder()+"==Id:=="+emailInfo.getId()+"==workSpaceId=="+emailWorkSpace.getId()+"==Started ON=="+new Date(System.currentTimeMillis()));
				EmailFlagsInfo emailFlagsInfo = new EmailFlagsInfo();
				emailFlagsInfo.setAdminMemberId(emailWorkSpace.getFromCloudId());
				emailFlagsInfo.setCloudId(emailWorkSpace.getToCloudId());
				emailFlagsInfo.setCopy(emailWorkSpace.isCopy());
				emailFlagsInfo.setUserId(emailWorkSpace.getUserId());
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
				emailFlagsInfo.setThreadId(emailInfo.getDestThreadId());
				emailFlagsInfo.setConvIndex(emailInfo.getConvIndex());
				emailFlagsInfo.setThread(emailInfo.getOrder()>0);
				emailFlagsInfo.setId(emailInfo.getId());
				emailFlagsInfo.setCopy(emailWorkSpace.isCopy());
				emailFlagsInfo.setCreatedTime(emailInfo.getMailCreatedTime());
				emailFlagsInfo.setSentTime(emailInfo.getMailModifiedTime());
				emailFlagsInfo.setDestId(emailInfo.getDestId());
				emailFlagsInfo.setFromName(emailInfo.getFromName());
				emailFlagsInfo.setFlagged(emailInfo.isFlagged());
				emailFlagsInfo.setRead(emailInfo.isRead());
				emailFlagsInfo.setLabels(Arrays.asList(emailInfo.getMovedFolder()));
				emailFlagsInfo.setEmailId(emailInfo.getEnv());
				emailFlagsInfo.setParentFolderId(emailInfo.getDestParent());
				//added to Stop fetching source calendar notification emails
				emailFlagsInfo.setStopCalendarSource(emailWorkSpace.isCalendar());
				emailFlagsInfo.setOrder(emailInfo.getOrder());
				emailFlagsInfo.setDeltaThread(emailInfo.isDeltaThread());
				if(emailInfo.isDraft()) {
					emailInfo.setAdminDeleted(true);
				}
				emailFlagsInfo.setFolder(emailInfo.getDestFolderPath());
				try {
					//if(!emailInfo.isDeleted()){
					if(emailInfo.isAttachMents() || (emailInfo.getAttachmentIds()!=null && !emailInfo.getAttachmentIds().isEmpty())) {
						setAttachments(emailInfo, emailFlagsInfo);
					}
					emailFlagsInfo.setFolder(emailInfo.getMailFolder());
					emailFlagsInfo.setCloudId(emailWorkSpace.getToCloudId());
					emailFlagsInfo.setId(emailInfo.getId());
					emailFlagsInfos.add(emailFlagsInfo);
					infoMap.put(emailInfo.getId(), emailInfo);
					//}

				}catch(Exception e) {
					emailInfo.setProcessStatus(EmailInfo.PROCESS.DRAFT_CREATION_CONFLICT);
					emailInfo.setRetryCount(emailInfo.getRetryCount()+1);
					emailInfo.setErrorDescription(ExceptionUtils.getStackTrace(e));
					connectorService.getEmailInfoRepoImpl().save(emailInfo);
				}
			}

			EmailFlagsInfo flagsInfo = new EmailFlagsInfo();
			flagsInfo.setCloudId(emailWorkSpace.getToCloudId());
			List<EmailFlagsInfo> createdInfos = new ArrayList<>();
			try {
				createdInfos = ((OutLookMailConnector)mailServiceFactory.getConnectorService(Clouds.CLOUD_NAME.OUTLOOK)).creatDraftBatchRequest(emailFlagsInfos, flagsInfo);
			} catch (Exception e1) {
				log.error(ExceptionUtils.getStackTrace(e1));
				connectorService.getEmailInfoRepoImpl().findAndUpdateByWorkSpace(emailWorkSpace.getId(), EmailInfo.PROCESS.DRAFT_CREATION_CONFLICT.name(), ExceptionUtils.getStackTrace(e1), ids,count);
				ids.clear();
				return;
			}
			for(EmailFlagsInfo info : createdInfos) {
				EmailInfo mapInfo = infoMap.get(info.getId());
				if(null!=info.getMessage()) {
					mapInfo.setExtraField(info.getMessage());
				}
				if(mapInfo!=null) {
					mapInfo.setPreviousFrom(info.getFrom());
				}
				log.info("==after draft creation--conflict status :--"+info.isConflict()+"=="+mapInfo.getId());
				try {
					if(info!=null && !info.isConflict()) {
						mapInfo.setConvIndex(info.getConvIndex());
						//For drafts we are not updating the metadata so the largeFileUpload won't happen 
						mapInfo.setLargeFile(info.isLargeFile());

						if(mapInfo.isLargeFile()) {
							uploadLargeFile(info,flagsInfo.getFolder(),info.getAttachments(),mapInfo);
						}
						mapInfo.setCreated(true);
						mapInfo.setDestThreadId(info.getThreadId());
						if(!mapInfo.isDraft()) {
							mapInfo.setProcessStatus(EmailInfo.PROCESS.DRAFT_NOTPROCESSED);
							mapInfo.setThreadBy(null);
						}else {
							mapInfo.setProcessStatus(EmailInfo.PROCESS.PROCESSED);
						}
						mapInfo.setErrorDescription(EmailInfo.PROCESS.DRAFT_CREATED.name());
						mapInfo.setDestParent(info.getDestParent());
						mapInfo.setDestId(info.getDestId());
					}else {
						mapInfo.setProcessStatus(EmailInfo.PROCESS.DRAFT_CREATION_CONFLICT);
						mapInfo.setRetryCount(mapInfo.getRetryCount()+1);
						mapInfo.setErrorDescription(info.getMessage()==null?"NOT_CREATED":info.getMessage());
						EmailFlagsInfo finfo = new EmailFlagsInfo();
						finfo.setCloudId(emailWorkSpace.getToCloudId());
						finfo.setId(info.getDestId());
						finfo.setConvIndex(info.getConvIndex());
						finfo.setFrom(mapInfo.getFromMail());
						try {
							mailServiceFactory.getConnectorService(Clouds.CLOUD_NAME.OUTLOOK).deleteEmails(finfo, false);
						} catch (Exception e) {
							log.error(ExceptionUtils.getStackTrace(e));
						}
					}
					if(EmailInfo.PROCESS.DRAFT_CREATION_INPROGRESS.equals(mapInfo.getProcessStatus())){
						mapInfo.setProcessStatus(EmailInfo.PROCESS.DRAFT_CREATION_CONFLICT);
					}
					if(mapInfo.isDraft()) {
						mapInfo.setAdminDeleted(true);
					}
				} catch (Exception e) {
					log.error(ExceptionUtils.getStackTrace(e));
					mapInfo.setProcessStatus(EmailInfo.PROCESS.DRAFT_CREATION_CONFLICT);
					mapInfo.setErrorDescription(ExceptionUtils.getStackTrace(e));
					EmailFlagsInfo finfo = new EmailFlagsInfo();
					finfo.setCloudId(emailWorkSpace.getToCloudId());
					finfo.setId(info.getDestId());
					finfo.setConvIndex(info.getConvIndex());
					finfo.setFrom(mapInfo.getFromMail());
					try {
						mailServiceFactory.getConnectorService(Clouds.CLOUD_NAME.OUTLOOK).deleteEmails(finfo, false);
					}catch(Exception e1) {
						log.error(ExceptionUtils.getStackTrace(e1));
					}
				}
				log.info("==after draft creation Saving the Record--conflict status :--"+info.isConflict()+"=="+mapInfo.getId()+"--processStatus--"+mapInfo.getProcessStatus());
				infosList.add(mapInfo);
			}
			connectorService.getEmailInfoRepoImpl().save(infosList);
			connectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
			ids.clear();
		} catch (Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
			emailWorkSpace.setErrorDescription(ExceptionUtils.getStackTrace(e));
			connectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
		}
	}

	private void setAttachments(EmailInfo emailInfo, EmailFlagsInfo emailFlagsInfo) {
		try {
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
				try {
					List<AttachmentsData> attachments = mailServiceFactory.getConnectorService(emailWorkSpace.getFromCloud()).getAttachments(emailFlagsInfo);
					emailFlagsInfo.setAttachments(attachments);
				} catch (Exception e) {
					log.error(ExceptionUtils.getStackTrace(e));
					emailFlagsInfo.setAttachments(null);
				}
			}
		} catch (Exception e) {
			emailInfo.setErrorDescription(ExceptionUtils.getStackTrace(e));
		}
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
	private void uploadLargeFile(EmailFlagsInfo metadata,String mailFolder,List<AttachmentsData>datas,EmailInfo emailInfo) throws IOException {
		try {
			EmailFlagsInfo emailFlagsInfo = new EmailFlagsInfo();
			emailFlagsInfo.setCloudId(emailWorkSpace.getFromCloudId());
			emailFlagsInfo.setId(emailInfo.getSourceId());
			emailFlagsInfo.setFrom(emailInfo.getFromMail());
			emailFlagsInfo.setCopy(emailWorkSpace.isCopy());
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
			if(datas==null || datas.isEmpty()){
				List<AttachmentsData> attachments = mailServiceFactory.getConnectorService(emailWorkSpace.getFromCloud()).getAttachments(emailFlagsInfo);
				emailFlagsInfo.setAttachments(attachments);
			}else {
				emailFlagsInfo.setAttachments(datas);
			}
			emailFlagsInfo.setId(metadata.getDestId());
			emailFlagsInfo.setFolder(mailFolder);
			emailFlagsInfo.setCloudId(emailWorkSpace.getToCloudId());
			List<String> attaches = mailServiceFactory.getConnectorService(emailWorkSpace.getToCloud()).addAttachment(emailFlagsInfo, false);
			if(attaches!=null) {
				emailInfo.getAttachmentIds().addAll(attaches);
				log.info("===== Large File upload is succeessFull==For=="+emailWorkSpace.getToCloud()+"==Cloud=="+emailWorkSpace.getId());
			}
		} catch (MailMigrationException e) {
			log.error(ExceptionUtils.getStackTrace(e));
		}
	}
}


