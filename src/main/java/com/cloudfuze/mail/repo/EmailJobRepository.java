package com.cloudfuze.mail.repo;

import java.util.List;

import com.cloudfuze.mail.repo.entities.EmailJobDetails;

public interface EmailJobRepository {

	public EmailJobDetails save(EmailJobDetails emailWorkSpace);
	public void save(List<EmailJobDetails> emailWorkSpace);
	public EmailJobDetails findOne(String id);
	public List<EmailJobDetails> getEmailJobDetails(String userId);
	List<EmailJobDetails> getEmailJobDEtails(String userId, int limit, int skip);
	List<EmailJobDetails> getInprogressJobDetails(int limit, int skip);
}
