package com.testing.mail.connectors.management;

import java.time.LocalDate;
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

import com.testing.mail.connectors.factory.MailServiceFactory;
import com.testing.mail.connectors.microsoft.data.AttachmentsData;
import com.testing.mail.constants.Const;
import com.testing.mail.constants.ExceptionConstants;
import com.testing.mail.dao.entities.CalenderFlags;
import com.testing.mail.dao.entities.ConnectFlags;
import com.testing.mail.dao.entities.EmailFlagsInfo;
import com.testing.mail.dao.entities.PermissionCache;
import com.testing.mail.dao.entities.UserGroups;
import com.testing.mail.exceptions.MailMigrationException;
import com.testing.mail.repo.entities.CalenderInfo;
import com.testing.mail.repo.entities.Clouds;
import com.testing.mail.repo.entities.EmailWorkSpace;
import com.testing.mail.repo.entities.GroupEmailDetails;
import com.testing.mail.repo.entities.MailChangeIds;
import com.testing.mail.repo.entities.MailChanges;
import com.testing.mail.repo.entities.Clouds.CLOUD_NAME;
import com.testing.mail.service.DBConnectorService;
import com.testing.mail.utils.ConvertionUtils;
import com.testing.mail.utils.EventRangeUtils;
import com.testing.mail.utils.TimeUtils;

import lombok.extern.slf4j.Slf4j;

/**
 For The Changes We will fetch the changes from the source based on changeId
 */


@Slf4j
public class CalendarChangesPickerTask implements Callable<Object> {

	private MailServiceFactory mailServiceFactory;
	private DBConnectorService connectorService; 
	EmailWorkSpace emailWorkSpace;
	MailChangeIds mailChangeId;
	Map<String, String> mappedPairs = new HashMap<>();
	Clouds fromAdminCloud = null;
	Clouds toAdminCloud = null;
	Clouds fromCloud ;
	Clouds toCloud ;



