package com.cloudfuze.mail.connectors.management;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.BeanUtils;

import com.cloudfuze.mail.connectors.factory.MailServiceFactory;
import com.cloudfuze.mail.connectors.microsoft.data.AttachmentsData;
import com.cloudfuze.mail.constants.Const;
import com.cloudfuze.mail.dao.entities.CalenderFlags;
import com.cloudfuze.mail.dao.entities.ConnectFlags;
import com.cloudfuze.mail.dao.entities.EmailFlagsInfo;
import com.cloudfuze.mail.repo.entities.CalenderInfo;
import com.cloudfuze.mail.repo.entities.Clouds;
import com.cloudfuze.mail.repo.entities.Clouds.CLOUD_NAME;
import com.cloudfuze.mail.repo.entities.EmailWorkSpace;
import com.cloudfuze.mail.repo.entities.EmailWorkSpace.PROCESS;
import com.cloudfuze.mail.repo.entities.EventsInfo;
import com.cloudfuze.mail.repo.entities.MemberDetails;
import com.cloudfuze.mail.service.DBConnectorService;
import com.cloudfuze.mail.utils.MappingUtils;

import lombok.extern.slf4j.Slf4j;

/**
 For migrating the calendar Events after Picked from the Source
*/

@Slf4j
public class CalendarMigrationTask implements Callable<Object> {

	private MailServiceFactory mailServiceFactory;
	private DBConnectorService connectorService; 
	EmailWorkSpace emailWorkSpace;
	EventsInfo emailInfo;




