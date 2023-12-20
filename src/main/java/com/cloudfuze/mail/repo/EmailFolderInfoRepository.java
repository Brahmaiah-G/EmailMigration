package com.cloudfuze.mail.repo;

import java.util.List;

import com.cloudfuze.mail.repo.entities.EmailFolderInfo;
import com.cloudfuze.mail.repo.entities.PROCESS;

public interface EmailFolderInfoRepository {

	public void saveAll(List<EmailFolderInfo> folderInfos);
	public EmailFolderInfo findOne(String id);
	public List<EmailFolderInfo> findByWorkSpaceId(String emailWorkSpaceId);
	public List<EmailFolderInfo> findByWorkSpaceIdUserId(String emailWorkSpaceId,String userId);
	EmailFolderInfo save(EmailFolderInfo folderInfo);
	List<EmailFolderInfo> findByProcessStatus(String emailWorkSpaceId,PROCESS processStatus);
	List<EmailFolderInfo> findByProcessStatus(String emailWorkSpaceId, List<PROCESS> processStatus);
	List<EmailFolderInfo> findConflictFolders(String workSpaceId, long retryCount);
	EmailFolderInfo getParentFolderInfo(String workSpaceId, String sourceParent);
	void updateEmailInfoForDestChildFolder(String emailWorkSpaceId, String sourceThreadId, String destParent);
}
