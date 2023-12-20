package com.testing.mail.repo;

import java.util.List;

import com.testing.mail.dao.entities.EMailRules;
import com.testing.mail.repo.entities.EmailInfo;
import com.testing.mail.repo.entities.EmailWorkSpace;
import com.testing.mail.repo.entities.GlobalReports;
import com.testing.mail.repo.entities.ThreadControl;
import com.testing.mail.repo.entities.EmailInfo.PROCESS;

public interface EmailInfoRepository {

	public EmailInfo save(EmailInfo emailInfo);
	public void save(List<EmailInfo> emailInfos);
	public EmailInfo findOne(String id);
	public List<EmailInfo> findByWorkSpace(String workSpaceId);
	public ThreadControl getThreadControl();
	public long countByWorkSpace(String workSpaceId);
	EmailWorkSpace getAggregartedResult(String workSpaceId);
	void findAndUpdateConflictsByWorkSpace(String workSpaceId, long retryCount);
	List<EmailInfo> findConflictFolders(String workSpaceId, long retryCount);
	void findAndUpdateByWorkSpace(String workSpaceId, long retryCount, String processStatus, boolean folder,
			String errorDescription);
	long getInprogressFolders(String emailWorkSpaceId);
	long getInprogressCount(String emailWorkSpaceId, boolean attachments);
	List<EmailInfo> findByWorkSpace(String workSpaceId, int limit, int skip, boolean folder);
	List<EmailInfo> findByWorkSpaceAndProcessStatus(String workSpaceId, int limit, int skip, String processStatus,
			boolean folder);
	List<EmailInfo> findByWorkSpaceWithPagination(String workSpaceId);
	long countConlfictsByFolder(String moveWorkSpaceId, String folder);
	List<EmailInfo> findByWorkSpace(String workSpaceId, int limit, int skip);
	long countTotalByFolder(String moveWorkSpaceId, String folder);
	EmailWorkSpace getAggregartedResultForFolder(String workSpaceId, String folder);
	EmailWorkSpace getAggregartedResultBasedOnJob(String workSpaceId);
	void saveMailBoxRules(List<EMailRules> rules);
	void removeEmails(String workSpaceId);
	EmailInfo findBySourceId(String jobId, String userId, String sourceId);
	EmailInfo getAggregartedResultForPremigration(String userId, String fromCloudId);
	EmailInfo getFolderBasedOnSourceId(String emailWorkSpaceID, String sourceId);
	EmailInfo getFolderBasedOnMailFolder(String emailWorkSpaceID, String sourceId);
	void findAndUpdateConflictFoldersByWorkSpace(String workSpaceId, long retryCount);
	void findAndUpdateByWorkSpaceForMailChangeIds(String workSpaceId, long retryCount, String processStatus,
			boolean folder, String errorDescription);
	void updateEmailInfoForDestThreadId(String emailWorkSpaceId, String sourceThreadId, String destThreadId);
	void saveMailBoxRule(EMailRules rule);
	List<EmailInfo> getUnDeletedMails(String emailWorkSpaceId, int limit, int skip);
	GlobalReports getGlobalReportConfigs();
	EmailInfo getDestThreadIdBasedOnSourceId(String moveWorkSpaceId, String sourceThreadId);
	void getNotProcessedEmailsByDuplicateSourceID(String workSpaceId);
	EmailInfo findByThreadId(String jobId, String userId, String threadId);
	EmailInfo getParentFolderInfo(String workSpaceId, String sourceParent);
	long countByProcessStatus(String processStatus);
	void updateEmailInfosByProcessStatus(String sourceProcessStatus, String destProcessStatus, int count);
	List<EMailRules> findMailBoxRuleBySourceId(String userId, String emailWorkSpaceId, String sourceId);
	void updateEmailInfoForDestChildFolder(String emailWorkSpaceId, String sourceThreadId, String destParent);
	long getEmailInfosBasedOnProcessStatus(String emailWorkSpaceId, long mailFolder, PROCESS processStatus,
			boolean folder);
	void findAndUpdateByWorkSpace(String workSpaceId, String processStatus, String errorDescription, List<String> ids,
			long retryCount);
	void findAndUpdateConflictsByWorkSpaceOutlookDraftCreation(String workSpaceId, long retryCount);
	void findAndUpdateConflictsByWorkSpaceOutlookDraftMigration(String workSpaceId, long retryCount);
	long countByProcessStatusAndError(List<String> processStatus, String errorDescription);
	void updateEmailInfoForDestThreadId(String emailWorkSpaceId, String sourceThreadId, String destThreadId, long order,
			String destConvIndex, List<String> env);
	void updateEmailInfoForDestThreadId(String emailWorkSpaceId, String sourceThreadId, String destThreadId, long order,
			String destConvIndex);
	EmailInfo checkEmailInfosBasedOnIds(String emailWorkSpaceId, String sourceId, boolean copy);
}
