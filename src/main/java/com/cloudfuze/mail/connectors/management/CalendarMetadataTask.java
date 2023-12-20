package com.testing.mail.connectors.management;

import java.util.List;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.BeanUtils;

import com.testing.mail.connectors.factory.MailServiceFactory;
import com.testing.mail.connectors.impl.OutLookMailConnector;
import com.testing.mail.connectors.microsoft.data.AttachmentsData;
import com.testing.mail.connectors.microsoft.data.CalenderViewValue;
import com.testing.mail.dao.entities.CalenderFlags;
import com.testing.mail.repo.entities.CalenderInfo;
import com.testing.mail.repo.entities.Clouds;
import com.testing.mail.repo.entities.EmailWorkSpace;
import com.testing.mail.repo.entities.EmailWorkSpace.PROCESS;
import com.testing.mail.repo.entities.EventsInfo;
import com.testing.mail.service.DBConnectorService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CalendarMetadataTask implements Runnable{

	private MailServiceFactory mailServiceFactory;
	private DBConnectorService connectorService; 
	EmailWorkSpace emailWorkSpace;
	EventsInfo emailInfo;

	com.testing.mail.repo.entities.EventsInfo.PROCESS processStatus = com.testing.mail.repo.entities.EventsInfo.PROCESS.METADATA_CONFLICT;

	public CalendarMetadataTask(MailServiceFactory mailServiceFactory, DBConnectorService connectorService,
			EmailWorkSpace emailWorkSpace, EventsInfo emailInfo) {
		this.mailServiceFactory = mailServiceFactory;
		this.connectorService = connectorService;
		this.emailWorkSpace = emailWorkSpace;
		this.emailInfo = emailInfo;
	}


	@Override
	public void run() {
		log.warn("==Entered for updating the metadata for the workspace==="+emailWorkSpace.getId());
		updateMetadata();
	}

	private void updateMetadata() {

		Thread.currentThread().setName(getClass().getSimpleName());
		log.warn("===Entered for updating the mails/Mail Folders for=="+emailWorkSpace.getId()+"==from mail=="+emailWorkSpace.getFromMailId()+"==toMail ID=="+emailWorkSpace.getToMailId()+"==fromFolder:="+emailWorkSpace.getFromFolderId());
		emailWorkSpace.setProcessStatus(PROCESS.IN_PROGRESS);
		connectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
		emailWorkSpace = connectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
		Clouds fromCloud = connectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getFromCloudId());
		Clouds toCloud = connectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getToCloudId());
		if(ObjectUtils.isEmpty(fromCloud) || ObjectUtils.isEmpty(toCloud) || StringUtils.isEmpty(emailInfo.getSourceId())) {
			emailInfo.setErrorDescription("required values for migration are null please check");
			emailInfo.setProcessStatus(EventsInfo.PROCESS.CONFLICT);
			return;
		}

		CalenderFlags emailFlagsInfo = new CalenderFlags();
		emailFlagsInfo.setCloudId(emailWorkSpace.getToCloudId());
		emailFlagsInfo.setUserId(fromCloud.getUserId());
		emailFlagsInfo.setEmailId(emailInfo.getOrganizer());
		emailFlagsInfo.setAttendees(emailInfo.getAttendees());
		emailFlagsInfo.setId(emailInfo.getDestId());
		emailFlagsInfo.setThreadId(emailInfo.getThreadId());
		emailFlagsInfo.setOrganizer(emailInfo.getOrganizer());
		emailFlagsInfo.setMetaData(emailInfo.isMetadata());
		emailFlagsInfo.setCalendar(emailInfo.getParentName());
		emailFlagsInfo.setCreatedTime(emailInfo.getCalenderCreatedTime());
		emailFlagsInfo.setSentTime(emailInfo.getCalenderModifiedTime());
		emailFlagsInfo.setDestId(emailInfo.getDestId());
		emailFlagsInfo.setStartTime(emailInfo.getStartTime());
		emailFlagsInfo.setEndTime(emailInfo.getEndTime());
		emailFlagsInfo.setTimeZone(emailInfo.getTimeZone());
		emailFlagsInfo.setFromName(emailInfo.getFromName());
		if(emailWorkSpace.isDeltaMigration()) {
			emailFlagsInfo.setDestId(emailInfo.getDestId());
		}
		emailFlagsInfo.setFlagged(emailInfo.isFlagged());
		try {
			CalenderInfo metadata = mailServiceFactory.getConnectorService(toCloud.getCloudName()).updateCalendarMetadata(emailFlagsInfo);
			if(metadata!=null) {
				emailInfo.setUpdatedMetadata(metadata.getUpdatedMetadata());
				emailInfo.setDestId(metadata.getId());
				processStatus = com.testing.mail.repo.entities.EventsInfo.PROCESS.PROCESSED;
				checkAndDeleteCalendarEvent();
				initiateEventCreationForExternalUser(emailFlagsInfo.getAttachments());
				if(!emailInfo.getOrganizer().equals(emailWorkSpace.getToMailId())) {
					CalenderFlags flags = new CalenderFlags();
					flags.setCloudId(emailWorkSpace.getToCloudId());
					flags.setICalUId(emailInfo.getICalUId());
					CalenderViewValue value = ((OutLookMailConnector)mailServiceFactory.getConnectorService(toCloud.getCloudName())).getEventByConversationId(flags);
					if(value!=null) {
						emailInfo.setDestId(value.getId());
					}
				}
				if(emailInfo.isInstances()) {
					processStatus = EventsInfo.PROCESS.INSTANCE_NOTPROCESSED;
				}
			}
		}catch(Exception e) {
			processStatus = com.testing.mail.repo.entities.EventsInfo.PROCESS.METADATA_CONFLICT;
			emailInfo.setRetryCount(emailInfo.getRetryCount()+1);
			emailInfo.setErrorDescription(ExceptionUtils.getStackTrace(e));
		}finally{
			emailInfo.setProcessStatus(processStatus);
			connectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
			connectorService.getCalendarInfoRepoImpl().save(emailInfo);
		}
	}
	
	private void checkAndDeleteCalendarEvent() {
		try {
			if(!emailWorkSpace.isEventNotifications()) {
				EventsInfo eventsInfo = new EventsInfo();
				BeanUtils.copyProperties(emailInfo, eventsInfo);
				new AttendeeEventDeleter(mailServiceFactory, connectorService, emailInfo.getAttendees(), eventsInfo, emailWorkSpace).run();
			}
		} catch (Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
		}
	}
	
	public void initiateEventCreationForExternalUser(List<AttachmentsData>attachments) {
		if(emailInfo.isExternalOrganizer()) {
			EventsInfo eventsInfo = new EventsInfo();
			BeanUtils.copyProperties(emailInfo, eventsInfo);
			new AttendeEventCreator(connectorService, emailWorkSpace, eventsInfo, mailServiceFactory, attachments).run();
		}
	}
}

