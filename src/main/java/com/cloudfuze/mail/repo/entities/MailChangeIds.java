package com.cloudfuze.mail.repo.entities;

import java.time.LocalDateTime;
import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Document
@TypeAlias(value="MailChangeIds")
public class MailChangeIds {
	@Id
	private String id;
	private String emailWorkSpaceId;
	private LocalDateTime createdTime;
	private LocalDateTime modifiedTime;
	private String userId;
	private PROCESS processStatus = PROCESS.NOT_PROCESSED;
	public enum PROCESS{
		IN_PROGRESS,PROCESSED,CONFLICT,RETRY,PAUSE,NOT_PROCESSED;
	}
	private String sourceId;
	private boolean folder;
	private boolean metadata;
	private String mailFolder;
	private long retryCount;
	private String destFolderPath;
	private String errorDescription;
	private String threadBy=null;
	private String fromCloudId;
	private String toCloudId;
	private String latestUpdatedId;
	private String jobId;
	private String updatedMetadata;
	private String nextPageToken;
	private Date nextPickTime;
	private long count;
	private boolean calendar;
	private boolean primaryCalendar;
	private boolean picking;
}
