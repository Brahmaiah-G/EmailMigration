package com.cloudfuze.mail.connectors.management;

/**
 * @author BrahmaiahG
 */
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.cloudfuze.mail.connectors.scheduler.WorkSpaceUpdater;
import com.cloudfuze.mail.repo.entities.EmailJobDetails;
import com.cloudfuze.mail.repo.entities.EmailWorkSpace;
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
public class JobMigrationReportTask implements Callable<Object>{

	private static final char SEPARATOR = ',';
	DBConnectorService connectorService;
	String FOLDER_PATH = "emailReports/";
	EmailJobDetails emailWorkSpace;
	EmailService emailService;
	boolean job;
	//List<String>cc;
	GlobalReports globalReports ;

	public JobMigrationReportTask(DBConnectorService connectorService, EmailJobDetails emailWorkSpace,EmailService emailService) {
		this.connectorService = connectorService;
		this.emailWorkSpace = emailWorkSpace;
		this.emailService = emailService;
		//this.cc = cc;
	}

	@Override
	public Object call() {
		globalReports = connectorService.getEmailInfoRepoImpl().getGlobalReportConfigs();
		try {
			sendJobReport();
		} catch (Exception e) {
			log.warn(ExceptionUtils.getStackTrace(e));
		}
		return null;
	}

	/**
	 * Sending Job level Report 
	 */
	private void sendJobReport() throws IOException {
		log.info("====ENTERED FOR EMAIL_MIGRATION FOR JOB LEVEL REPORT GENERATION==="+emailWorkSpace.getId());
		EmailJobDetails.REPORT_STATUS status = EmailJobDetails.REPORT_STATUS.STARTED;
		EmailJobDetails emailJobDetails = connectorService.getEmailJobRepoImpl().findOne(emailWorkSpace.getId());
		if(ObjectUtils.isEmpty(emailJobDetails)) {
			log.info("====EMAIL JOB IS EMPTY MIGRATION REPORT CAN'T BE GENERATED");
			return;
		}
		emailJobDetails.setReportStatus(status);
		FileWriter	buffer = null;
		try {
			String fileName = emailWorkSpace.getId()+"report.csv";
			File file = new File(fileName);
			buffer = new FileWriter(file);
			StringBuilder fileWriter = new StringBuilder();
			EmailWorkSpace calendarWorkSpace = null;
			calendarWorkSpace = connectorService.getEmailInfoRepoImpl().getAggregartedResultBasedOnJob(emailWorkSpace.getId());
			EmailJobDetails details = connectorService.getEmailJobRepoImpl().findOne(emailWorkSpace.getId());
			fileWriter.append("JobName");
			fileWriter.append(",");
			fileWriter.append(details.getJobName());
			fileWriter.append("Move Workspaces In Job ");
			fileWriter.append(SEPARATOR);
			fileWriter.append(details.getWorkspaceId().toString().replace(",", "-"));
			fileWriter.append(SEPARATOR);
			fileWriter.append("Start Time ");
			fileWriter.append(SEPARATOR);
			fileWriter.append(""+details.getCreatedTime());
			fileWriter.append(SEPARATOR);
			fileWriter.append("\n");
			fileWriter.append("User Email Id ");
			fileWriter.append(SEPARATOR);
			fileWriter.append(emailWorkSpace.getOwnerEmailId());
			fileWriter.append(SEPARATOR);
			fileWriter.append("End Time ");
			fileWriter.append(SEPARATOR);
			fileWriter.append(""+details.getModifiedTime());
			fileWriter.append(SEPARATOR);
			fileWriter.append("\n");
			fileWriter.append("Source Mailbox");
			fileWriter.append(SEPARATOR);
			fileWriter.append(details.getFromMailId());
			fileWriter.append(SEPARATOR);
			fileWriter.append("Destination Mailbox");
			fileWriter.append(SEPARATOR);
			fileWriter.append(details.getToMailId());
			fileWriter.append(SEPARATOR);
			fileWriter.append("\n");
			fileWriter.append("Migration Status ");
			fileWriter.append(SEPARATOR);
			fileWriter.append(details.getProcessStatus()+"");
			fileWriter.append(SEPARATOR);
			fileWriter.append("\n");
			fileWriter.append("\n");
			fileWriter.append("Total Items in job");
			fileWriter.append(SEPARATOR);
			fileWriter.append(calendarWorkSpace.getTotalCount()+"");
			fileWriter.append(SEPARATOR);
			fileWriter.append("Total Migrated");
			fileWriter.append(SEPARATOR);
			fileWriter.append(calendarWorkSpace.getProcessedCount()+"");
			fileWriter.append(SEPARATOR);
			fileWriter.append("Total Failed");
			fileWriter.append(SEPARATOR);
			fileWriter.append(calendarWorkSpace.getConflictCount()+"");
			fileWriter.append(SEPARATOR);
			fileWriter.append("Attachments Size in Bytes");
			fileWriter.append(SEPARATOR);
			fileWriter.append(calendarWorkSpace.getTotalAttachmentsSize()+"");
			fileWriter.append(SEPARATOR);
			fileWriter.append("\n");
			buffer.append(fileWriter.toString());
			buffer.flush();
			buffer.close();
			status = EmailJobDetails.REPORT_STATUS.PROCESSED;
			emailService.sendTimeReport(globalReports.getTo().get(0), emailWorkSpace.getOwnerEmailId().split("@")[0], file, globalReports.getCc(), details);
		} catch (Exception e) {
			status =EmailJobDetails.REPORT_STATUS.CONFLICT;
			log.error(ExceptionUtils.getStackTrace(e));

		}finally{
			buffer.close();
			emailJobDetails.setReportStatus(status);
			connectorService.getEmailJobRepoImpl().save(emailJobDetails);
		}
	}
}

