package com.testing.mail.connectors.management;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.BeanUtils;

import com.testing.mail.connectors.factory.MailServiceFactory;
import com.testing.mail.connectors.impl.OutLookMailConnector;
import com.testing.mail.connectors.microsoft.data.AttachmentsData;
import com.testing.mail.constants.Const;
import com.testing.mail.dao.entities.CalenderFlags;
import com.testing.mail.dao.entities.ConnectFlags;
import com.testing.mail.dao.entities.EmailFlagsInfo;
import com.testing.mail.repo.entities.CalenderInfo;
import com.testing.mail.repo.entities.Clouds;
import com.testing.mail.repo.entities.Clouds.CLOUD_NAME;
import com.testing.mail.repo.entities.EmailWorkSpace;
import com.testing.mail.repo.entities.EmailWorkSpace.PROCESS;
import com.testing.mail.repo.entities.EventInstacesInfo;
import com.testing.mail.repo.entities.EventsInfo;
import com.testing.mail.service.DBConnectorService;
import com.testing.mail.utils.EventRangeUtils;
import com.testing.mail.utils.MappingUtils;
import com.testing.mail.utils.TimeUtils;

import lombok.extern.slf4j.Slf4j;

/**
 For migrating the calendar Events after Picked from the Source
*/

@Slf4j
public class EventInstanceUpdationTask implements Callable<Object> {

	private MailServiceFactory mailServiceFactory;
	private DBConnectorService connectorService; 
	EmailWorkSpace emailWorkSpace;
	EventsInfo emailInfo;




