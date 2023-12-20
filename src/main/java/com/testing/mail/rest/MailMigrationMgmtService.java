package com.testing.mail.rest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.testing.mail.connectors.factory.MailServiceFactory;
import com.testing.mail.connectors.management.AttendeeEventDeleter;
import com.testing.mail.dao.entities.EmailFlagsInfo;
import com.testing.mail.exceptions.InvaildCredentialsException;
import com.testing.mail.repo.entities.CalendarPickingQueue;
import com.testing.mail.repo.entities.CalenderInfo;
import com.testing.mail.repo.entities.Clouds;
import com.testing.mail.repo.entities.EmailBatches;
import com.testing.mail.repo.entities.EmailFolderInfo;
import com.testing.mail.repo.entities.EmailInfo;
import com.testing.mail.repo.entities.EmailJobDetails;
import com.testing.mail.repo.entities.EmailPickingQueue;
import com.testing.mail.repo.entities.EmailWorkSpace;
import com.testing.mail.repo.entities.EventsInfo;
import com.testing.mail.repo.entities.MappedUsers;
import com.testing.mail.repo.entities.PlatformUser;
import com.testing.mail.repo.entities.PremigrationDetails;
import com.testing.mail.repo.entities.Clouds.CLOUD_NAME;
import com.testing.mail.repo.entities.EmailWorkSpace.PROCESS;
import com.testing.mail.service.DBConnectorService;
import com.testing.mail.utils.HttpUtils;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/move")
@Slf4j
public class MailMigrationMgmtService {

	@Autowired
	DBConnectorService connectorService;
	@Autowired
	MailServiceFactory mailServiceFactory;
	
