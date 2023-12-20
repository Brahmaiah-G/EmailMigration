package com.testing.mail.repo;

import java.util.List;

import com.testing.mail.repo.entities.CalenderInfo;
import com.testing.mail.repo.entities.EmailWorkSpace;
import com.testing.mail.repo.entities.EventInstacesInfo;
import com.testing.mail.repo.entities.EventsInfo;
import com.testing.mail.repo.entities.PROCESS;

public interface CalendarInfoRepo {

	public void save(List<EventsInfo>calenderInfos);
	long getInprogressFolders(String emailWorkSpaceId);
	long getInprogressCount(String emailWorkSpaceId, boolean attachments);
	void findAndUpdateConflictsByWorkSpace(String workSpaceId, long retryCount);
	void findAndUpdateByWorkSpace(String workSpaceId, long retryCount, String processStatus, boolean folder,
			String errorDescription);
	EmailWorkSpace getAggregartedResult(String workSpaceId);
	public long getMetaDataInprogressCount(String emailWorkSpaceId);
	List<EventsInfo> findByWorkSpace(String workSpaceId, int limit, int skip, boolean folder);
	EventsInfo findBySourceId(String jobId, String userId, String sourceId);
	void removeCalendars(String workSpaceId);
	long checkExistingCalendar(String calendarId, String userId, String jobId);
	List<EventsInfo> getUnDeletedMails(String emailWorkSpaceId, int limit, int skip);
	CalenderInfo getParentCalendarInfo(String workSpaceId, String sourceParent);
	List<EventsInfo> getCalendarEvents(String userId);
	void save(EventsInfo calenderInfo);
	List<CalenderInfo> findByWorkSpaceCalendars(String workSpaceId, int limit, int skip, boolean folder);
	List<CalenderInfo> findByProcessStatus(String emailWorkSpaceId, List<PROCESS> processStatus);
	void saveInstances(List<EventInstacesInfo> instances);
	List<EventInstacesInfo> getInstances(String emailWorkSpaceId, String recurenceId);
	EventInstacesInfo getParentInstances(String emailWorkSpaceId, String recurenceId);
}
