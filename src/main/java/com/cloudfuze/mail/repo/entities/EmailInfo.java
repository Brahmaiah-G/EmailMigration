package com.cloudfuze.mail.repo.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Document
@TypeAlias(value="EmailInfo")
public class EmailInfo {

	@Id
	private String id;
	private String emailWorkSpaceId;
	private LocalDateTime createdTime;
	private LocalDateTime modifiedTime;
	private String mailCreatedTime;
	private String mailModifiedTime;
	private String fromMail;
	private List<String> toMail;
	private List<String> replyTo;
	private List<String> cc;
	private List<String> bcc;
	private boolean attachMents;
	private String userId;
	private PROCESS processStatus = PROCESS.NOT_STARTED;
	public enum PROCESS{
		PICKING,IN_PROGRESS,PROCESSED,CONFLICT,NOT_STARTED,RETRY,PAUSE,NOT_PROCESSED,METADATA_STARTED,METADATA_INPROGRESS,METADATA_CONFLICT,THREAD_NOT_PROCESSED,PARENT_NOT_PROCESSED
		,DRAFT_CREATED,DRAFT_NOTPROCESSED,DRAFT_CREATION_INPROGRESS,DRAFT_MIGRATION_IN_PROGRESS,DRAFT_CREATION_CONFLICT,PRESCAN_MAIL, DUPLICATE_PROCESSED;
	}
	private String sourceId;
	private String destId;
	private String threadId;
	private boolean folder;
	private boolean metadata;
	private String mailFolder;
	private long retryCount;
	private String errorDescription;
	private String threadBy=null;
	private String subject;
	private boolean htmlContent;
	private String bodyPreview;
	private String htmlBodyContent;
	private String destThreadId;
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
	private String updatedMetadata;
	private boolean draft;
	private String nextPageToken;
	private String originalFrom;
	private List<String> attachmentIds = new ArrayList<>();
	private boolean read;
	private boolean deleted;
	private boolean flagged;
	private String fromName;
	private String originalMailFolder;
	private String importance;
	private boolean largeFile;
	private boolean adminDeleted=false;
	private boolean color;
	private String fromCloud;
	private String toCloud;
	private String adminStatus;
	private String movedFolder;
	private String fromAdminCloudId;
	private boolean subFolder;
	private String timeZone;
	private long order;
	private String env;
	private long priority=5;
	private String convIndex;
	private String extraField;
	private boolean groupEmail;
	private boolean moved;
	private String draftBatchId;
	private String migBatchId;
	private String previousFrom;
	private boolean deltaThread;
	
}
