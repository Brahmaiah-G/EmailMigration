package com.testing.mail.emailUtility;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.testing.mail.repo.entities.Clouds;
import com.testing.mail.repo.entities.EmailJobDetails;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter@Getter
@Service
public class EmailUtillity {
	
	
	@Value("${spring.sendgrid.toMail}")
	private String fromMail;

	private Email fromEmail;

	@Autowired
	private EmailContentCreatorUtil ContentUtil;

	@PostConstruct
	public void constructFreomEmail() {
		this.fromEmail = new Email(fromMail);
	}

	public Mail registrationSuccessfulMail(String email,String userName,String ent) {

		Email toEmail= new Email(email, userName);

		String text = ContentUtil.constructRegistrationSuccessBody(userName,ent);

		Content content = new Content(MediaType.TEXT_HTML_VALUE, text);

		log.warn("--Registration is successFul--"+text);
		return  new Mail(fromEmail, EmailContentCreatorUtil.RESGISTRATION_SUCCESS_SUBJECT, toEmail, content);

	}



	public Mail generateReport(String email,String userName) {

		Email toEmail= new Email(email, userName);

		String text = ContentUtil.generateReport(userName);

		Content content = new Content(MediaType.TEXT_HTML_VALUE, text);

		log.warn("--Registration is successFul--"+text);
		return  new Mail(fromEmail, EmailContentCreatorUtil.EMAIL_MIGRATION_REPORT_SUBJECT, toEmail, content);

	}
	
	
	public Mail generateTimeReport(String email,String userName,EmailJobDetails emailWorkSpace) {

		Email toEmail= new Email(email, userName);

		String text = ContentUtil.generateTimeReport(emailWorkSpace);

		Content content = new Content(MediaType.TEXT_HTML_VALUE, text);

		log.warn("--Registration is successFul--"+text);
		return  new Mail(fromEmail, EmailContentCreatorUtil.EMAIL_MIGRATION_TIME_REPORT_SUBJECT, toEmail, content);

	}

	public Mail passwordResetMail(String email,String userName,String token) {

		Email toEmail= new Email(email, userName);

		String text = ContentUtil.constructPasswordResetMailBody(email, userName, token);

		Content content = new Content(MediaType.TEXT_HTML_VALUE, text);
		log.warn("--Password reset is successFul--"+text);
		return  new Mail(fromEmail, EmailContentCreatorUtil.PASSWORD_RESET_SUBJECT, toEmail, content);

	}

	public Mail licenseRenewalNotification(Clouds sub) {

		Email toEmail= new Email(sub.getAdminEmailId(), sub.getDomain());

		String text = ContentUtil.constructLicenseRenwalNotifyMailBody(sub);
		Content content = new Content(MediaType.TEXT_HTML_VALUE, text);
		log.warn("--license Renewal notification  is successFul--"+text);
		return  new Mail(fromEmail, EmailContentCreatorUtil.LICENSE_NOTIFICATION, toEmail, content);


	}

}