	/**
	 * Initiating the migration based on List of EmailWorkSpaces
	*/
	@PostMapping("/initiate")
	public ResponseEntity<?> initiateMigration(@RequestBody List<EmailWorkSpace> emailWorkSpaces,@RequestAttribute("userId")String userId,@RequestParam(required=false,defaultValue="false")boolean preScan){
		log.warn("== Going for Creating a workspace for migration==");
		EmailJobDetails emailJobDetails = null;
		if(ObjectUtils.isEmpty(emailWorkSpaces)) {
			HttpUtils.BadRequest(HttpUtils.ERROR_REASONS.NO_CONTETNT_TO_PROCEED.name());
		}
		
		PlatformUser user = connectorService.getPlatformUserRepository().findById(userId).orElseThrow(InvaildCredentialsException :: new);
		
		for(EmailWorkSpace emailWorkSpace : emailWorkSpaces) {
			emailWorkSpace.setCopy(true);
			Clouds fromCloud = null;
			Clouds toCloud = null;
			if(user!=null) {
				emailWorkSpace.setOwnerEmailId(user.getEmail());
			}
			if(emailWorkSpace.getFromMailId()!=null && emailWorkSpace.getFromCloudId()==null) {
				fromCloud = connectorService.getCloudsRepoImpl().findCloudsByEmailId(userId, emailWorkSpace.getFromMailId(),emailWorkSpace.getFromCloud().name());
			}else {
				fromCloud = connectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getFromCloudId());
			}
			emailWorkSpace.setPreScan(preScan);
			emailWorkSpace.setEnt(user.getEnt());
			//emailWorkSpace.setCopy(true);
			EmailWorkSpace _emailWorkSpace = emailWorkSpaces.get(0);
			if(emailWorkSpace.isDeltaMigration()) {
				EmailWorkSpace emailWorkSpaceOld = connectorService.getWorkSpaceRepoImpl().getWorkSpaceBasedOnPaths(userId, _emailWorkSpace.getFromMailId(),
						_emailWorkSpace.getToMailId(), _emailWorkSpace.getFromCloud().name(), _emailWorkSpace.getToCloud().name(), _emailWorkSpace.getOwnerEmailId());
				if(emailWorkSpaceOld!=null) {
					emailJobDetails = connectorService.getEmailJobRepoImpl().findOne(emailWorkSpaceOld.getJobId());
					emailJobDetails.setCalFiltered(false);
					emailJobDetails.setMailsFiltered(false);
					emailJobDetails.setProcessStatus(EmailJobDetails.PROCESS.IN_PROGRESS);
				}
				emailWorkSpace.setLastEmailWorkSpaceId(emailWorkSpaceOld.getId());
				emailJobDetails.setDeltaMigration(true);
				emailWorkSpace.setSettings(emailWorkSpaceOld.isSettings());
				emailWorkSpace.setEventNotifications(emailWorkSpaceOld.isEventNotifications());
				emailWorkSpace.setMailRules(emailWorkSpaceOld.isMailRules());
				emailWorkSpace.setCalendar(emailWorkSpaceOld.isCalendar());
			}
			if(emailJobDetails==null){
				emailJobDetails =  createJob(_emailWorkSpace,userId);
				emailJobDetails = connectorService.getEmailJobRepoImpl().save(emailJobDetails);
			}
			if(emailWorkSpace.getToMailId()!=null && emailWorkSpace.getToCloudId()==null) {
				toCloud = connectorService.getCloudsRepoImpl().findCloudsByEmailId(userId, emailWorkSpace.getToMailId(),emailWorkSpace.getToCloud().name());
			}else {
				toCloud = connectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getToCloudId());
			}
			if(ObjectUtils.isEmpty(fromCloud) || ObjectUtils.isEmpty(toCloud)) {
				emailWorkSpace.setErrorDescription(HttpUtils.ERROR_REASONS.CLOUDS_NOT_AVAILABLE.name());
				emailWorkSpace.setProcessStatus(PROCESS.CONFLICT);
				emailWorkSpace.setUserId(userId);
				connectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
				continue;
			}
			emailWorkSpace.setFromAdminCloud(fromCloud.getAdminCloudId());
			emailWorkSpace.setToAdminCloud(toCloud.getAdminCloudId());
			emailWorkSpace.setEnt(user.getEnt());
			EmailBatches emailBatches = connectorService.getCloudsRepoImpl().getBatchPerCloud(userId, fromCloud.getId(), toCloud.getId());
			emailWorkSpace.setFromCloudId(fromCloud.getId());
			emailWorkSpace.setToCloudId(toCloud.getId());
			emailWorkSpace.setUserId(userId);
			emailWorkSpace.setCreatedTime(LocalDateTime.now());
			emailWorkSpace.setProcessStatus(PROCESS.NOT_PROCESSED);
			//By Default we are setting the next changes for every four hours
			boolean isCalendars = true;
			if(emailWorkSpace.getChangeHours()==null) {
				emailWorkSpace.setChangeHours("24H0M");
			}
			if(user.isTest()) {
				emailWorkSpace.setChangeHours("0H1M");
			}
			emailWorkSpace.setJobId(emailJobDetails.getId());
			emailWorkSpace =  connectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
			emailJobDetails.getWorkspaceId().add(emailWorkSpace.getId());
			emailJobDetails = connectorService.getEmailJobRepoImpl().save(emailJobDetails);
			if(emailBatches!=null) {
				emailBatches.setMoveWorkSpaceId(emailWorkSpace.getId());
				connectorService.getCloudsRepoImpl().saveBatches(Arrays.asList(emailBatches));
			}
			EmailFolderInfo emailInfo = new EmailFolderInfo();
			createPickingQueue(emailWorkSpace);
			emailInfo.setEmailWorkSpaceId(emailWorkSpace.getId());
			if("/".equals(emailWorkSpace.getFromFolderId())) {
				emailInfo.setSourceId("/");
			}else {
				isCalendars = false;
				emailInfo.setSourceId(emailWorkSpace.getFromFolderId());
				emailInfo.setSourceId(emailInfo.getSourceId().replace("/", ""));
				EmailFlagsInfo emailFlagsInfo = new EmailFlagsInfo();
				emailFlagsInfo.setCloudId(emailWorkSpace.getFromCloudId());
				emailFlagsInfo.setFolder(emailInfo.getSourceId());
				emailInfo.setMailFolder(emailInfo.getSourceId());
				emailInfo.setPreScan(preScan);
				if(emailWorkSpace.getFromCloud().equals(CLOUD_NAME.GMAIL)) {
					emailFlagsInfo.setFolder(emailFlagsInfo.getFolder().toUpperCase());
					emailInfo.setSourceId(emailInfo.getSourceId().toUpperCase());
					emailInfo.setMailFolder(emailInfo.getSourceId().toUpperCase());
				}
				EmailInfo parentFolder = mailServiceFactory.getConnectorService(emailWorkSpace.getFromCloud()).getLabel(emailFlagsInfo);
				if(parentFolder!=null) {
					emailInfo.setSourceParent(parentFolder.getId());
				}else{
					emailWorkSpace.setProcessStatus(PROCESS.CONFLICT);
					emailWorkSpace.setErrorDescription("From Folder Not Found wrong path");
					connectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
					continue;
				}
				// here check whether that label exist or not add that condition if not from the list of exisitng domains
			}
			if("/".equals(emailWorkSpace.getToFolderId())) {
				emailInfo.setDestFolderPath("/");
			}else {
				isCalendars = false;
				emailInfo.setDestFolderPath(emailWorkSpace.getToFolderId());
				emailInfo.setDestFolderPath(emailInfo.getDestFolderPath().replace("/", ""));
				EmailFlagsInfo emailFlagsInfo = new EmailFlagsInfo();
				emailFlagsInfo.setCloudId(emailWorkSpace.getToCloudId());
				emailFlagsInfo.setFolder(emailWorkSpace.getToFolderId().replace("/", ""));
				if(emailWorkSpace.getToCloud().equals(CLOUD_NAME.GMAIL)) {
					emailFlagsInfo.setFolder(emailFlagsInfo.getFolder().toUpperCase());
					emailInfo.setDestFolderPath(emailInfo.getDestFolderPath().toUpperCase());
				}
				EmailInfo parentFolder = mailServiceFactory.getConnectorService(emailWorkSpace.getToCloud()).getLabel(emailFlagsInfo);
				if(parentFolder!=null) {
					emailInfo.setDestParent(parentFolder.getId());
				}else{
					emailWorkSpace.setErrorDescription("Wrong CSV Path");
					emailWorkSpace.setProcessStatus(PROCESS.CONFLICT);
					emailJobDetails.setProcessStatus(EmailJobDetails.PROCESS.CONFLICT);
					connectorService.getEmailJobRepoImpl().save(emailJobDetails);
					connectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
					continue;
				}
			}
			//emailInfo.setMetadata(true);
			//emailInfo.setFolder(true);
			emailInfo.setUserId(userId);
			emailInfo.setEnv(user.getEnt());
			emailInfo.setFromCloudId(emailWorkSpace.getFromCloudId());
			emailInfo.setToCloudId(emailWorkSpace.getToCloudId());
			emailWorkSpace.setSourceLatestId(userId);
			emailInfo.setCreatedTime(LocalDateTime.now());
			emailInfo.setJobId(emailJobDetails.getId());
			if(emailWorkSpace.isCalendar() && isCalendars) {
				createCalendarPickingQueue(emailWorkSpace);
				createCalendarInfo(emailWorkSpace);
			}
			connectorService.getEmailFolderInfoRepoImpl().save(emailInfo);
		}
		return HttpUtils.Ok(HttpUtils.SUCESS_REASONS.INITIATED.name());
	}
	
	/**
	 * Initiating the PreScan Migration based on List of EmailWorkSpaces
	*/
	@PostMapping("/initiate/preScan")
	public ResponseEntity<?> initiatePreScanMigration(@RequestBody EmailWorkSpace emailWorkSpace,@RequestAttribute("userId")String userId){
		if(ObjectUtils.isEmpty(emailWorkSpace)) {
			HttpUtils.BadRequest(HttpUtils.ERROR_REASONS.NO_CONTETNT_TO_PROCEED.name());
		}
		PlatformUser user = connectorService.getPlatformUserRepository().findById(userId).orElseThrow(InvaildCredentialsException::new);
		emailWorkSpace.setUserId(userId);
		emailWorkSpace.setPreScan(true);
		
		EmailFolderInfo emailInfo = new EmailFolderInfo();
		if("/".equals(emailWorkSpace.getFromFolderId())) {
			emailInfo.setSourceId("/");
		}else {
			emailInfo.setSourceId(emailWorkSpace.getFromFolderId());
		}
		Clouds fromCloud = null;
		if(emailWorkSpace.getFromMailId()!=null && emailWorkSpace.getFromCloudId()==null) {
			fromCloud = connectorService.getCloudsRepoImpl().findCloudsByEmailId(userId, emailWorkSpace.getFromMailId(),emailWorkSpace.getFromCloud().name());
		}else {
			fromCloud = connectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getFromCloudId());
		}
		
		EmailWorkSpace workSpace = connectorService.getWorkSpaceRepoImpl().getPremigrationWorkSpace(fromCloud.getEmail(), fromCloud.getCloudName().name());
		if(workSpace!=null) {
			connectorService.getEmailInfoRepoImpl().removeEmails(workSpace.getId());
			connectorService.getCalendarInfoRepoImpl().removeCalendars(workSpace.getId());
			connectorService.getWorkSpaceRepoImpl().removeOne(workSpace.getId());
		}
		if(ObjectUtils.isEmpty(fromCloud)) {
			emailWorkSpace.setErrorDescription(HttpUtils.ERROR_REASONS.CLOUDS_NOT_AVAILABLE.name());
			emailWorkSpace.setProcessStatus(PROCESS.CONFLICT);
			emailWorkSpace.setUserId(userId);
			connectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
			return HttpUtils.BadRequest(HttpUtils.ERROR_REASONS.USER_OR_EMAIL_NOT_AVAILABLE.name());
		}
		emailWorkSpace.setOwnerEmailId(user.getEmail());
		emailWorkSpace.setEnt(user.getEnt());
		fromCloud.setPreMigrationStatus(EmailWorkSpace.PROCESS.STARTED.name());
		emailWorkSpace.setFromCloud(fromCloud.getCloudName());
		emailWorkSpace.setFromCloudId(fromCloud.getId());
		emailWorkSpace.setToCloud(fromCloud.getCloudName());
		emailWorkSpace.setToCloudId(fromCloud.getId());
		emailWorkSpace.setToMailId(fromCloud.getEmail());
		emailWorkSpace.setToFolderId("/");
		if(emailWorkSpace.getToFolderId().equals("/")) {
			emailInfo.setDestFolderPath("/");
		}else {
			emailInfo.setDestFolderPath(emailWorkSpace.getToFolderId());
		}
		emailInfo.setCreatedTime(LocalDateTime.now());
		connectorService.getCloudsRepoImpl().save(fromCloud);
		emailWorkSpace =  connectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
		createPickingQueue(emailWorkSpace);
		createPremigrationDetails(emailWorkSpace);
		emailInfo.setPreScan(true);
		emailInfo.setFromCloudId(emailWorkSpace.getFromCloudId());
		emailInfo.setToCloudId(emailWorkSpace.getToCloudId());
		emailInfo.setEmailWorkSpaceId(emailWorkSpace.getId());
		emailInfo.setUserId(userId);
		createCalendarInfo(emailWorkSpace);
		connectorService.getEmailFolderInfoRepoImpl().save(emailInfo);
		return HttpUtils.Ok(HttpUtils.SUCESS_REASONS.INITIATED.name());
	}
	
	
	private EmailJobDetails createJob(EmailWorkSpace emailWorkSpace,String userId) {
		EmailJobDetails emailJobDetails = new EmailJobDetails();
		emailJobDetails.setUserId(userId);
		emailJobDetails.setFromCloud(emailWorkSpace.getFromCloud());
		emailJobDetails.setToCloud(emailWorkSpace.getToCloud());
		emailJobDetails.setFromCloudId(emailWorkSpace.getFromCloudId());
		emailJobDetails.setToCloudId(emailWorkSpace.getToCloudId());
		emailJobDetails.setFromFolderId(emailWorkSpace.getFromFolderId());
		emailJobDetails.setToFolderId(emailWorkSpace.getToFolderId());
		emailJobDetails.setFromMailId(emailWorkSpace.getFromMailId());
		emailJobDetails.setToMailId(emailWorkSpace.getToMailId());
		emailJobDetails.setProcessStatus(EmailJobDetails.PROCESS.NOT_PROCESSED);
		emailJobDetails.setOwnerEmailId(emailWorkSpace.getOwnerEmailId());
		emailJobDetails.setJobName("Job Created On-"+LocalDateTime.now().toString());
		emailJobDetails.setDeltaMigration(emailWorkSpace.isDeltaMigration());
		emailJobDetails.setPreScan(emailWorkSpace.isPreScan());
		emailJobDetails.setCreatedTime(LocalDateTime.now());
		emailJobDetails.setWorkspaceId(new ArrayList<>());
		return emailJobDetails;
	}
	
	@PostMapping("/batches")
	public ResponseEntity<?>createBatchesForUser(@RequestAttribute("userId")String userId,@RequestBody List<MappedUsers> mappedUsers,@RequestParam("batch")String batchName){
		try {
			if(mappedUsers == null|| mappedUsers.isEmpty()) {
				return HttpUtils.Ok(HttpUtils.ERROR_REASONS.CAN_T_ABLETO_INITIATE.name());
			}
			int batchId = new Random().nextInt(1000);
			List<EmailBatches> emailBatches  = new ArrayList<>();
			mappedUsers.forEach(mappedUser->{
				EmailBatches batches = createBatches(mappedUser, batchName,batchId);
				emailBatches.add(batches);
			});
			if(!emailBatches.isEmpty()) {
				connectorService.getCloudsRepoImpl().saveBatches(emailBatches);
			}
		} catch (Exception e) {
			log.warn(ExceptionUtils.getStackTrace(e));
			return HttpUtils.BadRequest(ExceptionUtils.getStackTrace(e));
		}
		return HttpUtils.Ok(HttpUtils.SUCESS_REASONS.INITIATED.name());
	}
	
	
	
	
	private EmailBatches createBatches(MappedUsers mappedUsers,String batchName,int batchId) {
		EmailBatches emailBatches = new EmailBatches();
		emailBatches.setBatchName(batchName);
		if(ObjectUtils.isEmpty(mappedUsers)) {
			return null;
		}
		emailBatches.setFromAdminCloud(mappedUsers.getFromAdminCloud());
		emailBatches.setFromCloudId(mappedUsers.getFromCloudId());
		emailBatches.setToAdminCloud(mappedUsers.getToAdminCloud());
		emailBatches.setToCloudId(mappedUsers.getToCloudId());
		emailBatches.setFromMailId(mappedUsers.getFromMailId());
		emailBatches.setToMailId(mappedUsers.getToMailId());
		emailBatches.setUserId(mappedUsers.getUserId());
		emailBatches.setBatchId(batchId);
		return emailBatches;
	}
	
	
	private void createCalendarInfo(EmailWorkSpace emailWorkSpace) {
		CalenderInfo calenderInfo = new CalenderInfo();
		calenderInfo.setUserId(emailWorkSpace.getUserId());
		calenderInfo.setFromCloudId(emailWorkSpace.getFromCloudId());
		calenderInfo.setToCloudId(emailWorkSpace.getToCloudId());
		calenderInfo.setSourceId("/");
		calenderInfo.setDestFolderPath("/");
		calenderInfo.setEmailWorkSpaceId(emailWorkSpace.getId());
		calenderInfo.setJobId(emailWorkSpace.getJobId());
		calenderInfo.setMetadata(true);
		calenderInfo.setCalender(true);
		calenderInfo.setPreScan(emailWorkSpace.isPreScan());
		calenderInfo.setSubject("/");
		connectorService.getCalendarInfoRepoImpl().save(calenderInfo);
		
	}
	
	
	
	public void checkTheEventsForSendDeleteEvents(String userId) {
		List<EventsInfo> infos = connectorService.getCalendarInfoRepoImpl().getCalendarEvents(userId);
		Map<String,EmailWorkSpace> workSpaceDetails = new HashMap<>();
		if(!infos.isEmpty()) {
			for(EventsInfo info : infos) {
				if(info.getAttendees()==null || info.getAttendees().isEmpty()) {
					continue;
				}
				EmailWorkSpace emailWorkSpace = null;
				if(!workSpaceDetails.containsKey(info.getEmailWorkSpaceId())) {
					emailWorkSpace = connectorService.getWorkSpaceRepoImpl().findOne(info.getEmailWorkSpaceId());
					workSpaceDetails.put(info.getEmailWorkSpaceId(), emailWorkSpace);
				}else {
					emailWorkSpace = workSpaceDetails.get(info.getEmailWorkSpaceId());
				}
				new AttendeeEventDeleter(mailServiceFactory, connectorService, info.getAttendees(), info, emailWorkSpace).run();
			}
		}
	}
	
	private void createPickingQueue(EmailWorkSpace emailWorkSpace) {
		EmailPickingQueue emailQueue = new EmailPickingQueue();
		emailQueue.setUserId(emailWorkSpace.getUserId());
		emailQueue.setEmailWorkSpaceId(emailWorkSpace.getId());
		emailQueue.setJobId(emailWorkSpace.getJobId());
		emailQueue.setCreatedTime(LocalDateTime.now());
		emailQueue.setCloudName(emailWorkSpace.getToCloud());
		emailQueue.setContacts(emailWorkSpace.isContacts());
		emailQueue.setSettings(emailWorkSpace.isSettings());
		emailQueue.setMailRules(emailWorkSpace.isMailRules());
		connectorService.getEmailQueueRepoImpl().save(emailQueue); 
	}
	
	private void createCalendarPickingQueue(EmailWorkSpace emailWorkSpace) {
		CalendarPickingQueue emailQueue = new CalendarPickingQueue();
		emailQueue.setUserId(emailWorkSpace.getUserId());
		emailQueue.setEmailWorkSpaceId(emailWorkSpace.getId());
		emailQueue.setJobId(emailWorkSpace.getJobId());
		emailQueue.setCreatedTime(LocalDateTime.now());
		emailQueue.setCloudName(emailWorkSpace.getToCloud());
		connectorService.getEmailQueueRepoImpl().saveCalendarPickingQueue(emailQueue);
	}
	
	private void createPremigrationDetails(EmailWorkSpace emailWorkSpace) {
		PremigrationDetails details = new PremigrationDetails();
		details.setCloudId(emailWorkSpace.getFromCloudId());
		details.setCreatedTime(LocalDateTime.now());
		details.setModifiedTime(LocalDateTime.now());
		details.setProcessStatus(com.testing.mail.repo.entities.PROCESS.NOT_PROCESSED);
		details.setCloudName(emailWorkSpace.getFromCloud());
		details.setEmail(emailWorkSpace.getFromMailId());
		details.setTotalFolders(0);
		details.setTotalMails(0);
		details.setEmailWorkSpaceId(emailWorkSpace.getId());
		connectorService.getPremigrationRepoImpl().save(details);
	}
	
}
