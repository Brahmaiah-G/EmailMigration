package com.cloudfuze.mail.repo;

import java.util.List;

import com.cloudfuze.mail.repo.entities.MailChangeIds;

public interface MailChangeIdRepository {

	public MailChangeIds save(MailChangeIds changeIds);
	public void save(List<MailChangeIds> changeIds);
	public MailChangeIds findOne(String id);
	public void delete(String id);
	public List<MailChangeIds>find(String emailWorkSpaceId);
	long getInprogressCount(String emailWorkSpaceId);
	long getNot_StartedCount(String emailWorkSpaceId);
	void findAndUpdateMailChangeId(String workSpaceId, String newEmailWorkSpaceId);
}
