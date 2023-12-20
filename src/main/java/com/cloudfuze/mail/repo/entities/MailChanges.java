package com.cloudfuze.mail.repo.entities;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;


/**
 *Changes For <b>Delta</b> Migration
 *<p></p>
 *Calling from :{@link MailChangeIds}
*/
@Setter
@Getter
@Document
@TypeAlias(value="MailChanges")
public class MailChanges {

	@Id
	private String id;
	private String emailWorkSpaceId;
	private LocalDateTime createdTime;
	private LocalDateTime modifiedTime;
	private LocalDateTime mailCreatedTime;
	private LocalDateTime mailModifiedTime;
	private String fromMail;
	private List<String> toMail;
	private List<String> replyTo;
	private List<String> cc;
	private boolean created;
	private List<String> bcc;
	private boolean attachMents;
	private String userId;
	private PROCESS processStatus = PROCESS.NOT_PROCESSED;
	public enum PROCESS{
		IN_PROGRESS,PROCESSED,CONFLICT,RETRY,PAUSE,NOT_PROCESSED,THREAD_NOT_PROCESSED,DUPLICATE_PROCESSED;
	}
	private String sourceId;
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
	private String fromCloudId;
	private String toCloudId;
	private long attachmentsSize;
	private String sourceParent;
	private String mailChangeId;
	private String jobId;
	private boolean draft;
	private String originalFrom;
	private boolean read;
	private boolean deleted;
	private boolean flagged;
	private String fromName;
	private boolean events;
	private List<String>categories;
	private String startTime;
	private String endTime;
	private String timeZone;
	private String endTimeZone;
	private String recurrenceType;
	private String range ;//recurence range append like type:start: and end :timeZone
	private List<String> location;
	private String parent;
	private List<String>attachmentIds;
	private String movedFolder;
	private String importance;
	private boolean subFolder;
	private boolean primary;
	private long order;
	private String remainders;
}

