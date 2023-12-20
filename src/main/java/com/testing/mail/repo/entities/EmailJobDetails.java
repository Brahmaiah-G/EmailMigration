package com.testing.mail.repo.entities;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import com.testing.mail.repo.entities.Clouds.CLOUD_NAME;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Document
@TypeAlias(value = "EmailJobDetails")
public class EmailJobDetails {

	@Id
	private String id;
	private String fromCloudId;
	private String toCloudId;
	private CLOUD_NAME fromCloud;
	private CLOUD_NAME toCloud;
	private String fromMailId;
	private String toMailId;
	private String ownerEmailId;
	private long totalCount;
	private long conflictCount;
	private String errorDescription;
	private long processedCount;
	private PROCESS processStatus;
	private String jobName;
	public enum PROCESS{
		STARTED,IN_PROGRESS,NOT_PROCESSED,PROCESSED,CONFLICT,PAUSE,PROCESSED_WITH_CONFLICTS,PROCESSED_WITH_CONFLICT_AND_PAUSE;
		}
	private long inprogressCount;
	private String userId;
	private LocalDateTime createdTime;
	private LocalDateTime modifiedTime;
	private String type;
	private String fromFolderId;
	private String toFolderId;
	private boolean deltaMigration;
	private int priority=10;
	private long notProcessedCount;
	private boolean preScan;
	private List<String>workspaceId;
	private boolean calFiltered = false;
	private boolean mailsFiltered = false;
	private boolean calendars;
	private REPORT_STATUS reportStatus = REPORT_STATUS.NOT_PROCESSED;
	public enum REPORT_STATUS{
		STARTED,IN_PROGRESS,NOT_PROCESSED,PROCESSED,CONFLICT,PAUSE;
	}
}
