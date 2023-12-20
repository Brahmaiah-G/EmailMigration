package com.cloudfuze.mail.connectors.scheduler;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.cloudfuze.mail.connectors.management.JobMigrationReportTask;
import com.cloudfuze.mail.connectors.management.MigrationReportTask;
import com.cloudfuze.mail.repo.entities.CalendarMoveQueue;
import com.cloudfuze.mail.repo.entities.Clouds;
import com.cloudfuze.mail.repo.entities.Clouds.CLOUD_NAME;
import com.cloudfuze.mail.repo.entities.EmailFolderInfo;
import com.cloudfuze.mail.repo.entities.EmailInfo;
import com.cloudfuze.mail.repo.entities.EmailJobDetails;
import com.cloudfuze.mail.repo.entities.EmailMoveQueue;
import com.cloudfuze.mail.repo.entities.EmailPurgeQueue;
import com.cloudfuze.mail.repo.entities.EmailWorkSpace;
import com.cloudfuze.mail.repo.entities.EmailWorkSpace.REPORT_STATUS;
import com.cloudfuze.mail.repo.entities.PROCESS;
import com.cloudfuze.mail.repo.entities.PremigrationDetails;
import com.cloudfuze.mail.repo.entities.ThreadControl;
import com.cloudfuze.mail.repo.impl.EmailInfoRepoImpl;
import com.cloudfuze.mail.service.DBConnectorService;
import com.cloudfuze.mail.service.EmailService;
import com.cloudfuze.mail.utils.HttpUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
//@Component
public class WorkSpaceUpdater {


	@Autowired
	DBConnectorService dbConnectorService;
	@Autowired
	MongoTemplate mongoTemplate;
	@Autowired
	EmailService emailService;
	

	@Scheduled(cron ="0/10 * * * * ?")	
	public void processSchedule() {

		ThreadControl threadControl = dbConnectorService.getEmailInfoRepoImpl().getThreadControl();
		if(HttpUtils.checkController(threadControl) || threadControl.isStopUpdating()) {
			log.info("**********THREAD CONTROL IS DISABLED UPDATING WORKSPACES*********");
			return;
		}
		
		List<EmailWorkSpace> workspaces =  dbConnectorService.getWorkSpaceRepoImpl().getWorkspaces(threadControl.getWorkSpaceUpdates(), 0);
		log.info("====TOTAL WORKSPACES FOUND FOR UPDATION======"+workspaces.size());
		if(!workspaces.isEmpty()) {
			log.info("==Total workspace for updation===="+workspaces.size());
			workspaces.forEach(workspace->updateWorkSpaceStatus(workspace,threadControl));
		}

	}
	
