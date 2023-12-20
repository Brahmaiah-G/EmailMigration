package com.cloudfuze.mail.connectors.management;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.cloudfuze.mail.connectors.factory.MailServiceFactory;
import com.cloudfuze.mail.connectors.management.utility.ConnectorUtility;
import com.cloudfuze.mail.connectors.microsoft.data.AttachmentsData;
import com.cloudfuze.mail.constants.Const;
import com.cloudfuze.mail.constants.ExceptionConstants;
import com.cloudfuze.mail.contacts.dao.ContactsFlagInfo;
import com.cloudfuze.mail.contacts.entities.Contacts;
import com.cloudfuze.mail.contacts.entities.Emails;
import com.cloudfuze.mail.contacts.entities.PhoneNumbers;
import com.cloudfuze.mail.dao.entities.ConnectFlags;
import com.cloudfuze.mail.dao.entities.EMailRules;
import com.cloudfuze.mail.dao.entities.EmailFlagsInfo;
import com.cloudfuze.mail.dao.entities.EmailUserSettings;
import com.cloudfuze.mail.dao.entities.PermissionCache;
import com.cloudfuze.mail.dao.entities.UserGroups;
import com.cloudfuze.mail.exceptions.MailMigrationException;
import com.cloudfuze.mail.repo.entities.Clouds;
import com.cloudfuze.mail.repo.entities.Clouds.CLOUD_NAME;
import com.cloudfuze.mail.repo.entities.ContactsInfo;
import com.cloudfuze.mail.repo.entities.EmailFolderInfo;
import com.cloudfuze.mail.repo.entities.EmailInfo;
import com.cloudfuze.mail.repo.entities.EmailPickingQueue;
import com.cloudfuze.mail.repo.entities.EmailWorkSpace;
import com.cloudfuze.mail.repo.entities.GroupEmailDetails;
import com.cloudfuze.mail.repo.entities.MailChangeIds;
import com.cloudfuze.mail.repo.entities.PROCESS;
import com.cloudfuze.mail.repo.entities.ThreadControl;
import com.cloudfuze.mail.service.DBConnectorService;
import com.cloudfuze.mail.utils.ConvertionUtils;
import com.cloudfuze.mail.utils.MappingUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>
<b>Operations</b> : Picking the OeTime Mails based on Mailfolder(Inbox,sent,Drafts,Trash,Junk)
</p>
 *@see com.cloudfuze.mail.connectors.scheduler.MailPickerScheduler &#64;MailPickerScheduler
 */

@Slf4j
public class MailPickingV3 implements Callable<EmailFolderInfo> {

	private MailServiceFactory mailServiceFactory;
	private DBConnectorService connectorService; 
	EmailWorkSpace emailWorkSpace;
	EmailFolderInfo emailInfo;
	Map<String, String> mappedPairs = new HashMap<>();
	Map<String,String> mapping = new HashMap<>();
	Clouds fromCloud = null;
	Clouds toCloud = null;
	Clouds fromAdminCloud = null;
	Clouds toAdminCloud = null;

	public MailPickingV3(MailServiceFactory mailServiceFactory, DBConnectorService connectorService,
			EmailWorkSpace emailWorkSpace, EmailFolderInfo emailInfo) {
		this.mailServiceFactory = mailServiceFactory;
		this.connectorService = connectorService;
		this.emailWorkSpace = emailWorkSpace;
		this.emailInfo = emailInfo;
	}

	@Override
	public EmailFolderInfo call() {
		log.info("==***STARTING the Thread=="+Thread.currentThread().getName());
		mapping.put("INBOX", "inbox");
		mapping.put("SENT", "sentitems");
		mapping.put("DRAFT", "drafts");
		mapping.put("STARRED", "inbox");
		mapping.put("UNREAD", "inbox");
		mapping.put("TRASH", "deleteditems");
		mapping.put("Archive", "INBOX");
		mapping.put("DeletedItems", "TRASH");
		mapping.put("SPAM", "junkemail");
		mapping.put("JunkEmail", "SPAM");
		mapping.put("Inbox", "INBOX");
		mapping.put("SentItems", "SENT");
		mapping.put("Outbox", "SENT");
		mapping.put("Drafts", "DRAFT");

		initiateMigration();
		log.info("==***LEAVING the Thread=="+Thread.currentThread().getName());
		return emailInfo;

	}

