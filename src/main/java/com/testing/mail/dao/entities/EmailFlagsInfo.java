package com.testing.mail.dao.entities;

import java.util.List;

import com.testing.mail.connectors.microsoft.data.AttachmentsData;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class EmailFlagsInfo {

	private String cloudId;
	private String from;
	private List<String> to;
	private List<String> bcc;
	private List<String>cc;
	private List<AttachmentsData>attachments;
	private String subject;
	private String message;
	private String createdTime;
	private boolean htmlContent;
	private boolean metaData;
	private String htmlMessage;
	private String inviteEmail;
	private List<String>labels;
	private List<String>replyTo;
	private String threadId;
	private String id;
	private boolean flagged;
	private boolean draft;
	private long totalCount;
	private long sizeInBytes;
	private String parentFolderId;
	private String sentTime;
	private boolean hadAttachments;
	private String name;
	private String destParent;
	private long unreadCount;
	private String userId;
	private String emailId;
	private String adminMemberId;
	private String folder;
	private boolean mailFolder;
	private String bodyPreview;
	private String importance;
	private String nextPageToken;
	private boolean externalUser;
	private boolean read;
	private boolean deleted;
	private String destId;
	private String fromName;
	private boolean largeFile;
	private boolean events;
	private String startTime;
	private String endTime;
	private String timeZone;
	private String endTimeZone;
	private String onlineMeetingUrl; 
	private String recurrenceType;
	private String range;
	private List<String> location;
	private int remainderTime;
	private String color;
	private boolean thread;
	private boolean stopCalendarSource = false;
	private boolean customLabel = false;
	private boolean gCombo;
	private boolean subFolder = false;
	private long order;
	private String convIndex;
	private String remainders;
	private boolean groupMail;
	private long pickingCount;
	private boolean draftCreated;
	private boolean conflict;
	private boolean copy;
	private String previousFrom;
	private boolean deltaThread;
}
