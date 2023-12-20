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

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.cloudfuze.mail.connectors.factory.MailServiceFactory;
import com.cloudfuze.mail.connectors.microsoft.data.AttachmentsData;
import com.cloudfuze.mail.constants.Const;
import com.cloudfuze.mail.constants.ExceptionConstants;
import com.cloudfuze.mail.dao.entities.ConnectFlags;
import com.cloudfuze.mail.dao.entities.EmailFlagsInfo;
import com.cloudfuze.mail.dao.entities.PermissionCache;
import com.cloudfuze.mail.dao.entities.UserGroups;
import com.cloudfuze.mail.exceptions.MailMigrationException;
import com.cloudfuze.mail.repo.entities.Clouds;
import com.cloudfuze.mail.repo.entities.Clouds.CLOUD_NAME;
import com.cloudfuze.mail.repo.entities.EmailInfo;
import com.cloudfuze.mail.repo.entities.EmailWorkSpace;
import com.cloudfuze.mail.repo.entities.GroupEmailDetails;
import com.cloudfuze.mail.repo.entities.MailChangeIds;
import com.cloudfuze.mail.repo.entities.MailChanges;
import com.cloudfuze.mail.service.DBConnectorService;
import com.cloudfuze.mail.utils.ConvertionUtils;
import com.cloudfuze.mail.utils.MappingUtils;
import com.cloudfuze.mail.utils.TimeUtils;

import lombok.extern.slf4j.Slf4j;

/**
 *For The Changes We will fetch the changes from the source based on changeId
*/


@Slf4j
public class MailChangesPickerTask implements Callable<Object> {

	private MailServiceFactory mailServiceFactory;
	private DBConnectorService connectorService; 
	EmailWorkSpace emailWorkSpace;
	MailChangeIds mailChangeId;
	Map<String, String> mappedPairs = new HashMap<>();
	Clouds fromCloud ;
	Clouds toCloud ;
	Clouds fromAdminCloud = null;
	Clouds toAdminCloud = null;



	public MailChangesPickerTask(MailServiceFactory mailServiceFactory, DBConnectorService connectorService,
			MailChangeIds emailInfo,EmailWorkSpace emailWorkSpace) {
		this.mailServiceFactory = mailServiceFactory;
		this.connectorService = connectorService;
		this.mailChangeId = emailInfo;
		this.emailWorkSpace = emailWorkSpace;
	}

	@Override
	public Object call() throws Exception {
		initiateMigration();
		return null;
	}

