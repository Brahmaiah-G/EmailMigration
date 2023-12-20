package com.cloudfuze.mail.repo.entities;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Document
@TypeAlias(value="EmailFolderInfo")
public class EmailFolderInfo {
	@Id
	private String id;
	private String emailWorkSpaceId;
	private LocalDateTime createdTime;
	private LocalDateTime modifiedTime;
	private String userId;
	private PROCESS processStatus = PROCESS.NOT_STARTED;
	private String sourceId;
	private String destId;
	private String mailFolder;
	private long retryCount;
	private String errorDescription;
	private String threadBy=null;
	private String destFolderPath;
	private String fromCloudId;
	private String toCloudId;
	private long attachmentsSize;
	private long totalCount;
	private long totalSizeInBytes;
	private long unreadCount;
	private boolean preScan;
	private String sourceParent;
	private String destParent;
	private boolean created;
	private boolean picking;
	private String jobId;
	private String nextPageToken;
	private String fromCloud;
	private String toCloud;
	private String movedFolder;
	private String fromAdminCloudId;
	private boolean subFolder;
	private long order;
	private String env;
	private String originalMailFolder;
	private long priority=5;
	private String latestUpdatedId;
}
