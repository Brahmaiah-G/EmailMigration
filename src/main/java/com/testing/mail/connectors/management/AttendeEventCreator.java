package com.testing.mail.connectors.management;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.testing.mail.connectors.factory.MailServiceFactory;
import com.testing.mail.connectors.microsoft.data.AttachmentsData;
import com.testing.mail.dao.entities.CalenderFlags;
import com.testing.mail.repo.entities.CalenderInfo;
import com.testing.mail.repo.entities.Clouds;
import com.testing.mail.repo.entities.EmailWorkSpace;
import com.testing.mail.repo.entities.EventsInfo;
import com.testing.mail.repo.entities.Clouds.CLOUD_NAME;
import com.testing.mail.repo.entities.EmailWorkSpace.PROCESS;
import com.testing.mail.service.DBConnectorService;
import com.testing.mail.utils.ConnectUtils;

import lombok.extern.slf4j.Slf4j;
@Slf4j
public class AttendeEventCreator implements Runnable {

	private DBConnectorService connectorService;
	private EmailWorkSpace emailWorkSpace;
	private EventsInfo emailInfo;
	private MailServiceFactory mailServiceFactory;
	private List<AttachmentsData> attachments;

	
	
	
	public AttendeEventCreator(DBConnectorService connectorService, EmailWorkSpace emailWorkSpace,
			EventsInfo emailInfo, MailServiceFactory mailServiceFactory, List<AttachmentsData> attachments) {
		this.connectorService = connectorService;
		this.emailWorkSpace = emailWorkSpace;
		this.emailInfo = emailInfo;
		this.mailServiceFactory = mailServiceFactory;
		this.attachments = attachments;
	}

	@Override
	public void run() {
		try {
			createEventForAttendees();
		} catch (Exception e) {
		}
	}