	private void updateWorkSpaceStatus(EmailWorkSpace emailWorkSpace,ThreadControl threadControl) {
		log.info("===ENTERED FOR UPDATING THE WORKSPACE===="+emailWorkSpace.getId());
		EmailMoveQueue moveQueue = dbConnectorService.getEmailQueueRepoImpl().findMoveQueueByWorkSpace(emailWorkSpace.getId());

		updateCoflicts(emailWorkSpace, threadControl,dbConnectorService.getEmailInfoRepoImpl());
		EmailWorkSpace workSpace = dbConnectorService.getEmailInfoRepoImpl().getAggregartedResult(emailWorkSpace.getId());
		EmailWorkSpace calendarWorkSpace = null;
		if(emailWorkSpace.isCalendar()) {
			calendarWorkSpace = dbConnectorService.getCalendarInfoRepoImpl().getAggregartedResult(emailWorkSpace.getId());
			if(calendarWorkSpace!=null) {
				calendarWorkSpace.setId(emailWorkSpace.getId());
				calendarWorkSpace.setProcessStatus(EmailWorkSpace.PROCESS.IN_PROGRESS);
				updatePremigrtionBasedOnWorkSpace(calendarWorkSpace);
			}
		}
		if(workSpace!=null) {
			try {
				long count = dbConnectorService.getEmailFolderInfoRepoImpl().getInprogressFolders(emailWorkSpace.getId());
				emailWorkSpace.setInprogressCount(workSpace.getInprogressCount());
				emailWorkSpace.setNotProcessedCount(workSpace.getNotProcessedCount());
				emailWorkSpace.setProcessedCount(workSpace.getProcessedCount());
				emailWorkSpace.setConflictCount(workSpace.getConflictCount());
				emailWorkSpace.setPauseCount(workSpace.getPauseCount());
				emailWorkSpace.setRetryCount(workSpace.getRetryCount());
				emailWorkSpace.setTotalCount(workSpace.getTotalCount());
				emailWorkSpace.setTotalAttachmentsSize(workSpace.getTotalAttachmentsSize());
				emailWorkSpace.setModifiedTime(LocalDateTime.now());
				if(calendarWorkSpace!=null) {
					emailWorkSpace.setInprogressCount(emailWorkSpace.getInprogressCount()+calendarWorkSpace.getInprogressCount());
					emailWorkSpace.setNotProcessedCount(emailWorkSpace.getNotProcessedCount()+calendarWorkSpace.getNotProcessedCount());
					emailWorkSpace.setProcessedCount(emailWorkSpace.getProcessedCount()+calendarWorkSpace.getProcessedCount());
					emailWorkSpace.setConflictCount(emailWorkSpace.getConflictCount()+calendarWorkSpace.getConflictCount());
					emailWorkSpace.setPauseCount(emailWorkSpace.getPauseCount()+calendarWorkSpace.getPauseCount());
					emailWorkSpace.setRetryCount(emailWorkSpace.getRetryCount()+calendarWorkSpace.getRetryCount());
					emailWorkSpace.setTotalCount(emailWorkSpace.getTotalCount()+calendarWorkSpace.getTotalCount());
					emailWorkSpace.setTotalAttachmentsSize(emailWorkSpace.getTotalAttachmentsSize()+calendarWorkSpace.getTotalAttachmentsSize());
				}
				if(emailWorkSpace.isDeltaMigration()) {
					count = count+dbConnectorService.getMailChangeIdRepoImpl().getNot_StartedCount(emailWorkSpace.getId());
				}
				if(count<=0) {
					emailWorkSpace.setModifiedTime(LocalDateTime.now());
					if(moveQueue!=null && moveQueue.getProcessStatus().equals(PROCESS.IN_PROGRESS) && emailWorkSpace.getNotProcessedCount()<=0 && emailWorkSpace.getInprogressCount()<=0 && emailWorkSpace.getRetryCount()<=0 ) {
						if(emailWorkSpace.getTotalCount() == emailWorkSpace.getProcessedCount()) {
							emailWorkSpace.setProcessStatus(EmailWorkSpace.PROCESS.PROCESSED);
						}else if(emailWorkSpace.getConflictCount()>0 && emailWorkSpace.getProcessedCount()>0 && emailWorkSpace.getTotalCount() == (emailWorkSpace.getConflictCount()+emailWorkSpace.getProcessedCount())) {
							emailWorkSpace.setProcessStatus(EmailWorkSpace.PROCESS.PROCESSED_WITH_CONFLICTS);
						}else if(emailWorkSpace.getPauseCount()>0 && emailWorkSpace.getTotalCount() == (emailWorkSpace.getConflictCount()+emailWorkSpace.getProcessedCount()+emailWorkSpace.getPauseCount())) {
							emailWorkSpace.setProcessStatus(EmailWorkSpace.PROCESS.PROCESSED_WITH_CONFLICT_AND_PAUSE);
						}else if(emailWorkSpace.getConflictCount()>0 && emailWorkSpace.getTotalCount() == emailWorkSpace.getConflictCount()){
							emailWorkSpace.setProcessStatus(EmailWorkSpace.PROCESS.CONFLICT);
						}
						if(Arrays.asList(EmailWorkSpace.PROCESS.PROCESSED,EmailWorkSpace.PROCESS.CONFLICT).contains(emailWorkSpace.getProcessStatus()) && CLOUD_NAME.OUTLOOK.equals(emailWorkSpace.getToCloud())){
							createPurgeQueue(emailWorkSpace);
						}
					}
					updateMoveQueue(emailWorkSpace.getId(), PROCESS.valueOf(emailWorkSpace.getProcessStatus().name()), emailWorkSpace.getErrorDescription());
					if(emailWorkSpace.isCalendar()) {
						updateCalendarMoveQueue(emailWorkSpace.getId(), PROCESS.valueOf(emailWorkSpace.getProcessStatus().name()), emailWorkSpace.getErrorDescription());	
					}
					if(emailWorkSpace.isPreScan()) {
						updatePremigrtionBasedOnWorkSpace(emailWorkSpace);
						try {
							Clouds cloud=dbConnectorService.getCloudsRepoImpl().findOne(emailWorkSpace.getFromCloudId());
							cloud.setPreMigrationStatus(emailWorkSpace.getProcessStatus().name());
							dbConnectorService.getCloudsRepoImpl().save(cloud);
							emailWorkSpace.setReportStatus(REPORT_STATUS.PROCESSED);
						} catch (Exception e) {
							log.error(ExceptionUtils.getStackTrace(e));
						}
					}
				}
				if((emailWorkSpace.getProcessStatus()==EmailWorkSpace.PROCESS.PROCESSED_WITH_CONFLICTS || emailWorkSpace.getProcessStatus()==EmailWorkSpace.PROCESS.PROCESSED || emailWorkSpace.getProcessStatus()==EmailWorkSpace.PROCESS.PROCESSED_WITH_CONFLICT_AND_PAUSE || 
						emailWorkSpace.getProcessStatus()==EmailWorkSpace.PROCESS.CONFLICT )&& emailWorkSpace.getReportStatus()==REPORT_STATUS.NOT_PROCESSED) {
					log.info("====GENERATING THE MIGRATION REPORT= FOR THE WORKSPACE===="+emailWorkSpace.getId());
					emailWorkSpace.setReportStatus(REPORT_STATUS.IN_PROGRESS);
					updateMailFolderCount(emailWorkSpace);
					dbConnectorService.getMailChangeIdRepoImpl().findAndUpdateMailChangeId(emailWorkSpace.getLastEmailWorkSpaceId(), emailWorkSpace.getId());
					dbConnectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
					try {
						new MigrationReportTask(dbConnectorService, emailWorkSpace, emailService).call();
					} catch (Exception e) {
					}
				}
				updateJob(emailWorkSpace);
			}catch(Exception e) {
				log.warn(ExceptionUtils.getStackTrace(e));
			}
			finally {
				log.info("====LEAVING===PROCESS STATUS : "+emailWorkSpace.getProcessStatus()+"=====workSpaceID====="+emailWorkSpace.getId());
				if(emailWorkSpace.getPriority()<=1) {
					emailWorkSpace.setPriority(10);
				}else {
					emailWorkSpace.setPriority(emailWorkSpace.getPriority()-1);
				}
				dbConnectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
			}
		}
	}