	public CalendarChangesPickerTask(MailServiceFactory mailServiceFactory, DBConnectorService connectorService,
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
		log.warn("===Entered for fetching the Calendars/Events for=="+emailWorkSpace.getId()+"==from mail=="+emailWorkSpace.getFromMailId()+"==toMail ID=="+emailWorkSpace.getToMailId()+"==fromFolder:="+emailWorkSpace.getFromFolderId());
		emailWorkSpace = connectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
		fromCloud = connectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getFromCloudId());
		toCloud = connectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getToCloudId());
		if(ObjectUtils.isEmpty(fromCloud) || ObjectUtils.isEmpty(toCloud) || StringUtils.isEmpty(mailChangeId.getLatestUpdatedId())) {
			mailChangeId.setErrorDescription(ExceptionConstants.REQUIRED_MISSING+" check the ids");
			mailChangeId.setProcessStatus(MailChangeIds.PROCESS.CONFLICT);
			return;
		}
		if(mailChangeId.getSourceId()!=null && mailChangeId.getSourceId().equals("/")) {
			mailChangeId.setErrorDescription("COMPLETED");
			mailChangeId.setProcessStatus(MailChangeIds.PROCESS.PROCESSED);
			return;
		}
		fromAdminCloud = connectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getFromAdminCloud());
		toAdminCloud = connectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getToAdminCloud());
		mappedPermissions();
		EmailFlagsInfo emailFlagsInfo = new EmailFlagsInfo();
		emailFlagsInfo.setAdminMemberId(fromCloud.getAdminMemberId());
		emailFlagsInfo.setCloudId(emailWorkSpace.getFromCloudId());
		emailFlagsInfo.setUserId(fromCloud.getUserId());
		emailFlagsInfo.setEmailId(emailWorkSpace.getFromMailId());
		try {
			if(mailChangeId.getLatestUpdatedId()==null || mailChangeId.getLatestUpdatedId().isEmpty() || mailChangeId.getLatestUpdatedId().equals("0")) {
				getMails();
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
	private void getMails() {
		boolean hadData = false;
		MailChangeIds.PROCESS processStatus = MailChangeIds.PROCESS.NOT_PROCESSED;
		String message = "Completed";
		CalenderFlags emailFlagsInfo = new CalenderFlags(); 
		emailFlagsInfo.setNextPageToken(mailChangeId.getNextPageToken());
		emailFlagsInfo.setCalendar(mailChangeId.getMailFolder());
		emailFlagsInfo.setCloudId(emailWorkSpace.getFromCloudId());
		emailFlagsInfo.setId(mailChangeId.getSourceId());
		long count = 0;
		log.info("+====STARTED ON==="+emailWorkSpace.getId()+"=="+LocalDateTime.now());
		do {
			List<CalenderInfo> emails = new ArrayList<>();
			log.info("==Fetching emails for the USer==="+emailWorkSpace.getFromMailId()+"===MailFolder==="+mailChangeId.getMailFolder()+"==WorkSpaceId=="+emailWorkSpace.getId()+"==Total Fetched UptoNow=="+count);
			emailFlagsInfo.setNextPageToken(mailChangeId.getNextPageToken());
			if(mailChangeId.getSourceId()!=null && mailChangeId.getSourceId().equals("/")) {
				emails = mailServiceFactory.getConnectorService(emailWorkSpace.getFromCloud()).getCalendars(emailFlagsInfo);
				convertCalendarFlagsToCalendarInfo(emails);
			}else {
				emailFlagsInfo.setId(mailChangeId.getSourceId());
				emails = mailServiceFactory.getConnectorService(emailWorkSpace.getFromCloud()).getCalendarEvents(emailFlagsInfo);
				convertCalendarFlagsToEventsInfo(emails);
			}

			if(!hadData && (ObjectUtils.isEmpty(emails) || (emails!=null && emails.isEmpty()))) {
				message = "no Calendar Events to migrate";
			}else {
				hadData = true;	
			}
			processStatus = MailChangeIds.PROCESS.PROCESSED;
			if(emails!=null)
				count = count+emails.size();
			if(mailChangeId.getNextPageToken()!=null && mailChangeId.getNextPageToken().equals(emailFlagsInfo.getNextPageToken())){
				break;
			}
			mailChangeId.setNextPageToken(emailFlagsInfo.getNextPageToken());
			connectorService.getMailChangeIdRepoImpl().save(mailChangeId);
		}while(mailChangeId.getNextPageToken()!=null);
		mailChangeId.setCount(count);
		if(mailChangeId.getSourceId()!=null && mailChangeId.getSourceId().equals("/")) {
			processStatus = MailChangeIds.PROCESS.PROCESSED;
		}
		mailChangeId.setErrorDescription(message);
		mailChangeId.setProcessStatus(processStatus);
		mailChangeId.setPicking(false);
		if(!processStatus.equals(MailChangeIds.PROCESS.PROCESSED)) {
			mailChangeId.setThreadBy(null);
		}
		if(emailWorkSpace.isPreScan()) {
			mailChangeId.setProcessStatus(MailChangeIds.PROCESS.PROCESSED);
			mailChangeId.setErrorDescription("PreScan");
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
				_emailInfo.setEvents(true);
				_emailInfo.setEmailWorkSpaceId(emailWorkSpace.getId());
				_emailInfo.setUserId(emailWorkSpace.getUserId());
				_emailInfo.setJobId(emailWorkSpace.getJobId());
				_emailInfo.setMailFolder(emailFlagsInfo.getFolder());
				_emailInfo.setMailChangeId(mailChangeId.getId());
				_emailInfo.setCreatedTime(LocalDateTime.now());
				_emailInfo.setStartTime(emailFlagsInfo.getStartTime());
				_emailInfo.setEndTime(emailFlagsInfo.getEndTime());
				_emailInfo.setFolder(emailFlagsInfo.isMailFolder());
				_emailInfo.setMailFolder(emailFlagsInfo.getFolder());
				_emailInfo.setSourceParent(mailChangeId.getSourceId());
				_emailInfo.setSubject(emailFlagsInfo.getSubject());
				_emailInfo.setSourceId(emailFlagsInfo.getId());
				_emailInfo.setFolder(emailFlagsInfo.isMailFolder());
				List<String>attends = new ArrayList<>();
				if(emailFlagsInfo.getTo()!=null) {
					attends = mapMails(emailFlagsInfo.getTo(),fromAdminCloud.getDomains());
				}
				_emailInfo.setDeleted(emailFlagsInfo.isDeleted());
				emailFlagsInfo.setTo(attends);
				if(emailFlagsInfo.getAttachments()!=null && !emailFlagsInfo.getAttachments().isEmpty()) {
					List<String>attachs = new ArrayList<>();
					for(AttachmentsData attach : emailFlagsInfo.getAttachments()) {
						attachs.add(attach.getId()+":"+attach.getName()+":"+attach.getContentType()+":"+attach.getParentFolderId());
					}
					_emailInfo.setAttachmentIds(attachs);
				}
				_emailInfo.setFromMail(emailFlagsInfo.getFrom());
				_emailInfo.setOriginalFrom(emailFlagsInfo.getFrom());
				_emailInfo.setToMail(emailFlagsInfo.getTo());
				_emailInfo.setHtmlBodyContent(emailFlagsInfo.getHtmlMessage());
				_emailInfo.setBodyPreview(emailFlagsInfo.getBodyPreview());
				_emailInfo.setHtmlContent(emailFlagsInfo.isHtmlContent());
				_emailInfo.setParent(mailChangeId.getSourceId());
				_emailInfo.setTimeZone(emailFlagsInfo.getTimeZone());
				_emailInfo.setCc(emailFlagsInfo.getCc());
				_emailInfo.setDeleted(emailFlagsInfo.isDeleted());
				_emailInfo.setFlagged(emailFlagsInfo.isFlagged());
				_emailInfo.setRead(emailFlagsInfo.isRead());
				_emailInfo.setFromName(emailFlagsInfo.getFromName());
				_emailInfo.setEndTimeZone(emailFlagsInfo.getEndTimeZone());
				_emailInfo.setProcessStatus(MailChanges.PROCESS.NOT_PROCESSED);
				_emailInfo.setAttachMents(emailFlagsInfo.isHadAttachments());
				_emailInfo.setReplyTo(emailFlagsInfo.getReplyTo());
				_emailInfo.setThreadId(emailFlagsInfo.getThreadId());
				_emailInfo.setFromCloudId(emailWorkSpace.getFromCloudId());
				_emailInfo.setToCloudId(emailWorkSpace.getToCloudId());
				_emailInfo.setMetadata(emailWorkSpace.isMetadata());
				_emailInfo.setDraft(emailFlagsInfo.isDraft());
				_emailInfo.setFlagged(emailFlagsInfo.isFlagged());
				_emailInfo.setRange(emailFlagsInfo.getRange());
				_emailInfo.setPrimary(mailChangeId.isPrimaryCalendar());
				_emailInfo.setLocation(emailFlagsInfo.getLocation());
				_emailInfo.setRecurrenceType(emailFlagsInfo.getRecurrenceType());
				checkFutureEvent(_emailInfo.getEndTime(),_emailInfo,_emailInfo.getRange());
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
		log.info("===Get UpdatedMails for the user=="+emailWorkSpace.getFromMailId()+"==For the MailFolder=="+mailChangeId.getMailFolder());
		List<EmailFlagsInfo> emails = null;
		emailFlagsInfo.setFolder(mailChangeId.getMailFolder()==null ? mailChangeId.getSourceId():mailChangeId.getMailFolder());
		emailFlagsInfo.setEvents(true);
		emails = mailServiceFactory.getConnectorService(emailWorkSpace.getFromCloud()).getDeltaChanges(emailFlagsInfo, mailChangeId.getLatestUpdatedId());

		if(ObjectUtils.isEmpty(emails) || (emails!=null && emails.isEmpty())) {
			mailChangeId.setErrorDescription("no Updated Folder mails to migrate");
		}else {
			mailChangeId.setErrorDescription("STARTED");
			mailChangeId.setThreadBy(null);
		}
		convertEmailFlagsToMailChanges(emails);
		mailChangeId.setCount(mailChangeId.getCount()+(emails!=null?emails.size():0));
		mailChangeId.setProcessStatus(MailChangeIds.PROCESS.PROCESSED);
	}



	private void createMailChangeIds(MailChangeIds emailInfo,String latestDeltaId) {
		MailChangeIds changeIds = new MailChangeIds();
		changeIds.setMailFolder(emailInfo.getMailFolder());
		changeIds.setLatestUpdatedId(latestDeltaId);
		changeIds.setEmailWorkSpaceId(emailWorkSpace.getId());
		changeIds.setUserId(emailInfo.getUserId());
		changeIds.setCreatedTime(LocalDateTime.now());
		changeIds.setFolder(true);
		changeIds.setJobId(emailWorkSpace.getJobId());
		changeIds.setFromCloudId(emailWorkSpace.getFromCloudId());
		changeIds.setToCloudId(emailWorkSpace.getToCloudId());
		changeIds.setMetadata(emailWorkSpace.isMetadata());
		changeIds.setSourceId(emailInfo.getSourceId());
		changeIds.setDestFolderPath(emailInfo.getDestFolderPath());

		Date date = Calendar.getInstance().getTime();
		String changeHours[] = emailWorkSpace.getChangeHours().split("H");
		if(changeHours!=null && changeHours.length>1) {
			date.setHours(date.getHours()+Integer.parseInt(changeHours[0]));
			date.setMinutes(date.getMinutes()+Integer.parseInt(changeHours[1].substring(0,changeHours[1].length()-1)));
		}
		changeIds.setNextPickTime(date);
		connectorService.getMailChangeIdRepoImpl().save(changeIds);
		MailChanges changes = new MailChanges();
		changes.setEmailWorkSpaceId(emailWorkSpace.getId());
		changes.setUserId(emailWorkSpace.getUserId());
		changes.setSourceParent("/");
		changes.setEvents(true);
		changes.setSourceId(emailInfo.getSourceId());
		changes.setJobId(emailWorkSpace.getJobId());
		changes.setParent("/");
		changes.setPrimary(changeIds.isPrimaryCalendar());
		changes.setToCloudId(emailWorkSpace.getToCloudId());
		changes.setFolder(true);
		connectorService.getMailChangesRepoImpl().save(changes);




	}


	/**
	 * Mapping the PermissionCache Based on <b>SoruceAdmin</b> , <b>DestAdminCloud</b> and  <b>UserId </b>
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


	/**
	 *Storing the Fetched CalendarInfos in DataBase
	 */
	private void convertCalendarFlagsToEventsInfo(List<CalenderInfo> calenderInfos) throws MailMigrationException {

		List<MailChanges> emailInfos = new ArrayList<>();
		if(calenderInfos!=null && !calenderInfos.isEmpty()) {
			for(CalenderInfo emailFlagsInfo : calenderInfos) {
				MailChanges _emailInfo = new MailChanges();
				_emailInfo.setEvents(true);
				_emailInfo.setEmailWorkSpaceId(emailWorkSpace.getId());
				_emailInfo.setUserId(emailWorkSpace.getUserId());
				_emailInfo.setJobId(emailWorkSpace.getJobId());
				_emailInfo.setMailFolder(emailFlagsInfo.getSourceParent());
				_emailInfo.setMailChangeId(mailChangeId.getId());
				_emailInfo.setCreatedTime(LocalDateTime.now());
				_emailInfo.setStartTime(emailFlagsInfo.getStartTime());
				_emailInfo.setEndTime(emailFlagsInfo.getEndTime());
				_emailInfo.setMailFolder(emailFlagsInfo.getSourceParent());
				_emailInfo.setSourceParent(mailChangeId.getSourceId());
				_emailInfo.setSubject(emailFlagsInfo.getSubject());
				_emailInfo.setSourceId(emailFlagsInfo.getId());
				_emailInfo.setFolder(emailFlagsInfo.isCalender());
				List<String>attends = new ArrayList<>();
				if(emailFlagsInfo.getAttendees()!=null) {
					attends = mapMails(emailFlagsInfo.getAttendees(),fromAdminCloud.getDomains());
				}
				_emailInfo.setToMail(attends);
				_emailInfo.setDeleted(emailFlagsInfo.isDeleted());
				if(emailFlagsInfo.getAttachmentIds()!=null && !emailFlagsInfo.getAttachmentIds().isEmpty()) {
					_emailInfo.setAttachmentIds(emailFlagsInfo.getAttachmentIds());
				}
				_emailInfo.setFromMail(emailFlagsInfo.getOrganizer());
				_emailInfo.setOriginalFrom(emailFlagsInfo.getOriginalFrom());
				_emailInfo.setHtmlBodyContent(emailFlagsInfo.getHtmlBodyContent());
				_emailInfo.setBodyPreview(emailFlagsInfo.getBodyPreview());
				_emailInfo.setHtmlContent(emailFlagsInfo.isHtmlContent());
				_emailInfo.setParent(mailChangeId.getSourceId());
				_emailInfo.setTimeZone(emailFlagsInfo.getTimeZone());
				_emailInfo.setDeleted(emailFlagsInfo.isDeleted());
				_emailInfo.setFlagged(emailFlagsInfo.isFlagged());
				_emailInfo.setFromName(emailFlagsInfo.getFromName());
				_emailInfo.setEndTimeZone(emailFlagsInfo.getEndTimeZone());
				_emailInfo.setProcessStatus(MailChanges.PROCESS.NOT_PROCESSED);
				_emailInfo.setFromCloudId(emailWorkSpace.getFromCloudId());
				_emailInfo.setToCloudId(emailWorkSpace.getToCloudId());
				_emailInfo.setMetadata(emailWorkSpace.isMetadata());
				_emailInfo.setRange(emailFlagsInfo.getRange());
				_emailInfo.setPrimary(mailChangeId.isPrimaryCalendar());
				_emailInfo.setLocation(emailFlagsInfo.getLocation());
				_emailInfo.setRecurrenceType(emailFlagsInfo.getRecurrenceType());
				checkFutureEvent(_emailInfo.getEndTime(),_emailInfo,_emailInfo.getRange());
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


	private void convertCalendarFlagsToCalendarInfo(List<CalenderInfo> calenderInfos) throws MailMigrationException {
		List<CalenderInfo> emailInfos = new ArrayList<>();
		List<String>ids = new ArrayList<>();
		if(calenderInfos!=null && !calenderInfos.isEmpty()) {
			for(CalenderInfo emailFlagsInfo : calenderInfos) {

				CalenderInfo.PROCESS process = CalenderInfo.PROCESS.NOT_PROCESSED;
				CalenderInfo _emailInfo = new CalenderInfo();	
				_emailInfo.setEnv(System.getProperty("license.server.url"));
				_emailInfo.setFromCloudId(emailWorkSpace.getFromCloudId());
				_emailInfo.setToCloudId(emailWorkSpace.getToCloudId());
				_emailInfo.setFromName(emailFlagsInfo.getFromName());
				List<String>attends = new ArrayList<>();
				_emailInfo.setExternalOrganizer(emailFlagsInfo.isExternalOrganizer());
				_emailInfo.setAttendees(attends);
				_emailInfo.setAllDay(emailFlagsInfo.isAllDay());
				_emailInfo.setOriginalFrom(emailFlagsInfo.getFromName());
				_emailInfo.setSourceId(emailFlagsInfo.getId());
				//adding for the organizer mapping if exists in destination
				_emailInfo.setOrganizer(mappedPairs.containsKey(emailFlagsInfo.getOrganizer())?mappedPairs.get(emailFlagsInfo.getOrganizer()):emailFlagsInfo.getOrganizer());

				if(mailChangeId.isPrimaryCalendar() && mailChangeId.isCalendar()) {
					String calendar =emailWorkSpace.getToCloud().equals(CLOUD_NAME.OUTLOOK)?"Calendar":emailWorkSpace.getToMailId();
					_emailInfo.setParentName(calendar);
				}else {
					_emailInfo.setParentName(emailFlagsInfo.getSubject());
				}
				_emailInfo.setOnlineMeeting(emailFlagsInfo.isOnlineMeeting());
				_emailInfo.setOnlineMeetingUrl(emailFlagsInfo.getOnlineMeetingUrl());
				_emailInfo.setBodyPreview(emailFlagsInfo.getBodyPreview());
				_emailInfo.setHtmlBodyContent(emailFlagsInfo.getHtmlBodyContent());
				_emailInfo.setHtmlContent(true);
				_emailInfo.setColor(emailFlagsInfo.getColor());
				_emailInfo.setCalenderCreatedTime(emailFlagsInfo.getCalenderCreatedTime());
				_emailInfo.setCalenderModifiedTime(emailFlagsInfo.getCalenderModifiedTime());
				_emailInfo.setCategories(emailFlagsInfo.getCategories());
				_emailInfo.setEndTimeZone(emailFlagsInfo.getEndTimeZone());
				if(emailFlagsInfo.isAttachMents()) {
					_emailInfo.setAttachmentIds(emailFlagsInfo.getAttachmentIds());
					_emailInfo.setAttachMents(true);
				}
				_emailInfo.setDeleted(emailFlagsInfo.isDeleted());
				_emailInfo.setCalender(emailFlagsInfo.isCalender());
				_emailInfo.setStartTime(emailFlagsInfo.getStartTime());
				_emailInfo.setTimeZone(emailFlagsInfo.getTimeZone());
				_emailInfo.setEndTime(emailFlagsInfo.getEndTime());
				_emailInfo.setPrimaryCalender(emailFlagsInfo.isPrimaryCalender());
				_emailInfo.setSubject(emailFlagsInfo.getSubject());
				_emailInfo.setSourceParent(mailChangeId.getSourceId());
				_emailInfo.setDestFolderPath(mailChangeId.getDestFolderPath());
				_emailInfo.setRecurrenceType(emailFlagsInfo.getRecurrenceType());
				_emailInfo.setRange(emailFlagsInfo.getRange());
				_emailInfo.setLocation(emailFlagsInfo.getLocation());
				_emailInfo.setRemainders(emailFlagsInfo.getRemainders());
				_emailInfo.setVisibility(emailFlagsInfo.getVisibility());
				_emailInfo.setShowAs(emailFlagsInfo.getShowAs());
				_emailInfo.setReadOnly(emailFlagsInfo.isReadOnly());
				if(_emailInfo.isCalender()) {
					if(emailFlagsInfo.isReadOnly()) {
						process = CalenderInfo.PROCESS.PROCESSED;
						_emailInfo.setErrorDescription("Read Only Can't edit or Create events");
					}else {
						process = CalenderInfo.PROCESS.NOT_STARTED;
					}
				}
				_emailInfo.setEmailWorkSpaceId(emailWorkSpace.getId());
				_emailInfo.setJobId(emailWorkSpace.getJobId());
				_emailInfo.setUserId(emailWorkSpace.getUserId());
				_emailInfo.setICalUId(emailFlagsInfo.getICalUId());
				_emailInfo.setPreScan(emailWorkSpace.isPreScan());
				if(emailWorkSpace.isPreScan() && !_emailInfo.isCalender()) {
					_emailInfo.setPreScan(true);
					process = CalenderInfo.PROCESS.PROCESSED;
				}
				_emailInfo.setProcessStatus(process);

				ids.add(emailFlagsInfo.getId().split("_")[0]);
				emailInfos.add(_emailInfo);
				if(emailInfos.size()>20) {
					connectorService.getCalendarInfoRepoImpl().saveCalendars(emailInfos);
					emailInfos.clear();
				}
			}
			connectorService.getCalendarInfoRepoImpl().saveCalendars(emailInfos);
			emailInfos.clear();
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
				String _attendees = attendees.split(":")[0];
				if(mappedPairs.containsKey(_attendees)) {
					attends.add(attendees.replace(_attendees, mappedPairs.get(_attendees)));
				}else {
					if(domains!=null && domains.contains(_attendees.split(Const.ATTHERATE)[1])) {
						String destMail = checkAndCreateGroupEmails(_attendees);
						if(!destMail.equals(_attendees)) {
							mappedPairs.put(_attendees, destMail);
							attends.add(attendees.replace(_attendees, mappedPairs.get(_attendees)));
							//createPermissionCacheForGroup(_attendees, destMail);
						}else if(mappedPairs.containsKey(_attendees)){
							attends.add(attendees.replace(_attendees, mappedPairs.get(_attendees)));
						}else {
							attends.add(attendees);
						}
					}else {
						attends.add(attendees);
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


	private void checkFutureEvent(String endTime,MailChanges calenderInfo,String recurenceRule) {
		if(emailWorkSpace.isFutureEvents() && !emailWorkSpace.isPreScan() && !calenderInfo.isDeleted()) {
			if(null==recurenceRule) {
				if(!TimeUtils.checkEventType(endTime,calenderInfo.getEndTimeZone())) {
					calenderInfo.setProcessStatus(MailChanges.PROCESS.PROCESSED);
					calenderInfo.setErrorDescription("OldEvent");
				}
			}else {
				if(checkRecurenceRuleForOlderEvents(recurenceRule,calenderInfo.getRecurrenceType())) {
					calenderInfo.setProcessStatus(MailChanges.PROCESS.PROCESSED);
					calenderInfo.setErrorDescription("OldEvent");
				}
			}
		}
	}

	private boolean checkRecurenceRuleForOlderEvents(String rule,String recurenceType) {
		if(CLOUD_NAME.GMAIL.equals(emailWorkSpace.getFromCloud())) {
			EventRangeUtils range = EventRangeUtils.setRangeForGmailAsSource(rule,recurenceType);
			return checkOldEvent(range);
		}else {
			EventRangeUtils range = EventRangeUtils.setRangeForOutlookAsSource(rule,recurenceType);
			return checkOldEvent(range);
		}
	}

	private boolean checkOldEvent(EventRangeUtils range) {
		if(range.getEndDate()!=null) {
			if(LocalDate.now().isAfter(TimeUtils.convertTimeToLocalDate(range.getEndDate()))){
				return true;
			}
		}else if(range.getLastOccurence()!=null) {
			if(LocalDate.now().isAfter(range.getLastOccurence())){
				return true;
			}
		}
		else if( "noEnd".equals(range.getType())) {
			return false;
		}
		return false;
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
					destGroupEmailDetails.setUserId(emailWorkSpace.getUserId());
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
}
