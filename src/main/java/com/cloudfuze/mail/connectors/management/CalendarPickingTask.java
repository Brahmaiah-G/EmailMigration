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
import org.springframework.beans.BeanUtils;

import com.testing.mail.connectors.factory.MailServiceFactory;
import com.testing.mail.connectors.microsoft.data.AttachmentsData;
import com.testing.mail.constants.Const;
import com.testing.mail.dao.entities.CalenderFlags;
import com.testing.mail.dao.entities.ConnectFlags;
import com.testing.mail.dao.entities.EmailFlagsInfo;
import com.testing.mail.dao.entities.PermissionCache;
import com.testing.mail.dao.entities.UserGroups;
import com.testing.mail.exceptions.MailMigrationException;
import com.testing.mail.repo.entities.CalenderInfo;
import com.testing.mail.repo.entities.Clouds;
import com.testing.mail.repo.entities.Clouds.CLOUD_NAME;
import com.testing.mail.repo.entities.EmailWorkSpace;
import com.testing.mail.repo.entities.EmailWorkSpace.PROCESS;
import com.testing.mail.repo.entities.EventInstacesInfo;
import com.testing.mail.repo.entities.EventsInfo;
import com.testing.mail.repo.entities.GroupEmailDetails;
import com.testing.mail.repo.entities.MailChangeIds;
import com.testing.mail.repo.entities.MailChanges;
import com.testing.mail.service.DBConnectorService;
import com.testing.mail.utils.ConvertionUtils;
import com.testing.mail.utils.EventRangeUtils;
import com.testing.mail.utils.TimeUtils;

import lombok.extern.slf4j.Slf4j;


/**
 *<p>
Operations : Picking the OneTime and delta Calendars and Events based on Mailfolder(Inbox,sent)
</p>
 *Calling this Thread from, CalendarPickerScheduler
 *@see com.testing.mail.connectors.scheduler.CalendarPickerScheduler &#64;CalendarPickerScheduler
 */



@Slf4j
public class CalendarPickingTask implements Callable<Object> {

	private MailServiceFactory mailServiceFactory;
	private DBConnectorService connectorService; 
	EmailWorkSpace emailWorkSpace;
	CalenderInfo emailInfo;
	Map<String, String> mappedPairs = new HashMap<>();
	Clouds fromCloud ;
	Clouds toCloud ;
	Clouds fromAdminCloud = null;
	Clouds toAdminCloud = null;

	public CalendarPickingTask(MailServiceFactory mailServiceFactory, DBConnectorService connectorService,
			EmailWorkSpace emailWorkSpace, CalenderInfo emailInfo) {
		this.mailServiceFactory = mailServiceFactory;
		this.connectorService = connectorService;
		this.emailWorkSpace = emailWorkSpace;
		this.emailInfo = emailInfo;
	}

	@Override
	public Object call() throws Exception {
		initiateMigration();
		return null;
	}