	private void updateCoflicts(EmailWorkSpace emailWorkSpace,ThreadControl threadControl,EmailInfoRepoImpl emailInfoRepoImpl) {
		log.warn("===Entered for updating the Workspace CONFLICTS ===="+emailWorkSpace.getId());
		if(emailWorkSpace.getToCloud().equals(CLOUD_NAME.OUTLOOK)) {
			emailInfoRepoImpl.findAndUpdateConflictsByWorkSpaceOutlookDraftCreation(emailWorkSpace.getId(), threadControl.getRetryCount());
			emailInfoRepoImpl.findAndUpdateConflictsByWorkSpaceOutlookDraftMigration(emailWorkSpace.getId(), threadControl.getRetryCount());
			dbConnectorService.getCalendarInfoRepoImpl().findAndUpdateConflictsByWorkSpace(emailWorkSpace.getId(), threadControl.getRetryCount());
			dbConnectorService.getCalendarInfoRepoImpl().findAndUpdateEventInstanceConflictsByWorkSpace(emailWorkSpace.getId(), threadControl.getRetryCount());
			dbConnectorService.getCalendarInfoRepoImpl().findAndUpdateMetadataConflictsByWorkSpace(emailWorkSpace.getId(), threadControl.getRetryCount());
		}else {
			emailInfoRepoImpl.findAndUpdateConflictsByWorkSpace(emailWorkSpace.getId(), threadControl.getRetryCount());
		}
		emailInfoRepoImpl.findAndUpdateMetadataConflictsByWorkSpace(emailWorkSpace.getId(), threadControl.getRetryCount());
		updateConflictFolder(emailWorkSpace.getId(), threadControl);
	}
	
