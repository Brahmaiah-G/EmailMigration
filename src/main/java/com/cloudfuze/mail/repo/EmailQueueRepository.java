package com.cloudfuze.mail.repo;

import java.util.List;

import com.cloudfuze.mail.repo.entities.CalendarMoveQueue;
import com.cloudfuze.mail.repo.entities.CalendarPickingQueue;
import com.cloudfuze.mail.repo.entities.EmailMetadataQueue;
import com.cloudfuze.mail.repo.entities.EmailMoveQueue;
import com.cloudfuze.mail.repo.entities.EmailPickingQueue;
import com.cloudfuze.mail.repo.entities.EmailPurgeQueue;
import com.cloudfuze.mail.repo.entities.PROCESS;

public interface EmailQueueRepository {

	public void save(EmailPickingQueue emailQueue);
	public void saveAll(List<EmailPickingQueue>emailQueues);
	public EmailPickingQueue findOne(String id);
	public EmailPickingQueue findByWorkSpace(String emailWorkSpaceId);
	List<EmailPickingQueue> findPickingProcessStatus(PROCESS processStatus);
	void saveQueue(EmailMoveQueue emailQueue);
	void saveMovequeues(List<EmailMoveQueue> emailQueues);
	EmailMoveQueue findMoveQueue(String id);
	EmailMoveQueue findMoveQueueByWorkSpace(String emailWorkSpaceId);
	List<EmailMoveQueue> findMoveQueueByProcessStatus(PROCESS processStatus);
	List<EmailMoveQueue> findMoveQueueByProcessStatusWithPazination(List<PROCESS> processStatus, int pageSize,
			int pageNum);
	List<EmailPickingQueue> findPickingByProcessStatusWithPazination(List<PROCESS> processStatus, int pageSize,
			int pageNum);
	List<EmailMoveQueue> findMoveQueueByProcessStatusWithPazination(List<PROCESS> processStatus, int pageSize,
			int pageNum, String cloudName);
	long findPickingByProcessStatusWithPazination(List<PROCESS> processStatus, String userId);
	List<EmailPurgeQueue> findPurgeQueueByProcessStatusWithPazination(List<PROCESS> processStatus, int pageSize,
			int pageNum);
	void saveMetadataQueue(EmailMetadataQueue emailQueue);
	void savePurgeQueue(EmailPurgeQueue emailQueue);
	void saveCalendarQueue(CalendarMoveQueue emailQueue);
	void saveCalendarPickingQueue(CalendarPickingQueue emailQueue);
	CalendarPickingQueue findCalendarPickingQueueByWorkSpace(String emailWorkSpaceId);
	CalendarMoveQueue findCalendarMoveQueueByWorkSpace(String emailWorkSpaceId);
	CalendarPickingQueue findCalendarPickingQueueByJobId(String jobId);
	List<CalendarPickingQueue> findCalendarPickingByProcessStatusWithPazination(List<PROCESS> processStatus,
			int pageSize, int pageNum);
	List<CalendarMoveQueue> findCalendarMoveQueueByProcessStatusWithPazination(List<PROCESS> processStatus,
			int pageSize, int pageNum);
	List<CalendarPickingQueue> findCalendarsPickingQueueByJob(String jobId);
}