	private void initiateMigration() {
		Thread.currentThread().setName(getClass().getSimpleName());
		log.warn("===Entered for fetching the mails/Mail Folders for=="+emailWorkSpace.getId()+"==from mail=="+emailWorkSpace.getFromMailId()+"==toMail ID=="+emailWorkSpace.getToMailId()+"==fromFolder:="+emailWorkSpace.getFromFolderId());
		emailWorkSpace = connectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
		fromCloud = connectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getFromCloudId());
		toCloud = connectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getToCloudId());
		
		if(ObjectUtils.isEmpty(fromCloud) || ObjectUtils.isEmpty(toCloud) || StringUtils.isEmpty(mailChangeId.getLatestUpdatedId())) {
			mailChangeId.setErrorDescription(ExceptionConstants.REQUIRED_MISSING+" check the ids");
			mailChangeId.setProcessStatus(MailChangeIds.PROCESS.CONFLICT);
			connectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
			connectorService.getMailChangeIdRepoImpl().save(mailChangeId);
			return;
		}
		fromAdminCloud = connectorService.getCloudsRepoImpl().findOne(fromCloud.getAdminCloudId());
		toAdminCloud = connectorService.getCloudsRepoImpl().findOne(toCloud.getAdminCloudId());
		if(mailChangeId.getSourceId()!=null && mailChangeId.getSourceId().equals("/")) {
			mailChangeId.setErrorDescription("COMPLETED");
			mailChangeId.setProcessStatus(MailChangeIds.PROCESS.PROCESSED);
			connectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
			connectorService.getMailChangeIdRepoImpl().save(mailChangeId);
			return;
		}
		
		mappedPermissions();
		
		EmailFlagsInfo emailFlagsInfo = new EmailFlagsInfo();
		emailFlagsInfo.setAdminMemberId(fromCloud.getAdminMemberId());
		emailFlagsInfo.setCloudId(emailWorkSpace.getFromCloudId());
		emailFlagsInfo.setUserId(fromCloud.getUserId());
		emailFlagsInfo.setEmailId(emailWorkSpace.getFromMailId());
		
		if(emailWorkSpace.isCalendar()) {
			emailFlagsInfo.setStopCalendarSource(true);
		}
		try {
			//added for if the onetime Drats is empty getting for migrating in delta
			if(mailChangeId.getLatestUpdatedId()==null || mailChangeId.getLatestUpdatedId().isEmpty() || mailChangeId.getLatestUpdatedId().equals("0")) {
				getMails(emailFlagsInfo);
			}else {
				getUpdatedMails(emailFlagsInfo);
			}
			
			emailFlagsInfo.setCreatedTime(LocalDateTime.now().toString());
			String latestDeltaId = null;
			if(emailWorkSpace.getFromCloud().equals(CLOUD_NAME.GMAIL)) {
				latestDeltaId = emailFlagsInfo.getParentFolderId();
			}else {
				latestDeltaId = mailServiceFactory.getConnectorService(emailWorkSpace.getFromCloud()).getDeltaChangeId(emailFlagsInfo);
			}
			createMailChangeIds(mailChangeId, latestDeltaId);
		}catch(Exception e) {
			mailChangeId.setProcessStatus(MailChangeIds.PROCESS.CONFLICT);
			mailChangeId.setRetryCount(mailChangeId.getRetryCount()+1);
			mailChangeId.setErrorDescription(ExceptionUtils.getStackTrace(e));
		}
		finally {
			connectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
			connectorService.getMailChangeIdRepoImpl().save(mailChangeId);
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

	private void convertEmailFlagsToMailChanges(List<EmailFlagsInfo> emailFlagsInfos) throws MailMigrationException {
		List<MailChanges> emailInfos = new ArrayList<>();
		if(emailFlagsInfos!=null && !emailFlagsInfos.isEmpty()) {
			for(EmailFlagsInfo emailFlagsInfo : emailFlagsInfos) {
				MailChanges _emailInfo = new MailChanges();
				emailFlagsInfo.setTo(mapMails(emailFlagsInfo.getTo(), fromAdminCloud.getDomains()));
				emailFlagsInfo.setCc(mapMails(emailFlagsInfo.getCc(), fromAdminCloud.getDomains()));
				emailFlagsInfo.setBcc(mapMails(emailFlagsInfo.getBcc(), fromAdminCloud.getDomains()));
				_emailInfo.setCc(emailFlagsInfo.getCc());
				_emailInfo.setBcc(emailFlagsInfo.getBcc());
				_emailInfo.setEmailWorkSpaceId(emailWorkSpace.getId());
				_emailInfo.setUserId(emailWorkSpace.getUserId());
				_emailInfo.setJobId(emailWorkSpace.getJobId());
				_emailInfo.setMailChangeId(mailChangeId.getId());
				_emailInfo.setCreatedTime(LocalDateTime.now());
				_emailInfo.setMailCreatedTime(TimeUtils.convertStringToTime(emailFlagsInfo.getCreatedTime()));
				_emailInfo.setMailModifiedTime(TimeUtils.convertStringToTime(emailFlagsInfo.getSentTime()));
				_emailInfo.setFolder(emailFlagsInfo.isMailFolder());
				_emailInfo.setMailFolder(emailFlagsInfo.getFolder());
				_emailInfo.setImportance(emailFlagsInfo.getImportance());
				_emailInfo.setSourceParent(emailFlagsInfo.getParentFolderId()==null?emailFlagsInfo.getFolder():emailFlagsInfo.getParentFolderId());
				_emailInfo.setSubject(emailFlagsInfo.getSubject());
				_emailInfo.setSourceId(emailFlagsInfo.getId());
				if("sentitems".equals(mailChangeId.getSourceId())) {
					_emailInfo.setFromMail(emailWorkSpace.getToMailId());
				}else {
					_emailInfo.setFromMail(emailFlagsInfo.getFrom());
				}
				_emailInfo.setAttachMents(emailFlagsInfo.isHadAttachments());
				if(emailFlagsInfo.getAttachments()!=null && !emailFlagsInfo.getAttachments().isEmpty()) {
					List<String> attachs = new ArrayList<>();
					for(AttachmentsData data : emailFlagsInfo.getAttachments()) {
						attachs.add(data.getId()+":"+data.getName()+":"+data.getContentType()+":"+data.getSize()+":"+data.isInline());
					}
					_emailInfo.setAttachmentIds(attachs);
				}
				_emailInfo.setSubFolder(emailFlagsInfo.isSubFolder());
				_emailInfo.setOriginalFrom(emailFlagsInfo.getFrom());
				_emailInfo.setCreated(true);
				_emailInfo.setToMail(emailFlagsInfo.getTo());
				_emailInfo.setHtmlBodyContent(emailFlagsInfo.getHtmlMessage());
				_emailInfo.setBodyPreview(emailFlagsInfo.getBodyPreview());
				_emailInfo.setHtmlContent(emailFlagsInfo.isHtmlContent());
				_emailInfo.setDeleted(emailFlagsInfo.isDeleted());
				_emailInfo.setFlagged(emailFlagsInfo.isFlagged());
				_emailInfo.setRead(emailFlagsInfo.isRead());
				_emailInfo.setFromName(emailFlagsInfo.getFromName());
				_emailInfo.setProcessStatus(MailChanges.PROCESS.NOT_PROCESSED);
				_emailInfo.setOrder(emailFlagsInfo.getOrder());
				_emailInfo.setMovedFolder(emailFlagsInfo.getFolder());
				_emailInfo.setAttachMents(emailFlagsInfo.isHadAttachments());
				_emailInfo.setReplyTo(emailFlagsInfo.getReplyTo());
				_emailInfo.setThreadId(emailFlagsInfo.getThreadId());
				_emailInfo.setFromCloudId(emailWorkSpace.getFromCloudId());
				_emailInfo.setToCloudId(emailWorkSpace.getToCloudId());
				_emailInfo.setMetadata(emailWorkSpace.isMetadata());
				_emailInfo.setSourceParent(_emailInfo.getSourceParent());
				_emailInfo.setDraft(emailFlagsInfo.isDraft());
				_emailInfo.setFlagged(emailFlagsInfo.isFlagged());
				emailInfos.add(_emailInfo);
				if(emailInfos.size()>20) {
					connectorService.getMailChangesRepoImpl().save(emailInfos);
					emailInfos.clear();
				}
			}
			connectorService.getMailChangesRepoImpl().save(emailInfos);
			emailInfos.clear();
		}
	}

	
	private void getUpdatedMails(EmailFlagsInfo emailFlagsInfo) {
		emailFlagsInfo.setNextPageToken(null);
		log.warn("===Get UpdatedMAils for the user=="+emailWorkSpace.getFromMailId()+"==For the MailFolder=="+mailChangeId.getMailFolder());
		List<EmailFlagsInfo> emails = null;
		if(MappingUtils.isCustomFolder(mailChangeId.getMailFolder())) {
			//if it is customFolder checking with the SourceId
			emailFlagsInfo.setFolder(mailChangeId.getSourceId());
		}else {
			//Added for gmail System mail folders are UpperCase so converting to upperCase
			emailFlagsInfo.setFolder(mailChangeId.getSourceId());
		}
		
		emails = mailServiceFactory.getConnectorService(emailWorkSpace.getFromCloud()).getDeltaChanges(emailFlagsInfo, mailChangeId.getLatestUpdatedId());

		if(ObjectUtils.isEmpty(emails) || emails.isEmpty()) {
			mailChangeId.setErrorDescription(ExceptionConstants.NO_MAILS_FOLDER);
		}else {
			mailChangeId.setErrorDescription(ExceptionConstants.STARTED);
			mailChangeId.setThreadBy(null);
		}
		convertEmailFlagsToMailChanges(emails);
		mailChangeId.setCount(mailChangeId.getCount()+emails.size());
		mailChangeId.setProcessStatus(MailChangeIds.PROCESS.PROCESSED);
	}
 
	private void createMailChangeIds(MailChangeIds emailInfo,String latestDeltaId) {
		MailChangeIds changeIds = new MailChangeIds();
		changeIds.setId(emailInfo.getId());
		changeIds.setMailFolder(emailInfo.getMailFolder());
		changeIds.setLatestUpdatedId(latestDeltaId);
		changeIds.setEmailWorkSpaceId(emailWorkSpace.getId());//Changing from lastWorkspaceId to currentId
		changeIds.setUserId(emailInfo.getUserId());
		changeIds.setCreatedTime(LocalDateTime.now());
		changeIds.setFolder(true);
		changeIds.setJobId(emailWorkSpace.getJobId());
		changeIds.setFromCloudId(emailWorkSpace.getFromCloudId());
		changeIds.setDestFolderPath(emailInfo.getDestFolderPath());
		changeIds.setToCloudId(emailWorkSpace.getToCloudId());
		changeIds.setMetadata(emailWorkSpace.isMetadata());
		changeIds.setSourceId(emailInfo.getSourceId());

		Date date = Calendar.getInstance().getTime();
		
		String changeHours[] = emailWorkSpace.getChangeHours().split("H");
		if(changeHours!=null && changeHours.length>0) {
			date.setHours(date.getHours()+Integer.parseInt(changeHours[0]));
			date.setMinutes(date.getMinutes()+Integer.parseInt(changeHours[1].substring(0,changeHours[1].length()-1)));
		}
		changeIds.setNextPickTime(date);
		connectorService.getMailChangeIdRepoImpl().save(changeIds);
		MailChanges changes = new MailChanges();
		changes.setEmailWorkSpaceId(emailWorkSpace.getId());
		changes.setUserId(emailWorkSpace.getUserId());
		changes.setSourceParent("/");
		changes.setSourceId(emailInfo.getSourceId());
		// for custom folders need to add here so those won't create again
		if(isGoogleCombo() && MappingUtils.isCustomFolder(emailInfo.getSourceId())){
			EmailInfo info = connectorService.getEmailInfoRepoImpl().findBySourceId(emailWorkSpace.getJobId(), emailWorkSpace.getUserId(), emailInfo.getSourceId());
			if(info!=null) {
				changes.setMailFolder(info.getMailFolder());
			}
		}
		changes.setJobId(emailWorkSpace.getJobId());
		changes.setToCloudId(emailWorkSpace.getToCloudId());
		changes.setFolder(true);
		connectorService.getMailChangesRepoImpl().save(changes);
	}

	
	/**
	*Getting the Mapped Pairs from the PermissionCache based on FromAdminCloudId,ToAdminCloudId,UserId
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
			log.error(ExceptionUtils.getStackTrace(e));
		}
		return false;
	}
	
	
	private void getMails(EmailFlagsInfo emailFlagsInfo) {
		boolean hadData = false;
		MailChangeIds.PROCESS processStatus = MailChangeIds.PROCESS.NOT_PROCESSED;
		String message = "Completed";
		long count = 0;
		log.warn("+====STARTED ON==="+emailWorkSpace.getId()+"=="+LocalDateTime.now());
		if(mailChangeId.getMailFolder()!=null && mailChangeId.getMailFolder().startsWith("/")) {
			mailChangeId.setMailFolder(mailChangeId.getMailFolder().replace("/", ""));
		}
		emailFlagsInfo.setFolder(mailChangeId.getMailFolder()==null?mailChangeId.getSourceId():mailChangeId.getMailFolder());
		do {
			List<EmailFlagsInfo> emails = new ArrayList<>();
			log.warn("==Fetching emails for the User==="+emailWorkSpace.getFromMailId()+"===MailFolder==="+mailChangeId.getMailFolder()+"==WorkSpaceId=="+emailWorkSpace.getId()+"==Total Fetched UptoNow=="+count);
			emails = mailServiceFactory.getConnectorService(emailWorkSpace.getFromCloud()).getListOfMails(emailFlagsInfo);

			if(!hadData && (ObjectUtils.isEmpty(emails) || emails.isEmpty())) {
				processStatus = MailChangeIds.PROCESS.PROCESSED;
				message = ExceptionConstants.NO_MAILS_FOLDER;
			}else {
				hadData = true;	
			}
			count = count+emails.size();
			convertEmailFlagsToMailChanges(emails);
			mailChangeId.setCount(mailChangeId.getCount()+emails.size());
			mailChangeId.setNextPageToken(emailFlagsInfo.getNextPageToken());
			connectorService.getMailChangeIdRepoImpl().save(mailChangeId);
			if(emails.size()<50) {
				processStatus = MailChangeIds.PROCESS.PROCESSED;
				emails.clear();
				break;
			}
		}while(emailFlagsInfo.getNextPageToken()!=null );
		emailFlagsInfo.setCloudId(emailWorkSpace.getFromCloudId());
		mailChangeId.setErrorDescription(message);
		mailChangeId.setProcessStatus(processStatus);
	}

	private boolean isGoogleCombo() {
		return emailWorkSpace.getFromCloud().equals(CLOUD_NAME.GMAIL) && emailWorkSpace.getToCloud().equals(CLOUD_NAME.GMAIL);
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
			sourceGroupEmailDetails.setUserId(emailWorkSpace.getUserId());
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
					destGroupEmailDetails.setUserId(emailWorkSpace.getUserId());
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
				log.error(ExceptionUtils.getStackTrace(e));
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
				String _attendees = attendees.split(":")[0];
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
			String _attendees = attendees.split(Const.HASHTAG)[0];
			if(mappedPairs.containsKey(_attendees)) {
				attends.add(mappedPairs.get(_attendees));
			}else {
				attends.add(_attendees);
			}
		}
		return attends;
	}
}