	private void updateConflictFolder(String moveWorkSpaceId,ThreadControl threadControl) {
		log.warn("===Entered for updating the Workspace CONFLICTS FOLDERS ===="+moveWorkSpaceId);
		List<EmailFolderInfo> emailFolders = dbConnectorService.getEmailFolderInfoRepoImpl().findConflictFolders(moveWorkSpaceId, threadControl.getRetryCount());
		log.warn("===Entered for updating the Workspace CONFLICTS FOLDERS ===="+moveWorkSpaceId+"==Folders Found===:"+emailFolders.size());
		if(ObjectUtils.isNotEmpty(emailFolders) && !emailFolders.isEmpty()) {
			emailFolders.stream().forEach(folder->{
				folder.setProcessStatus(PROCESS.NOT_STARTED);
				folder.setThreadBy(null);
				folder.setErrorDescription(null);
				dbConnectorService.getEmailFolderInfoRepoImpl().save(folder);
			});
		}
	}
	
	
	//findConflictFolders
	
	private void updateJob(EmailWorkSpace emailWorkSpace) {
		EmailJobDetails emailJobDetails = dbConnectorService.getEmailJobRepoImpl().findOne(emailWorkSpace.getJobId());
		if(emailJobDetails!=null) {
			emailJobDetails.setConflictCount(emailWorkSpace.getConflictCount());
			emailJobDetails.setNotProcessedCount(emailWorkSpace.getNotProcessedCount());
			emailJobDetails.setInprogressCount(emailWorkSpace.getInprogressCount());
			emailJobDetails.setProcessedCount(emailWorkSpace.getProcessedCount());
			emailJobDetails.setTotalCount(emailWorkSpace.getTotalCount());
			if(emailWorkSpace.getProcessStatus()==EmailWorkSpace.PROCESS.PROCESSED_WITH_CONFLICTS || emailWorkSpace.getProcessStatus()==EmailWorkSpace.PROCESS.PROCESSED || emailWorkSpace.getProcessStatus()==EmailWorkSpace.PROCESS.PROCESSED_WITH_CONFLICT_AND_PAUSE || 
					emailWorkSpace.getProcessStatus()==EmailWorkSpace.PROCESS.CONFLICT) {
				emailJobDetails.setProcessStatus(EmailJobDetails.PROCESS.valueOf(emailWorkSpace.getProcessStatus().name()));
				if(emailJobDetails.getReportStatus().equals(EmailJobDetails.REPORT_STATUS.NOT_PROCESSED) && validateWorkspaceCompletion(emailJobDetails)) {
					try {
						emailJobDetails.setReportStatus(EmailJobDetails.REPORT_STATUS.PROCESSED);
						dbConnectorService.getEmailJobRepoImpl().save(emailJobDetails);
						new JobMigrationReportTask(dbConnectorService, emailJobDetails, emailService).call();
					} catch (Exception e) {
						emailJobDetails.setErrorDescription(ExceptionUtils.getStackTrace(e));
						emailJobDetails.setReportStatus(EmailJobDetails.REPORT_STATUS.PROCESSED);
					}
				}
			}else {
				emailJobDetails.setProcessStatus(EmailJobDetails.PROCESS.IN_PROGRESS);
			}
			emailJobDetails.setProcessStatus(EmailJobDetails.PROCESS.valueOf(emailWorkSpace.getProcessStatus().name()));
			emailJobDetails.setModifiedTime(LocalDateTime.now());
			dbConnectorService.getEmailJobRepoImpl().save(emailJobDetails);
		}
	}

