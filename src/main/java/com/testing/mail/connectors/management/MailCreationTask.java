package com.testing.mail.connectors.management;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.springframework.web.multipart.MultipartFile;

import com.testing.mail.connectors.factory.MailServiceFactory;
import com.testing.mail.connectors.microsoft.data.AttachmentsData;
import com.testing.mail.dao.entities.CalenderFlags;
import com.testing.mail.dao.entities.EmailFlagsInfo;
import com.testing.mail.repo.entities.CalenderInfo;
import com.testing.mail.repo.entities.Clouds;
import com.testing.mail.repo.entities.EmailInfo;
import com.testing.mail.service.DBConnectorService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MailCreationTask implements Runnable {


	DBConnectorService connectorService;
	MailServiceFactory mailServiceFactory;
	Clouds cloud;
	long count;
	String message;
	String mailFolder;
	boolean events;
	List<MultipartFile> attachments = new ArrayList<>();




	public MailCreationTask(DBConnectorService connectorService, MailServiceFactory mailServiceFactory, Clouds cloud,
			long count, String message,String mailFolder,boolean events,List<MultipartFile> attachments) {
		this.connectorService = connectorService;
		this.mailServiceFactory = mailServiceFactory;
		this.cloud = cloud;
		this.count = count;
		this.message = message;
		this.mailFolder = mailFolder;
		this.events = events;
		this.attachments = attachments;
	}





	@Override
	public void run() {
		if(events) {
			createEvents();
		}else {
			createMails();
		}
	}

	private void createMails() {
		for(int i=0;i<count;i++) {
			List<AttachmentsData> attachmentsDatas = new ArrayList<>();
			EmailFlagsInfo emailFlagsInfo = new EmailFlagsInfo();
			if(attachments!=null && !attachments.isEmpty()) {
				emailFlagsInfo.setHadAttachments(true);
				Base64 x = new Base64();
				for(MultipartFile inputStream : attachments) {
					try {
						AttachmentsData data = new AttachmentsData();
						data.setName(inputStream.getName());
						data.setSize(inputStream.getSize());
						data.setContentType(inputStream.getContentType());
						data.setContentBytes(x.encodeAsString(inputStream.getBytes()));
						data.setCompleted(false);
						attachmentsDatas.add(data);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			emailFlagsInfo.setAttachments(attachmentsDatas);
			emailFlagsInfo.setCloudId(cloud.getId());
			emailFlagsInfo.setTo(Arrays.asList(cloud.getEmail()));
			emailFlagsInfo.setFrom("erik@filefuze.co");
			emailFlagsInfo.setFolder(mailFolder);
			emailFlagsInfo.setSubject("TestMail Generation For Test Data"+i);
			log.info("==Total mail created as of now=="+i+"==User Email=="+cloud.getEmail());
			emailFlagsInfo.setMessage(message+"-"+i);
			emailFlagsInfo.setHtmlMessage(emailFlagsInfo.getMessage());
			emailFlagsInfo.setSubject(emailFlagsInfo.getMessage()+"--"+i);
			EmailInfo info = mailServiceFactory.getConnectorService(cloud.getCloudName()).sendEmail(emailFlagsInfo);
			if(info!=null) {
				log.info("==Dummy mails creation in the text=="+cloud.getId());
			}
		}
	}

	private void createEvents() {
		CalenderFlags emailFlagsInfo = new CalenderFlags();
		emailFlagsInfo.setCloudId(cloud.getId());
		emailFlagsInfo.setAttendees(Arrays.asList(cloud.getAdminEmailId()));
		emailFlagsInfo.setCalendar(cloud.getEmail());
		emailFlagsInfo.setSubject("CalendarEvent ");
		emailFlagsInfo.setHtmlMessage(message);
		emailFlagsInfo.setTimeZone("UTC");
		for(int i=0;i<count;i++) {
			emailFlagsInfo.setStartTime(LocalDateTime.now().plusDays(i).toString());
			emailFlagsInfo.setEndTime(LocalDateTime.now().plusDays(i).plusMinutes(30).toString());
			log.info("==Total mail created as of now=="+i+"==User Email=="+cloud.getEmail());
			emailFlagsInfo.setMessage(message+"-"+i);
			emailFlagsInfo.setHtmlMessage(emailFlagsInfo.getMessage());
			emailFlagsInfo.setSubject(emailFlagsInfo.getMessage()+"--"+i);
			CalenderInfo info = mailServiceFactory.getConnectorService(cloud.getCloudName()).createCalenderEvent(emailFlagsInfo);
			if(info!=null) {
				log.info("==Dummy mails creation in the text=="+cloud.getId());
			}
		}
	}

}
