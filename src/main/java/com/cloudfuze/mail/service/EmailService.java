package com.cloudfuze.mail.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.cloudfuze.mail.emailUtility.EmailUtillity;
import com.cloudfuze.mail.repo.entities.Clouds;
import com.cloudfuze.mail.repo.entities.EmailJobDetails;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Setter@Getter@NoArgsConstructor@Slf4j
@Service
public class EmailService {

	@Autowired
	private SendGrid sendGrid;

	@Autowired 
	EmailUtillity emailtUtil;


	public void registrationSuccessfulMail(String email,String userName,String ent) {

		log.info("Sending Registration Email to  EmailId: {}.",email);
		log.info("Sending Registration Email with  Username: {}.", userName);        

		Mail mail = emailtUtil.registrationSuccessfulMail(email, userName,ent);
		Personalization personalization = new Personalization();
		Email bcc = new Email();
		bcc.setEmail("support@cloudfuze.com");
		personalization.addBcc(bcc);
		mail.addPersonalization(personalization);
		Request request = new Request();
		try {
			//configure email request
			request.setMethod(Method.POST);
			request.setEndpoint("mail/send");
			request.setBody(mail.build());
			//get email generation response
			Response response = sendGrid.api(request);
			//validate if success
			boolean success = HttpStatus.valueOf(response.getStatusCode()).is2xxSuccessful();

			log.info("Registration Email Send  {}",success?"Success":"Failed" );
		} 
		catch (IOException ex) {
			log.error(ex.getMessage());			
		}
	}


	public void sendReport(String email,String userName,File file,List<String>cc) {

		log.info("Sending Report Email to  EmailId: {}.",email);
		log.info("Sending Report Email with  Username: {}.", userName);        

		Mail mail = emailtUtil.generateReport(email, userName);
		Personalization personalization = mail.getPersonalization().get(0);
		Attachments attachments;
		try {
			attachments = new Attachments();
			Base64 x = new Base64();
			String fileData = x.encodeAsString(IOUtils.toByteArray(new FileInputStream(file)));
			attachments.setContent(fileData);
			attachments.setDisposition("attachment");
			attachments.setFilename("Migration Report.csv");
			attachments.setType("text/csv");
			mail.addAttachments(attachments);
			mail.setSubject("Your CloudFuze Email Migration Report");
			if(cc!=null && !cc.isEmpty()) {
				for(String ccEmail : cc) {
					Email bcc = new Email();
					bcc.setEmail(ccEmail);
					personalization.addBcc(bcc);
				}
			}
			log.info("====File appends=="+file.length()+"==email=="+email);
		} catch (Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
		}
		Request request = new Request();
		try {
			//configure email request
			request.setMethod(Method.POST);
			request.setEndpoint("mail/send");
			request.setBody(mail.build());
			//get email generation response
			Response response = sendGrid.api(request);
			//validate if success
			boolean success = HttpStatus.valueOf(response.getStatusCode()).is2xxSuccessful();

			log.info("Registration Email Send  {}",success?"Success":"Failed" );
		} 
		catch (IOException ex) {
			log.error(ex.getMessage());			
		}
	}
	
	
	
	public void sendTimeReport(String email,String userName,File file,List<String>cc,EmailJobDetails emailWorkSpace) {

		log.info("Sending Report Email to  EmailId: {}.",email);

		Mail mail = emailtUtil.generateTimeReport(email, userName, emailWorkSpace);
		Personalization personalization = mail.getPersonalization().get(0);
		Attachments attachments;
		try {
			attachments = new Attachments();
			Base64 x = new Base64();
			String fileData = x.encodeAsString(IOUtils.toByteArray(new FileInputStream(file)));
			attachments.setContent(fileData);
			attachments.setDisposition("attachment");
			attachments.setFilename("MigrationTimeReport.csv");
			attachments.setType("text/csv");
			mail.addAttachments(attachments);
			mail.setSubject("Your CloudFuze Email Migration Report Job Wise (internal)");
			if(cc!=null && !cc.isEmpty()) {
				for(String ccEmail : cc) {
					Email bcc = new Email();
					bcc.setEmail(ccEmail);
					personalization.addBcc(bcc);
				}
			}
			log.info("====File appends=="+file.length()+"==email=="+email);
		} catch (Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
		}
		Request request = new Request();
		try {
			//configure email request
			request.setMethod(Method.POST);
			request.setEndpoint("mail/send");
			request.setBody(mail.build());
			//get email generation response
			Response response = sendGrid.api(request);
			//validate if success
			boolean success = HttpStatus.valueOf(response.getStatusCode()).is2xxSuccessful();

			log.info("Time Report Email Send  {}",success?"Success":"Failed" );
		} 
		catch (IOException ex) {
			log.error(ex.getMessage());			
		}
	}





	public Boolean passwordResetMail(String email, String name,String token) {

		Boolean success = false;

		log.info("Sending Password Reset Email to  EmailId: {}.",email);
		log.info("Sending Password Reset Email with  Username: {}.", name);   

		Mail mail = emailtUtil.passwordResetMail(email, name, token);

		Request request = new Request();
		try {
			//configure email request
			request.setMethod(Method.POST);
			request.setEndpoint("mail/send");
			request.setBody(mail.build());
			//get email generation response
			Response response = sendGrid.api(request);
			//validate if success
			success = HttpStatus.valueOf(response.getStatusCode()).is2xxSuccessful();

			log.info("Password Reset Email Send  {}",Boolean.TRUE.equals(success)?"Success":"Failed" );
		} 
		catch (IOException ex) {
			log.error(ex.getMessage());
			success = false;
		}

		return success;				
	}

	public Boolean licenseRenewalNotification(Clouds sub) {


		Boolean success = false;

		log.info("Sending License renewal  Email to  EmailId: {}.",sub.getAdminEmailId());

		Mail mail = emailtUtil.licenseRenewalNotification(sub);

		Request request = new Request();
		try {
			//configure email request
			request.setMethod(Method.POST);
			request.setEndpoint("mail/send");
			request.setBody(mail.build());
			//get email generation response
			Response response = sendGrid.api(request);
			//validate if success
			success = HttpStatus.valueOf(response.getStatusCode()).is2xxSuccessful();

			log.info("Password Reset Email Send  {}",Boolean.TRUE.equals(success)?"Success":"Failed" );
		} 
		catch (IOException ex) {
			log.error(ex.getMessage());
			success = false;
		}

		return success;				

	}

}