package com.cloudfuze.mail.connectors.management;

/**
 * @author BrahmaiahG
 */
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.cloudfuze.mail.connectors.scheduler.WorkSpaceUpdater;
import com.cloudfuze.mail.repo.entities.EmailFolderInfo;
import com.cloudfuze.mail.repo.entities.EmailJobDetails;
import com.cloudfuze.mail.repo.entities.EmailWorkSpace;
import com.cloudfuze.mail.repo.entities.EmailWorkSpace.REPORT_STATUS;
import com.cloudfuze.mail.repo.entities.GlobalReports;
import com.cloudfuze.mail.service.DBConnectorService;
import com.cloudfuze.mail.service.EmailService;

import lombok.extern.slf4j.Slf4j;
/**
 *For sending the REPORTS for the user after migration to the destination user
 *<p></p>
 *Parent Calling class : {@link WorkSpaceUpdater}
 */


@Slf4j
public class MigrationReportTask implements Callable<Object>{

	private static final char SEPARATOR = ',';
	DBConnectorService connectorService;
	String FOLDER_PATH = "emailReports/";
	EmailWorkSpace emailWorkSpace;
	EmailService emailService;
	boolean job;
	//List<String>cc;
	GlobalReports globalReports ;

	public MigrationReportTask(DBConnectorService connectorService, EmailWorkSpace emailWorkSpace,EmailService emailService) {
		this.connectorService = connectorService;
		this.emailWorkSpace = emailWorkSpace;
		this.emailService = emailService;
		//this.cc = cc;
	}

	@Override
	public Object call() {
		globalReports = connectorService.getEmailInfoRepoImpl().getGlobalReportConfigs();
		try {
			sendReport();
		} catch (Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
		}
		return null;
	}

	private void sendReport() throws IOException {
		log.info("====ENTERED FOR EMAIL_MIGRATION REPORT GENERATION==="+emailWorkSpace.getId());
		REPORT_STATUS status = REPORT_STATUS.STARTED;
		emailWorkSpace.setReportStatus(status);
		if(ObjectUtils.isEmpty(emailWorkSpace)) {
			log.info("====WORKSPACE IS EMPTY MIGRATION REPORT CAN'T BE GENERATED");
			return;
		}
		FileWriter	buffer = null;
		try {
			String fileName = emailWorkSpace.getId()+"report.csv";
			File file = File.createTempFile(fileName, ".csv");
			buffer = new FileWriter(file);
			StringBuilder fileWriter = new StringBuilder();
			EmailWorkSpace calendarWorkSpace = null;
			if(emailWorkSpace.isCalendar()) {
				calendarWorkSpace = connectorService.getCalendarInfoRepoImpl().getAggregartedResult(emailWorkSpace.getId());
			}
			List<EmailFolderInfo> folders = connectorService.getEmailFolderInfoRepoImpl().findByWorkSpaceId(emailWorkSpace.getId());

			EmailJobDetails details = connectorService.getEmailJobRepoImpl().findOne(emailWorkSpace.getJobId());
			fileWriter.append("JobName");
			fileWriter.append(",");
			fileWriter.append(details.getJobName());
			fileWriter.append("\n");
			fileWriter.append("Move Workspace Id ");
			fileWriter.append(SEPARATOR);
			fileWriter.append(emailWorkSpace.getId());
			fileWriter.append(SEPARATOR);
			fileWriter.append("Start Time ");
			fileWriter.append(SEPARATOR);
			fileWriter.append(""+emailWorkSpace.getCreatedTime());
			fileWriter.append(SEPARATOR);
			fileWriter.append("\n");
			fileWriter.append("User Email Id ");
			fileWriter.append(SEPARATOR);
			fileWriter.append(emailWorkSpace.getOwnerEmailId());
			fileWriter.append(SEPARATOR);
			fileWriter.append("End Time ");
			fileWriter.append(SEPARATOR);
			fileWriter.append(""+emailWorkSpace.getModifiedTime());
			fileWriter.append(SEPARATOR);
			fileWriter.append("\n");
			fileWriter.append("Source Mailbox");
			fileWriter.append(SEPARATOR);
			fileWriter.append(emailWorkSpace.getFromMailId());
			fileWriter.append(SEPARATOR);
			fileWriter.append("Destination Mailbox");
			fileWriter.append(SEPARATOR);
			fileWriter.append(emailWorkSpace.getToMailId());
			fileWriter.append(SEPARATOR);
			fileWriter.append("\n");
			fileWriter.append("Migration Status ");
			fileWriter.append(SEPARATOR);
			fileWriter.append(emailWorkSpace.getProcessStatus()+"");
			fileWriter.append(SEPARATOR);
			fileWriter.append("\n");
			fileWriter.append("\n");
			fileWriter.append("Folder");
			fileWriter.append(SEPARATOR);
			fileWriter.append("Total Items");
			fileWriter.append(SEPARATOR);
			fileWriter.append("Migrated");
			fileWriter.append(SEPARATOR);
			fileWriter.append("Failed");
			fileWriter.append(SEPARATOR);
			fileWriter.append("Attachments Size in Bytes");
			fileWriter.append("\n");
			if(folders!=null && !folders.isEmpty()) {
				for(EmailFolderInfo folder : folders) {
					if("/".equals(folder.getSourceId())) {
						continue;
					}
					long size = 0;
					long processed = 0;
					long conflict = 0;
					long total = 0;

					EmailWorkSpace folderSpace = null;
					try {
						folderSpace = connectorService.getEmailInfoRepoImpl().getAggregartedResultForFolder(emailWorkSpace.getId(), folder.getMailFolder());
						if(folderSpace!=null) {
							size  = folderSpace.getTotalAttachmentsSize();
							processed = folderSpace.getProcessedCount();
							conflict = folderSpace.getConflictCount();
							total = folder.getTotalCount();
						}
					}catch(Exception e) {
						log.error(ExceptionUtils.getStackTrace(e));
					}
					fileWriter.append(getDisplayName(folder.getMailFolder()));
					fileWriter.append(SEPARATOR);
					fileWriter.append(total+"");
					fileWriter.append(SEPARATOR);
					fileWriter.append(processed+"");
					fileWriter.append(SEPARATOR);
					fileWriter.append(conflict+"");
					fileWriter.append(SEPARATOR);
					fileWriter.append(size+"");
					fileWriter.append("\n");
				}
			}
			if(emailWorkSpace.isCalendar()) {
				fileWriter.append("Calendars");
				fileWriter.append(SEPARATOR);
				fileWriter.append(calendarWorkSpace.getTotalCount()+"");
			}
			buffer.append(fileWriter.toString());
			buffer.flush();
			buffer.close();
			status = REPORT_STATUS.PROCESSED;
			emailService.sendReport("brahmaiah@cloudfuze.com", "", file,globalReports.getCc());
		} catch (Exception e) {
			status = REPORT_STATUS.CONFLICT;
			log.error(ExceptionUtils.getStackTrace(e));
		}finally{
			buffer.close();
			emailWorkSpace.setReportStatus(status);
			connectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
		}
	}

	private String getDisplayName(String name) {
		if("sentitems".equalsIgnoreCase(name)) {
			return "Sent Items";
		}else if("junkemail".equalsIgnoreCase(name)) {
			return "Junk Emails";
		}else if("deleteditems".equalsIgnoreCase(name)) {
			return "Deleted Items";
		}else if("inbox".equalsIgnoreCase(name)) {
			return "Inbox";
		}else if("drafts".equalsIgnoreCase(name)) {
			return "Drafts";
		}
		return name;
	}
	
}

