package com.cloudfuze.mail.repo.entities;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import com.cloudfuze.mail.repo.entities.Clouds.CLOUD_NAME;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Document
@TypeAlias(value = "EmailWorkSpace")
public class EmailWorkSpace {

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
	public enum PROCESS{
		STARTED,IN_PROGRESS,NOT_PROCESSED,PROCESSED,CONFLICT,PAUSE,PROCESSED_WITH_CONFLICTS,PROCESSED_WITH_CONFLICT_AND_PAUSE;
	}
	private long inprogressCount;
	private String userId;
	private LocalDateTime createdTime;
	private LocalDateTime modifiedTime;
	private String type;
	private String sourceLatestId;
	private String fromFolderId;
	private String toFolderId;
	private boolean deltaMigration;
	private boolean metadata;
	private int priority=10;
	private long notProcessedCount;
	private boolean preScan;
	private String jobId;
	private boolean picking;
	private long totalAttachmentsSize;
	private long retryCount;
	private long pauseCount;
	private String changeHours;
	private String fromAdminCloud;
	private String lastEmailWorkSpaceId;
	private String toAdminCloud;
	private boolean calendar = false;
	private boolean mapping=true;
	private String ent;
	private REPORT_STATUS reportStatus = REPORT_STATUS.NOT_PROCESSED;
	public enum REPORT_STATUS{
		STARTED,IN_PROGRESS,NOT_PROCESSED,PROCESSED,CONFLICT,PAUSE;
	}
	private boolean mailRules = true;
	private boolean settings;
	private boolean eventNotifications = false;
	private boolean createGroups = true;
	private boolean futureEvents = true;
	private boolean contacts;
	private boolean copy = true;
	private long totalFolders = 0;
}
