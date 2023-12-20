package com.cloudfuze.mail.repo;

import java.util.List;

import com.cloudfuze.mail.repo.entities.MailChanges;

public interface MailChangesRepository {

	public void save(MailChanges changes);
	public void save(List<MailChanges>chanegs);
	public List<MailChanges> find(String mailChangeId);
	public List<MailChanges>findByWorkSpace(String emailWorkSpaceId);
	List<MailChanges> findByWorkSpaceParent(String emailWorkSpaceId, String sourceParent, boolean folder,
			boolean events);
	List<MailChanges> findByWorkSpaceParent(String emailWorkSpaceId, String sourceParent, boolean events);
	List<MailChanges> findByWorkSpaceParent(String emailWorkSpaceId, boolean folder, boolean events);
	
}
