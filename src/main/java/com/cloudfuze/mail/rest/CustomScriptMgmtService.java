package com.cloudfuze.mail.rest;

import java.util.List;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cloudfuze.mail.connectors.factory.MailServiceFactory;
import com.cloudfuze.mail.connectors.management.EmailDestroyer;
import com.cloudfuze.mail.connectors.management.MailCreationTask;
import com.cloudfuze.mail.repo.entities.Clouds;
import com.cloudfuze.mail.service.DBConnectorService;
import com.cloudfuze.mail.utils.HttpUtils;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(path = "/custom")
@Slf4j
public class CustomScriptMgmtService {

	@Autowired
	DBConnectorService connectorService;

	@Autowired
	MailServiceFactory mailServiceFactory;

	@Autowired
	ThreadPoolTaskExecutor taskExecutor;

	@PostMapping("/mails/create/{cloudId}")
	public ResponseEntity<?> createMailsInCloud(@RequestAttribute("userId")String userId,@PathVariable("cloudId")String cloudId,@RequestParam("count")long count,
			@RequestParam(value="text",defaultValue="random")String text,@RequestParam(value="mailFolder",defaultValue="INBOX")String mailFolder,
			@RequestParam(value="event",defaultValue="false")boolean event, @RequestParam("files") List<MultipartFile> files){
		log.info("==Large creation of test mails for=="+cloudId+"== with the count=="+count);
		Clouds cloud = connectorService.getCloudsRepoImpl().findOne(cloudId);
		if(ObjectUtils.isNotEmpty(cloud)) {
//			taskExecutor.submit(new MailCreationTask(connectorService, mailServiceFactory, cloud, count, text,
//					mailFolder, event,files)) ;
			new MailCreationTask(connectorService, mailServiceFactory, cloud, count, text,
					mailFolder, event,files).run();
		}else {
			return HttpUtils.BadRequest(HttpUtils.ERROR_REASONS.CLOUDS_NOT_AVAILABLE.name());
		}
		return HttpUtils.Ok(HttpUtils.SUCESS_REASONS.INITIATED.name());
	}



	@PostMapping("/mails/delete")
	public ResponseEntity<?> deleteMailsBasedOnWorkSpceId(@RequestAttribute("userId")String userId,List<String>workSpaceIds,
			@RequestParam(value="event",defaultValue="false")boolean events){
		log.info("==Deletion of test mails for=="+workSpaceIds);

		if(!workSpaceIds.isEmpty()) {
			taskExecutor.submit(new EmailDestroyer(mailServiceFactory, workSpaceIds, connectorService,events));
		}else {
			return HttpUtils.BadRequest(HttpUtils.ERROR_REASONS.CAN_T_ABLETO_INITIATE.name());
		}
		return HttpUtils.Ok(HttpUtils.SUCESS_REASONS.INITIATED.name());
	}


}