	public CalendarMigrationTask(MailServiceFactory mailServiceFactory, DBConnectorService connectorService,
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
		emailInfo.setProcessStatus(EventsInfo.PROCESS.IN_PROGRESS);
		connectorService.getCalendarInfoRepoImpl().save(emailInfo);
		Clouds fromCloud = connectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getFromCloudId());
		Clouds toCloud = connectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getToCloudId());
		if(ObjectUtils.isEmpty(fromCloud) || ObjectUtils.isEmpty(toCloud)) {
			emailInfo.setErrorDescription("required values for migration are null please check");
			emailInfo.setProcessStatus(EventsInfo.PROCESS.CONFLICT);
			return;
		}
		
		CalenderFlags emailFlagsInfo = new CalenderFlags();
		emailFlagsInfo.setCloudId(emailWorkSpace.getToCloudId());
		emailFlagsInfo.setUserId(fromCloud.getUserId());
		emailFlagsInfo.setEmailId(emailInfo.getOrganizer());
		// Adding the to's so that the from user won't get notification as we are sending email to those users in CF_EMAIL_MIGRATION
		//tos.remove(emailWorkSpace.getFromMailId());
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
		emailFlagsInfo.setExternalOrg(emailInfo.isExternalOrganizer());
		if(emailInfo.isPrimaryCalender()) {
			String calendar =emailWorkSpace.getToCloud().equals(CLOUD_NAME.OUTLOOK)?"Calendar":emailWorkSpace.getToMailId();
			emailFlagsInfo.setCalendar(calendar);
		}
		Map<String,String>srcMembers = getMemberDetails(emailWorkSpace.getUserId(), fromCloud.getAdminCloudId());
		if(emailFlagsInfo.getCalendar()==null) {
			String calendar =emailWorkSpace.getToCloud().equals(CLOUD_NAME.OUTLOOK)?"Calendar":emailWorkSpace.getToMailId();
			emailFlagsInfo.setCalendar(calendar);
		}
		
		
		try {
			if(emailWorkSpace.getToCloud().equals(CLOUD_NAME.GMAIL) && emailInfo.isAttachMents()) {
				EmailFlagsInfo info = getAttachments(srcMembers,toCloud.getAdminCloudId());
				List<AttachmentsData> attachments = mailServiceFactory.getConnectorService(fromCloud.getCloudName()).getAttachments(info);
				emailFlagsInfo.setAttachments(attachments);
			}
			CalenderInfo created = null;
			if(!emailInfo.isDeleted()){
				emailFlagsInfo.setCloudId(emailWorkSpace.getToCloudId());
				if((srcMembers.containsKey(emailInfo.getOrganizer()) || srcMembers.isEmpty()) && emailWorkSpace.getToCloud().equals(CLOUD_NAME.GMAIL)) {
					Clouds cloud = connectorService.getCloudsRepoImpl().findCloudsByEmailIdUsingAdmin(emailInfo.getUserId(), emailInfo.getOrganizer(), toCloud.getAdminCloudId());
					if(cloud!=null) {
						emailFlagsInfo.setCloudId(cloud.getId());
					}else if(!emailFlagsInfo.getCalendar().equals(emailInfo.getOrganizer())) {
						cloud = connectorService.getCloudsRepoImpl().findCloudsByEmailIdUsingAdmin(emailInfo.getUserId(), emailFlagsInfo.getCalendar(), toCloud.getAdminCloudId());
						if(cloud!=null) 
							emailFlagsInfo.setCloudId(cloud.getId());
					}
				}
				created = mailServiceFactory.getConnectorService(toCloud.getCloudName()).createCalenderEvent(emailFlagsInfo);
				emailInfo.setCreated(true);
				emailInfo.setICalUId(created.getICalUId());
				initiateEventCreationForExternalUser(emailFlagsInfo.getAttachments());
				if(emailInfo.isAttachMents() && !emailWorkSpace.getToCloud().equals(CLOUD_NAME.GMAIL)) {
					EmailFlagsInfo info = getAttachments(new HashMap<String, String>(),null);
					List<AttachmentsData> attachments = mailServiceFactory.getConnectorService(fromCloud.getCloudName()).getAttachments(info);
					info.setAttachments(attachments);
					info.setFolder(emailInfo.getDestParent());
					info.setCloudId(emailWorkSpace.getToCloudId());
					info.setId(created.getId());
					info.setSentTime(emailFlagsInfo.getEndTime());
					info.setEndTimeZone(emailInfo.getEndTimeZone());
					info.setTimeZone(emailFlagsInfo.getTimeZone());
					info.setStartTime(emailFlagsInfo.getStartTime());
					info.setBodyPreview(emailFlagsInfo.getBodyPreview());
					info.setSubject(emailFlagsInfo.getSubject());
					info.setHtmlMessage(emailFlagsInfo.getHtmlMessage());
					info.setFrom(emailInfo.getOrganizer());
					List<String> attaches = mailServiceFactory.getConnectorService(toCloud.getCloudName()).addAttachment(info, true);
					
					if(attaches!=null) {
						///emailInfo.setAttachmentIds(attaches);
						log.info("===== Attachment Event upload is succeessFull==For=="+toCloud.getCloudName()+"==Cloud=="+emailWorkSpace.getId());
					}
				}
			}else {
				//For Deleting the Event from inbox directly moving to deletedItems
				emailFlagsInfo.setCloudId(emailWorkSpace.getToCloudId());
				//emailFlagsInfo.setFolder("deleteditems");
				emailFlagsInfo.setDestId(emailInfo.getDestId());
				created = new CalenderInfo();
				created.setId(emailInfo.getDestId());
				EmailFlagsInfo flagsInfo = new EmailFlagsInfo();
				flagsInfo.setCloudId(emailWorkSpace.getToCloudId());
				flagsInfo.setId(emailInfo.getDestId());
				boolean deleted = mailServiceFactory.getConnectorService(toCloud.getCloudName()).deleteEmails(flagsInfo, true);
				created.setDeleted(deleted);
			}
			if(created!=null) {
				log.warn("==Created calendar event=="+emailInfo.getSubject()+"--fileID=="+emailInfo.getId()+"-"+emailWorkSpace.getId());
				emailInfo.setDestId(created.getId());
				emailInfo.setICalUId(created.getICalUId());
				emailInfo.setLargeFile(emailFlagsInfo.isLargeFile());
				emailInfo.setDestParent(emailInfo.getDestFolderPath());
				if(!emailInfo.isCalender() && !emailWorkSpace.getToCloud().equals(CLOUD_NAME.GMAIL) && !emailWorkSpace.isDeltaMigration()) {
					emailInfo.setProcessStatus(EventsInfo.PROCESS.METADATA_STARTED);
				}else {
					emailInfo.setProcessStatus(EventsInfo.PROCESS.PROCESSED);
				}
				emailInfo.setErrorDescription("COMPLETED");
				//emailInfo.setAttachmentIds(created.getAttachmentIds());
				emailInfo.setUpdatedMetadata(created.getUpdatedMetadata());
				emailInfo.setDeleted(created.isDeleted());
			}
		}catch(Exception e) {
			emailInfo.setProcessStatus(EventsInfo.PROCESS.CONFLICT);
			emailInfo.setRetryCount(emailInfo.getRetryCount()+1);
			emailInfo.setErrorDescription(ExceptionUtils.getStackTrace(e));
		}
		finally {
			connectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
			if(emailInfo.getProcessStatus().equals(EventsInfo.PROCESS.IN_PROGRESS)) {
				emailInfo.setProcessStatus(EventsInfo.PROCESS.CONFLICT);
				emailInfo.setRetryCount(emailInfo.getRetryCount()+1);
				emailInfo.setErrorDescription("InternalException Occured Check logs");
			}
			connectorService.getCalendarInfoRepoImpl().save(emailInfo);
		}
	}

	private EmailFlagsInfo getAttachments(Map<String,String>srcMembers,String adminCloudId) {
		EmailFlagsInfo info = new EmailFlagsInfo();
		info.setId(emailInfo.getSourceId());
		info.setCloudId(emailWorkSpace.getFromCloudId());
		if((srcMembers.containsKey(emailInfo.getOrganizer()) || srcMembers.isEmpty()) && adminCloudId!=null) {
			Clouds cloud = connectorService.getCloudsRepoImpl().findCloudsByEmailIdUsingAdmin(emailInfo.getUserId(), emailInfo.getOrganizer(), adminCloudId);
			if(cloud!=null) {
				info.setCloudId(cloud.getId());
			}
		}
		info.setEvents(true);
		if(emailWorkSpace.getFromCloud().equals(CLOUD_NAME.GMAIL) && emailInfo.getAttachmentIds()!=null && !emailInfo.getAttachmentIds().isEmpty()) {
			List<AttachmentsData> attacs = new ArrayList<>();
			for(String data : emailInfo.getAttachmentIds()) {
				try {
					AttachmentsData attach = new AttachmentsData();
					attach.setId(data.split(":")[0]);
					if(data.split(":").length>2) {
						attach.setName(data.split(":")[1]);
						attach.setContentType(data.split(":")[2]);
						attach.setParentFolderId(data.split(":")[3]);
					}
					attacs.add(attach);
				} catch (Exception e) {
					log.warn(ExceptionUtils.getStackTrace(e));
				}
			}
			info.setAttachments(attacs);
		}
		return info;
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
	
	public Map<String,String>getMemberDetails(String userId,String adminCloudId){
		MemberDetails memberDetails = connectorService.getCloudsRepoImpl().findMemberDetails(userId, adminCloudId);
		Map<String, String> resultMap = new HashMap<>();
		if(memberDetails!=null) {
			for(String member : memberDetails.getMembers()) {
				if(!resultMap.containsKey(member.split(Const.HASHTAG)[0])) {
					resultMap.put(member.split(Const.HASHTAG)[0], member.split(Const.HASHTAG)[1]);
				}
			}
		}
		return resultMap;
	}
	
}
