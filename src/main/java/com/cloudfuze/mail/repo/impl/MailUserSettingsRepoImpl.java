package com.cloudfuze.mail.repo.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import com.cloudfuze.mail.dao.entities.EmailUserSettings;
import com.cloudfuze.mail.repo.MailUserSettingsRepository;

@Repository
public class MailUserSettingsRepoImpl implements MailUserSettingsRepository {

	@Autowired
	MongoTemplate mongoTemplate;
	
	@Override
	public void save(EmailUserSettings emailUserSettings) {
		mongoTemplate.save(emailUserSettings);
	}

	@Override
	public void save(List<EmailUserSettings> emailUserSettings) {
		mongoTemplate.save(emailUserSettings);
	}

	@Override
	public EmailUserSettings findByUser(String userId) {
		// TODO Auto-generated method stub
		return null;
	}

}