	private void createEventForAttendees() throws Exception {
		log.info("===Entered for Creating evetns for attendes for=="+emailWorkSpace.getId()+"==Event id=="+emailInfo.getId());
		emailWorkSpace.setProcessStatus(PROCESS.IN_PROGRESS);
		emailWorkSpace = connectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
		connectorService.getCalendarInfoRepoImpl().save(emailInfo);
		Clouds fromCloud = connectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getFromCloudId());
		Clouds toCloud = connectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getToCloudId());
		ConnectUtils.checkClouds(fromCloud);
		ConnectUtils.checkClouds(toCloud);
		CalenderFlags emailFlagsInfo = new CalenderFlags();
		emailFlagsInfo.setCloudId(emailWorkSpace.getToCloudId());
		emailFlagsInfo.setUserId(fromCloud.getUserId());
		emailFlagsInfo.setEmailId(emailInfo.getOrganizer());
		emailFlagsInfo.setAttendees(emailInfo.getAttendees());
		emailFlagsInfo.setThreadId(emailInfo.getThreadId());
		emailFlagsInfo.setOnlineMeeting(emailInfo.isOnlineMeeting());
		emailFlagsInfo.setRemainderOn(emailInfo.isReminderOn());
		emailFlagsInfo.setOnlineMeetingUrl(emailInfo.getOnlineMeetingUrl());
		emailFlagsInfo.setOrganizer(emailInfo.getOrganizer());
		emailFlagsInfo.setSubject(emailInfo.getSubject());
		emailFlagsInfo.setHadAttachments(emailInfo.isAttachMents());
		emailFlagsInfo.setMetaData(emailInfo.isMetadata());
		emailFlagsInfo.setHtmlContent(emailInfo.isHtmlContent());
		emailFlagsInfo.setHtmlMessage(emailInfo.getHtmlBodyContent());
		emailFlagsInfo.setBodyPreview(emailInfo.getBodyPreview());
		emailFlagsInfo.setDeleted(emailInfo.isDeleted());
		emailFlagsInfo.setLocation(emailInfo.getLocation());
		emailFlagsInfo.setRange(emailInfo.getRange());
		emailFlagsInfo.setColour(emailInfo.getColor());
		emailFlagsInfo.setRecurrenceType(emailInfo.getRecurrenceType());
		emailFlagsInfo.setMessage(emailInfo.getHtmlBodyContent());
		emailFlagsInfo.setDestId(emailInfo.getDestId());
		emailFlagsInfo.setICalUId(emailInfo.getICalUId());
		if(emailInfo.isCalender()) {
			emailFlagsInfo.setCalendar(emailInfo.getSubject());
		}else {
			emailFlagsInfo.setCalendar(emailInfo.getParentName()==null ? emailInfo.getDestFolderPath() : emailInfo.getParentName());
		}
		emailFlagsInfo.setCreatedTime(emailInfo.getCalenderCreatedTime());
		emailFlagsInfo.setSentTime(emailInfo.getCalenderModifiedTime());
		emailFlagsInfo.setDestId(emailInfo.getDestId());
		emailFlagsInfo.setStartTime(emailInfo.getStartTime());
		emailFlagsInfo.setEndTime(emailInfo.getEndTime());
		emailFlagsInfo.setTimeZone(emailInfo.getTimeZone());
		emailFlagsInfo.setFromName(emailInfo.getFromName());
		emailFlagsInfo.setFlagged(emailInfo.isFlagged());
		emailFlagsInfo.setEndTimeZone(emailInfo.getEndTimeZone());
		emailFlagsInfo.setExternalOrg(true);
		if(emailInfo.isPrimaryCalender()) {
			String calendar =emailWorkSpace.getToCloud().equals(CLOUD_NAME.OUTLOOK)?"Calendar":emailWorkSpace.getToMailId();
			emailFlagsInfo.setCalendar(calendar);
		}
		if(emailFlagsInfo.getCalendar()==null) {
			String calendar =emailWorkSpace.getToCloud().equals(CLOUD_NAME.OUTLOOK)?"Calendar":emailWorkSpace.getToMailId();
			emailFlagsInfo.setCalendar(calendar);
		}


		try {
			if(attachments!=null && !attachments.isEmpty() && emailInfo.isAttachMents()) {
				emailFlagsInfo.setAttachments(attachments);
			}

			CalenderInfo mailFolder =  connectorService.getCalendarInfoRepoImpl().getParentCalendarInfo(emailWorkSpace.getId(), emailInfo.getSourceParent());
			if(mailFolder!=null) {
				emailInfo.setDestParent(mailFolder.getDestId());
				emailFlagsInfo.setCalendar(mailFolder.getDestId());
			}
			emailInfo.setErrorDescription(null);
			List<String>dups = new ArrayList<>();
			for(String attendees :emailFlagsInfo.getAttendees()) {
				String _attendees = attendees.split(":")[0];
				if(emailWorkSpace.getToMailId().equals(_attendees) || dups.contains(_attendees) || _attendees.equals(emailInfo.getOrganizer()) || _attendees.equals(emailInfo.getParentName())) {
					continue;
				}
				dups.add(_attendees);
				Clouds cloud = connectorService.getCloudsRepoImpl().findCloudsByEmailIdUsingAdmin(emailInfo.getUserId(), _attendees, toCloud.getAdminCloudId());
				if(cloud==null) {
					log.info("--For External User Skipping the event creation---"+emailInfo.getId()+"--"+emailInfo.getSourceId());
					emailInfo.setErrorDescription(emailInfo.getErrorDescription()+"-"+_attendees);
					continue;	
				}
				if(emailWorkSpace.getToCloud().equals(CLOUD_NAME.OUTLOOK)) {
					emailFlagsInfo.setCloudId(cloud.getId());
					emailFlagsInfo.setId(emailInfo.getDestId());
					CalenderInfo metadata = mailServiceFactory.getConnectorService(toCloud.getCloudName()).updateCalendarMetadata(emailFlagsInfo);
					if(metadata!=null) {
						emailInfo.setUpdatedMetadata(metadata.getUpdatedMetadata());
						emailInfo.setDestId(metadata.getId());
					}
				}else {
					emailFlagsInfo.setCloudId(cloud.getId());
					emailFlagsInfo.setCalendar(cloud.getEmail());
					CalenderInfo created = mailServiceFactory.getConnectorService(toCloud.getCloudName()).createCalenderEvent(emailFlagsInfo);
					if(created!=null) {
						emailInfo.setErrorDescription(emailInfo.getErrorDescription()+"-"+cloud.getEmail());
					}
				}
			}

		}catch(Exception e) {
			emailInfo.setProcessStatus(EventsInfo.PROCESS.CONFLICT);
			emailInfo.setRetryCount(emailInfo.getRetryCount()+1);
			emailInfo.setErrorDescription(ExceptionUtils.getStackTrace(e));
			throw e;
		}
		finally {
			connectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
			if(emailInfo.getProcessStatus().equals(EventsInfo.PROCESS.IN_PROGRESS)) {
				emailInfo.setProcessStatus(EventsInfo.PROCESS.CONFLICT);
				emailInfo.setErrorDescription("ExceptionOccured");
			}
			connectorService.getCalendarInfoRepoImpl().save(emailInfo);
		}
	}
}