	private void initiateMigration() {
		log.info("===Entered for fetching the Mails/Mail Folders for=="+emailWorkSpace.getId()+"==from mail=="+emailWorkSpace.getFromMailId()+"==toMail ID=="+emailWorkSpace.getToMailId()+"==fromFolder:="+emailWorkSpace.getFromFolderId());
		emailWorkSpace.setProcessStatus(EmailWorkSpace.PROCESS.IN_PROGRESS);
		emailWorkSpace = connectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
		fromCloud = connectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getFromCloudId());
		fromAdminCloud = connectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getFromAdminCloud());
		toAdminCloud = connectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getToAdminCloud());
		toCloud = connectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getToCloudId());
		if(ObjectUtils.isEmpty(fromCloud) || ObjectUtils.isEmpty(toCloud) || StringUtils.isEmpty(emailInfo.getSourceId())) {
			emailInfo.setErrorDescription("required values for migration are null please check");
			emailInfo.setRetryCount(10);
			emailInfo.setProcessStatus(PROCESS.CONFLICT);
			return;
		}

		if(EmailInfo.PROCESS.NOT_STARTED.name().equals(emailInfo.getProcessStatus().name())) {
			return;
		}

		emailInfo.setProcessStatus(PROCESS.IN_PROGRESS);
		emailInfo.setThreadBy(Thread.currentThread().getName());
		emailInfo = connectorService.getEmailFolderInfoRepoImpl().save(emailInfo);

		mappedPermissions();

		EmailFlagsInfo emailFlagsInfo = new EmailFlagsInfo();
		emailFlagsInfo.setGCombo(isGoogleCombo());
		emailFlagsInfo.setAdminMemberId(fromCloud.getAdminMemberId());
		emailFlagsInfo.setCloudId(emailWorkSpace.getFromCloudId());
		emailFlagsInfo.setUserId(fromCloud.getUserId());
		emailFlagsInfo.setEmailId(emailWorkSpace.getFromMailId());
		emailFlagsInfo.setId(emailInfo.getSourceId());
		if(emailWorkSpace.isCalendar()) {
			emailFlagsInfo.setStopCalendarSource(true);
		}
		try {
			if(!"/".equals(emailInfo.getSourceId())) {
				createMailFolder();
			}
			getMails(emailFlagsInfo);
		}catch(Exception e) {
			emailInfo.setNextPageToken(emailFlagsInfo.getNextPageToken());
			emailInfo.setProcessStatus(PROCESS.CONFLICT);
			emailInfo.setRetryCount(emailInfo.getRetryCount()+1);
			emailInfo.setErrorDescription(ExceptionUtils.getStackTrace(e));
		}
		finally {
			if(emailInfo.getProcessStatus().equals(PROCESS.IN_PROGRESS)) {
				emailInfo.setProcessStatus(PROCESS.NOT_PROCESSED);
			}
			connectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
			connectorService.getEmailFolderInfoRepoImpl().save(emailInfo);
		}
	}

	private void getMails(EmailFlagsInfo emailFlagsInfo) throws Exception {
		boolean hadData = false;
		PROCESS processStatus = PROCESS.NOT_PROCESSED;
		String message = "Completed";
		long count = 0;
		long attachmentsSize = 0;
		log.info("====STARTED ON==="+emailWorkSpace.getId()+"=="+LocalDateTime.now());
		if(emailInfo.getMailFolder()==null) {
			emailInfo.setMailFolder(emailInfo.getSourceId());
		}
		if(StringUtils.isEmpty(emailInfo.getMailFolder())) {
			emailInfo.setMailFolder(emailInfo.getMovedFolder());
		}
		if(MappingUtils.isCustomFolder(emailInfo.getMailFolder())) {
			emailFlagsInfo.setFolder(emailInfo.getSourceId());
		}else {
			emailFlagsInfo.setFolder(emailInfo.getMailFolder());
		}
		if(emailInfo.getMailFolder()!=null && emailInfo.getMailFolder().startsWith("/")) {
			emailInfo.setMailFolder(emailInfo.getMailFolder().replace("/", ""));
		}
		Map<String,Long> threads = new HashMap<>();
		List<String>ids = new ArrayList<>();
		String original = emailInfo.getMailFolder();
		emailFlagsInfo.setCopy(emailWorkSpace.isCopy());
		if(!"/".equals(emailInfo.getSourceId())) {
			emailFlagsInfo.setCustomLabel(MappingUtils.isCustomFolder(emailFlagsInfo.getFolder()));
		}
		do {
			boolean folder = false;
			ThreadControl control = connectorService.getEmailInfoRepoImpl().getThreadControl();
			List<EmailFlagsInfo> emails;
			log.info("==Picking emails for the User==="+emailWorkSpace.getFromMailId()+"===MailFolder==="+emailInfo.getMailFolder()+"==WorkSpaceId=="+emailWorkSpace.getId()+"==Total Fetched UptoNow=="+count);
			emailInfo.setPicking(true);
			emailFlagsInfo.setNextPageToken(emailInfo.getNextPageToken());
			emailFlagsInfo.setFolder(emailInfo.getSourceId());
			emailFlagsInfo.setCloudId(emailWorkSpace.getFromCloudId());
			emailFlagsInfo.setCopy(true);
			if(emailInfo.getSourceId()!=null && "/".equals(emailInfo.getSourceId())) {
				emailFlagsInfo.setFolder(null);
				folder = true;
				emails = mailServiceFactory.getConnectorService(emailWorkSpace.getFromCloud()).getListOfMailFolders(emailFlagsInfo);
			}else {
				emails = mailServiceFactory.getConnectorService(emailWorkSpace.getFromCloud()).getListOfMails(emailFlagsInfo);
			}
			if(!hadData && (ObjectUtils.isEmpty(emails) || (emails!=null && emails.isEmpty()))) {
				processStatus = PROCESS.PROCESSED;
				message = "no Folder mails to migrate";
			}else {
				hadData = true;	
			}
			emailInfo.setNextPageToken(emailFlagsInfo.getNextPageToken());
			emailInfo = connectorService.getEmailFolderInfoRepoImpl().save(emailInfo);
			if(folder) {
				count = count+convertEmailFlagsToEmailFolderInfo(emails,ids,original,emailInfo.getDestId());
			}else{
				count = count+convertEmailFlagsToEmailInfo(emails,attachmentsSize,ids,original,emailInfo.getDestId(),threads);
			}
			emailInfo.setMailFolder(original);
			if(emails.size()<500) {
				processStatus = PROCESS.PROCESSED;
				emails.clear();
				break;
			}
			if(!control.isActive() || control.isStopPicking()) {
				emailInfo.setProcessStatus(PROCESS.NOT_STARTED);
				connectorService.getEmailFolderInfoRepoImpl().save(emailInfo);
				return;
			}
		}while(StringUtils.isNotBlank(emailFlagsInfo.getNextPageToken()));
		ids.clear();
		if(emailInfo.getProcessStatus().equals(PROCESS.PICKING)) {
			processStatus = PROCESS.PROCESSED;
			emailInfo.setProcessStatus(processStatus);
		}
		emailInfo.setTotalCount(count);
		emailInfo.setAttachmentsSize(attachmentsSize);
		if(emailInfo.getSourceId()!=null && ("/".equals(emailWorkSpace.getFromFolderId()) && "/".equals(emailInfo.getSourceId())) && !emailWorkSpace.isPreScan()) {
			pickMailBoxRules(emailFlagsInfo);
			pickEmailSettings(emailFlagsInfo);
			pickContacts();
		}
		emailFlagsInfo.setCloudId(emailWorkSpace.getFromCloudId());
		if(!(emailInfo.getSourceId()==null || "/".equals(emailInfo.getSourceId())) && !emailWorkSpace.isPreScan()) {
			//# adding the change id if the from source we need to fetch the change ids for delta
			log.info("==Entered for generating the delta change id="+emailInfo.getId()+"==folder=="+emailInfo.getMailFolder()+"=="+emailWorkSpace.getId());
			emailFlagsInfo.setFolder(emailInfo.getMailFolder()==null?emailInfo.getSourceId():emailInfo.getMailFolder());
			emailFlagsInfo.setCreatedTime(LocalDateTime.now().toString());
			String latestDeltaId = null;
			if(emailWorkSpace.getFromCloud().equals(CLOUD_NAME.GMAIL)) {
				latestDeltaId = emailFlagsInfo.getParentFolderId();
			}else {
				latestDeltaId = mailServiceFactory.getConnectorService(emailWorkSpace.getFromCloud()).getDeltaChangeId(emailFlagsInfo);
			}
			connectorService.getMailChangeIdRepoImpl().save(createMailChangeIds(emailInfo, latestDeltaId));
			emailInfo.setLatestUpdatedId(latestDeltaId);
		}
		if(emailInfo.getSourceId()!=null && "/".equals(emailInfo.getSourceId())) {
			processStatus = PROCESS.PROCESSED;
		}

		emailInfo.setErrorDescription(message);
		emailInfo.setProcessStatus(processStatus);
		emailInfo.setPicking(false);
		if(emailWorkSpace.isPreScan()) {
			emailInfo.setProcessStatus(PROCESS.PROCESSED);
			emailInfo.setErrorDescription("PreScan");
		}
	}


	/**
	 * As Rules are very less compared to mails so migrating at a time only after picking<br></br>
	 * Migrating Rules
	 * @param EmailFlagsInfo : emailFlags like cloudId and other required details
	 */
	private void pickMailBoxRules(EmailFlagsInfo emailFlagsInfo) {
		List<EMailRules> emailRules;
		if(emailWorkSpace.isMailRules()) {
			try {
				log.info("==Piking the mailBox rules =="+emailWorkSpace.getId());
				emailRules = mailServiceFactory.getConnectorService(emailWorkSpace.getFromCloud()).getMailBoxRules(emailFlagsInfo);
				log.info("==Total Mail Rules found=="+emailRules.size());
				if(emailRules!=null && !emailRules.isEmpty()) {
					for(EMailRules rules : emailRules) {
						boolean onlyCustomFolderRules = emailWorkSpace.isSettings();
						rules.setUserId(emailWorkSpace.getUserId());
						rules.setEmailWorkSpaceId(emailWorkSpace.getId());
						rules.setProcessStatus(PROCESS.NOT_PROCESSED);
						try {
							if(rules.getMailFolder()!=null && !rules.getMailFolder().isEmpty()) {
								boolean customFolder = false;
								for(String mailFolder : rules.getMailFolder()) {
									customFolder = MappingUtils.isCustomFolder(mailFolder);
									rules.setCustomFolder(mailFolder);
									if(customFolder) {
										break;
									}
								}
								if(!customFolder) {
									rules.setProcessStatus(PROCESS.PROCESSED);
									rules.setErrorDescription("No Custom Folder to update Rules");
								}
							}else if(!onlyCustomFolderRules) {
								rules.setProcessStatus(PROCESS.PROCESSED);
							}
						} catch (Exception e) {
							log.error(ExceptionUtils.getStackTrace(e));
							rules.setProcessStatus(PROCESS.CONFLICT);
							rules.setErrorDescription(ExceptionUtils.getStackTrace(e));
						}finally {
							connectorService.getEmailInfoRepoImpl().saveMailBoxRule(rules);
						}
					}
				}
			} catch (Exception e) {
				log.error(ExceptionUtils.getStackTrace(e));
			}
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
	 *Converting the EmailFlagsInfo values to the EmailInfo<p></p> 
	 *Adding the required parameters for the MailMigration
	 *@see EmailInfo
	 *@see EmailFlagsInfo
	 */
	private synchronized long convertEmailFlagsToEmailInfo(List<EmailFlagsInfo> emailFlagsInfos,long attachmentsSize,List<String>ids,
			String originalMailFolder,String destId,Map<String,Long>threads) throws MailMigrationException {
		long totlaCount = 0;
		List<EmailInfo> emailInfos = new ArrayList<>();
		if(emailFlagsInfos!=null && !emailFlagsInfos.isEmpty()) {
			for(EmailFlagsInfo emailFlagsInfo : emailFlagsInfos) {
				try {
					if(emailFlagsInfo.isMailFolder()) {
						convertEmailFlagsToEmailFolderInfo(Arrays.asList(emailFlagsInfo), ids, originalMailFolder, destId);
						continue;
					}
					EmailInfo count = null;
					if(CLOUD_NAME.OUTLOOK.equals(emailWorkSpace.getToCloud())) {
						count = connectorService.getEmailInfoRepoImpl().checkEmailInfosBasedOnIds(emailWorkSpace.isCopy()?emailWorkSpace.getId():emailWorkSpace.getUserId(), emailFlagsInfo.getId(),emailWorkSpace.isCopy());
					}else if(emailFlagsInfo.getOrder()==0 && CLOUD_NAME.OUTLOOK.equals(emailWorkSpace.getFromCloud())) {
						count =  connectorService.getEmailInfoRepoImpl().findByThreadId(emailWorkSpace.getId(), emailFlagsInfo.getThreadId());
						if(count!=null) {
							emailFlagsInfo.setOrder(count.getOrder()+1);
							count = null;
						}
					}
					EmailInfo _emailInfo = new EmailInfo();
					_emailInfo.setFromCloud(emailWorkSpace.getFromCloud().name());
					_emailInfo.setJobId(emailWorkSpace.getJobId());
					_emailInfo.setToCloud(emailWorkSpace.getToCloud().name());
					_emailInfo.setEmailWorkSpaceId(emailWorkSpace.getId());
					_emailInfo.setOriginalMailFolder(emailInfo.getMailFolder());
					String mailFolder = MappingUtils.isGoogleCombo(emailWorkSpace.getFromCloud())?emailFlagsInfo.getFolder():emailInfo.getMailFolder();
					emailInfo.setMailFolder(mailFolder);
					_emailInfo.setUserId(emailWorkSpace.getUserId());
					_emailInfo.setMailFolder(emailFlagsInfo.getFolder());
					_emailInfo.setCreatedTime(LocalDateTime.now());
					_emailInfo.setMailCreatedTime(emailFlagsInfo.getCreatedTime());
					_emailInfo.setMailModifiedTime(emailFlagsInfo.getSentTime());
					_emailInfo.setSubFolder(emailFlagsInfo.isSubFolder());
					_emailInfo.setFolder(emailFlagsInfo.isMailFolder());
					if(emailFlagsInfo.getFolder()==null) {
						emailFlagsInfo.setFolder(emailInfo.getMailFolder());
					}
					_emailInfo.setMovedFolder(emailFlagsInfo.getFolder());
					if(!emailWorkSpace.getFromCloud().equals(emailWorkSpace.getToCloud()) && !_emailInfo.isFolder()) {
						_emailInfo.setMailFolder(mapping.containsKey(emailFlagsInfo.getFolder().replace(" ", ""))?mapping.get(emailFlagsInfo.getFolder().replace(" ", "")):emailInfo.getMailFolder());
					}else {
						_emailInfo.setMailFolder(emailFlagsInfo.getFolder());
					}
					_emailInfo.setSourceParent(emailFlagsInfo.getParentFolderId());
					_emailInfo.setSubject(emailFlagsInfo.getSubject());
					_emailInfo.setSourceId(emailFlagsInfo.getId());
					_emailInfo.setRead(emailFlagsInfo.isRead());
					_emailInfo.setFromAdminCloudId(emailWorkSpace.getFromAdminCloud());
					_emailInfo.setFromName(emailFlagsInfo.getFromName());
					_emailInfo.setFromMail(emailFlagsInfo.getFrom());
					_emailInfo.setOriginalFrom(emailFlagsInfo.getFrom());
					_emailInfo.setDeleted(emailFlagsInfo.isDeleted());
					_emailInfo.setTotalCount(emailFlagsInfo.getTotalCount());
					_emailInfo.setTotalSizeInBytes(emailFlagsInfo.getSizeInBytes());
					_emailInfo.setUnreadCount(emailFlagsInfo.getUnreadCount());
					if(emailFlagsInfo.getFrom()!=null && fromAdminCloud.getDomains()!=null && fromAdminCloud.getDomains().contains(emailFlagsInfo.getFrom().split(Const.ATTHERATE)[1]) && !mappedPairs.containsKey(emailFlagsInfo.getFrom())) {
						String from = checkAndCreateGroupEmails(emailFlagsInfo.getFrom());
						if(!from.equals(emailFlagsInfo.getFrom())) {
							mappedPairs.put(emailFlagsInfo.getFrom(), from);
						}
					}
					emailFlagsInfo.setTo(mapMails(emailFlagsInfo.getTo(), fromAdminCloud.getDomains()));
					if(emailFlagsInfo.getFrom()!=null) {
						if(mappedPairs.containsKey(emailFlagsInfo.getFrom())) {
							_emailInfo.setFromMail(mappedPairs.get(emailFlagsInfo.getFrom()));
							_emailInfo.setFromName(mappedPairs.get(emailFlagsInfo.getFrom()).split("@")[0]);
						}else {
							_emailInfo.setFromMail(emailFlagsInfo.getFrom());
							_emailInfo.setFromName(emailFlagsInfo.getFrom().split("@")[0]);
						}
					}
					emailFlagsInfo.setCc(mapMails(emailFlagsInfo.getCc(), fromAdminCloud.getDomains()));
					emailFlagsInfo.setBcc(mapMails(emailFlagsInfo.getBcc(), fromAdminCloud.getDomains()));
					_emailInfo.setToMail(emailFlagsInfo.getTo());
					_emailInfo.setFlagged(emailFlagsInfo.isFlagged());
					_emailInfo.setHtmlBodyContent(emailFlagsInfo.getHtmlMessage());
					_emailInfo.setImportance(emailFlagsInfo.getImportance());
					_emailInfo.setBodyPreview(emailFlagsInfo.getBodyPreview());
					_emailInfo.setHtmlContent(emailFlagsInfo.isHtmlContent());
					_emailInfo.setMetadata(true);
					_emailInfo.setCc(emailFlagsInfo.getCc());
					_emailInfo.setBcc(emailFlagsInfo.getBcc());
					_emailInfo.setPriority(MappingUtils.setPriority(emailFlagsInfo.getFolder()));
					_emailInfo.setTimeZone(emailFlagsInfo.getTimeZone());
					checkForOutlookSource(threads, emailFlagsInfo);
					setProcessStatus(emailFlagsInfo, _emailInfo);
					_emailInfo.setDraft(emailFlagsInfo.isDraft());
					if(emailInfo.getMailFolder()!=null && (emailInfo.getMailFolder().equalsIgnoreCase(MappingUtils.MAIL_FOLDERS.DRAFT.name()) || emailInfo.getMailFolder().equalsIgnoreCase(MappingUtils.MAIL_FOLDERS.DRAFTS.name()))) {
						_emailInfo.setDraft(true);
					}
					_emailInfo.setPreScan(emailWorkSpace.isPreScan());
					_emailInfo.setAttachMents(emailFlagsInfo.isHadAttachments());
					if(emailFlagsInfo.getAttachments()!=null && !emailFlagsInfo.getAttachments().isEmpty()) {
						attachmentsSize = setAttachments(attachmentsSize, emailFlagsInfo, _emailInfo);
					}
					_emailInfo.setReplyTo(emailFlagsInfo.getReplyTo());
					_emailInfo.setThreadId(emailFlagsInfo.getThreadId());
					_emailInfo.setFromCloudId(emailWorkSpace.getFromCloudId());
					_emailInfo.setToCloudId(emailWorkSpace.getToCloudId());
					_emailInfo.setPriority(MappingUtils.setPriority(emailFlagsInfo.getFolder()));
					_emailInfo.setConvIndex(emailFlagsInfo.getConvIndex());
					_emailInfo.setGroupEmail(emailFlagsInfo.isGroupMail());
					SetDestFolderPath(emailFlagsInfo, _emailInfo, mailFolder,destId);
					if(emailWorkSpace.isDeltaMigration() && !mailFolder.equalsIgnoreCase(emailFlagsInfo.getFolder())) {
						_emailInfo.setMovedFolder(emailFlagsInfo.getFolder());
					}
					if(emailWorkSpace.isPreScan() || (null!=count || ids.contains(emailFlagsInfo.getId()))) {
						checkExistingEmail(destId, emailFlagsInfo, count, _emailInfo);
					}
					if(emailWorkSpace.isDeltaMigration()) {
						checkDeltaEmail(emailFlagsInfo, count, _emailInfo);
					}
					if(_emailInfo.getMailFolder().equals(originalMailFolder)) {
						totlaCount = totlaCount+1;
					}
					if(emailFlagsInfo.isSubFolder()) {
						_emailInfo.setProcessStatus(EmailInfo.PROCESS.PARENT_NOT_PROCESSED);
					}
					ids.add(emailFlagsInfo.getId());
					emailInfos.add(_emailInfo);
					if(emailInfos.size()==20) {
						connectorService.getEmailInfoRepoImpl().save(emailInfos);
						emailInfos.clear();
					}
				} catch (Exception e) {
					emailInfo.setErrorDescription(ExceptionUtils.getStackTrace(e));
				}
			}
			if(!emailInfos.isEmpty()) {
				connectorService.getEmailInfoRepoImpl().save(emailInfos);
				emailInfos.clear();
			}

		}
		return totlaCount;
	}

	private void setProcessStatus(EmailFlagsInfo emailFlagsInfo, EmailInfo _emailInfo) {
		if(emailFlagsInfo.isMailFolder()) {
			_emailInfo.setPicking(true);
			emailFlagsInfo.setCloudId(emailWorkSpace.getFromCloudId());
			_emailInfo.setProcessStatus(EmailInfo.PROCESS.NOT_STARTED);
		}else {
			_emailInfo.setProcessStatus(EmailInfo.PROCESS.NOT_PROCESSED);
			if(emailFlagsInfo.getOrder()>0 && !emailWorkSpace.isDeltaMigration()) {
				_emailInfo.setProcessStatus(EmailInfo.PROCESS.THREAD_NOT_PROCESSED);
				_emailInfo.setOrder(emailFlagsInfo.getOrder());
			}
		}
	}

	private void checkForOutlookSource(Map<String, Long> threads, EmailFlagsInfo emailFlagsInfo) {
		if(CLOUD_NAME.OUTLOOK.equals(emailWorkSpace.getFromCloud())) {
			if(threads.containsKey(emailFlagsInfo.getThreadId())) {
				threads.put(emailFlagsInfo.getThreadId(), threads.get(emailFlagsInfo.getThreadId())+1);
				emailFlagsInfo.setOrder(threads.get(emailFlagsInfo.getThreadId()));
			}else {
				threads.put(emailFlagsInfo.getThreadId(), 0L);
			}
		}
	}

	private long setAttachments(long attachmentsSize, EmailFlagsInfo emailFlagsInfo, EmailInfo _emailInfo) {
		List<String> attachs = new ArrayList<>();
		for(AttachmentsData data : emailFlagsInfo.getAttachments()) {
			attachs.add(data.getId()+":"+data.getName()+":"+data.getContentType()+":"+data.getSize()+":"+data.isInline()+":"+data.isCompleted());
			attachmentsSize = attachmentsSize+data.getSize();
		}
		_emailInfo.setAttachmentIds(attachs);
		return attachmentsSize;
	}

	private void checkExistingEmail(String destId, EmailFlagsInfo emailFlagsInfo, EmailInfo count,
			EmailInfo _emailInfo) {
		_emailInfo.setExtraField("Alredy Exisiting");
		_emailInfo.setAdminDeleted(true);
		if(!emailWorkSpace.isPreScan() && null!=count && !count.getEmailWorkSpaceId().equals(emailWorkSpace.getId()) && !emailWorkSpace.isCopy() && emailWorkSpace.isDeltaMigration()){
			_emailInfo.setConvIndex(count.getConvIndex());
			_emailInfo.setMoved(true);
			_emailInfo.setDestFolderPath(destId);
			_emailInfo.setDestId(count.getDestId());
			_emailInfo.setDestThreadId(count.getDestThreadId());
			_emailInfo.setProcessStatus(EmailInfo.PROCESS.METADATA_STARTED);
		}else if(emailWorkSpace.isPreScan()){
			_emailInfo.setProcessStatus(EmailInfo.PROCESS.PRESCAN_MAIL);
		}else if(emailFlagsInfo.getFolder().equals(count.getMovedFolder())){
			_emailInfo.setProcessStatus(EmailInfo.PROCESS.PROCESSED);
		}else if(CLOUD_NAME.OUTLOOK.equals(emailWorkSpace.getToCloud())){
			_emailInfo.setProcessStatus(EmailInfo.PROCESS.METADATA_STARTED);	
		}
		if(CLOUD_NAME.GMAIL.equals(emailWorkSpace.getToCloud())) {
			_emailInfo.setProcessStatus(EmailInfo.PROCESS.PROCESSED);
		}
	}

	private void checkDeltaEmail(EmailFlagsInfo emailFlagsInfo, EmailInfo count, EmailInfo _emailInfo) {
		EmailInfo _count = connectorService.getEmailInfoRepoImpl().checkEmailInfosBasedOnIds(emailWorkSpace.getUserId(), emailFlagsInfo.getId(),false);
		if(_count!=null ) {
			_emailInfo.setDestId(_count.getDestId());
			_emailInfo.setConvIndex(_count.getConvIndex());
			_emailInfo.setMoved(true);
			_emailInfo.setDestThreadId(_count.getDestThreadId());
			if(_count.getEmailWorkSpaceId().equals(emailWorkSpace.getId())) {
				_emailInfo.setProcessStatus(EmailInfo.PROCESS.PROCESSED);
			}else if(CLOUD_NAME.OUTLOOK.equals(emailWorkSpace.getToCloud())){
				_emailInfo.setProcessStatus(EmailInfo.PROCESS.METADATA_STARTED);
			}
		}
		if(count!=null && emailWorkSpace.getId().equals(count.getEmailWorkSpaceId())) {
			_emailInfo.setProcessStatus(EmailInfo.PROCESS.PROCESSED);
		}
		if(_count==null && !emailFlagsInfo.getId().equalsIgnoreCase(emailFlagsInfo.getThreadId()) ) {

			EmailInfo oldThread = connectorService.getEmailInfoRepoImpl().findByThreadId(emailWorkSpace.getJobId(),emailWorkSpace.getUserId(), emailFlagsInfo.getThreadId());
			if(oldThread!=null) {
				_emailInfo.setConvIndex(oldThread.getConvIndex());
				if(!emailFlagsInfo.isDraft()) {
					if(emailFlagsInfo.getOrder()==0) {
						_emailInfo.setDeltaThread(true);
					}
					_emailInfo.setOrder(oldThread.getOrder()+emailFlagsInfo.getOrder()+1);
				}
				_emailInfo.setDestThreadId(oldThread.getDestThreadId());
				if(emailFlagsInfo.getOrder()>0) {
					_emailInfo.setProcessStatus(EmailInfo.PROCESS.THREAD_NOT_PROCESSED);
				}
				if(emailFlagsInfo.isDraft() && CLOUD_NAME.OUTLOOK.equals(emailWorkSpace.getToCloud())) {
					_emailInfo.setProcessStatus(EmailInfo.PROCESS.METADATA_STARTED);
				}
			}
		}
	}

	private void SetDestFolderPath(EmailFlagsInfo emailFlagsInfo, EmailFolderInfo _emailInfo, String mailFolder,String destId) {
		if(isGoogleCombo() && "/".equals(emailWorkSpace.getToFolderId()) && !MappingUtils.isCustomFolder(mailFolder)) {
			_emailInfo.setDestFolderPath(mailFolder);
		}else {
			_emailInfo.setDestFolderPath(destId);
		}
	}

	private void SetDestFolderPath(EmailFlagsInfo emailFlagsInfo, EmailInfo _emailInfo, String mailFolder,String destId) {
		if(isGoogleCombo() && "/".equals(emailWorkSpace.getToFolderId()) && !MappingUtils.isCustomFolder(mailFolder)) {
			_emailInfo.setDestFolderPath(mailFolder);
		}else {
			_emailInfo.setDestFolderPath(destId);
		}
	}

	private MailChangeIds createMailChangeIds(EmailFolderInfo emailInfo,String latestDeltaId) {
		log.info("==Entered for creating mailChange ids for the workspace=="+emailWorkSpace.getId()+"==folder name =="+this.emailInfo.getMailFolder());
		MailChangeIds changeIds = new MailChangeIds();
		changeIds.setMailFolder(emailInfo.getMailFolder());
		changeIds.setLatestUpdatedId(latestDeltaId);
		changeIds.setEmailWorkSpaceId(emailInfo.getEmailWorkSpaceId());
		changeIds.setUserId(emailInfo.getUserId());
		changeIds.setCreatedTime(LocalDateTime.now());
		changeIds.setFolder(true);
		changeIds.setJobId(emailWorkSpace.getJobId());
		changeIds.setFromCloudId(emailWorkSpace.getFromCloudId());
		changeIds.setDestFolderPath(emailInfo.getDestId());
		changeIds.setToCloudId(emailWorkSpace.getToCloudId());
		changeIds.setMetadata(emailWorkSpace.isMetadata());
		Date date = Calendar.getInstance().getTime();
		// try to implement in minutes and hours using the change expression so that will be good for testing

		String changeHours[] = emailWorkSpace.getChangeHours().split("H");
		if(changeHours!=null && changeHours.length>0) {
			date.setHours(date.getHours()+Integer.parseInt(changeHours[0]));
			date.setMinutes(date.getMinutes()+Integer.parseInt(changeHours[1].substring(0,changeHours[1].length()-1)));
		}
		changeIds.setNextPickTime(date);
		changeIds.setSourceId(emailInfo.getSourceId());
		return changeIds;
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

	boolean verifyMatch(String sourceMail,String destMail) {
		try {
			if(StringUtils.isNotBlank(sourceMail) && StringUtils.isNotBlank(destMail)) {
				return sourceMail.split(Const.ATTHERATE)[0].equals(destMail.split(Const.ATTHERATE)[0]);
			}
		} catch (Exception e) {
		}
		return false;
	}

	private boolean isGoogleCombo() {
		return emailWorkSpace.getFromCloud().equals(CLOUD_NAME.GMAIL) && emailWorkSpace.getToCloud().equals(CLOUD_NAME.GMAIL);
	}

	void pickEmailSettings(EmailFlagsInfo emailFlagsInfo){
		log.info("==Migrating the Email settings in the user Workspace in --"+emailWorkSpace.getId());
		if(emailWorkSpace.isSettings()) {
			List<EmailUserSettings> settings = mailServiceFactory.getConnectorService(emailWorkSpace.getFromCloud()).getSettings(emailFlagsInfo);
			if(!settings.isEmpty()) {
				settings.forEach(setting->{
					try {
						setting.setUserId(emailWorkSpace.getUserId());
						setting.setFromCloudId(emailWorkSpace.getFromCloudId());
						setting.setToCloudId(emailWorkSpace.getToCloudId());
						setting.setWorkSpaceId(emailWorkSpace.getId());
						emailFlagsInfo.setCloudId(emailWorkSpace.getToCloudId());
						setting.setProcessStatus(PROCESS.NOT_PROCESSED);
						setting.setEmail(ConnectorUtility.mapMails(Arrays.asList(setting.getEmail()), mappedPairs).get(0));
						if(setting.getAutoForwardSettings()!=null) {
							setting.getAutoForwardSettings().setEmailAddress(ConnectorUtility.mapMails(Arrays.asList(setting.getAutoForwardSettings().getEmailAddress()), mappedPairs).get(0));
						}
						if(setting.getForwardingAddresses()!=null && !setting.getForwardingAddresses().isEmpty()) {
							setting.getForwardingAddresses().forEach(forward->
							forward.setForwardingEmail(ConnectorUtility.mapMails(Arrays.asList(forward.getForwardingEmail()), mappedPairs).get(0))
									);
						}
						connectorService.getMailUserSettingsRepoImpl().save(setting);
					} catch (Exception e) {
						log.error(ExceptionUtils.getStackTrace(e));
						setting.setErrorDescription(ExceptionUtils.getStackTrace(e));
						setting.setProcessStatus(PROCESS.CONFLICT);
					}
					finally {
						connectorService.getMailUserSettingsRepoImpl().save(setting);
					}
				});
			}
		}
	}


	private String checkAndCreateGroupEmails(String email) {
		if(emailWorkSpace.isCreateGroups()) {
			try {
				return updateGroupMail(fromCloud, toCloud,email);
			} catch (Exception e) {
				log.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return email;
	}

	private String updateGroupMail(Clouds fromCloud, Clouds toCloud,String email) {
		GroupEmailDetails sourceGroupEmailDetails =  connectorService.getCloudsRepoImpl().findGroupDetails(emailWorkSpace.getUserId(), emailWorkSpace.getFromAdminCloud(), email);
		UserGroups sourceGroup = null;
		if(null==sourceGroupEmailDetails) {
			sourceGroupEmailDetails = new GroupEmailDetails();
			sourceGroupEmailDetails.setUserId(emailInfo.getUserId());
			sourceGroupEmailDetails.setAdminCloudId(emailWorkSpace.getFromAdminCloud());
			sourceGroupEmailDetails.setProcessStatus(com.cloudfuze.mail.repo.entities.PROCESS.NOT_PROCESSED);
			sourceGroup = mailServiceFactory.getConnectorService(fromCloud.getCloudName()).getSingleGroupEmailDetails(fromCloud.getAdminCloudId(), email);
			if(sourceGroup!=null) {
				sourceGroupEmailDetails.setName(sourceGroup.getName());
				sourceGroupEmailDetails.setEmail(sourceGroup.getEmail());
				sourceGroupEmailDetails.setGroupId(sourceGroup.getId());
				sourceGroupEmailDetails.setDescription(sourceGroup.getDescription());
				connectorService.getCloudsRepoImpl().saveGroupDetails(Arrays.asList(sourceGroupEmailDetails));
			}
		}else {
			sourceGroup = ConvertionUtils.convertGroupEmailDetailsToUserGroup(sourceGroupEmailDetails);
		}
		if(sourceGroup!=null) {
			try {
				if(sourceGroup.getMembers()==null) {
					List<String>members = mailServiceFactory.getConnectorService(fromCloud.getCloudName()).getMembersFromGroup(fromCloud.getAdminCloudId(), sourceGroup.getId());
					if(!members.isEmpty()) {
						sourceGroupEmailDetails.setMembers(members);
						sourceGroup.setMembers(members);
						sourceGroupEmailDetails.setProcessStatus(com.cloudfuze.mail.repo.entities.PROCESS.PROCESSED);
						connectorService.getCloudsRepoImpl().saveGroupDetails(Arrays.asList(sourceGroupEmailDetails));
					}
				}
			} catch (Exception e) {
				sourceGroupEmailDetails.setProcessStatus(com.cloudfuze.mail.repo.entities.PROCESS.CONFLICT);
				sourceGroupEmailDetails.setErrorDescription(ExceptionUtils.getStackTrace(e));
			}finally {
				connectorService.getCloudsRepoImpl().saveGroupDetails(Arrays.asList(sourceGroupEmailDetails));
			}
		}
		if(sourceGroup!=null) {
			try {
				String destEmailGroup = email.split(Const.ATTHERATE)[0];
				UserGroups destGroup = null;
				GroupEmailDetails destGroupEmailDetails = null;
				for(String domain : toAdminCloud.getDomains()) {
					destGroupEmailDetails = connectorService.getCloudsRepoImpl().findGroupDetails(emailWorkSpace.getUserId(), emailWorkSpace.getToAdminCloud(), destEmailGroup+Const.ATTHERATE+domain);
					if(null!=destGroupEmailDetails) {
						destGroup = ConvertionUtils.convertGroupEmailDetailsToUserGroup(destGroupEmailDetails);
						break;
					}
				}
				if(null == destGroupEmailDetails) {
					destGroupEmailDetails = new GroupEmailDetails();
					destGroupEmailDetails.setUserId(emailInfo.getUserId());
					destGroupEmailDetails.setAdminCloudId(emailWorkSpace.getFromAdminCloud());
					destGroupEmailDetails.setProcessStatus(com.cloudfuze.mail.repo.entities.PROCESS.NOT_PROCESSED);
					destGroup = mailServiceFactory.getConnectorService(toCloud.getCloudName()).getSingleGroupEmailDetails(toCloud.getAdminCloudId(), destEmailGroup+Const.ATTHERATE+fromAdminCloud.getEmail().split(Const.ATTHERATE)[1]);
					if(null!=destGroup) {
						destGroupEmailDetails.setName(destGroup.getName());
						destGroupEmailDetails.setEmail(destGroup.getEmail());
						destGroupEmailDetails.setGroupId(destGroup.getId());
						destGroupEmailDetails.setDescription(destGroup.getDescription());
						connectorService.getCloudsRepoImpl().saveGroupDetails(Arrays.asList(destGroupEmailDetails));
					}
				}
				if(destGroup!=null) {
					try {
						if(destGroup.getMembers()==null) {
							List<String> members;
							try {
								members = mailServiceFactory.getConnectorService(toCloud.getCloudName()).getMembersFromGroup(toCloud.getAdminCloudId(), destGroup.getId());
								if(!members.isEmpty()) {
									destGroupEmailDetails.setMembers(members);
									destGroup.setMembers(members);
									destGroupEmailDetails.setProcessStatus(com.cloudfuze.mail.repo.entities.PROCESS.PROCESSED);
									connectorService.getCloudsRepoImpl().saveGroupDetails(Arrays.asList(destGroupEmailDetails));
								}
							} catch (Exception e) {
								if(e.getMessage().contains("Request_ResourceNotFound")) {
									sourceGroup.setMembers(mapMails(sourceGroup.getMembers()));
									destGroup = mailServiceFactory.getConnectorService(toCloud.getCloudName()).createGroup(toCloud.getAdminCloudId(), sourceGroup.getEmail(), sourceGroup.getErrorDescription(), sourceGroup.getName(), sourceGroup.getMembers());
									destGroupEmailDetails.setName(destGroup.getName());
									destGroupEmailDetails.setEmail(destGroup.getEmail());
									destGroupEmailDetails.setGroupId(destGroup.getId());
									destGroupEmailDetails.setDescription(destGroup.getDescription());
									connectorService.getCloudsRepoImpl().saveGroupDetails(Arrays.asList(destGroupEmailDetails));
								}
							}
							List<String>newMemebrs = checkExistingGroupMembers(sourceGroup.getMembers(), destGroup.getMembers());
							newMemebrs = mapMails(newMemebrs);
							if(!newMemebrs.isEmpty()) {
								members = mailServiceFactory.getConnectorService(toCloud.getCloudName()).addMembersToGroup(toCloud.getAdminCloudId(), newMemebrs, destGroup.getId());
								destGroup.setMembers(members);
							}
						}

					} catch (Exception e) {
						destGroupEmailDetails.setProcessStatus(com.cloudfuze.mail.repo.entities.PROCESS.CONFLICT);
						destGroupEmailDetails.setErrorDescription(ExceptionUtils.getStackTrace(e));
					}finally {
						connectorService.getCloudsRepoImpl().saveGroupDetails(Arrays.asList(destGroupEmailDetails));
					}
				}else {
					sourceGroup.setMembers(mapMails(sourceGroup.getMembers()));
					destGroup = mailServiceFactory.getConnectorService(toCloud.getCloudName()).createGroup(toCloud.getAdminCloudId(), sourceGroup.getEmail(), sourceGroup.getErrorDescription(), sourceGroup.getName(), sourceGroup.getMembers());
					destGroupEmailDetails.setName(destGroup.getName());
					destGroupEmailDetails.setEmail(destGroup.getEmail());
					destGroupEmailDetails.setGroupId(destGroup.getId());
					destGroupEmailDetails.setDescription(destGroup.getDescription());
					connectorService.getCloudsRepoImpl().saveGroupDetails(Arrays.asList(destGroupEmailDetails));
				}
				if(destGroup!=null) {
					return destGroup.getEmail();
				}else {
					sourceGroupEmailDetails.setProcessStatus(com.cloudfuze.mail.repo.entities.PROCESS.CONFLICT);
					sourceGroupEmailDetails.setErrorDescription("Can't able to create in Destination");
					connectorService.getCloudsRepoImpl().saveGroupDetails(Arrays.asList(sourceGroupEmailDetails));
				}
			} catch (Exception e) {
			}
		}
		return email;
	}

	private List<String>checkExistingGroupMembers(List<String>srcMemebrs,List<String>destMembers){
		List<String>newMembers = new ArrayList<>();
		Map<String,String>destMembersMap = new HashMap<>();
		if(srcMemebrs==null || srcMemebrs.isEmpty()) {
			return newMembers;
		}
		if(destMembers!=null && !destMembers.isEmpty()) {
			for(String member : destMembers) {
				String _member = member.split(Const.HASHTAG)[0];
				destMembersMap.put(_member, member.split(Const.HASHTAG)[1]);
			}
		}
		for(String member : srcMemebrs) {
			String _member = member.split(Const.HASHTAG)[0];
			if(destMembersMap.containsKey(_member)) {
				continue;
			}
			newMembers.add(member);
		}
		return newMembers;
	}


	public List<String> mapMails(List<String>to,List<String>domains) {
		List<String> attends = new ArrayList<>();
		if(to==null || to.isEmpty()) {
			return attends;
		}
		for(String attendees : to) {
			try {
				if(attendees.isEmpty() || attendees==null) {
					continue;
				}
				String _attendees = attendees.split(":")[0].trim();
				if(mappedPairs.containsKey(_attendees)) {
					attends.add(mappedPairs.get(_attendees));
				}else {
					if(domains!=null && domains.contains(_attendees.split(Const.ATTHERATE)[1])) {
						String destMail = checkAndCreateGroupEmails(_attendees);
						if(!destMail.equals(_attendees)) {
							mappedPairs.put(_attendees, destMail);
							attends.add(mappedPairs.get(_attendees));
							//createPermissionCacheForGroup(_attendees, destMail);
						}else if(mappedPairs.containsKey(_attendees)){
							attends.add(mappedPairs.get(_attendees));
						}else {
							attends.add(_attendees);
						}
					}else {
						attends.add(_attendees);
					}
				}
			} catch (Exception e) {
			}
		}
		return attends;
	}

	public List<String> mapMails(List<String>to) {
		List<String> attends = new ArrayList<>();
		if(to==null || to.isEmpty()) {
			return attends;
		}
		for(String attendees : to) {
			String _attendees = attendees.split(Const.HASHTAG)[0].trim();
			if(mappedPairs.containsKey(_attendees)) {
				attends.add(mappedPairs.get(_attendees)+Const.HASHTAG+attendees.split(Const.HASHTAG)[1].trim());
			}else {
				attends.add(_attendees+Const.HASHTAG+attendees.split(Const.HASHTAG)[0].trim());
			}
		}
		return attends;
	}

	private String createMailFolder() throws Exception {
		String message = "SuccessFul";
		try {
			EmailFlagsInfo emailFlagsInfo = new EmailFlagsInfo();
			emailFlagsInfo.setCloudId(emailWorkSpace.getToCloudId());
			emailFlagsInfo.setFolder(StringUtils.isEmpty(emailInfo.getMailFolder())?emailInfo.getMovedFolder():emailInfo.getMailFolder());
			mailFolderMapping(emailFlagsInfo);
			if(emailWorkSpace.isDeltaMigration()) {
				EmailFolderInfo previoudFolder = connectorService.getEmailFolderInfoRepoImpl().getParentFolderInfo(emailWorkSpace.getLastEmailWorkSpaceId(), emailInfo.getSourceId());
				if(null!=previoudFolder) {
					emailFlagsInfo.setFolder(previoudFolder.getMailFolder());
				}
			}
			EmailFolderInfo _emailInfo = null;
			emailInfo.setProcessStatus(PROCESS.IN_PROGRESS);
			connectorService.getEmailFolderInfoRepoImpl().save(emailInfo);
			emailFlagsInfo.setCustomLabel(MappingUtils.isCustomFolder(emailFlagsInfo.getFolder()));
			EmailInfo mailFolder =  mailServiceFactory.getConnectorService(toCloud.getCloudName()).getLabel(emailFlagsInfo);
			if(emailInfo.isSubFolder()) {
				_emailInfo = connectorService.getEmailFolderInfoRepoImpl().getParentFolderInfo(emailWorkSpace.getId(), emailInfo.getSourceParent());
				if(_emailInfo!=null) {
					String sourceParent = _emailInfo.getSourceParent();
					String path = "/"+_emailInfo.getMailFolder();
					if(emailWorkSpace.getToCloud().equals(CLOUD_NAME.OUTLOOK)) {
						emailFlagsInfo.setId(_emailInfo.getDestId());
					}else {
						while(true) {
							if("/".equals(sourceParent)) {
								break;
							}else {
								EmailFolderInfo parentFolder = connectorService.getEmailFolderInfoRepoImpl().getParentFolderInfo(emailWorkSpace.getId(), sourceParent);
								if(parentFolder!=null) {
									sourceParent = parentFolder.getSourceParent();
									path  = parentFolder.getMailFolder()+"/"+path;
								}else {
									break;
								}
							}
						}
						emailFlagsInfo.setId(path.replace("//", "/"));
					}
				}else if(_emailInfo==null && emailInfo.getSourceParent().equals("/")) {
					emailFlagsInfo.setId(emailInfo.getSourceParent());
					//added for fixing the mailFolder creation if parent folder not found
					String oldFolder = emailFlagsInfo.getFolder();
					if(emailWorkSpace.getToCloud().equals(CLOUD_NAME.OUTLOOK)) {
						emailFlagsInfo.setCloudId(emailWorkSpace.getToCloudId());
						emailFlagsInfo.setFolder(emailInfo.getSourceParent());
						mailFolder =  mailServiceFactory.getConnectorService(toCloud.getCloudName()).getLabel(emailFlagsInfo);
						if(mailFolder!=null) {
							emailFlagsInfo.setId(mailFolder.getId());
						}
					}
					emailFlagsInfo.setFolder(oldFolder);
				}else {
					emailInfo.setProcessStatus(PROCESS.NOT_PROCESSED);
					emailInfo.setThreadBy(null);
					return null;
				}

			}
			emailFlagsInfo.setSubFolder(emailInfo.isSubFolder());

			EmailInfo created = null;
			if(mailFolder==null) {
				created =  mailServiceFactory.getConnectorService(toCloud.getCloudName()).createAMailFolder(emailFlagsInfo);
				emailInfo.setCreated(true);
			}
			if(created!=null) {
				emailInfo.setDestId(created.getId());
				emailInfo.setDestParent(created.getDestParent());
				emailInfo.setProcessStatus(PROCESS.PICKING);
				connectorService.getEmailFolderInfoRepoImpl().updateEmailInfoForDestChildFolder(emailWorkSpace.getId(), StringUtils.isEmpty(emailInfo.getSourceId())?emailInfo.getMovedFolder():emailInfo.getSourceId(),emailInfo.getDestId());
			}else if(mailFolder!=null){
				emailInfo.setDestId(mailFolder.getId());
				emailInfo.setDestParent(mailFolder.getSourceParent()==null?"/":mailFolder.getSourceParent());
				emailInfo.setProcessStatus(PROCESS.PICKING);
				connectorService.getEmailFolderInfoRepoImpl().updateEmailInfoForDestChildFolder(emailWorkSpace.getId(), StringUtils.isEmpty(emailInfo.getSourceId())?emailInfo.getMovedFolder():emailInfo.getSourceId(),emailInfo.getDestId());
			}
			if(StringUtils.isEmpty(emailInfo.getMailFolder())) {
				emailInfo.setMailFolder(emailFlagsInfo.getFolder());
			}

		}catch(Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
			message = ExceptionUtils.getStackTrace(e);
			emailInfo.setProcessStatus(PROCESS.CONFLICT);
			emailInfo.setErrorDescription(message);
			throw e;
		}finally {
			emailInfo.setErrorDescription(message);
			if(null==emailInfo.getDestId() && null!=emailInfo.getErrorDescription()) {
				emailInfo.setProcessStatus(PROCESS.CONFLICT);
				connectorService.getEmailFolderInfoRepoImpl().save(emailInfo);
				throw new Exception("Can't able to create");
			}
			connectorService.getEmailFolderInfoRepoImpl().save(emailInfo);
		}
		return emailInfo.getDestId();
	}


	private void mailFolderMapping(EmailFlagsInfo emailFlagsInfo) {
		String folder = MappingUtils.mapMailFolder(emailFlagsInfo.getFolder(), emailWorkSpace.getFromCloud(), emailWorkSpace.getToCloud());
		emailFlagsInfo.setFolder(folder);
		emailInfo.setDestFolderPath(folder);
	}

	private long convertEmailFlagsToEmailFolderInfo(List<EmailFlagsInfo> emailFlagsInfos,List<String>ids,String originalMailFolder,
			String destId) throws MailMigrationException {
		long totlaCount =0;
		List<EmailFolderInfo> emailInfos = new ArrayList<>();
		if(emailFlagsInfos!=null && !emailFlagsInfos.isEmpty()) {
			for(EmailFlagsInfo emailFlagsInfo : emailFlagsInfos) {
				EmailFolderInfo count = connectorService.getEmailInfoRepoImpl().checkEmailInfosBasedOnIds(emailWorkSpace.getId(), emailFlagsInfo.getId());
				if(null!=count) {
					continue;
				}
				EmailFolderInfo _emailInfo = new EmailFolderInfo();
				_emailInfo.setFromCloud(emailWorkSpace.getFromCloud().name());
				_emailInfo.setJobId(emailWorkSpace.getJobId());
				_emailInfo.setToCloud(emailWorkSpace.getToCloud().name());
				_emailInfo.setEmailWorkSpaceId(emailWorkSpace.getId());
				_emailInfo.setOriginalMailFolder(emailInfo.getMailFolder());
				String mailFolder = MappingUtils.isGoogleCombo(emailWorkSpace.getFromCloud())?emailFlagsInfo.getFolder():emailInfo.getMailFolder();
				//emailInfo.setMailFolder(mailFolder);
				_emailInfo.setUserId(emailWorkSpace.getUserId());
				_emailInfo.setMailFolder(emailFlagsInfo.getFolder());
				_emailInfo.setCreatedTime(LocalDateTime.now());
				_emailInfo.setSubFolder(emailFlagsInfo.isSubFolder());
				_emailInfo.setMovedFolder(emailFlagsInfo.getFolder());
				if(!emailWorkSpace.getFromCloud().equals(emailWorkSpace.getToCloud())) {
					_emailInfo.setMailFolder(mapping.containsKey(emailFlagsInfo.getFolder().replace(" ", ""))?mapping.get(emailFlagsInfo.getFolder().replace(" ", "")):emailFlagsInfo.getFolder());
				}else {
					_emailInfo.setMailFolder(emailFlagsInfo.getFolder());
				}
				_emailInfo.setSourceParent(emailFlagsInfo.getParentFolderId());
				_emailInfo.setSourceId(emailFlagsInfo.getId());
				_emailInfo.setFromAdminCloudId(emailWorkSpace.getFromAdminCloud());
				_emailInfo.setTotalCount(emailFlagsInfo.getTotalCount());
				_emailInfo.setTotalSizeInBytes(emailFlagsInfo.getSizeInBytes());
				_emailInfo.setUnreadCount(emailFlagsInfo.getUnreadCount());
				if(emailFlagsInfo.getFrom()!=null && fromAdminCloud.getDomains()!=null && fromAdminCloud.getDomains().contains(emailFlagsInfo.getFrom().split(Const.ATTHERATE)[1]) && !mappedPairs.containsKey(emailFlagsInfo.getFrom())) {
					String from = checkAndCreateGroupEmails(emailFlagsInfo.getFrom());
					if(!from.equals(emailFlagsInfo.getFrom())) {
						mappedPairs.put(emailFlagsInfo.getFrom(), from);
					}
				}
				_emailInfo.setPriority(MappingUtils.setPriority(emailFlagsInfo.getFolder()));
				_emailInfo.setPicking(true);
				emailFlagsInfo.setCloudId(emailWorkSpace.getFromCloudId());
				_emailInfo.setProcessStatus(PROCESS.NOT_STARTED);
				_emailInfo.setPreScan(emailWorkSpace.isPreScan());
				_emailInfo.setFromCloudId(emailWorkSpace.getFromCloudId());
				_emailInfo.setToCloudId(emailWorkSpace.getToCloudId());
				_emailInfo.setPriority(MappingUtils.setPriority(emailFlagsInfo.getFolder()));
				SetDestFolderPath(emailFlagsInfo, _emailInfo, StringUtils.isEmpty(mailFolder)?emailFlagsInfo.getFolder():mailFolder,destId);
				if(emailWorkSpace.isPreScan() ) {
					_emailInfo.setProcessStatus(PROCESS.PROCESSED);
				}
				if(emailInfo.getMailFolder().equals(originalMailFolder)) {
					totlaCount = totlaCount+1;
				}
				if(emailFlagsInfo.isSubFolder()) {
					_emailInfo.setProcessStatus(PROCESS.PARENT_NOT_PROCESSED);
				}
				ids.add(emailFlagsInfo.getId());
				emailInfos.add(_emailInfo);
				if(emailInfos.size()>10) {
					connectorService.getEmailFolderInfoRepoImpl().saveAll(emailInfos);
					emailInfos.clear();
				}
			}
			if(!emailInfos.isEmpty()) {
				connectorService.getEmailFolderInfoRepoImpl().saveAll(emailInfos);
				emailInfos.clear();
			}
			updatePickingQueue(emailWorkSpace.getId(), PROCESS.IN_PROGRESS, ExceptionConstants.STARTED);
		}
		return totlaCount;
	}

	private void updatePickingQueue(String emailWorkSpaceId,PROCESS processStatus,String errorDescription) {
		EmailPickingQueue moveQueue = 	connectorService.getEmailQueueRepoImpl().findByWorkSpace(emailWorkSpaceId);
		if(moveQueue!=null) {
			moveQueue.setProcessStatus(processStatus);
			moveQueue.setErrorDescription(errorDescription);
			connectorService.getEmailQueueRepoImpl().save(moveQueue);
		}
	}
	private void pickContacts() {
		if(emailWorkSpace.isContacts()) {
			log.info("==Piking the Contacts=="+emailWorkSpace.getId());
			ContactsFlagInfo contactsFlagInfo = new ContactsFlagInfo();
			contactsFlagInfo.setCloudId(emailWorkSpace.getFromCloudId());
			List<ContactsInfo> contactsInfos = new ArrayList<>();
			List<Contacts> contacts = mailServiceFactory.getConnectorService(emailWorkSpace.getFromCloud()).listContacts(contactsFlagInfo);
			if(!contacts.isEmpty()) {
				for(Contacts _contact : contacts) {
					ContactsInfo contactsInfo = new ContactsInfo();
					contactsInfo.setEmailWorkSpaceId(emailWorkSpace.getId());
					contactsInfo.setUserId(emailWorkSpace.getUserId());
					contactsInfo.setEmailAddress(_contact.getEmailAddresses().stream().map(Emails::getEmailAddress).collect(Collectors.toList()));
					contactsInfo.setName(_contact.getFirstName()+":"+_contact.getLastName());
					contactsInfo.setSourceId(_contact.getId());
					contactsInfo.setPhoneNumbers(_contact.getPhoneNumbers().stream().map(PhoneNumbers::getPhoneNo).collect(Collectors.toList()));
					contactsInfo.setProcessStatus(com.cloudfuze.mail.repo.entities.PROCESS.NOT_PROCESSED);
					contactsInfo.setJobId(emailWorkSpace.getJobId());
					contactsInfos.add(contactsInfo);
				}
				connectorService.getEmailInfoRepoImpl().saveContacts(contactsInfos);
				contactsInfos.clear();
			}
		}
	}
}

