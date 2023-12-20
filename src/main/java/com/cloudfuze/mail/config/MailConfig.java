package com.cloudfuze.mail.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cloudfuze.mail.dao.impl.AppMongoOpsManager;
import com.sendgrid.SendGrid;

@Configuration
public class MailConfig {	
	
	@Autowired
	private AppMongoOpsManager appMongoOpsManager;
	
	@Bean
	public SendGrid sendGridClientfromDb() {		
		return new SendGrid(appMongoOpsManager.fetchSendGridAPIKey());
	}
}