	/**
	 * For Validating the EmailWorkspaces completed in Job
	*/
	private boolean validateWorkspaceCompletion(EmailJobDetails details) {
		if(details==null) {
			return false;
		}
		boolean allCmp = false;
		long totalCount = 0;
		long processedCount = 0;
		for(String wks : details.getWorkspaceId()) {
			EmailWorkSpace emailWorkSpace = dbConnectorService.getWorkSpaceRepoImpl().findOne(wks);
			if(emailWorkSpace!=null && (emailWorkSpace.getProcessStatus()==EmailWorkSpace.PROCESS.PROCESSED_WITH_CONFLICTS || emailWorkSpace.getProcessStatus()==EmailWorkSpace.PROCESS.PROCESSED || emailWorkSpace.getProcessStatus()==EmailWorkSpace.PROCESS.PROCESSED_WITH_CONFLICT_AND_PAUSE || 
					emailWorkSpace.getProcessStatus()==EmailWorkSpace.PROCESS.CONFLICT)){
				allCmp = true;
			}else {
				allCmp = false;
			}
			totalCount =totalCount+emailWorkSpace.getTotalCount();
			processedCount = processedCount+emailWorkSpace.getProcessedCount();
		}
		details.setTotalCount(totalCount);
		details.setProcessedCount(processedCount);
		return allCmp;
	}

	private void updateMailFolderCount(EmailWorkSpace emailWorkSpace) {
		List<EmailFolderInfo> folders = dbConnectorService.getEmailFolderInfoRepoImpl().findByWorkSpaceId(emailWorkSpace.getId());
		if(!folders.isEmpty()) {
			folders.forEach(emailInfo->{
				long folderSpace = dbConnectorService.getEmailInfoRepoImpl().getlistOfMails(emailInfo.getEmailWorkSpaceId(), emailInfo.getMailFolder());
				emailInfo.setTotalCount(folderSpace);
				dbConnectorService.getEmailFolderInfoRepoImpl().save(emailInfo);
			});
		}
	}
	
	private void updateMoveQueue(String emailWorkSpaceId,PROCESS processStatus,String errorDescription) {
		EmailMoveQueue moveQueue = 	dbConnectorService.getEmailQueueRepoImpl().findMoveQueueByWorkSpace(emailWorkSpaceId);
		if(moveQueue!=null) {
			moveQueue.setProcessStatus(processStatus);
			moveQueue.setErrorDescription(errorDescription);
			dbConnectorService.getEmailQueueRepoImpl().saveQueue(moveQueue);
		}
	}
	
	private void updateCalendarMoveQueue(String emailWorkSpaceId,PROCESS processStatus,String errorDescription) {
		CalendarMoveQueue moveQueue = 	dbConnectorService.getEmailQueueRepoImpl().findCalendarMoveQueueByWorkSpace(emailWorkSpaceId);
		if(moveQueue!=null) {
			moveQueue.setProcessStatus(processStatus);
			moveQueue.setErrorDescription(errorDescription);
			dbConnectorService.getEmailQueueRepoImpl().saveCalendarQueue(moveQueue);
		}
	}
	
	private void createPurgeQueue(EmailWorkSpace pickingQueue) {
		EmailPurgeQueue emailQueue = new EmailPurgeQueue();
		emailQueue.setUserId(pickingQueue.getUserId());
		emailQueue.setEmailWorkSpaceId(pickingQueue.getId());
		emailQueue.setJobId(pickingQueue.getJobId());
		emailQueue.setCreatedTime(LocalDateTime.now());
		emailQueue.setCloudName(pickingQueue.getToCloud());
		dbConnectorService.getEmailQueueRepoImpl().savePurgeQueue(emailQueue); 
	}
	
	private void updatePremigrtionBasedOnWorkSpace(EmailWorkSpace emailWorkSpace) {
		PremigrationDetails premigrationDetails = dbConnectorService.getPremigrationRepoImpl().findByWorkSpace(emailWorkSpace.getId());
		if(null!=premigrationDetails) {
			premigrationDetails.setModifiedTime(LocalDateTime.now());
			premigrationDetails.setTotalMails(emailWorkSpace.getTotalCount());
			premigrationDetails.setTotalSize(premigrationDetails.getTotalSize()+emailWorkSpace.getTotalAttachmentsSize());
			premigrationDetails.setTotalCalendarInvites(emailWorkSpace.getTotalCount());
			premigrationDetails.setProcessStatus(PROCESS.valueOf(emailWorkSpace.getProcessStatus().name()));
		}
	}
	
	
}
