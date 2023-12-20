package com.testing.mail.repo;

import java.util.List;

import com.testing.mail.repo.entities.EmailWorkSpace;

public interface WorkSpaceRepository {

	public EmailWorkSpace save(EmailWorkSpace emailWorkSpace);
	public void save(List<EmailWorkSpace> emailWorkSpace);
	public EmailWorkSpace findOne(String id);
	public List<EmailWorkSpace> getWorkspaces(String userId);
	List<EmailWorkSpace> getWorkspaces(int limit, int skip);
	EmailWorkSpace getWorkSpaceBasedOnPaths(String userId, String fromMailId, String toMailId, String fromCloudName,
			String toCloudName, String ownerEmailId);
	List<EmailWorkSpace> getWorkspacesByJob(String jobId, int limit, int skip);
	EmailWorkSpace getPremigrationWorkSpace(String fromEmail, String cloud);
	void removeOne(String workSpaceId);
}
