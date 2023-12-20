package com.cloudfuze.mail.repo;

import java.util.List;

import com.cloudfuze.mail.dao.entities.EmailUserSettings;

public interface MailUserSettingsRepository {

	void save(EmailUserSettings emailUserSettings);
	void save(List<EmailUserSettings> emailUserSettings);
	EmailUserSettings findByUser(String userId);
}
