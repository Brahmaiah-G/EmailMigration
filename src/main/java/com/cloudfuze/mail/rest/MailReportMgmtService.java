package com.cloudfuze.mail.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cloudfuze.mail.connectors.management.JobMigrationReportTask;
import com.cloudfuze.mail.connectors.management.MigrationReportTask;
import com.cloudfuze.mail.constants.Const;
import com.cloudfuze.mail.repo.entities.EmailInfo;
import com.cloudfuze.mail.repo.entities.EmailJobDetails;
import com.cloudfuze.mail.repo.entities.EmailJobDetails.REPORT_STATUS;
import com.cloudfuze.mail.repo.entities.EmailWorkSpace;
import com.cloudfuze.mail.repo.entities.EventsInfo;
import com.cloudfuze.mail.service.DBConnectorService;
import com.cloudfuze.mail.service.EmailService;
import com.cloudfuze.mail.utils.HttpUtils;
import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.extern.slf4j.Slf4j;

@Slf4j 
@RestController
@RequestMapping("/report")
public class MailReportMgmtService {

	@Autowired
	DBConnectorService connectorService;
	@Autowired
	EmailService emailService;
	
	@Autowired
	ThreadPoolTaskExecutor taskExecutor;
	
	@GetMapping("/status/{emailWorkSpaceId}")
	public ResponseEntity<?> checkReportStatus(@PathVariable("emailWorkSpaceId")String emailWorkSpaceId){
		log.warn("==Going for getting the Report for the WorkSpaceId=="+emailWorkSpaceId);
		EmailWorkSpace emailWorkSpace = connectorService.getWorkSpaceRepoImpl().findOne(emailWorkSpaceId);
		if(emailWorkSpace==null) {
			return HttpUtils.BadRequest("WorkSpace Not Found");
		}else if(EmailWorkSpace.REPORT_STATUS.IN_PROGRESS.equals(emailWorkSpace.getReportStatus())){
			return HttpUtils.Ok("Please Wait for the completion");
		}
		File file = checkReportIsAlreadyExists(emailWorkSpaceId);
		if(file!=null) {
			try {
				return HttpUtils.buildStreamOutResponse(new FileInputStream(file), "emailReport-"+emailWorkSpaceId+".csv");
			} catch (JsonProcessingException | FileNotFoundException e) {
				log.warn(ExceptionUtils.getStackTrace(e));
			}
		}else if(EmailWorkSpace.REPORT_STATUS.IN_PROGRESS.equals(emailWorkSpace.getReportStatus())){
			return HttpUtils.Ok("Please Wait for the completion");
		}else {
			emailWorkSpace.setReportStatus(com.cloudfuze.mail.repo.entities.EmailWorkSpace.REPORT_STATUS.IN_PROGRESS);
			connectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
			taskExecutor.submit(new MigrationReportTask(connectorService, emailWorkSpace, emailService));
		}
		return HttpUtils.Ok("Please Wait for the completion");
	}
	
	
	@GetMapping("/status/job/{jobId}")
	public ResponseEntity<?> checkReportStatusForJob(@PathVariable("jobId")String emailWorkSpaceId){
		log.warn("==Going for getting the Report for the WorkSpaceId=="+emailWorkSpaceId);
		EmailJobDetails details = connectorService.getEmailJobRepoImpl().findOne(emailWorkSpaceId);
		if(details==null) {
			return HttpUtils.BadRequest("Job Not Found");
		}
		File file = checkReportIsAlreadyExists(emailWorkSpaceId);
		if(file!=null) {
			try {
				return HttpUtils.buildStreamOutResponse(new FileInputStream(file), "emailReport-"+emailWorkSpaceId+".csv");
			} catch (JsonProcessingException | FileNotFoundException e) {
				log.warn(ExceptionUtils.getStackTrace(e));
			}
		}else if(REPORT_STATUS.IN_PROGRESS.equals(details.getReportStatus())){
			return HttpUtils.Ok("Please Wait for the completion");
		}else {
			details.setReportStatus(REPORT_STATUS.IN_PROGRESS);
			connectorService.getEmailJobRepoImpl().save(details);
			taskExecutor.submit(new JobMigrationReportTask(connectorService, details, emailService));
		}
		return HttpUtils.Ok("Please Wait for the completion");
	}
	
	private File checkReportIsAlreadyExists(String serverFileName) {

		//final String reportPrefix = moveWorkSpace.getFileName()+"CloudFuze_MigrateReport"+moveWorkSpaceId+"_";
		//final String reportPrefix = REPORT_PATH == null ? "CloudFuze_MigrateReport"+moveWorkSpaceId+"_" : moveWorkSpace.getFileName()+"CloudFuze_MigrateReport"+moveWorkSpaceId+"_";
		final String reportPrefix = serverFileName;
		String FOLDER_PATH = Const.REPORT_PATH;
		File folder = new File(FOLDER_PATH);
		//File folder = new File(REPORT_FOLDER_PATH);
		//File[] listOfFiles = folder.listFiles();


		File[] listOfFiles = folder.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				boolean matched = false;
				if(name.equals(reportPrefix) && name.endsWith(".csv")){
					matched = true;
				}
				return matched;
			}
		});

		long lastModifiedTime = 0;
		File reportFile = null;
		if (listOfFiles!=null) {
			for (int i = 0; i < listOfFiles.length; i++) {
				File eachFile = listOfFiles[i];
				//if (eachFile.isFile() && eachFile.getName().startsWith(reportPrefix)) {
				if(eachFile.lastModified() > lastModifiedTime){
					reportFile = eachFile;
					lastModifiedTime = eachFile.lastModified();
				}
				//} 
			}
		}

		return reportFile;
	}
	
	
	@GetMapping("/pre/{fromMail}")
	public ResponseEntity<?> getPremigartionForUser(@PathVariable("fromMail")String from,@RequestAttribute("userId")String userId,@RequestParam("cloud")String cloud){
		EmailWorkSpace emailWorkSpace = connectorService.getWorkSpaceRepoImpl().getPremigrationWorkSpace(from,cloud);
		List<Object> objects = new ArrayList<>();
		JSONObject body = new JSONObject();
		if(emailWorkSpace!=null) {
			try {
				body.put("status", emailWorkSpace.getProcessStatus());
				List<EmailInfo> infos = connectorService.getEmailInfoRepoImpl().findByWorkSpace(emailWorkSpace.getId(), 100, 0, true);
				objects.addAll(infos);
				List<EventsInfo> cInfo =connectorService.getCalendarInfoRepoImpl().findByWorkSpace(emailWorkSpace.getId(), 100, 0, true);
				objects.addAll(cInfo);
				body.put("items", objects);
			} catch (Exception e) {
				log.warn(ExceptionUtils.getStackTrace(e));
				return HttpUtils.BadRequest(ExceptionUtils.getStackTrace(e));
			}
		}
		return HttpUtils.Ok(body.toString());
	}
	
	
	//getPremigrationWorkSpace
	
	
	
	
}
