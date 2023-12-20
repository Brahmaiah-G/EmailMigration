package com.testing.mail.connectors.management;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.testing.mail.connectors.factory.MailServiceFactory;
import com.testing.mail.connectors.microsoft.data.AttachmentsData;
import com.testing.mail.constants.Const;
import com.testing.mail.constants.ExceptionConstants;
import com.testing.mail.dao.entities.EmailFlagsInfo;
import com.testing.mail.dao.entities.PermissionCache;
import com.testing.mail.dao.entities.UserGroups;
import com.testing.mail.exceptions.MailMigrationException;
import com.testing.mail.repo.entities.Clouds;
import com.testing.mail.repo.entities.Clouds.CLOUD_NAME;
import com.testing.mail.repo.entities.EmailFolderInfo;
import com.testing.mail.repo.entities.EmailInfo;
import com.testing.mail.repo.entities.EmailPickingQueue;
import com.testing.mail.repo.entities.EmailWorkSpace;
import com.testing.mail.repo.entities.GroupEmailDetails;
import com.testing.mail.repo.entities.MailChangeIds;
import com.testing.mail.repo.entities.MailChanges;
import com.testing.mail.repo.entities.PROCESS;
import com.testing.mail.repo.entities.ThreadControl;
import com.testing.mail.service.DBConnectorService;
import com.testing.mail.utils.ConvertionUtils;
import com.testing.mail.utils.MappingUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeltaMailsPickingTask implements Runnable{

	
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

	public DeltaMailsPickingTask(MailServiceFactory mailServiceFactory, DBConnectorService connectorService,
			EmailWorkSpace emailWorkSpace, EmailFolderInfo emailInfo) {
		this.mailServiceFactory = mailServiceFactory;
		this.connectorService = connectorService;
		this.emailWorkSpace = emailWorkSpace;
		this.emailInfo = emailInfo;
	}
	
	
	@Override
	public void run() {
		log.info("==***STARTING the Thread Delta=="+Thread.currentThread().getName()+"---"+emailWorkSpace.getId());
		initiateMigration();
		log.info("==***Leaving the Thread Delta=="+Thread.currentThread().getName()+"---"+emailWorkSpace.getId());
	}
	
	private void initiateMigration() {
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
		
		List<MailChanges> mailchanges = new ArrayList<>();
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
			if(emailWorkSpace.isDeltaMigration()) {
				if(emailInfo.getSourceId()!=null && emailInfo.getSourceId().equals("/")) {
					List<String> dups = new ArrayList<>();
					mailchanges = connectorService.getMailChangesRepoImpl().findByWorkSpaceParent(emailWorkSpace.getLastEmailWorkSpaceId(), true,false);
					if(!ObjectUtils.isEmpty(mailchanges)) {
						List<EmailFolderInfo> infos = new ArrayList<>();
						for(MailChanges changes : mailchanges) {
							if(dups.contains(changes.getSourceId())) {
								continue;
							}
							dups.add(changes.getSourceId());
							EmailFolderInfo _emailInfo = new EmailFolderInfo();
							_emailInfo.setSourceId(changes.getMailFolder()==null ? changes.getSourceId():changes.getMailFolder());
							if(isGoogleCombo()) {
								_emailInfo.setSourceId(changes.getSourceId());
							}
							_emailInfo.setSourceParent(changes.getSourceParent());
							_emailInfo.setEmailWorkSpaceId(emailWorkSpace.getId());
							_emailInfo.setJobId(emailWorkSpace.getJobId());
							_emailInfo.setUserId(emailWorkSpace.getUserId());
							_emailInfo.setSubFolder(changes.isSubFolder());
							_emailInfo.setToCloudId(emailWorkSpace.getToCloudId());
							_emailInfo.setFromCloudId(emailWorkSpace.getFromCloudId());
							
							_emailInfo.setMailFolder(changes.getMailFolder()==null ? changes.getSourceId():changes.getMailFolder());
							infos.add(_emailInfo);
						}
						emailInfo.setProcessStatus(PROCESS.PROCESSED);
						connectorService.getEmailFolderInfoRepoImpl().saveAll(infos);
						infos.clear();
					}else {
						getMails(emailFlagsInfo);
						emailInfo.setProcessStatus(PROCESS.PROCESSED);
					}
				}else {
					getAndMapChanges();
				}
			}
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
				connectorService.getEmailFolderInfoRepoImpl().updateEmailInfoForDestChildFolder(emailWorkSpace.getId(), StringUtils.isEmpty(emailInfo.getMailFolder())?emailInfo.getMovedFolder():emailInfo.getMailFolder(),emailInfo.getDestId());
			}else if(mailFolder!=null){
				emailInfo.setDestId(mailFolder.getId());
				emailInfo.setDestParent(mailFolder.getSourceParent()==null?"/":mailFolder.getSourceParent());
				emailInfo.setProcessStatus(PROCESS.PICKING);
				connectorService.getEmailFolderInfoRepoImpl().updateEmailInfoForDestChildFolder(emailWorkSpace.getId(), StringUtils.isEmpty(emailInfo.getMailFolder())?emailInfo.getMovedFolder():emailInfo.getMailFolder(),emailInfo.getDestId());
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
	
	private boolean isGoogleCombo() {
		return emailWorkSpace.getFromCloud().equals(CLOUD_NAME.GMAIL) && emailWorkSpace.getToCloud().equals(CLOUD_NAME.GMAIL);
	}
	private void mailFolderMapping(EmailFlagsInfo emailFlagsInfo) {
		String folder = MappingUtils.mapMailFolder(emailFlagsInfo.getFolder(), emailWorkSpace.getFromCloud(), emailWorkSpace.getToCloud());
		emailFlagsInfo.setFolder(folder);
		emailInfo.setDestFolderPath(folder);
	}
	
	private void getUpdatedMails() {
		EmailFlagsInfo emailFlagsInfo = new EmailFlagsInfo();
		Map<String,Long> threads = new HashMap<>();
		emailFlagsInfo.setAdminMemberId(fromCloud.getAdminMemberId());
		emailFlagsInfo.setCloudId(emailWorkSpace.getFromCloudId());
		emailFlagsInfo.setUserId(fromCloud.getUserId());
		emailFlagsInfo.setEmailId(emailWorkSpace.getFromMailId());
		if(emailWorkSpace.isCalendar()) {
			emailFlagsInfo.setStopCalendarSource(true);
		}
		emailFlagsInfo.setNextPageToken(null);
		long totalCount = 0;
		log.warn("===Get UpdatedMails for the user=="+emailWorkSpace.getFromMailId()+"==For the MailFolder=="+emailInfo.getMailFolder());
		List<EmailFlagsInfo> emails = null;
		List<String>ids = new ArrayList<>();
		String original = emailInfo.getMailFolder();
		MailChangeIds mailChangeIds = connectorService.getMailChangeIdRepoImpl().findBySourceId(emailWorkSpace.getLastEmailWorkSpaceId(), emailInfo.getSourceId());
		if(null!=mailChangeIds) {
			emailInfo.setLatestUpdatedId(mailChangeIds.getLatestUpdatedId());
			emailInfo.setDestFolderPath(mailChangeIds.getDestFolderPath());
		}else {
			EmailFolderInfo emailFolderInfo = connectorService.getEmailFolderInfoRepoImpl().getParentFolderInfo(emailWorkSpace.getLastEmailWorkSpaceId(), emailInfo.getSourceId());
			if(null!=emailFolderInfo) {
				emailInfo.setLatestUpdatedId(emailFolderInfo.getLatestUpdatedId());
			}
		}
		emailFlagsInfo.setFolder(emailInfo.getSourceId());
		if(null==emailInfo.getLatestUpdatedId()) {
			emails = mailServiceFactory.getConnectorService(emailWorkSpace.getFromCloud()).getListOfMails(emailFlagsInfo);
		}else {
			emails = mailServiceFactory.getConnectorService(emailWorkSpace.getFromCloud()).getDeltaChanges(emailFlagsInfo, emailInfo.getLatestUpdatedId());
		}

		if(ObjectUtils.isEmpty(emails) || emails.isEmpty()) {
			emailInfo.setErrorDescription(ExceptionConstants.NO_MAILS_FOLDER);
		}else {
			emailInfo.setErrorDescription(ExceptionConstants.STARTED);
			emailInfo.setThreadBy(null);
		}
		totalCount = totalCount+convertEmailFlagsToEmailInfo(emails,0,ids,original,emailInfo.getDestId(),threads);
		emailInfo.setTotalCount(emailInfo.getTotalCount()+emails.size());
		emailInfo.setProcessStatus(PROCESS.PROCESSED);
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

	private void SetDestFolderPath(EmailFlagsInfo emailFlagsInfo, EmailInfo _emailInfo, String mailFolder,String destId) {
		if(isGoogleCombo() && "/".equals(emailWorkSpace.getToFolderId()) && !MappingUtils.isCustomFolder(mailFolder)) {
			_emailInfo.setDestFolderPath(mailFolder);
		}else {
			_emailInfo.setDestFolderPath(destId);
		}
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
			sourceGroupEmailDetails.setProcessStatus(com.testing.mail.repo.entities.PROCESS.NOT_PROCESSED);
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
						sourceGroupEmailDetails.setProcessStatus(com.testing.mail.repo.entities.PROCESS.PROCESSED);
						connectorService.getCloudsRepoImpl().saveGroupDetails(Arrays.asList(sourceGroupEmailDetails));
					}
				}
			} catch (Exception e) {
				sourceGroupEmailDetails.setProcessStatus(com.testing.mail.repo.entities.PROCESS.CONFLICT);
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
					destGroupEmailDetails.setProcessStatus(com.testing.mail.repo.entities.PROCESS.NOT_PROCESSED);
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
									destGroupEmailDetails.setProcessStatus(com.testing.mail.repo.entities.PROCESS.PROCESSED);
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
						destGroupEmailDetails.setProcessStatus(com.testing.mail.repo.entities.PROCESS.CONFLICT);
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
					sourceGroupEmailDetails.setProcessStatus(com.testing.mail.repo.entities.PROCESS.CONFLICT);
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
	private void SetDestFolderPath(EmailFlagsInfo emailFlagsInfo, EmailFolderInfo _emailInfo, String mailFolder,String destId) {
		if(isGoogleCombo() && "/".equals(emailWorkSpace.getToFolderId()) && !MappingUtils.isCustomFolder(mailFolder)) {
			_emailInfo.setDestFolderPath(mailFolder);
		}else {
			_emailInfo.setDestFolderPath(destId);
		}
	}
	
	private void updatePickingQueue(String emailWorkSpaceId,PROCESS processStatus,String errorDescription) {
		EmailPickingQueue moveQueue = 	connectorService.getEmailQueueRepoImpl().findByWorkSpace(emailWorkSpaceId);
		if(moveQueue!=null) {
			moveQueue.setProcessStatus(processStatus);
			moveQueue.setErrorDescription(errorDescription);
			connectorService.getEmailQueueRepoImpl().save(moveQueue);
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
			//pickMailBoxRules(emailFlagsInfo);
			//pickEmailSettings(emailFlagsInfo);
			//pickContacts();
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
	

	private EmailInfo createInfo(MailChanges mailChanges,EmailInfo destMail) {
		EmailInfo _emailInfo = new EmailInfo();
		_emailInfo.setSourceId(mailChanges.getSourceId());
		if(isGoogleCombo() && mailChanges.getMailFolder()!=null && mailChanges.getMailFolder().equalsIgnoreCase(destMail.getMailFolder())) {
			_emailInfo.setMailFolder(destMail.getMailFolder());
		}else {
			_emailInfo.setMailFolder(mailChanges.getMailFolder());
		}
		_emailInfo.setJobId(emailWorkSpace.getJobId());
		_emailInfo.setUserId(emailWorkSpace.getUserId());
		_emailInfo.setEmailWorkSpaceId(emailWorkSpace.getId());
		_emailInfo.setSubFolder(mailChanges.isSubFolder());
		_emailInfo.setFromCloudId(emailWorkSpace.getFromCloudId());
		_emailInfo.setToCloudId(emailWorkSpace.getToCloudId());
		if(!destMail.isAttachMents() && mailChanges.isAttachMents()) {
			_emailInfo.setAttachMents(true);
		}else {
			_emailInfo.setAttachMents(false);
		}
		_emailInfo.setBodyPreview(mailChanges.getBodyPreview());
		_emailInfo.setMetadata(emailWorkSpace.isMetadata());
		_emailInfo.setHtmlContent(mailChanges.isHtmlContent());
		if(mailChanges.getFromMail()!=null && fromAdminCloud.getDomains()!=null && fromAdminCloud.getDomains().contains(mailChanges.getFromMail().split(Const.ATTHERATE)[1]) && !mappedPairs.containsKey(mailChanges.getFromMail())) {
			String from = checkAndCreateGroupEmails(mailChanges.getFromMail());
			if(!from.equals(mailChanges.getFromMail())) {
				mappedPairs.put(mailChanges.getFromMail(), from);
			}
		}
		mailChanges.setToMail(mapMails(mailChanges.getToMail(), fromAdminCloud.getDomains()));
		if(mailChanges.getFromMail()!=null) {
			if(mappedPairs.containsKey(mailChanges.getFromMail())) {
				_emailInfo.setFromMail(mappedPairs.get(mailChanges.getFromMail()));
				_emailInfo.setFromName(mappedPairs.get(mailChanges.getFromMail()).split("@")[0]);
			}else {
				_emailInfo.setFromMail(mailChanges.getFromMail());
				_emailInfo.setFromName(mailChanges.getFromMail().split("@")[0]);
			}
		}
		mailChanges.setCc(mapMails(mailChanges.getCc(), fromAdminCloud.getDomains()));
		mailChanges.setBcc(mapMails(mailChanges.getBcc(), fromAdminCloud.getDomains()));
		_emailInfo.setToMail(mailChanges.getToMail());
		if(!mailChanges.isFolder()) {
			_emailInfo.setProcessStatus(EmailInfo.PROCESS.NOT_PROCESSED);
		}
		if(mailChanges.getOrder()>0) {
			_emailInfo.setProcessStatus(EmailInfo.PROCESS.THREAD_NOT_PROCESSED);
		}
		_emailInfo.setHtmlBodyContent(mailChanges.getHtmlBodyContent());
		_emailInfo.setDeleted(mailChanges.isDeleted());
		_emailInfo.setDraft(mailChanges.isDraft());
		_emailInfo.setSubject(mailChanges.getSubject());
		_emailInfo.setSourceParent(emailInfo.getSourceParent());
		_emailInfo.setMailCreatedTime(mailChanges.getMailCreatedTime().toString());
		_emailInfo.setMailModifiedTime(mailChanges.getMailModifiedTime().toString());
		_emailInfo.setDestId(destMail.getDestId());
		_emailInfo.setDestFolderPath(emailInfo.getDestFolderPath());
		_emailInfo.setDestThreadId(destMail.getDestThreadId());
		_emailInfo.setDestParent(destMail.getDestParent());
		_emailInfo.setMovedFolder(mailChanges.getMovedFolder());
		_emailInfo.setThreadId(destMail.getThreadId());
		_emailInfo.setFlagged(mailChanges.isFlagged());
		_emailInfo.setFolder(mailChanges.isFolder());
		_emailInfo.setRead(mailChanges.isRead());
		_emailInfo.setImportance(mailChanges.getImportance());
		_emailInfo.setErrorDescription("Existing Data");
		return _emailInfo;
	}

	private EmailInfo createInfo(MailChanges mailChanges) {
		EmailInfo _emailInfo = new EmailInfo();
		_emailInfo.setSourceId(mailChanges.getSourceId());
		_emailInfo.setFromCloud(emailWorkSpace.getFromCloud().name());
		_emailInfo.setToCloud(emailWorkSpace.getToCloud().name());
		_emailInfo.setMailFolder(mailChanges.getMailFolder());
		_emailInfo.setJobId(emailWorkSpace.getJobId());
		_emailInfo.setUserId(emailWorkSpace.getUserId());
		_emailInfo.setEmailWorkSpaceId(emailWorkSpace.getId());
		_emailInfo.setFromMail(mailChanges.getFromMail());
		_emailInfo.setAttachMents(mailChanges.isAttachMents());
		_emailInfo.setMailFolder(mailChanges.getMailFolder());
		_emailInfo.setReplyTo(mailChanges.getReplyTo());
		_emailInfo.setOriginalFrom(mailChanges.getFromMail());
		_emailInfo.setFromCloudId(emailWorkSpace.getFromCloudId());
		_emailInfo.setToCloudId(emailWorkSpace.getToCloudId());
		_emailInfo.setToMail(mailChanges.getToMail());
		if(!mailChanges.isFolder()) {
			_emailInfo.setProcessStatus(EmailInfo.PROCESS.NOT_PROCESSED);
		}
		if(mailChanges.getAttachmentIds()!=null && !mailChanges.getAttachmentIds().isEmpty()) {
			_emailInfo.setAttachmentIds(mailChanges.getAttachmentIds());
		}
		_emailInfo.setCc(mailChanges.getCc());
		_emailInfo.setBcc(mailChanges.getBcc());
		_emailInfo.setBodyPreview(mailChanges.getBodyPreview());
		_emailInfo.setMetadata(emailWorkSpace.isMetadata());
		_emailInfo.setHtmlContent(mailChanges.isHtmlContent());
		_emailInfo.setHtmlBodyContent(mailChanges.getHtmlBodyContent());
		_emailInfo.setDeleted(mailChanges.isDeleted());
		_emailInfo.setSubFolder(mailChanges.isSubFolder());
		_emailInfo.setDraft(mailChanges.isDraft());
		_emailInfo.setSubject(mailChanges.getSubject());
		_emailInfo.setSourceParent(emailInfo.getSourceParent());
		_emailInfo.setMovedFolder(mailChanges.getMovedFolder());
		_emailInfo.setMailCreatedTime(mailChanges.getMailCreatedTime().toString());
		_emailInfo.setMailModifiedTime(mailChanges.getMailModifiedTime().toString());
		_emailInfo.setDestFolderPath(emailInfo.getDestFolderPath());
		_emailInfo.setFlagged(mailChanges.isFlagged());
		_emailInfo.setFolder(mailChanges.isFolder());
		_emailInfo.setRead(mailChanges.isRead());
		_emailInfo.setImportance(mailChanges.getImportance());
		_emailInfo.setFromName(mailChanges.getFromName());
		_emailInfo.setErrorDescription("Newly created");
		_emailInfo.setOrder(mailChanges.getOrder());
		return _emailInfo;
	}
	private void getAndMapChanges() {
		List<EmailInfo>changesInfo = new ArrayList<>();
		List<MailChanges> modified = new ArrayList<>();
		List<String>dups = new ArrayList<>();
		List<MailChanges> mailChanges = connectorService.getMailChangesRepoImpl().findByWorkSpaceParent(emailWorkSpace.getLastEmailWorkSpaceId(), emailInfo.getSourceId(),false);
		String message = "Completed";
		log.info("===Total MailChanges=="+mailChanges.size()+"===for the workspaceId=="+emailWorkSpace.getId());
		if(!mailChanges.isEmpty()) {
			for(MailChanges changes : mailChanges) {
				changes.setProcessStatus(MailChanges.PROCESS.PROCESSED);
				if(dups.contains(changes.getSourceId())) {
					modified.add(changes);
					continue;
				}
				dups.add(changes.getSourceId());
				EmailInfo _info = null;
				if(changes!=null && changes.getSourceId()!=null) {
					log.info("==For the Change==="+changes.getId()+"===For the workSpaceId==="+emailWorkSpace.getId());
					EmailInfo info = connectorService.getEmailInfoRepoImpl().findBySourceId(emailWorkSpace.getJobId(), emailWorkSpace.getUserId(), changes.getSourceId());
					if(info!=null) {
						log.info("==Exisiting mail found=="+emailWorkSpace.getId()+"--"+emailInfo.getId());
						_info = createInfo(changes, info);
						_info.setDestThreadId(info.getDestThreadId());
						_info.setOrder(changes.getOrder());
						if(info.getMovedFolder()!=null && changes.getMovedFolder()!=null && !info.getMovedFolder().equalsIgnoreCase(changes.getMovedFolder())) {
							_info.setMailFolder(info.getMovedFolder());
							_info.setDeleted(true);
						}
						if(info.isFolder()) {
							continue;
						}
					}else {
						EmailInfo threadInfo = connectorService.getEmailInfoRepoImpl().findByThreadId(emailWorkSpace.getJobId(), emailWorkSpace.getUserId(), changes.getSourceId());
						_info = createInfo(changes);
						if(threadInfo!=null) {
							_info.setDestThreadId(threadInfo.getDestThreadId());
						}
						_info.setDeleted(false);
						log.info("==No Exisiting info found so going for creating a new EmailInfo for migraion=="+emailWorkSpace.getId()+"==changeID=="+changes.getId());
					}
					if(_info!=null)
						if(changes.getOrder()>1) {
							_info.setProcessStatus(EmailInfo.PROCESS.THREAD_NOT_PROCESSED);
						}
					changesInfo.add(_info);
					if(changesInfo.size()>20) {
						connectorService.getEmailInfoRepoImpl().save(changesInfo);
						changesInfo.clear();
					}
				}
				modified.add(changes);
			}
		}else {
			getUpdatedMails();
		}
		connectorService.getMailChangesRepoImpl().save(modified);
		modified.clear();
		if(!changesInfo.isEmpty()) {
			connectorService.getEmailInfoRepoImpl().save(changesInfo);
			changesInfo.clear();
		}
		emailInfo.setErrorDescription(message);
		emailInfo.setProcessStatus(PROCESS.PROCESSED);
		emailInfo.setPicking(false);
	}
	
}