	public EventInstanceUpdationTask(MailServiceFactory mailServiceFactory, DBConnectorService connectorService,
			EmailWorkSpace emailWorkSpace, EventsInfo emailInfo) {
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
		log.warn("===Entered for fetching the CalendarEvents/Calendars for=="+emailWorkSpace.getId()+"==from mail=="+emailWorkSpace.getFromMailId()+"==toMail ID=="+emailWorkSpace.getToMailId()+"==fromFolder:="+emailWorkSpace.getFromFolderId());
		emailWorkSpace.setProcessStatus(PROCESS.IN_PROGRESS);
		emailWorkSpace = connectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
		emailInfo.setThreadBy(Thread.currentThread().getName());
		emailInfo.setProcessStatus(EventsInfo.PROCESS.INSTANCE_INPROGRESS);
		connectorService.getCalendarInfoRepoImpl().save(emailInfo);
		Clouds fromCloud = connectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getFromCloudId());
		Clouds toCloud = connectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getToCloudId());
		if(ObjectUtils.isEmpty(fromCloud) || ObjectUtils.isEmpty(toCloud)) {
			emailInfo.setErrorDescription("required values for migration are null please check");
			emailInfo.setProcessStatus(EventsInfo.PROCESS.CONFLICT);
			return;
		}
		if(emailInfo.isDeleted() && emailInfo.getEndTime()==null) {
			EventInstacesInfo deletedInfos = connectorService.getCalendarInfoRepoImpl().getSingleInstance(emailWorkSpace.isDeltaMigration()?emailWorkSpace.getLastEmailWorkSpaceId():emailWorkSpace.getId(), emailInfo.getSourceId());
			if(null!=deletedInfos) {
				emailInfo.setOrganizer(deletedInfos.getOrganizer());
				emailInfo.setStartTime(deletedInfos.getStartTime());
				emailInfo.setEndTime(deletedInfos.getEndTime());
				emailInfo.setRecurrenceType(deletedInfos.getRecurrenceType());
			}
		}
		EventInstacesInfo parentEventInstacesInfo = null;
		Map<String,String>attendeResponseStatus = new HashMap<>();
		Map<String, CalenderInfo> destInstancesMap = fetchDestEventInstances(parentEventInstacesInfo);
		if(destInstancesMap==null) {
			emailInfo.setProcessStatus(EventsInfo.PROCESS.INSTANCE_CONFLICT);
			return;
		}
		parentEventInstacesInfo = convertEventsInfoToEventInstanceInfo(destInstancesMap.values().iterator().next());
		//EventInstacesInfo parentEventInstacesInfo = connectorService.getCalendarInfoRepoImpl().getParentInstances(emailWorkSpace.isDeltaMigration()?emailWorkSpace.getLastEmailWorkSpaceId():emailWorkSpace.getId(), emailInfo.getSourceId().split("_")[0]);
		List<EventInstacesInfo> eventInstances = connectorService.getCalendarInfoRepoImpl().getInstances(emailWorkSpace.isDeltaMigration()?emailWorkSpace.getLastEmailWorkSpaceId():emailWorkSpace.getId(), emailInfo.getSourceId().split("_")[0]);
		if(emailWorkSpace.isDeltaMigration()) {
			parentEventInstacesInfo = connectorService.getCalendarInfoRepoImpl().getSingleInstance(emailWorkSpace.isDeltaMigration()?emailWorkSpace.getLastEmailWorkSpaceId():emailWorkSpace.getId(), emailInfo.getSourceId());
			if(null==parentEventInstacesInfo) {
				parentEventInstacesInfo = connectorService.getCalendarInfoRepoImpl().getSingleInstanceByRec(emailWorkSpace.isDeltaMigration()?emailWorkSpace.getLastEmailWorkSpaceId():emailWorkSpace.getId(), emailInfo.getSourceId().split("_")[0]);
			}
		}
		if(null==parentEventInstacesInfo && !eventInstances.isEmpty()) {
			parentEventInstacesInfo = eventInstances.get(0);
		}
		if(emailWorkSpace.isDeltaMigration()) {
			eventInstances = Arrays.asList(convertEventsInfoToEventInstanceInfo());
		}
		
		for(String attendee : parentEventInstacesInfo.getAttendees()) {
			attendeResponseStatus.put(attendee.split(Const.COLON)[0], attendee.split(Const.COLON)[1]);
		}
		List<EventInstacesInfo> _eventInstances = new ArrayList<>();
		List<EventInstacesInfo> modifiedInstances = new ArrayList<>();
		if(parentEventInstacesInfo!=null) {
			checkModifiedEventInstances(attendeResponseStatus, destInstancesMap, parentEventInstacesInfo,
					eventInstances, modifiedInstances,parentEventInstacesInfo);
		}
		try {
			for(EventInstacesInfo instances : modifiedInstances) {
				try {
					CalenderInfo created = null;
					CalenderFlags emailFlagsInfo = new CalenderFlags();
					emailFlagsInfo.setCloudId(emailWorkSpace.getToCloudId());
					if(instances.getDestId()==null) {
						instances.setProcessStatus(com.testing.mail.repo.entities.PROCESS.INSTANCE_CONFLICT);
						instances.setErrorDescription("DestId Not Found");
						continue;
					}
					if(!instances.isDeleted()){
						createFlagsForEvntInstanceUpdate(fromCloud, instances, emailFlagsInfo);
						created = ((OutLookMailConnector)mailServiceFactory.getConnectorService(toCloud.getCloudName())).updateEventInstance(emailFlagsInfo);
						instances.setCreated(true);
						instances.setICalUId(created.getICalUId());
					}else {
						emailFlagsInfo.setCloudId(emailWorkSpace.getToCloudId());
						emailFlagsInfo.setDestId(instances.getDestId());
						created = new CalenderInfo();
						created.setId(instances.getDestId());
						EmailFlagsInfo flagsInfo = new EmailFlagsInfo();
						flagsInfo.setCloudId(emailWorkSpace.getToCloudId());
						flagsInfo.setId(instances.getDestId());
						flagsInfo.setFrom(emailWorkSpace.getToMailId());
						boolean deleted = mailServiceFactory.getConnectorService(toCloud.getCloudName()).deleteEmails(flagsInfo, true);
						created.setDeleted(deleted);
					}
					if(created!=null) {
						log.warn("==Created calendar event=="+emailInfo.getSubject()+"--fileID=="+emailInfo.getId()+"-"+emailWorkSpace.getId());
						instances.setDestId(created.getId());
						instances.setICalUId(created.getICalUId());
						instances.setLargeFile(emailFlagsInfo.isLargeFile());
						instances.setDestParent(emailInfo.getDestFolderPath());
						instances.setProcessStatus(com.testing.mail.repo.entities.PROCESS.INSTANCE_PROCESSED);
						instances.setErrorDescription("COMPLETED");
						instances.setUpdatedMetadata(created.getUpdatedMetadata());
						instances.setDeleted(created.isDeleted());
					}
				} catch (Exception e) {
					instances.setProcessStatus(com.testing.mail.repo.entities.PROCESS.INSTANCE_CONFLICT);
					instances.setErrorDescription(ExceptionUtils.getStackTrace(e));
					instances.setRetryCount(instances.getRetryCount()+1);
				}
				finally {
					_eventInstances.add(instances);
					if(_eventInstances.size()>20) {
						connectorService.getCalendarInfoRepoImpl().saveInstances(_eventInstances);
						_eventInstances.clear();
					}
				}
			}
			if(!_eventInstances.isEmpty()) {
				connectorService.getCalendarInfoRepoImpl().saveInstances(_eventInstances);
				_eventInstances.clear();
			}
			emailInfo.setProcessStatus(EventsInfo.PROCESS.PROCESSED);
		}catch(Exception e) {
			emailInfo.setProcessStatus(EventsInfo.PROCESS.CONFLICT);
			emailInfo.setRetryCount(emailInfo.getRetryCount()+1);
			emailInfo.setErrorDescription(ExceptionUtils.getStackTrace(e));
		}
		finally {
			connectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
			if(emailInfo.getProcessStatus().equals(EventsInfo.PROCESS.INSTANCE_INPROGRESS)) {
				emailInfo.setProcessStatus(EventsInfo.PROCESS.INSTANCE_CONFLICT);
				emailInfo.setRetryCount(emailInfo.getRetryCount()+1);
				if(null==emailInfo.getErrorDescription()) {
					emailInfo.setErrorDescription("InternalException Occured Check logs");
				}
			}
			connectorService.getCalendarInfoRepoImpl().save(emailInfo);
		}
	}

	private void createFlagsForEvntInstanceUpdate(Clouds fromCloud, EventInstacesInfo instances,
			CalenderFlags emailFlagsInfo) {
		emailFlagsInfo.setUserId(fromCloud.getUserId());
		emailFlagsInfo.setEmailId(instances.getOrganizer());
		emailFlagsInfo.setAttendees(instances.getAttendees());
		emailFlagsInfo.setThreadId(instances.getThreadId());
		emailFlagsInfo.setOnlineMeeting(instances.isOnlineMeeting());
		emailFlagsInfo.setRemainderOn(instances.isReminderOn());
		emailFlagsInfo.setOnlineMeetingUrl(instances.getOnlineMeetingUrl());
		emailFlagsInfo.setOrganizer(instances.getOrganizer());
		emailFlagsInfo.setSubject(instances.getSubject());
		emailFlagsInfo.setHadAttachments(instances.isAttachMents());
		emailFlagsInfo.setMetaData(instances.isMetadata());
		emailFlagsInfo.setHtmlContent(instances.isHtmlContent());
		emailFlagsInfo.setHtmlMessage(instances.getHtmlBodyContent());
		emailFlagsInfo.setBodyPreview(instances.getBodyPreview());
		emailFlagsInfo.setDeleted(instances.isDeleted());
		emailFlagsInfo.setLocation(instances.getLocation());
		emailFlagsInfo.setRange(instances.getRange());
		emailFlagsInfo.setICalUId(instances.getICalUId());
		emailFlagsInfo.setColour(instances.getColor());
		emailFlagsInfo.setRecurrenceType(instances.getRecurrenceType());
		emailFlagsInfo.setMessage(instances.getHtmlBodyContent());
		emailFlagsInfo.setDestId(instances.getDestId());
		emailFlagsInfo.setCalendar(instances.getParentName()==null ? instances.getDestFolderPath() : instances.getParentName());
		emailFlagsInfo.setCreatedTime(instances.getCalenderCreatedTime());
		emailFlagsInfo.setSentTime(instances.getCalenderModifiedTime());
		emailFlagsInfo.setDestId(instances.getDestId());
		emailFlagsInfo.setStartTime(instances.getStartTime());
		emailFlagsInfo.setEndTime(instances.getEndTime());
		emailFlagsInfo.setTimeZone(instances.getTimeZone());
		emailFlagsInfo.setFromName(instances.getFromName());
		emailFlagsInfo.setFlagged(instances.isFlagged());
		emailFlagsInfo.setEndTimeZone(instances.getEndTimeZone());
		emailFlagsInfo.setExternalOrg(instances.isExternalOrganizer());
		if(emailInfo.isPrimaryCalender()) {
			String calendar =emailWorkSpace.getToCloud().equals(CLOUD_NAME.OUTLOOK)?"Calendar":emailWorkSpace.getToMailId();
			emailFlagsInfo.setCalendar(calendar);
		}
		if(emailFlagsInfo.getCalendar()==null) {
			String calendar =emailWorkSpace.getToCloud().equals(CLOUD_NAME.OUTLOOK)?"Calendar":emailWorkSpace.getToMailId();
			emailFlagsInfo.setCalendar(calendar);
		}
		emailFlagsInfo.setId(instances.getDestId());
		emailFlagsInfo.setCloudId(emailWorkSpace.getToCloudId());
	}

	private void checkModifiedEventInstances(Map<String, String> attendeResponseStatus,
			Map<String, CalenderInfo> destInstancesMap, EventInstacesInfo parentEventInstacesInfo,
			List<EventInstacesInfo> eventInstances, List<EventInstacesInfo> modifiedInstances,EventInstacesInfo parentInstanceInfo) {
		List<EventInstacesInfo> unModifiedInstances = new ArrayList<>();
		for(EventInstacesInfo instacesInfo : eventInstances) {
			boolean modified = false;
			if( instacesInfo.isDeleted() || instacesInfo.getAttendees().size()!=parentEventInstacesInfo.getAttendees().size() ||
					!instacesInfo.getSubject().equalsIgnoreCase(parentEventInstacesInfo.getSubject()) 
					|| !instacesInfo.getHtmlBodyContent().equalsIgnoreCase(parentEventInstacesInfo.getHtmlBodyContent())) {
				modified = true;
			}
			for(String attendee : instacesInfo.getAttendees()) {
				if(attendeResponseStatus.containsKey(attendee.split(Const.COLON)[0]) && !attendeResponseStatus.get(attendee.split(Const.COLON)[0]).equals(attendee.split(Const.COLON)[1])) {
					modified = true;
				}
			}
			if(destInstancesMap.containsKey(instacesInfo.getStartTime()!=null?instacesInfo.getStartTime().split("T")[0]:instacesInfo.getSourceId().split("_")[1])) {
				instacesInfo.setDestId(destInstancesMap.get(instacesInfo.getStartTime().split("T")[0]).getId());
				instacesInfo.setICalUId(destInstancesMap.get(instacesInfo.getStartTime().split("T")[0]).getICalUId());
			}
			if(instacesInfo.isDeleted() && instacesInfo.getDestId()==null) {
				String id= TimeUtils.convertRecurenceTimeToLocalDate(instacesInfo.getSourceId().split("_")[1].split("T")[0]).toString();
				if(instacesInfo.getStartTime()!=null) {
					id = instacesInfo.getStartTime().split("T")[0];
				}
				if(destInstancesMap.containsKey(id)) {
					instacesInfo.setDestId(destInstancesMap.get(id).getId());
					instacesInfo.setICalUId(destInstancesMap.get(id).getICalUId());
				}
				id= TimeUtils.convertRecurenceTimeToLocalDate(instacesInfo.getSourceId().split("_")[1],parentEventInstacesInfo.getEndTimeZone()).toString();
				if(destInstancesMap.containsKey(id)) {
					instacesInfo.setDestId(destInstancesMap.get(id).getId());
					instacesInfo.setICalUId(destInstancesMap.get(id).getICalUId());
				}
			}

			if(modified) {
				modifiedInstances.add(instacesInfo);
			}else {
				instacesInfo.setProcessStatus(com.testing.mail.repo.entities.PROCESS.INSTANCE_NOTMODIFIED);
				unModifiedInstances.add(instacesInfo);
			}
			if(unModifiedInstances.size()>20) {
				connectorService.getCalendarInfoRepoImpl().saveInstances(unModifiedInstances);
				unModifiedInstances.clear();
			}
		}
		if(!unModifiedInstances.isEmpty()) {
			connectorService.getCalendarInfoRepoImpl().saveInstances(unModifiedInstances);
			unModifiedInstances.clear();
		}
	}


	private Map<String, CalenderInfo> fetchDestEventInstances(EventInstacesInfo parentEventInstacesInfo) {
		try {
			CalenderFlags calenderFlags = new CalenderFlags();
			calenderFlags.setId(emailInfo.getDestId());
			calenderFlags.setCloudId(emailWorkSpace.getToCloudId());
			if(emailInfo.getRange()!=null) {
				EventRangeUtils eventRangeUtils = checkRecurenceRuleForOlderEvents(emailInfo.getRange(), emailInfo.getRecurrenceType());
				if(eventRangeUtils!=null) {
					calenderFlags.setStartTime(eventRangeUtils.getStartDate());
					calenderFlags.setEndTime(null==eventRangeUtils.getLastOccurence()?eventRangeUtils.getEndDate():eventRangeUtils.getLastOccurence().toString());
					if(null==calenderFlags.getEndTime() && eventRangeUtils.getType().equals("noEnd")) {
						calenderFlags.setEndTime(LocalDate.parse(eventRangeUtils.getStartDate()).plusYears(1).toString());
					}
					if(calenderFlags.getEndTime()!=null) {
						setEndTime(calenderFlags,calenderFlags.getEndTime());					
					}
				}else {
					calenderFlags.setStartTime(emailInfo.getStartTime());
					calenderFlags.setEndTime(emailInfo.getEndTime());
				}
			}else {
				calenderFlags.setStartTime(emailInfo.getStartTime().split("T")[0]);
				setEndTime(calenderFlags,emailInfo.getEndTime());
			}
			calenderFlags.setCalendar("Calendar");
			calenderFlags.setOrganizer(emailInfo.getOrganizer());
			Map<String,CalenderInfo>destInstancesMap = new HashMap<>();
			List<CalenderInfo>destEventInstances = mailServiceFactory.getConnectorService(emailWorkSpace.getToCloud()).getEventInstances(calenderFlags);
			for(CalenderInfo instance : destEventInstances) {
				parentEventInstacesInfo = convertEventsInfoToEventInstanceInfo(instance);
				destInstancesMap.put(instance.getStartTime().split("T")[0], instance);
			}
			
			return destInstancesMap;
		} catch (Exception e) {
			emailInfo.setErrorDescription(ExceptionUtils.getStackTrace(e));
		}
		return null;
	}

	private void setEndTime(CalenderFlags calenderFlags,String endTime) {
		if(emailInfo.getRecurrenceType()!=null) {
			if(emailInfo.getRecurrenceType().equalsIgnoreCase("WEEKLY")) {
				calenderFlags.setEndTime(LocalDate.parse(endTime.split("T")[0]).plusWeeks(1).toString());
			}else if(emailInfo.getRecurrenceType().equalsIgnoreCase("MONTHLY")) {
				calenderFlags.setEndTime(LocalDate.parse(endTime.split("T")[0]).plusMonths(1).toString());
			}else if(emailInfo.getRecurrenceType().equalsIgnoreCase("YEARLY")) {
				calenderFlags.setEndTime(LocalDate.parse(endTime.split("T")[0]).plusYears(1).toString());
			}else {
				calenderFlags.setEndTime(LocalDate.parse(endTime.split("T")[0]).plusDays(1).toString());
			}
		}else {
			calenderFlags.setEndTime(LocalDate.parse(endTime.split("T")[0]).plusDays(1).toString());
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
	
	public void initiateEventCreationForExternalUser(List<AttachmentsData>attachments) {
		if(emailInfo.isExternalOrganizer() && MappingUtils.isGoogleCombo(emailWorkSpace.getFromCloud(),emailWorkSpace.getToCloud())) {
			EventsInfo eventsInfo = new EventsInfo();
			BeanUtils.copyProperties(emailInfo, eventsInfo);
			new AttendeEventCreator(connectorService, emailWorkSpace, eventsInfo, mailServiceFactory, attachments).run();
		}
	}
	
	private EventRangeUtils checkRecurenceRuleForOlderEvents(String rule,String recurenceType) {
		if(CLOUD_NAME.GMAIL.equals(emailWorkSpace.getFromCloud())) {
			return EventRangeUtils.setRangeForGmailAsSource(rule,recurenceType);
		}else {
			return EventRangeUtils.setRangeForOutlookAsSource(rule,recurenceType);
		}
	}
	
	private EventInstacesInfo convertEventsInfoToEventInstanceInfo() {
		EventInstacesInfo eventInstacesInfo = new EventInstacesInfo();
		BeanUtils.copyProperties(emailInfo, eventInstacesInfo);
		return eventInstacesInfo;
	}
	
	private EventInstacesInfo convertEventsInfoToEventInstanceInfo(CalenderInfo calenderInfo) {
		EventInstacesInfo eventInstacesInfo = new EventInstacesInfo();
		BeanUtils.copyProperties(calenderInfo, eventInstacesInfo);
		return eventInstacesInfo;
	}
	
}