	private void initiateMigration() {
		log.info("===Entered for fetching the Mails/Mail Folders for=="+emailWorkSpace.getId()+"==from mail=="+emailWorkSpace.getFromMailId()+"==toMail ID=="+emailWorkSpace.getToMailId()+"==fromFolder:="+emailWorkSpace.getFromFolderId());
		emailWorkSpace.setProcessStatus(PROCESS.IN_PROGRESS);
		emailInfo.setThreadBy(Thread.currentThread().getName());
		emailInfo.setProcessStatus(CalenderInfo.PROCESS.IN_PROGRESS);
		connectorService.getCalendarInfoRepoImpl().save(emailInfo);
		emailWorkSpace = connectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
		fromCloud = connectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getFromCloudId());
		toCloud = connectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getToCloudId());
		if(ObjectUtils.isEmpty(fromCloud) || ObjectUtils.isEmpty(toCloud) || StringUtils.isEmpty(emailInfo.getSourceId())) {
			emailInfo.setErrorDescription("required values for migration are null please check");
			emailInfo.setProcessStatus(CalenderInfo.PROCESS.CONFLICT);
			return;
		}
		fromAdminCloud = connectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getFromAdminCloud());
		toAdminCloud = connectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getToAdminCloud());
		mappedPermissions();
		List<MailChanges> mailchanges = new ArrayList<>();
		CalenderFlags emailFlagsInfo = new CalenderFlags();
		emailFlagsInfo.setCloudId(emailWorkSpace.getFromCloudId());
		emailFlagsInfo.setUserId(fromCloud.getUserId());
		emailFlagsInfo.setEmailId(emailWorkSpace.getFromMailId());

		if(emailWorkSpace.isDeltaMigration()) {
			String id = emailInfo.getSourceId()==null?(emailInfo.getSourceParent()==null ? emailInfo.getSourceId() : emailInfo.getSourceParent()):emailInfo.getSourceId();
			log.info("==For Delta going for the changes to fetch for the workspaceId=="+emailWorkSpace.getId());
			mailchanges = connectorService.getMailChangesRepoImpl().findByWorkSpaceParent(emailWorkSpace.getLastEmailWorkSpaceId(), id, false, true);
		}


		try {
			if(emailWorkSpace.isDeltaMigration()) {
				if(emailInfo.getSourceId()!=null && emailInfo.getSourceId().equals("/")) {
					List<String> dups = new ArrayList<>();
					mailchanges = connectorService.getMailChangesRepoImpl().findByWorkSpaceParent(emailWorkSpace.getLastEmailWorkSpaceId(), emailInfo.getSourceId(), true, true);
					if(!ObjectUtils.isEmpty(mailchanges)) {
						updateDeltaChange(mailchanges, dups);
					}else {
						getMails(emailFlagsInfo);
						emailInfo.setProcessStatus(CalenderInfo.PROCESS.PROCESSED);
						emailInfo.setErrorDescription("Dynamic Changes");
					}
				}else {
					getAndMapChanges(mailchanges);
				}
			}else {
				getMails(emailFlagsInfo);
			}
		}catch(Exception e) {
			emailInfo.setNextPageToken(emailFlagsInfo.getNextPageToken());
			emailInfo.setProcessStatus(CalenderInfo.PROCESS.CONFLICT);
			emailInfo.setRetryCount(emailInfo.getRetryCount()+1);
			emailInfo.setErrorDescription(ExceptionUtils.getStackTrace(e));
		}
		finally {
			connectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
			connectorService.getCalendarInfoRepoImpl().save(emailInfo);
		}
	}

	private void updateDeltaChange(List<MailChanges> mailchanges, List<String> dups) {
		List<CalenderInfo> infos = new ArrayList<>();
		for(MailChanges changes : mailchanges) {
			if(dups.contains(changes.getSourceId())) {
				continue;
			}
			dups.add(changes.getSourceId());
			if(changes.isFolder()) {
				CalenderInfo _emailInfo = new CalenderInfo();
				_emailInfo.setSourceId(changes.getSourceId());
				_emailInfo.setSourceParent(changes.getSourceParent());
				_emailInfo.setEmailWorkSpaceId(emailWorkSpace.getId());
				_emailInfo.setJobId(emailWorkSpace.getJobId());
				_emailInfo.setUserId(emailWorkSpace.getUserId());
				_emailInfo.setCalender(true);
				_emailInfo.setToCloudId(emailWorkSpace.getToCloudId());
				_emailInfo.setFromCloudId(emailWorkSpace.getFromCloudId());
				_emailInfo.setParentName(changes.getSourceId());
				_emailInfo.setPrimaryCalender(changes.isPrimary());
				infos.add(_emailInfo);
			}
		}
		connectorService.getCalendarInfoRepoImpl().saveCalendars(infos);
		infos.clear();
		emailInfo.setProcessStatus(CalenderInfo.PROCESS.PROCESSED);
		emailInfo.setErrorDescription("Changes Completed");
	}

	private void getMails(CalenderFlags emailFlagsInfo) {
		boolean hadData = false;
		CalenderInfo.PROCESS processStatus = CalenderInfo.PROCESS.NOT_PROCESSED;
		String message = "Completed";
		long attachmentsSize = 0;
		long count = 0;
		log.info("+====STARTED ON==="+emailWorkSpace.getId()+"=="+LocalDateTime.now());
		do {
			List<CalenderInfo> emails = new ArrayList<>();
			log.info("==Fetching emails for the USer==="+emailWorkSpace.getFromMailId()+"===MailFolder==="+emailInfo.getSubject()+"==WorkSpaceId=="+emailWorkSpace.getId()+"==Total Fetched UptoNow=="+count);
			emailInfo.setPicking(true);
			emailFlagsInfo.setNextPageToken(emailInfo.getNextPageToken());
			if(emailInfo.getSourceId()!=null && emailInfo.getSourceId().equals("/")) {
				emails = mailServiceFactory.getConnectorService(emailWorkSpace.getFromCloud()).getCalendars(emailFlagsInfo);
				convertCalendarFlagsToCalendarInfo(emails);
			}else {
				emailFlagsInfo.setId(emailInfo.getSourceId());
				emails = mailServiceFactory.getConnectorService(emailWorkSpace.getFromCloud()).getCalendarEvents(emailFlagsInfo);
				convertCalendarFlagsToEventsInfo(emails);
			}

			if(!hadData && (ObjectUtils.isEmpty(emails) || (emails!=null && emails.isEmpty()))) {
				message = "no Calendar Events to migrate";
			}else {
				hadData = true;	
			}
			processStatus = CalenderInfo.PROCESS.PROCESSED;
			if(emails!=null)
				count = count+emails.size();
			if(emailInfo.getNextPageToken()!=null && emailInfo.getNextPageToken().equals(emailFlagsInfo.getNextPageToken())){
				break;
			}
			emailInfo.setNextPageToken(emailFlagsInfo.getNextPageToken());
			connectorService.getCalendarInfoRepoImpl().save(emailInfo);
		}while(emailInfo.getNextPageToken()!=null);
		emailInfo.setCount(count);
		emailInfo.setAttachmentsSize(attachmentsSize);
		if(emailInfo.isCalender() && !emailWorkSpace.isPreScan() && !emailInfo.getSourceId().equals("/")) {
			//# adding the change id if the from source we need to fetch the change ids for delta
			emailFlagsInfo.setCalendar(emailInfo.getSourceId());
			String latestDeltaId = null;
			if(emailWorkSpace.getFromCloud().equals(CLOUD_NAME.GMAIL)) {
				latestDeltaId = emailFlagsInfo.getParentFolderId();
			}else {
				EmailFlagsInfo flagsInfo = new EmailFlagsInfo();
				flagsInfo.setCloudId(emailWorkSpace.getFromCloudId());
				flagsInfo.setEvents(true);
				flagsInfo.setFolder(emailInfo.getSubject());
				flagsInfo.setCreatedTime(LocalDateTime.now().toString());
				latestDeltaId = mailServiceFactory.getConnectorService(emailWorkSpace.getFromCloud()).getDeltaChangeId(flagsInfo);
			}
			emailInfo.setLatestDeltaId(latestDeltaId);
			createMailChangeIds(emailInfo, latestDeltaId);
		}
		if(emailInfo.getSourceId()!=null && emailInfo.getSourceId().equals("/")) {
			processStatus = CalenderInfo.PROCESS.PROCESSED;
		}

		emailInfo.setErrorDescription(message);
		emailInfo.setProcessStatus(processStatus);
		emailInfo.setPicking(false);
		if(!processStatus.equals(CalenderInfo.PROCESS.PROCESSED)) {
			emailInfo.setThreadBy(null);
		}
		if(emailWorkSpace.isPreScan()) {
			emailInfo.setProcessStatus(CalenderInfo.PROCESS.PROCESSED);
			emailInfo.setErrorDescription("PreScan");
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
	 *Storing the Fetched CalendarInfos in DataBase
	 */
	private void convertCalendarFlagsToEventsInfo(List<CalenderInfo> calenderInfos) throws MailMigrationException {
		List<EventsInfo> emailInfos = new ArrayList<>();
		List<String>ids = new ArrayList<>();
		if(calenderInfos!=null && !calenderInfos.isEmpty()) {
			for(CalenderInfo emailFlagsInfo : calenderInfos) {
				long count = connectorService.getCalendarInfoRepoImpl().checkExistingCalendar(emailFlagsInfo.getSourceId(), emailWorkSpace.getUserId(), emailWorkSpace.getJobId());
				EventsInfo _emailInfo = createEvent(emailFlagsInfo);
				if (count>0 || ids.contains(emailFlagsInfo.getId()) || (CLOUD_NAME.GMAIL.equals(emailWorkSpace.getFromCloud()) && emailFlagsInfo.getId().split("_").length>1)) {
					log.info("--duplicate ********--"+emailFlagsInfo.getId());
					_emailInfo.setErrorDescription("Already Event exist in User level");
					_emailInfo.setProcessStatus(EventsInfo.PROCESS.DUPLICATE_PROCESSED);
					_emailInfo.setDeleted(true);
				}else if(!_emailInfo.isDeleted()){
					if(null!=_emailInfo.getRange()) {
						try {
							checkInstances(_emailInfo.getSourceId(),emailFlagsInfo,_emailInfo.getEndTime());
							if(!_emailInfo.getRange().equals(emailFlagsInfo.getRange())) {
								_emailInfo.setRange(emailFlagsInfo.getRange());
							}
							checkFutureEvent(_emailInfo.getEndTime(),_emailInfo,_emailInfo.getRange());
							_emailInfo.setInstances(true);
							_emailInfo.setSyncToken(_emailInfo.getSyncToken());
						} catch (Exception e) {
							_emailInfo.setErrorDescription(ExceptionUtils.getStackTrace(e));
						}
					}
				}else {
					_emailInfo.setErrorDescription("Event Deleted In Source");
					_emailInfo.setProcessStatus(EventsInfo.PROCESS.PROCESSED);
					_emailInfo.setDeleted(true);
				}
				ids.add(emailFlagsInfo.getId().split("_")[0]);
				emailInfos.add(_emailInfo);
				if(emailInfos.size()>20) {
					connectorService.getCalendarInfoRepoImpl().save(emailInfos);
					emailInfos.clear();
				}
			}
			connectorService.getCalendarInfoRepoImpl().save(emailInfos);
			emailInfos.clear();
		}
	}

	private EventsInfo createEvent(CalenderInfo emailFlagsInfo) {
		EventsInfo.PROCESS process = EventsInfo.PROCESS.NOT_PROCESSED;
		EventsInfo _emailInfo = new EventsInfo();	
		_emailInfo.setEnv(System.getProperty("license.server.url"));
		_emailInfo.setFromCloudId(emailWorkSpace.getFromCloudId());
		_emailInfo.setToCloudId(emailWorkSpace.getToCloudId());
		_emailInfo.setFromName(emailFlagsInfo.getFromName());
		List<String>attends = new ArrayList<>();
		if(emailFlagsInfo.getAttendees()!=null) {
			attends = mapMails(emailFlagsInfo.getAttendees(),fromAdminCloud.getDomains());
		}
		_emailInfo.setExternalOrganizer(emailFlagsInfo.isExternalOrganizer());
		_emailInfo.setAttendees(attends);
		_emailInfo.setAllDay(emailFlagsInfo.isAllDay());
		_emailInfo.setOriginalFrom(emailFlagsInfo.getFromName());
		_emailInfo.setCalenderCreatedTime(emailFlagsInfo.getCalenderCreatedTime());
		_emailInfo.setCalenderModifiedTime(emailFlagsInfo.getCalenderModifiedTime());
		_emailInfo.setSourceId(emailFlagsInfo.getId());
		//adding for the organizer mapping if exists in destination
		if(null!=emailFlagsInfo.getOrganizer()) {
			_emailInfo.setOrganizer(mappedPairs.containsKey(emailFlagsInfo.getOrganizer().trim())?mappedPairs.get(emailFlagsInfo.getOrganizer().trim()):emailFlagsInfo.getOrganizer());
		}

		if(emailInfo.isPrimaryCalender() && emailInfo.isCalender()) {
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
		_emailInfo.setSourceParent(emailInfo.getSourceId());
		_emailInfo.setDestFolderPath(emailInfo.getSubject()==null ? emailInfo.getDestFolderPath() : emailInfo.getSubject());
		_emailInfo.setRecurrenceType(emailFlagsInfo.getRecurrenceType());
		_emailInfo.setRange(emailFlagsInfo.getRange());
		_emailInfo.setLocation(emailFlagsInfo.getLocation());
		_emailInfo.setRemainders(emailFlagsInfo.getRemainders());
		_emailInfo.setVisibility(emailFlagsInfo.getVisibility());
		_emailInfo.setShowAs(emailFlagsInfo.getShowAs());
		_emailInfo.setReadOnly(emailFlagsInfo.isReadOnly());
		if(emailFlagsInfo.isCalender()) {
			if(emailFlagsInfo.isReadOnly()) {
				process = EventsInfo.PROCESS.PROCESSED;
				_emailInfo.setErrorDescription("Read Only Can't edit or Create events");
			}else {
				process = EventsInfo.PROCESS.NOT_STARTED;
			}
		}else {
			process = EventsInfo.PROCESS.NOT_PROCESSED;
		}
		_emailInfo.setEmailWorkSpaceId(emailWorkSpace.getId());
		_emailInfo.setJobId(emailWorkSpace.getJobId());
		_emailInfo.setUserId(emailWorkSpace.getUserId());
		_emailInfo.setICalUId(emailFlagsInfo.getICalUId());
		_emailInfo.setPreScan(emailWorkSpace.isPreScan());
		if(emailWorkSpace.isPreScan() && !_emailInfo.isCalender()) {
			_emailInfo.setPreScan(true);
			process = EventsInfo.PROCESS.PROCESSED;
		}
		_emailInfo.setProcessStatus(process);
		checkFutureEvent(_emailInfo.getEndTime(),_emailInfo,_emailInfo.getRange());
		return _emailInfo;
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

				if(emailInfo.isPrimaryCalender() && emailInfo.isCalender()) {
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
				_emailInfo.setSourceParent(emailInfo.getSourceId());
				_emailInfo.setDestFolderPath(emailInfo.getSubject()==null ? emailInfo.getDestFolderPath() : emailInfo.getSubject());
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

	/**
	 * For Getting the Changes From the Source
	 */
	private void createMailChangeIds(CalenderInfo emailInfo,String latestDeltaId) {
		MailChangeIds changeIds = new MailChangeIds();
		changeIds.setLatestUpdatedId(latestDeltaId);
		changeIds.setCalendar(true);
		changeIds.setEmailWorkSpaceId(emailInfo.getEmailWorkSpaceId());
		changeIds.setUserId(emailInfo.getUserId());
		changeIds.setCreatedTime(LocalDateTime.now());
		changeIds.setPrimaryCalendar(emailInfo.isPrimaryCalender());
		changeIds.setFolder(true);
		changeIds.setJobId(emailWorkSpace.getJobId());
		changeIds.setFromCloudId(emailWorkSpace.getFromCloudId());
		changeIds.setToCloudId(emailWorkSpace.getToCloudId());
		changeIds.setMetadata(emailWorkSpace.isMetadata());
		changeIds.setDestFolderPath(emailInfo.getDestId());
		Date date = Calendar.getInstance().getTime();
		// try to implement in minutes and hours using the change expression so that will be good for testing
		String changeHours[] = emailWorkSpace.getChangeHours().split("H");
		if(changeHours!=null && changeHours.length>0) {
			date.setHours(date.getHours()+Integer.parseInt(changeHours[0]));
			date.setMinutes(date.getMinutes()+Integer.parseInt(changeHours[1].substring(0,changeHours[1].length()-1)));
		}
		changeIds.setNextPickTime(date);
		changeIds.setSourceId(emailInfo.getSourceId());
		connectorService.getMailChangeIdRepoImpl().save(changeIds);
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
	 * Getting the Changes From DataBase
	 * @see MailChanges
	 */
	private void getAndMapChanges(List<MailChanges>mailChanges) {
		List<EventsInfo>changesInfo = new ArrayList<>();
		List<EventInstacesInfo>eventChanges = new ArrayList<>();
		List<String>dupIds = new ArrayList<>();
		List<MailChanges> modified = new ArrayList<>();
		List<String> dups = new ArrayList<>();
		log.info("===Total CalendarChanges=="+mailChanges.size()+"===for the workspaceId=="+emailWorkSpace.getId());
		if(!mailChanges.isEmpty()) {
			for(MailChanges changes : mailChanges) {
				changes.setProcessStatus(MailChanges.PROCESS.PROCESSED);
				if(changes!=null && changes.getSourceId()!=null) {
					if(dups.contains(changes.getSourceId())) {
						changes.setProcessStatus(MailChanges.PROCESS.DUPLICATE_PROCESSED);
						modified.add(changes);
						continue;
					}
					dups.add(changes.getSourceId());
					log.info("==For the Change==="+changes.getId()+"===For the workSpaceId==="+emailWorkSpace.getId());
					EventsInfo info = connectorService.getCalendarInfoRepoImpl().findBySourceId(emailWorkSpace.getJobId(), emailWorkSpace.getUserId(), changes.getSourceId().split("_")[0]);
					if(info!=null) {
						LocalDateTime sourceModified = null;
						if(info.getCalenderModifiedTime()!=null && changes.getMailModifiedTime()!=null) {
							try {
								sourceModified = LocalDateTime.parse(info.getCalenderModifiedTime());
							} catch (Exception e) {
								sourceModified = LocalDateTime.now();
							}
							if(sourceModified.isAfter(changes.getMailModifiedTime())) {
								log.info("==skipping as the CreatedFile is older then the changes==="+emailWorkSpace.getId()+"===changeId=="+changes.getId());
								continue;
							}
						}
						EventInstacesInfo eventInfo = connectorService.getCalendarInfoRepoImpl().getEventInstanceInfoById(emailWorkSpace.getId(), changes.getSourceId());
						// check for the attachments in that with same or diff
						EventsInfo _info = createInfo(changes, info);
						_info.setProcessStatus(EventsInfo.PROCESS.INSTANCE_NOTPROCESSED);
						EventInstacesInfo eventInstacesInfo = new EventInstacesInfo();
						BeanUtils.copyProperties(_info, eventInstacesInfo);
						if(eventInfo!=null){
							eventInstacesInfo.setDestId(eventInfo.getDestId());
						}
						eventInstacesInfo.setProcessStatus(com.testing.mail.repo.entities.PROCESS.NOT_PROCESSED);
						eventChanges.add(eventInstacesInfo);
						if(!dupIds.contains(_info.getSourceId().split("_")[0])) {
							changesInfo.add(_info);
						}
						dupIds.add(_info.getSourceId().split("_")[0]);
						if(changesInfo.size()>20) {
							connectorService.getCalendarInfoRepoImpl().save(changesInfo);
							changesInfo.clear();
						}
						if(eventChanges.size()>20) {
							connectorService.getCalendarInfoRepoImpl().saveInstances(eventChanges);
							eventChanges.clear();
						}
					}else {
						log.info("==No Exisiting info found so going for creating a new CalendarInfo for migraion=="+emailWorkSpace.getId()+"==changeID=="+changes.getId());
						EventsInfo _info = createInfo(changes, null);
						changesInfo.add(_info);
						if(changesInfo.size()>20) {
							connectorService.getCalendarInfoRepoImpl().save(changesInfo);
							changesInfo.clear();
						}
					}
				}
				modified.add(changes);
			}

		}else {
			getUpdatedMails();
		}
		connectorService.getMailChangesRepoImpl().save(modified);
		modified.clear();
		emailInfo.setProcessStatus(CalenderInfo.PROCESS.PROCESSED);
		if(!changesInfo.isEmpty()) {
			connectorService.getCalendarInfoRepoImpl().save(changesInfo);
			changesInfo.clear();
		}
		if(!eventChanges.isEmpty()) {
			connectorService.getCalendarInfoRepoImpl().saveInstances(eventChanges);
			eventChanges.clear();
		}
	}
	private EventsInfo createInfo(MailChanges mailChanges,EventsInfo destMail) {
		EventsInfo _emailInfo = new EventsInfo();
		_emailInfo.setSourceId(mailChanges.getSourceId());
		_emailInfo.setParentName(mailChanges.getMailFolder());
		_emailInfo.setJobId(emailWorkSpace.getJobId());
		_emailInfo.setUserId(emailWorkSpace.getUserId());
		_emailInfo.setEmailWorkSpaceId(emailWorkSpace.getId());
		_emailInfo.setOrganizer(mailChanges.getFromMail());
		_emailInfo.setFromCloudId(emailWorkSpace.getFromCloudId());
		_emailInfo.setToCloudId(emailWorkSpace.getToCloudId());
		if(mailChanges.getToMail()!=null) {
			_emailInfo.setAttendees(mapMails(mailChanges.getToMail(),fromAdminCloud.getDomains()));
		}
		if(destMail!=null) {
			_emailInfo.setDestId(destMail.getDestId());
			_emailInfo.setDestThreadId(destMail.getDestThreadId());
			_emailInfo.setDestParent(destMail.getDestParent());
			_emailInfo.setThreadId(destMail.getThreadId());
		}
		_emailInfo.setAttachMents(mailChanges.isAttachMents());
		_emailInfo.setAttachmentIds(mailChanges.getAttachmentIds());
		_emailInfo.setEndTimeZone(mailChanges.getEndTimeZone());
		_emailInfo.setBodyPreview(mailChanges.getBodyPreview());
		_emailInfo.setMetadata(emailWorkSpace.isMetadata());
		_emailInfo.setHtmlContent(mailChanges.isHtmlContent());
		_emailInfo.setHtmlBodyContent(mailChanges.getHtmlBodyContent());
		_emailInfo.setDeleted(mailChanges.isDeleted());
		_emailInfo.setDraft(mailChanges.isDraft());
		_emailInfo.setStartTime(mailChanges.getStartTime());
		_emailInfo.setEndTime(mailChanges.getEndTime());
		_emailInfo.setSubject(mailChanges.getSubject());
		_emailInfo.setSourceParent(emailInfo.getSourceParent());
		if(mailChanges.getMailCreatedTime()!=null) {
			_emailInfo.setCalenderCreatedTime(mailChanges.getMailCreatedTime().toString());
			_emailInfo.setCalenderModifiedTime(mailChanges.getMailModifiedTime().toString());
		}
		_emailInfo.setDestFolderPath(emailInfo.getDestFolderPath());
		_emailInfo.setFlagged(mailChanges.isFlagged());
		_emailInfo.setCalender(mailChanges.isFolder());
		_emailInfo.setRange(mailChanges.getRange());
		_emailInfo.setTimeZone(mailChanges.getTimeZone());
		_emailInfo.setRecurrenceType(mailChanges.getRecurrenceType());
		if(mailChanges.getToMail()!=null) {
			List<String>attends = new ArrayList<>();
			for(String attendees : mailChanges.getToMail()) {
				if(mappedPairs.containsKey(attendees.split(":")[0])) {
					attends.add(mappedPairs.get(attendees.split(":")[0])+":"+attendees.split(":")[1]);
				}else {
					attends.add(attendees);
				}
			}
			mailChanges.setToMail(attends);
			_emailInfo.setAttendees(attends);
		}
		if(!mailChanges.isFolder()) {
			_emailInfo.setProcessStatus(EventsInfo.PROCESS.NOT_PROCESSED);
		}
		_emailInfo.setLocation(mailChanges.getLocation());
		_emailInfo.setEmailWorkSpaceId(emailWorkSpace.getId());
		_emailInfo.setJobId(emailWorkSpace.getJobId());
		_emailInfo.setUserId(emailWorkSpace.getUserId());
		return _emailInfo;
	}

	private void checkFutureEvent(String endTime,EventsInfo calenderInfo,String recurenceRule) {
		if(emailWorkSpace.isFutureEvents() && !emailWorkSpace.isPreScan() && !calenderInfo.isDeleted()) {
			if(null==recurenceRule) {
				if(!TimeUtils.checkEventType(endTime,calenderInfo.getEndTimeZone())) {
					calenderInfo.setProcessStatus(EventsInfo.PROCESS.PROCESSED);
					calenderInfo.setErrorDescription("OldEvent");
				}else {
					calenderInfo.setProcessStatus(EventsInfo.PROCESS.NOT_PROCESSED);
				}
			}else {
				if(checkRecurenceRuleForOlderEvents(recurenceRule,calenderInfo.getRecurrenceType())) {
					calenderInfo.setProcessStatus(EventsInfo.PROCESS.PROCESSED);
					calenderInfo.setErrorDescription("OldEvent");
				}else {
					calenderInfo.setProcessStatus(EventsInfo.PROCESS.NOT_PROCESSED);
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
		if(range.getEndDate()!=null && !range.getEndDate().equals("0001-01-01")) {
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
						}else if(mappedPairs.containsKey(_attendees)){
							attends.add(attendees.replace(_attendees, mappedPairs.get(_attendees)));
						}else {
							mappedPairs.put(_attendees, destMail);
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

	private void checkInstances(String recurenceId,CalenderInfo eventsInfo,String endTime) {
		EventRangeUtils rangeUtils = null;
		if(CLOUD_NAME.OUTLOOK.equals(emailWorkSpace.getFromCloud())) {
			rangeUtils = EventRangeUtils.setRangeForOutlookAsSource(eventsInfo.getRange(), eventsInfo.getRecurrenceType());	
		}else{
			rangeUtils = EventRangeUtils.setRangeForGmailAsSource(eventsInfo.getRange(), eventsInfo.getRecurrenceType());	
		}
		CalenderFlags calenderFlags = new CalenderFlags();
		calenderFlags.setCloudId(emailWorkSpace.getFromCloudId());
		calenderFlags.setId(recurenceId);
		calenderFlags.setCalendar(emailInfo.getSourceId());
		calenderFlags.setRecurrenceType(eventsInfo.getRecurrenceType());
		calenderFlags.setStartTime(eventsInfo.getStartTime());
		calenderFlags.setEndTime(rangeUtils.getEndDate());
		List<CalenderInfo>emails = mailServiceFactory.getConnectorService(emailWorkSpace.getFromCloud()).getEventInstances(calenderFlags);
		if(null!=emails && !emails.isEmpty()) {
			List<EventInstacesInfo> instances = new ArrayList<>();
			String lastOccurenceId = null;
			if(calenderFlags.getLastOccurence()==null) {
				lastOccurenceId = emails.get(emails.size()-1).getId();
				if(CLOUD_NAME.OUTLOOK.equals(emailWorkSpace.getFromCloud())) {
					lastOccurenceId = emails.get(emails.size()-1).getEndTime();
				}
			}else {
				lastOccurenceId = recurenceId+"_"+calenderFlags.getLastOccurence();
			}
			if(eventsInfo.getRange()!=null) {
				rangeUtils = EventRangeUtils.setRangeForGmailAsSource(eventsInfo.getRange(), eventsInfo.getRecurrenceType());
				if(rangeUtils!=null &&  rangeUtils.getType()!=null && rangeUtils.getType().equals("endDate")) {
					lastOccurenceId = TimeUtils.convertRecurenceTimeToLocalDate(lastOccurenceId.split("_")[1].split("T")[0]).toString();
					if(!rangeUtils.getEndDate().split("T")[0].equals(lastOccurenceId)){
						eventsInfo.setRange(eventsInfo.getRange().replace(rangeUtils.getEndDate().split("T")[0], lastOccurenceId));
					}
				}else if(rangeUtils.getType().equals("noEnd")) {
					if(LocalDate.now().isAfter(TimeUtils.convertRecurenceTimeToLocalDate(lastOccurenceId.split("_")[1].split("T")[0]))) {
						//Type#startDate#endDate#FirstDay#WeekDays#interval from Outlook
						rangeUtils.setType("endDate");
						rangeUtils.setEndDate(TimeUtils.convertRecurenceTimeToLocalDate(lastOccurenceId.split("_")[1].split("T")[0]).toString());
						eventsInfo.setRange(rangeUtils.getType()+Const.HASHTAG+rangeUtils.getStartDate()+Const.HASHTAG+rangeUtils.getEndDate()+Const.HASHTAG
								+rangeUtils.getWkst()+Const.HASHTAG+rangeUtils.getDays()+Const.HASHTAG+rangeUtils.getOccurences()+Const.HASHTAG+rangeUtils.getInterval());
					}
					
				}
			}
			for(CalenderInfo emailFlagsInfo : emails) {
				EventsInfo _emailInfo = createEvent(emailFlagsInfo);
				EventInstacesInfo target = new EventInstacesInfo();
				BeanUtils.copyProperties(_emailInfo, target);
				target.setProcessStatus(com.testing.mail.repo.entities.PROCESS.valueOf(_emailInfo.getProcessStatus().name()));
				target.setRecurenceId(recurenceId);
				target.setParent(emailFlagsInfo.isPicking());
				instances.add(target);
				if(instances.size()>20) {
					connectorService.getCalendarInfoRepoImpl().saveInstances(instances);
					instances.clear();
				}
			}
			if(!instances.isEmpty()) {
				connectorService.getCalendarInfoRepoImpl().saveInstances(instances);
				instances.clear();
			}
		}
	}

	private void getUpdatedMails() {
		EmailFlagsInfo emailFlagsInfo = new EmailFlagsInfo();
		emailFlagsInfo.setAdminMemberId(fromCloud.getAdminMemberId());
		emailFlagsInfo.setCloudId(emailWorkSpace.getFromCloudId());
		emailFlagsInfo.setUserId(fromCloud.getUserId());
		emailFlagsInfo.setEmailId(emailWorkSpace.getFromMailId());

		emailFlagsInfo.setNextPageToken(null);
		MailChangeIds mailChangeIds = connectorService.getMailChangeIdRepoImpl().findBySourceId(emailWorkSpace.getLastEmailWorkSpaceId(), emailInfo.getSourceId());
		if(null!=mailChangeIds) {
			emailInfo.setLatestDeltaId(mailChangeIds.getLatestUpdatedId());
		}else {
			CalenderInfo calenderInfo = connectorService.getCalendarInfoRepoImpl().findBySourceId(emailWorkSpace.getLastEmailWorkSpaceId(), emailInfo.getSourceId());
			if(null!=calenderInfo) {
				emailInfo.setLatestDeltaId(calenderInfo.getLatestDeltaId());
			}
		}
		log.info("===Get UpdatedMails for the user=="+emailWorkSpace.getFromMailId()+"==For the MailFolder=="+emailInfo.getSourceId());
		List<EmailFlagsInfo> emails = null;
		emailFlagsInfo.setFolder(emailInfo.getSourceId());
		emailFlagsInfo.setEvents(true);
		emails = mailServiceFactory.getConnectorService(emailWorkSpace.getFromCloud()).getDeltaChanges(emailFlagsInfo, emailInfo.getLatestDeltaId());

		if(ObjectUtils.isEmpty(emails) || (emails!=null && emails.isEmpty())) {
			emailInfo.setErrorDescription("no Updated Folder mails to migrate");
		}else {
			emailInfo.setErrorDescription("STARTED");
			emailInfo.setThreadBy(null);
		}
		convertEmailFlagsToMailChanges(emails);
		emailInfo.setCount(emailInfo.getCount()+(emails!=null?emails.size():0));
		emailInfo.setProcessStatus(CalenderInfo.PROCESS.PROCESSED);
	}
	private void convertEmailFlagsToMailChanges(List<EmailFlagsInfo> emailFlagsInfos) throws MailMigrationException {
		List<EventsInfo> emailInfos = new ArrayList<>();
		List<String>dups = new ArrayList<>();
		// need to handle creating and modifying the same new event in delta 
		if(emailFlagsInfos!=null && !emailFlagsInfos.isEmpty()) {
			for(EmailFlagsInfo emailFlagsInfo : emailFlagsInfos) {
				Map<String,String>attendeResponseStatus = new HashMap<>();
				EventsInfo info = connectorService.getCalendarInfoRepoImpl().findBySourceId(emailWorkSpace.getJobId(), emailWorkSpace.getUserId(), emailFlagsInfo.getId().split("_")[0]);
				EventsInfo _emailInfo = new EventsInfo();
				if(null!=info) {
					_emailInfo.setDestId(info.getDestId());
					for(String attendee : info.getAttendees()) {
						attendeResponseStatus.put(attendee.split(Const.COLON)[0], attendee.split(Const.COLON)[1]);
					}
				}
				if(dups.contains(emailFlagsInfo.getId())) {
					continue;
				}
				dups.add(emailFlagsInfo.getId());
				_emailInfo.setEmailWorkSpaceId(emailWorkSpace.getId());
				_emailInfo.setUserId(emailWorkSpace.getUserId());
				_emailInfo.setJobId(emailWorkSpace.getJobId());
				_emailInfo.setCreatedTime(LocalDateTime.now());
				_emailInfo.setStartTime(emailFlagsInfo.getStartTime());
				_emailInfo.setEndTime(emailFlagsInfo.getEndTime());
				_emailInfo.setSourceParent(emailInfo.getSourceId());
				_emailInfo.setSubject(emailFlagsInfo.getSubject());
				_emailInfo.setSourceId(emailFlagsInfo.getId());
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
				_emailInfo.setOrganizer(mappedPairs.containsKey(emailFlagsInfo.getFrom())?mappedPairs.get(emailFlagsInfo.getFrom()):emailFlagsInfo.getFrom());
				_emailInfo.setOriginalFrom(emailFlagsInfo.getFrom());
				_emailInfo.setAttendees(emailFlagsInfo.getTo());
				_emailInfo.setHtmlBodyContent(emailFlagsInfo.getHtmlMessage());
				_emailInfo.setBodyPreview(emailFlagsInfo.getBodyPreview());
				_emailInfo.setHtmlContent(emailFlagsInfo.isHtmlContent());
				_emailInfo.setTimeZone(emailFlagsInfo.getTimeZone());
				_emailInfo.setDeleted(emailFlagsInfo.isDeleted());
				_emailInfo.setFlagged(emailFlagsInfo.isFlagged());
				_emailInfo.setFromName(emailFlagsInfo.getFromName());
				_emailInfo.setEndTimeZone(emailFlagsInfo.getEndTimeZone());
				_emailInfo.setProcessStatus(EventsInfo.PROCESS.NOT_PROCESSED);
				if(_emailInfo.getSourceId().split("_").length>1 && null!=info) {
					_emailInfo.setProcessStatus(EventsInfo.PROCESS.INSTANCE_NOTPROCESSED);
				}
				_emailInfo.setAttachMents(emailFlagsInfo.isHadAttachments());
				_emailInfo.setFromCloudId(emailWorkSpace.getFromCloudId());
				_emailInfo.setToCloudId(emailWorkSpace.getToCloudId());
				_emailInfo.setMetadata(emailWorkSpace.isMetadata());
				_emailInfo.setDraft(emailFlagsInfo.isDraft());
				_emailInfo.setFlagged(emailFlagsInfo.isFlagged());
				_emailInfo.setRange(emailFlagsInfo.getRange());
				_emailInfo.setLocation(emailFlagsInfo.getLocation());
				_emailInfo.setRecurrenceType(emailFlagsInfo.getRecurrenceType());
				if(info!=null) {
					if(_emailInfo.isDeleted() || info.getSubject().equals(_emailInfo.getSubject()) || info.getAttendees().size()==_emailInfo.getAttendees().size() || info.getAttachmentIds().size()==_emailInfo.getAttachmentIds().size()
							|| _emailInfo.getLocation().size()== info.getLocation().size() || _emailInfo.getHtmlBodyContent().equals(info.getHtmlBodyContent())) {
						boolean modified = false;
						for(String attendee : _emailInfo.getAttendees()) {
							if(attendeResponseStatus.containsKey(attendee.split(Const.COLON)[0]) && !attendeResponseStatus.get(attendee.split(Const.COLON)[0]).equals(attendee.split(Const.COLON)[1])) {
								modified = true;
							}
						}
						if(!modified && _emailInfo.getSourceId().split("_").length<=0) {
							_emailInfo.setProcessStatus(EventsInfo.PROCESS.PROCESSED);
						}
					}
				}
				checkFutureEvent(_emailInfo.getEndTime(),_emailInfo,_emailInfo.getRange());
				emailInfos.add(_emailInfo);
				if(emailInfos.size()>20) {
					connectorService.getCalendarInfoRepoImpl().save(emailInfos);
					emailInfos.clear();
				}
			}
			connectorService.getCalendarInfoRepoImpl().save(emailInfos);
			emailInfos.clear();
		}
	}

}
